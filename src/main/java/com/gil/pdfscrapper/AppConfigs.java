package com.gil.pdfscrapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ToString
public class AppConfigs
{
    public final String BASE_URL;
    public final String CHECKPOINT_FILE;
    public final String SUCCESS_FILE_FOLDER;
    public final int BATCH_SIZE;
    public final int MAX_URL_NUMBER;



    public AppConfigs(
            @Value("${app.base-url:https://prod-usercontent.azureedge.net/Content/UserContent/Documents}") String baseUrl,
            @Value("${app.checkpoint-file:internal/checkpoint.json}") String checkpointFile,
            @Value("${app.success-file-folder:results}") String successFileFolder,
            @Value("${app.max-url-number:999999}") int maxUrlNumber,
            @Value("${app.batch-size:100}") int batchSize
    ){
        BASE_URL = baseUrl;
        CHECKPOINT_FILE = checkpointFile;
        SUCCESS_FILE_FOLDER = successFileFolder;
        BATCH_SIZE = batchSize;
        MAX_URL_NUMBER=maxUrlNumber;
    }

    @PostConstruct
    public void init(){
        log.info("Loaded configurations: {}", this);
    }

}
