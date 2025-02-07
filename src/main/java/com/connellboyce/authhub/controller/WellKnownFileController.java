package com.connellboyce.authhub.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping(value = {"", "/.well_known"})
public class WellKnownFileController {
	@GetMapping("/robots.txt")
	public ResponseEntity<?> getRobotsDotTxt() {
		try {
			return getTxtFileContents("robots.txt");
		} catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping("/humans.txt")
	public ResponseEntity<?> getHumansDotTxt() {
		try {
			return getTxtFileContents("humans.txt");
		} catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	private ResponseEntity<?> getTxtFileContents(String fileName) throws IOException {
		String path = "classpath:static/" + fileName;
		File file = ResourceUtils.getFile(path);
		String contents = new String(Files.readAllBytes(file.toPath()));
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.body(contents);
	}
}
