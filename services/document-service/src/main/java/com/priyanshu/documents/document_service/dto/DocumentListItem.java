package com.priyanshu.documents.document_service.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentListItem(
    String documentId,
    String title,
    String description,
    String status,
    Instant createdAt
) {

   public DocumentListItem(
       UUID documentId,
       String title,
       String description,
       String status,
       Instant createdAt
   ) {
       this(
           documentId.toString(),
           title,
           description,
           status,
           createdAt
       );
   } 
}

