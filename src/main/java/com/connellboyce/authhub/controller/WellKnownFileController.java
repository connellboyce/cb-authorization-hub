package com.connellboyce.authhub.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(value = {"", "/.well-known"})
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

	// Must read via ClassPathResource#getInputStream, not ResourceUtils.getFile: once
	// packaged into the executable jar, these files live inside BOOT-INF/classes/static
	// and cannot be resolved to a java.io.File, only ever an InputStream.
	private ResponseEntity<?> getTxtFileContents(String fileName) throws IOException {
		ClassPathResource resource = new ClassPathResource("static/" + fileName);
		String contents;
		try (var inputStream = resource.getInputStream()) {
			contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.body(contents);
	}
}
