package africa.pk.attendance.service.interfaces;

import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;
import java.util.List;

public interface AdminService {
    RegisterAdminResponse registerAdmin(RegisterAdminRequest registerAdminRequest);
    LoginAdminResponse loginAdmin(LoginAdminRequest loginAdminRequest);
    RegisterNativeResponse addNative(RegisterNativeRequest registerNativeRequest);

    // View attendance methods returning List<AttendanceHistoryResponse>
    List<AttendanceHistoryResponse> viewAllAttendance();
    List<AttendanceHistoryResponse> viewAllAttendanceByTime(AllTimeBasedAttendanceRequest request);
    List<AttendanceHistoryResponse> viewCohortAttendance(CohortAttendanceRequest request);
    List<AttendanceHistoryResponse> viewCohortAttendanceByTime(CohortTimeBasedAttendanceRequest request);
    List<AttendanceHistoryResponse> viewNativeAttendance(NativeAttendanceRequest request);
    List<AttendanceHistoryResponse> viewNativeAttendanceByTime(NativeTimeBasedAttendanceRequest request);

    
    byte[] viewAllAttendanceAsExcel();
    byte[] viewAllAttendanceByTimeAsExcel(AllTimeBasedAttendanceRequest request);
    byte[] viewCohortAttendanceAsExcel(CohortAttendanceRequest request);
    byte[] viewCohortAttendanceByTimeAsExcel(CohortTimeBasedAttendanceRequest request);
    byte[] viewNativeAttendanceAsExcel(NativeAttendanceRequest request);
    byte[] viewNativeAttendanceByTimeAsExcel(NativeTimeBasedAttendanceRequest request);


    byte[] exportAttendanceToExcel();

    TotalNumberOfAttendanceResponse viewNativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest);
    double getAttendancePercentage(AttendancePercentageRequest request);
    LogoutAdminResponse logoutAdmin(LogoutAdminRequest logoutAdminRequest);
}