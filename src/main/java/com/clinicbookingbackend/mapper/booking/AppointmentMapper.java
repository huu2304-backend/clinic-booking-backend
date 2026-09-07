package com.clinicbookingbackend.mapper.booking;

import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.entity.appointment.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "doctorSchedule.id", target = "doctorScheduleId")
    @Mapping(source = "doctorSchedule.doctorProfile.id", target = "doctorId")
    @Mapping(source = "doctorSchedule.doctorProfile.fullName", target = "doctorFullName")
    @Mapping(source = "doctorSchedule.workDate", target = "workDate")
    @Mapping(source = "doctorSchedule.startTime", target = "startTime")
    @Mapping(source = "doctorSchedule.endTime", target = "endTime")
    AppointmentResponse toResponse(Appointment appointment);
}
