package com.priyanshu.documents.search_service.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.priyanshu.documents.search_service.dto.SearchResponse;
import com.priyanshu.documents.search_service.service.DocumentSearchService;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final DocumentSearchService searchService;

    public SearchController(DocumentSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {

        return searchService.search(q, page, size);
    }
}

