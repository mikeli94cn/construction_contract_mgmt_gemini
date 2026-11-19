package com.ccm.changeorder.api;

import com.ccm.changeorder.domain.ChangeOrderStatus;
import com.ccm.changeorder.domain.ChangeOrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ChangeOrderDtos {

    private ChangeOrderDtos() {
    }

    public record CreateChangeOrderRequest(
            @NotBlank(message = "Change order number is required")
            @Size(max = 60)
            String changeOrderNo,

            @NotNull(message = "Change order type is required")
            ChangeOrderType changeType,

            @NotBlank(message = "Change order title is required")
            @Size(max = 300)
            String title,

            @NotBlank(message = "Engineering/commercial justification reason is required")
            String reason,

            @NotNull(message = "Net amount delta is required")
            BigDecimal amountDeltaNet,

            Integer scheduleExtensionDays
    ) {
    }

    public record ChangeOrderResponse(
            Long id,
            Long contractId,
            String contractNo,
            Long projectId,
            String changeOrderNo,
            ChangeOrderType changeType,
            String title,
            String reason,
            BigDecimal amountDeltaNet,
            BigDecimal taxRate,
            BigDecimal amountDeltaTax,
            BigDecimal amountDeltaGross,
            int scheduleExtensionDays,
            ChangeOrderStatus status,
            Instant approvedAt,
            Long version,
            Instant createdAt,
            String createdBy
    ) {
    }
}
