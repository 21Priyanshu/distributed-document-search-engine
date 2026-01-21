
package com.priyanshu.index.indexing_service.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.priyanshu.index.indexing_service.entity.Document;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Service
public class ElasticsearchService {

    private final ElasticsearchClient client;

    public ElasticsearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public void index(Document doc, String content) throws IOException {

        Map<String, Object> data = new HashMap<>();
        data.put("id", doc.getId().toString());
        data.put("title", doc.getTitle());
        data.put("description", doc.getDescription());
        data.put("content", content);
        data.put("createdAt", doc.getCreatedAt().toString());

        client.index(i -> i
            .index("documents")
            .id(doc.getId().toString())
            .document(data)
        );
    }
}
