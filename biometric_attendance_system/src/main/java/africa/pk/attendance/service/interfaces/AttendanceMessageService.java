package africa.pk.attendance.service.interfaces;


import africa.pk.attendance.dtos.request.AttendanceMessage;
import africa.pk.attendance.dtos.response.AttendanceProcessingResult;

public interface AttendanceMessageService {
    AttendanceProcessingResult addMessage(AttendanceMessage attendanceMessage);
}