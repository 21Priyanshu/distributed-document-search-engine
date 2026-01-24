package com.priyanshu.documents.search_service.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.priyanshu.documents.search_service.dto.SearchResponse;
import com.priyanshu.documents.search_service.dto.SearchResultItem;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Service
public class DocumentSearchService {

    private final ElasticsearchClient client;

    public DocumentSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public SearchResponse search(String query, int page, int size, String userId) throws IOException {

    int from = page * size;

    var response = client.search(s -> s
            .index("documents_index_v2")
            .from(from)
            .size(size)
            .query(q -> q
                .bool(b -> b
                    .must(m -> m
                        .multiMatch(mm -> mm
                            .fields("title", "description", "content")
                            .query(query)
                        )
                    )
                    .filter(f -> f
                        .term(t -> t
                            .field("ownerId")
                            .value(userId)
                        )
                    )
                )
            ),
            Map.class
    );

    List<SearchResultItem> results = response.hits().hits().stream()
            .map(hit -> {
                Map<String, Object> src = hit.source();

                return new SearchResultItem(
                        hit.id(),
                        (String) src.get("title"),
                        (String) src.get("description"),
                        String.valueOf(src.get("createdAt"))   // safer
                );
            })
            .toList();

    return new SearchResponse(response.hits().total().value(), results);
}
}

