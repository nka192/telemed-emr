package com.nayoung.telemed.patient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nayoung.telemed.enums.BloodGroup;
import com.nayoung.telemed.enums.Genotype;
import com.nayoung.telemed.users.dto.UserDTO;
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
public class PatientDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String phone;

    private String knownAllergies;

    private BloodGroup bloodGroup;

    private Genotype genotype;

    private UserDTO user;
}
