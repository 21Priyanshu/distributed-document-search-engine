package com.priyanshu.documents.document_service.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.priyanshu.documents.document_service.dto.UploadDocResponse;
import com.priyanshu.documents.document_service.entity.Document;
import com.priyanshu.documents.document_service.entity.DocumentStatus;
import com.priyanshu.documents.document_service.exception.DocumentServiceException;
import com.priyanshu.documents.document_service.repository.DocumentRepository;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import com.priyanshu.documents.common.events.DocumentUploadedEvent;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final MinioClient minioClient;
    private final DocumentEventProducer producer;

    @Value("${minio.bucket}")
    private String bucket;

    public DocumentService(DocumentRepository repository,
                           MinioClient minioClient,
                           DocumentEventProducer producer) {
        this.repository = repository;
        this.minioClient = minioClient;
        this.producer = producer;
    }

    @Transactional
    public UploadDocResponse upload(MultipartFile file, String title, String description, List<String> tags, String userId) {
        logger.info("Starting document upload for user: {}, file: {}, size: {} bytes",
                   userId, file.getOriginalFilename(), file.getSize());

        UUID documentId = UUID.randomUUID();
        String objectName = documentId + "-" + file.getOriginalFilename();

        try {
            // 1. Upload to MinIO
            logger.debug("Uploading file to MinIO: {}", objectName);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            logger.debug("File uploaded to MinIO successfully: {}", objectName);

            // 2. Save metadata
            logger.debug("Saving document metadata to database");
            Document doc = new Document();
            doc.setId(documentId);
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setContentType(file.getContentType());
            doc.setStoragePath(objectName);
            doc.setStatus(DocumentStatus.UPLOADED);
            doc.setOwnerId(userId);

            Document saved = repository.save(doc);
            logger.debug("Document metadata saved with ID: {}", saved.getId());

            // 3. Publish Kafka event
            logger.debug("Publishing document uploaded event for: {}", documentId);
            DocumentUploadedEvent event = new DocumentUploadedEvent(
                documentId.toString(),
                objectName,
                title,
                description,
                userId,
                0
            );

            producer.publishDocumentUploaded(event);
            logger.info("Document uploaded successfully: {} for user: {}", documentId, userId);

            return new UploadDocResponse(
                saved.getId(),
                "UPLOADED",
                "Document uploaded successfully"
            );

        } catch (MinioException e) {
            logger.error("MinIO error during upload for document: {}", documentId, e);
            throw new DocumentServiceException("Failed to upload file to storage: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during document upload: {}", documentId, e);
            throw new DocumentServiceException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    public Document getDocument(UUID documentId, String userId) {
        logger.debug("Retrieving document: {} for user: {}", documentId, userId);

        Document document = repository.findByIdAndOwnerId(documentId, userId)
                .orElseThrow(() -> {
                    logger.warn("Document not found: {} for user: {}", documentId, userId);
                    return new EntityNotFoundException("Document not found or access denied: " + documentId);
                });

        logger.debug("Document retrieved successfully: {} for user: {}", documentId, userId);
        return document;
    }

    public InputStream downloadFile(String storagePath) {
        logger.debug("Downloading file from MinIO: {}", storagePath);

        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
            logger.debug("File downloaded successfully from MinIO: {}", storagePath);
            return stream;

        } catch (MinioException e) {
            logger.error("MinIO error during download: {}", storagePath, e);
            throw new DocumentServiceException("Failed to download file from storage: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during file download: {}", storagePath, e);
            throw new DocumentServiceException("Failed to download file: " + e.getMessage(), e);
        }
    }

    public DocumentStatus getDocumentStatus(UUID documentId, String userId) {
        logger.debug("Getting status for document: {} by user: {}", documentId, userId);

        Document doc = repository.findByIdAndOwnerId(documentId, userId)
                .orElseThrow(() -> {
                    logger.warn("Document not found for status check: {} by user: {}", documentId, userId);
                    return new EntityNotFoundException("Document not found or access denied: " + documentId);
                });

        DocumentStatus status = doc.getStatus();
        logger.debug("Document status retrieved: {} - {}", documentId, status);
        return status;
    }

    @Transactional
    public void updateStatus(UUID documentId, DocumentStatus status, String userId, boolean isService) {
        logger.info("Updating status for document: {} to {} by user: {} (service: {})",
                   documentId, status, userId, isService);

        try {
            if (!isService) {
                // For user calls, verify ownership
                logger.debug("Verifying ownership for document: {} by user: {}", documentId, userId);
                Document doc = repository.findByIdAndOwnerId(documentId, userId)
                        .orElseThrow(() -> {
                            logger.warn("Document not found or access denied for status update: {} by user: {}", documentId, userId);
                            return new EntityNotFoundException("Document not found or access denied: " + documentId);
                        });
            }

            int updated = repository.updateStatus(documentId, status);

            if (updated == 0) {
                logger.warn("No document found to update status: {}", documentId);
                throw new EntityNotFoundException("Document not found: " + documentId);
            }

            logger.info("Document status updated successfully: {} to {}", documentId, status);

        } catch (EntityNotFoundException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            logger.error("Unexpected error during status update for document: {}", documentId, e);
            throw new DocumentServiceException("Failed to update document status: " + e.getMessage(), e);
        }
    }
}

