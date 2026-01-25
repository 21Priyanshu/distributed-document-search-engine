package com.priyanshu.index.indexing_service.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DocumentStatusClient {

    private IndexingService indexingService;
    private final RestTemplate restTemplate = new RestTemplate();

    public void updateStatus(String documentId, String status) {
        String url = "http://localhost:8081/documents/" + documentId + "/status?status=" + status;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(indexingService.generateServiceToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

    }
}

