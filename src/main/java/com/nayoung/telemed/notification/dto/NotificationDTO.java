package com.nayoung.telemed.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nayoung.telemed.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {

    private Long id;

    private String subject;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    private String message;

    private NotificationType type;

    private LocalDateTime createdAt;

    private String templateName;

    private Map<String, Object> templateVariables;
}
