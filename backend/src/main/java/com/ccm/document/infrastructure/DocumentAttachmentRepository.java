package com.ccm.document.infrastructure;

import com.ccm.document.domain.DocumentAttachment;
import com.ccm.document.domain.DocumentOwnerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentAttachmentRepository extends JpaRepository<DocumentAttachment, Long> {

    List<DocumentAttachment> findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(
            DocumentOwnerType ownerType,
            Long ownerId
    );

    @Query("""
            SELECT COALESCE(MAX(d.versionNo), 0)
            FROM DocumentAttachment d
            WHERE d.ownerType = :ownerType
              AND d.ownerId = :ownerId
              AND LOWER(d.originalFilename) = LOWER(:filename)
            """)
    int findMaxVersionNo(
            @Param("ownerType") DocumentOwnerType ownerType,
            @Param("ownerId") Long ownerId,
            @Param("filename") String filename
    );
}
