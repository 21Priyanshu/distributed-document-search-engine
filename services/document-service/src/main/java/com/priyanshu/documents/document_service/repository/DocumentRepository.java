package com.priyanshu.documents.document_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.priyanshu.documents.document_service.entity.Document;
import com.priyanshu.documents.document_service.entity.DocumentStatus;

@EnableJpaRepositories
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findById(UUID id);

    @Modifying
    @Query("update Document d set d.status = :status where d.id = :id")
    int updateStatus(UUID id, DocumentStatus status);

    // New methods for ownerId-based queries
    Optional<Document> findByIdAndOwnerId(UUID id, String ownerId);
    List<Document> findAllByOwnerId(String ownerId);
}