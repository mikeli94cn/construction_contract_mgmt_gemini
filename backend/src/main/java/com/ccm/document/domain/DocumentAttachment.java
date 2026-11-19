package com.ccm.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private DocumentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 60)
    private DocumentType documentType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    private String sha256Checksum;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected DocumentAttachment() {
    }

    public DocumentAttachment(
            Long projectId,
            DocumentOwnerType ownerType,
            Long ownerId,
            DocumentType documentType,
            String originalFilename,
            String contentType,
            long fileSize,
            String sha256Checksum,
            String storageKey,
            int versionNo,
            String uploadedBy
    ) {
        this.projectId = projectId;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sha256Checksum = sha256Checksum;
        this.storageKey = storageKey;
        this.versionNo = versionNo;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public DocumentOwnerType getOwnerType() {
        return ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
