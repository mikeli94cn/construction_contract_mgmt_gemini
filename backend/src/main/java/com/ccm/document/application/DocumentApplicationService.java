package com.ccm.document.application;

import com.ccm.audit.application.AuditTrailService;
import com.ccm.common.exception.BusinessRuleViolationException;
import com.ccm.common.exception.ResourceNotFoundException;
import com.ccm.common.security.SecurityContextUtils;
import com.ccm.document.api.DocumentDtos.DocumentAttachmentMeta;
import com.ccm.document.api.DocumentDtos.DocumentResponse;
import com.ccm.document.api.DocumentDtos.DownloadedDocument;
import com.ccm.document.domain.DocumentAttachment;
import com.ccm.document.domain.DocumentOwnerType;
import com.ccm.document.domain.DocumentType;
import com.ccm.document.infrastructure.DocumentAttachmentRepository;
import com.ccm.project.application.ProjectAccessGuard;
import com.ccm.project.infrastructure.ProjectRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentApplicationService {

    private final DocumentAttachmentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final AuditTrailService auditTrailService;
    private final Path storageRoot;

    public DocumentApplicationService(
            DocumentAttachmentRepository documentRepository,
            ProjectRepository projectRepository,
            ProjectAccessGuard projectAccessGuard,
            AuditTrailService auditTrailService,
            @Value("${ccm.storage.local-path:./target/ccm-documents}") String localStoragePath
    ) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.auditTrailService = auditTrailService;
        this.storageRoot = Paths.get(localStoragePath).toAbsolutePath().normalize();
    }

    @Transactional
    public DocumentResponse uploadDocument(
            Long projectId,
            DocumentOwnerType ownerType,
            Long ownerId,
            DocumentType documentType,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("Uploaded document file cannot be empty");
        }
        projectAccessGuard.assertCanViewProject(projectId);
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        byte[] contentBytes;
        try {
            contentBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Failed to read uploaded file content");
        }

        String rawName = file.getOriginalFilename();
        String safeFilename = (rawName == null || rawName.isBlank())
                ? "document.bin"
                : Paths.get(rawName).getFileName().toString();
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? "application/octet-stream"
                : file.getContentType();

        String sha256 = computeSha256(contentBytes);
        int nextVersion = documentRepository.findMaxVersionNo(ownerType, ownerId, safeFilename) + 1;
        String storageKey = ownerType.name().toLowerCase() + "-" + ownerId + "-" + UUID.randomUUID();

        try {
            Files.createDirectories(storageRoot);
            Path targetFile = storageRoot.resolve(storageKey).normalize();
            if (!targetFile.startsWith(storageRoot)) {
                throw new BusinessRuleViolationException("Invalid storage path");
            }
            Files.write(targetFile, contentBytes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist document file to storage volume", e);
        }

        String actor = SecurityContextUtils.getCurrentUsername();
        DocumentAttachment attachment = new DocumentAttachment(
                projectId,
                ownerType,
                ownerId,
                documentType,
                safeFilename,
                contentType,
                contentBytes.length,
                sha256,
                storageKey,
                nextVersion,
                actor
        );
        DocumentAttachment saved = documentRepository.saveAndFlush(attachment);

        auditTrailService.record(
                "DOCUMENT",
                "DocumentAttachment",
                saved.getId(),
                "DOCUMENT_UPLOADED",
                "Uploaded document " + safeFilename + " (v" + nextVersion + ", sha256=" + sha256.substring(0, 12) + "...)",
                Map.of(
                        "ownerType", ownerType.name(),
                        "ownerId", ownerId,
                        "filename", safeFilename,
                        "versionNo", nextVersion,
                        "sha256", sha256
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(
            Long projectId,
            DocumentOwnerType ownerType,
            Long ownerId
    ) {
        projectAccessGuard.assertCanViewProject(projectId);
        return documentRepository.findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(ownerType, ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedDocument downloadDocument(Long documentId) {
        DocumentAttachment doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        projectAccessGuard.assertCanViewProject(doc.getProjectId());

        Path targetFile = storageRoot.resolve(doc.getStorageKey()).normalize();
        if (!Files.exists(targetFile)) {
            throw new ResourceNotFoundException("Stored document binary not found on disk: " + doc.getOriginalFilename());
        }
        try {
            byte[] bytes = Files.readAllBytes(targetFile);
            String actualHash = computeSha256(bytes);
            if (!actualHash.equalsIgnoreCase(doc.getSha256Checksum())) {
                throw new IllegalStateException("Document integrity check failed: SHA-256 mismatch");
            }
            return new DownloadedDocument(
                    new DocumentAttachmentMeta(doc.getOriginalFilename(), doc.getContentType(), doc.getSha256Checksum()),
                    bytes
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored document file", e);
        }
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private DocumentResponse toResponse(DocumentAttachment d) {
        return new DocumentResponse(
                d.getId(),
                d.getProjectId(),
                d.getOwnerType(),
                d.getOwnerId(),
                d.getDocumentType(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getFileSize(),
                d.getSha256Checksum(),
                d.getVersionNo(),
                d.getUploadedBy(),
                d.getUploadedAt()
        );
    }
}
