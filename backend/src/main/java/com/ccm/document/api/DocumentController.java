package com.ccm.document.api;

import com.ccm.document.api.DocumentDtos.DocumentResponse;
import com.ccm.document.api.DocumentDtos.DownloadedDocument;
import com.ccm.document.application.DocumentApplicationService;
import com.ccm.document.domain.DocumentOwnerType;
import com.ccm.document.domain.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Versioned document attachments with SHA-256 integrity verification")
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;

    public DocumentController(DocumentApplicationService documentApplicationService) {
        this.documentApplicationService = documentApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a versioned document attachment linked to a project/contract/change-order/payment/settlement")
    public DocumentResponse uploadDocument(
            @RequestParam Long projectId,
            @RequestParam DocumentOwnerType ownerType,
            @RequestParam Long ownerId,
            @RequestParam(defaultValue = "GENERAL") DocumentType documentType,
            @RequestParam("file") MultipartFile file
    ) {
        return documentApplicationService.uploadDocument(projectId, ownerType, ownerId, documentType, file);
    }

    @GetMapping
    @Operation(summary = "List document attachments for an entity")
    public List<DocumentResponse> listDocuments(
            @RequestParam Long projectId,
            @RequestParam DocumentOwnerType ownerType,
            @RequestParam Long ownerId
    ) {
        return documentApplicationService.listDocuments(projectId, ownerType, ownerId);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document binary after verifying its SHA-256 checksum")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        DownloadedDocument downloaded = documentApplicationService.downloadDocument(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(downloaded.meta().contentType()));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(downloaded.meta().originalFilename()).build()
        );
        headers.add("X-Document-SHA256", downloaded.meta().sha256Checksum());
        return new ResponseEntity<>(downloaded.bytes(), headers, HttpStatus.OK);
    }
}
