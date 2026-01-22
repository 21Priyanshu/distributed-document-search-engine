package com.priyanshu.documents.document_service.dto;

public record DocumentUploadedEvent(
        String documentId,
        String storagePath,
        String title,
        String description,
        String ownerId
) {}

