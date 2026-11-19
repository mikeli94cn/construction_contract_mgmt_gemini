package com.ccm.audit.application;

import com.ccm.audit.domain.AuditLog;
import com.ccm.audit.infrastructure.AuditLogRepository;
import com.ccm.common.security.SecurityContextUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditTrailService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            String module,
            String entityType,
            Object entityId,
            String action,
            String summary,
            Map<String, Object> details
    ) {
        String payloadJson = null;
        if (details != null && !details.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException ignored) {
                payloadJson = "{}";
            }
        }
        AuditLog log = new AuditLog(
                SecurityContextUtils.getCurrentUserId(),
                SecurityContextUtils.getCurrentUsername(),
                module,
                entityType,
                String.valueOf(entityId),
                action,
                summary,
                payloadJson
        );
        auditLogRepository.save(log);
    }
}
