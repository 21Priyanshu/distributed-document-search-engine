package com.priyanshu.documents.document_service.controller;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.priyanshu.documents.document_service.dto.DocumentStatusResponse;
import com.priyanshu.documents.document_service.dto.UploadDocResponse;
import com.priyanshu.documents.document_service.entity.Document;
import com.priyanshu.documents.document_service.entity.DocumentStatus;
import com.priyanshu.documents.document_service.service.DocumentService;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadDocResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String tags
    ) throws Exception {

        List<String> tagList = tags != null ? Arrays.asList(tags.split(",")) : List.of();

        UploadDocResponse response = service.upload(file, title, description, tagList);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws Exception {

        Document doc = service.getDocument(id);

        InputStream stream = service.downloadFile(doc.getStoragePath());

        InputStreamResource resource = new InputStreamResource(stream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(resource);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getStatus(@PathVariable UUID id){
        DocumentStatus status = service.gDocumentStatus(id);

        DocumentStatusResponse response = new DocumentStatusResponse(id, status);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestParam DocumentStatus status
    ) {
        service.updateStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}

