package com.gil.pdfscrapper;

import com.gil.pdfscrapper.services.WebScrapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class PdfScrapperApplication implements CommandLineRunner {
	private final WebScrapperService webScrapperService;

	public static void main(String[] args) {
		SpringApplication.run(PdfScrapperApplication.class, args);
	}

	@Override
	public void run(String... args) throws IOException, InterruptedException {
		Scanner scanner = new Scanner(System.in);
		LocalDate filterDate = null;

		final String green = "\033[32m"; // Green text color
		final String red = "\033[31m";   // Red text color
		final String reset = "\033[0m";  // Reset to default color

		System.out.println(green + "Welcome!" + reset);
		System.out.println(green + "To use today (" + LocalDate.now() + ") enter TODAY" + reset);

		while (filterDate == null) {
			System.out.println(green + "Please enter a date in the format YYYY-MM-DD (e.g., 2023-01-01) to filter URLs:" + reset);
			String inputDate = scanner.nextLine().trim();

			if (inputDate.equalsIgnoreCase("TODAY")) {
				filterDate = LocalDate.now();
			} else {
				try {
					filterDate = LocalDate.parse(inputDate, DateTimeFormatter.ISO_LOCAL_DATE);
				} catch (DateTimeParseException e) {
					System.out.println(red + "The date entered is malformed. Please try again using the correct format or enter TODAY to use the current date.\n" + reset);
				}
			}
		}

		log.info("Starting with date filter: {}...", filterDate);
		webScrapperService.run(filterDate);
	}
}
