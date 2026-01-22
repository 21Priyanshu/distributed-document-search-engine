package com.priyanshu.index.indexing_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DocumentStatusClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public void updateStatus(String documentId, String status) {

        String url = "http://localhost:8081/documents/" + documentId + "/status?status=" + status;

        restTemplate.put(url, null);
    }
}

