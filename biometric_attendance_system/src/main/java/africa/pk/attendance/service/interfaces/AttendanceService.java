package africa.pk.attendance.service.interfaces;

import africa.pk.attendance.dtos.request.*;

import africa.pk.attendance.dtos.response.AddAttendanceResponse;
import africa.pk.attendance.dtos.response.AttendanceHistoryResponse;
import africa.pk.attendance.dtos.response.TotalNumberOfAttendanceResponse;

import java.util.List;

public interface AttendanceService {
    AddAttendanceResponse addAttendance(AddAttendanceRequest addAttendanceRequest);
    List<AttendanceHistoryResponse> getNativeAttendanceHistory(AttendanceHistoryRequest attendanceHistoryRequest);
    TotalNumberOfAttendanceResponse nativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest);
    List<AttendanceHistoryResponse> viewAllAttendance();
    //List<AttendanceHistoryResponse> viewAllAttendanceByTime(AllTimeBasedAttendanceRequest request);
}