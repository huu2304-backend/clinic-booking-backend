package com.clinicbookingbackend.mapper.booking;

import com.clinicbookingbackend.dto.booking.DoctorAppointmentResponse;
import com.clinicbookingbackend.entity.appointment.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorAppointmentMapper {

    // patientFullName không nằm trong quan hệ JPA của Appointment (chỉ lưu patientAccountId thô)
    // nên phải resolve ở Service (batch query PatientProfileRepository) rồi truyền vào đây —
    // mapper không được phép tự gọi Repository.
    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "appointment.doctorSchedule.workDate", target = "workDate")
    @Mapping(source = "appointment.doctorSchedule.startTime", target = "startTime")
    @Mapping(source = "appointment.doctorSchedule.endTime", target = "endTime")
    @Mapping(source = "appointment.status", target = "status")
    @Mapping(source = "patientFullName", target = "patientFullName")
    DoctorAppointmentResponse toResponse(Appointment appointment, String patientFullName);
}
