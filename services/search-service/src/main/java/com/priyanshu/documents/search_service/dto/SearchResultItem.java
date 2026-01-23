package com.priyanshu.documents.search_service.dto;

public record SearchResultItem(
    String documentId,
    String title,
    String description,
    String createdAt
) {

}
