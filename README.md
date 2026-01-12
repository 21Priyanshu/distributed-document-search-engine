# Distributed Document Search Engine

A scalable backend system that allows users to upload documents and perform fast, ranked full-text search with metadata filtering.

## Features
- JWT-based authentication
- Document upload and storage (MinIO)
- Metadata storage (PostgreSQL)
- Asynchronous indexing using Kafka
- Full-text search using Elasticsearch
- Redis-based caching for search queries
- Pagination and filtering support

## Architecture
- API Gateway
- Auth Service
- Document Service
- Indexing Service
- Search Service
- Kafka, Redis, PostgreSQL, MinIO, Elasticsearch

(Architecture diagrams to be added)

## Tech Stack
Java, Spring Boot, Kafka, Elasticsearch, Redis, PostgreSQL, MinIO, Docker
