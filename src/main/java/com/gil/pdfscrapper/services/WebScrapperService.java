package com.gil.pdfscrapper.services;


import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.gil.pdfscrapper.AppConfigs;
import com.gil.pdfscrapper.AppConstants;
import com.gil.pdfscrapper.models.CheckpointFileDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class WebScrapperService {
    private final AppConfigs appConfigs;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService requestsExecutorService;
    private final ConcurrentLinkedQueue<String> successfulBatchUrls = new ConcurrentLinkedQueue<>();

    public WebScrapperService(AppConfigs appConfigs, RestClient restClient, ObjectMapper objectMapper, @Qualifier(AppConstants.REQUESTS_EXECUTORS_BEAN_QUALIFIER) ExecutorService requestsExecutorService) {
        this.appConfigs = appConfigs;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.requestsExecutorService = requestsExecutorService;
    }

    public void run(LocalDate filterDate) throws IOException, InterruptedException {
        Path successFile = getSuccessFile();
        CheckpointFileDto checkpointFileDto = loadCheckpoint();
        final int currentCheckpoint = checkpointFileDto.getCheckpoint();
        final int batchSize = appConfigs.BATCH_SIZE;

        for (int batchStart = currentCheckpoint + 1; batchStart <= appConfigs.MAX_URL_NUMBER; batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize - 1, appConfigs.MAX_URL_NUMBER);

            processBatch(batchEnd, batchStart, filterDate);

            updateSuccessFile(successfulBatchUrls, successFile);
            successfulBatchUrls.clear(); //* Clear the queue for the next batch

            updateCheckpointFile(batchEnd);
            log.info("Processed up to URL number {}", batchEnd);
        }


    }

    private void processBatch(int batchEnd, int batchStart, LocalDate filterDate) throws MalformedURLException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(batchEnd - batchStart + 1);

        for (int i = batchStart; i <= batchEnd; i++) {
            final String endpoint = String.format("%06d", i) + ".pdf";

            final String url = URI.create(String.join("/", appConfigs.BASE_URL, endpoint)).toURL().toString();

            requestsExecutorService.submit(() -> {
                try {
                    if (checkIfUrlIsValid(url, filterDate)) {
                        successfulBatchUrls.add(url);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Wait for the batch to complete
    }

    private void updateSuccessFile(ConcurrentLinkedQueue<String> successfulUrls, Path successFile) throws IOException {
        //* Append to the file, instead of overwriting
        Files.write(successFile, successfulUrls.stream().toList(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }


    private Path getSuccessFile() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.SUCCESS_FILE_DATETIME_FORMAT);
        String formattedDateTime = now.format(formatter);

        String successFileName = "results-%s.txt".formatted(formattedDateTime);

        Path successFile = Paths.get(appConfigs.SUCCESS_FILE_FOLDER, successFileName);


        try {
            //* Ensure the parent directory exists and create the file if it does not exist.
            FileUtils.touch(successFile.toFile());
        } catch (IOException e) {
            log.error("Could not create success file at {}", successFile, e);
            throw e;
        }

        return successFile;

    }

    private boolean checkIfUrlIsValid(String url, LocalDate filterDate) {
        try {
            ResponseEntity<Void> responseEntity = restClient.head().uri(url).retrieve().toBodilessEntity();
            boolean isSuccessful = responseEntity.getStatusCode().is2xxSuccessful();
            if (isSuccessful) {
                String lastModifiedDateStr = responseEntity.getHeaders().getFirst("Last-Modified");
                if (lastModifiedDateStr != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                    try {
                        LocalDate lastModifiedDate = LocalDate.parse(lastModifiedDateStr, formatter);
                        if (!lastModifiedDate.isBefore(filterDate)) {
                            log.info("URL {} is valid and meets the date criteria.", url);
                            return true;
                        }
                        else{
                            log.info("URL {} exists but does not meet the date criteria.", url);
                            return false;
                        }
                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse Last-Modified date: {}", lastModifiedDateStr, e);
                        return false;
                    }
                }
                else{
                    log.warn("URL {} exists but does not have a date field", url);
                    return false;
                }
            }

            log.info("URL {} failed.", url);
            return false;
        } catch (Exception e) {
            log.info("URL {} failed.", url);
            return false;
        }
    }

    private CheckpointFileDto loadCheckpoint() {
        Path path = Paths.get(appConfigs.CHECKPOINT_FILE);
        log.info("Loading checkpoint file from {}", path);
        CheckpointFileDto checkpointFileDto;
        try {
            FileTime fileTime = Files.getLastModifiedTime(path);
            LocalDateTime lastModifiedTime = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            checkpointFileDto = objectMapper.readValue(path.toFile(), CheckpointFileDto.class);
            log.info("Loaded checkpoint={}, created at {}", checkpointFileDto.getCheckpoint(), lastModifiedTime.format(formatter));
        } catch (IOException e) {
            log.warn("Could not load checkpoint file. Starting from checkpoint=0");
            return new CheckpointFileDto(0);
        }

        return checkpointFileDto;

    }

    private void updateCheckpointFile(int newCheckpoint) throws IOException {
        CheckpointFileDto checkpointFileDto = new CheckpointFileDto(newCheckpoint);
        Path checkpointFilePath = Paths.get(appConfigs.CHECKPOINT_FILE);
        //* Ensure directory exists
        Path parentDir = checkpointFilePath.getParent();
        if (parentDir != null && Files.notExists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(checkpointFilePath.toFile(), checkpointFileDto);
//
//        String checkpointData = objectMapper.writeValueAsString(checkpointFileDto);
//        Files.write(checkpointFilePath, checkpointData.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

}
