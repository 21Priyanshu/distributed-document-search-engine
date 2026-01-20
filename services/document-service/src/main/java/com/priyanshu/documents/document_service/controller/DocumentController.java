package com.priyanshu.documents.document_service.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.priyanshu.documents.document_service.service.DocumentService;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String tags
    ) throws Exception {

        List<String> tagList = tags != null ? Arrays.asList(tags.split(",")) : List.of();

        UUID id = service.upload(file, title, description, tagList);

        return ResponseEntity.ok(Map.of(
                "documentId", id,
                "status", "UPLOADED"
        ));
    }
}

