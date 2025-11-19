package com.nayoung.telemed.appointment.service;

import com.nayoung.telemed.appointment.dto.AppointmentDTO;
import com.nayoung.telemed.res.Response;

import java.util.List;

public interface AppointmentService {
    Response<AppointmentDTO> bookAppointment(AppointmentDTO appointmentDTO);
    Response<List<AppointmentDTO>> getMyAppointments();
    Response<AppointmentDTO> cancelAppointment(Long appointmentId);
    Response<?> completeAppointment(Long appointmentId);
}
