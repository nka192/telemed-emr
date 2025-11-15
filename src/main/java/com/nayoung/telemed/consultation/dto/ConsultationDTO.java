package com.nayoung.telemed.consultation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationDTO {

    private Long id;

    private Long appointmentId;

    private LocalDate consultationDate;

    private String subjectiveNotes;

    private String objectiveFindings;

    private String assessment;

    private String plan;
}
