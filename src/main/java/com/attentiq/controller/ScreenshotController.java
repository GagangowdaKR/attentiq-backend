package com.attentiq.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@RestController
@RequestMapping("/api/screenshot")
@RequiredArgsConstructor
public class ScreenshotController {

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getScreenshot(@PathVariable String filename) {
        try {
            File cleanFilename = new File(filename);
            String safeFilename = cleanFilename.getName();

            String userDir = System.getProperty("user.dir");
            File file = new File(userDir + File.separator + "screenshots" + File.separator + safeFilename);

            if (!file.exists()) {
                log.info("Image : {} is missing", safeFilename);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(file);

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("Image : {} is returned to frontend", safeFilename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}