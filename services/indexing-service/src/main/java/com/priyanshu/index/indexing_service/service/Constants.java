package com.priyanshu.index.indexing_service.service;

public class Constants {
    public static final String INDEX_MAPPING_JSON = """
                    {
                    "settings": {
                        "analysis": {
                        "filter": {
                            "edge_ngram_filter": {
                            "type": "edge_ngram",
                            "min_gram": 2,
                            "max_gram": 20
                            }
                        },
                        "analyzer": {
                            "autocomplete_analyzer": {
                            "type": "custom",
                            "tokenizer": "standard",
                            "filter": ["lowercase", "edge_ngram_filter"]
                            },
                            "search_analyzer": {
                            "type": "standard"
                            }
                        }
                        }
                    },
                    "mappings": {
                        "properties": {
                        "documentId": { "type": "keyword" },
                        "ownerId": { "type": "keyword" },
                        "title": {
                            "type": "text",
                            "analyzer": "autocomplete_analyzer",
                            "search_analyzer": "search_analyzer"
                        },
                        "description": {
                            "type": "text",
                            "analyzer": "autocomplete_analyzer",
                            "search_analyzer": "search_analyzer"
                        },
                        "content": {
                            "type": "text",
                            "analyzer": "autocomplete_analyzer",
                            "search_analyzer": "search_analyzer"
                        },
                        "tags": { "type": "keyword" },
                        "createdAt": { "type": "date" }
                        }
                    }
                    }
                    """;

}
