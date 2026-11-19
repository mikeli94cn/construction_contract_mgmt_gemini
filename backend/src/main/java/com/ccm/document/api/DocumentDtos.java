package com.ccm.document.api;

import com.ccm.document.domain.DocumentOwnerType;
import com.ccm.document.domain.DocumentType;
import java.time.Instant;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentResponse(
            Long id,
            Long projectId,
            DocumentOwnerType ownerType,
            Long ownerId,
            DocumentType documentType,
            String originalFilename,
            String contentType,
            long fileSize,
            String sha256Checksum,
            int versionNo,
            String uploadedBy,
            Instant uploadedAt
    ) {
    }

    public record DownloadedDocument(
            DocumentAttachmentMeta meta,
            byte[] bytes
    ) {
    }

    public record DocumentAttachmentMeta(
            String originalFilename,
            String contentType,
            String sha256Checksum
    ) {
    }
}
