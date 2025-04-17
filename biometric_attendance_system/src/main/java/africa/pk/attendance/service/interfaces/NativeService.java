package africa.pk.attendance.service.interfaces;

import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;

import java.util.List;

public interface NativeService {
    RegisterNativeResponse registerNative(RegisterNativeRequest registerNativeRequest);
    FindNativeByFingerprintResponse findNativeByFingerprintId(FindNativeByFingerprintRequest findNativeByFingerprintIdRequest);
    FindNativeByFingerprintResponse findNativeByFingerprintId(String fingerprintId);
    List<AttendanceHistoryResponse> getNativeAttendanceHistory(AttendanceHistoryRequest attendanceHistoryRequest);
    TotalNumberOfAttendanceResponse nativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest);
    List<AttendanceHistoryResponse> viewNativeAttendance(NativeAttendanceRequest request);
    List<AttendanceHistoryResponse> viewNativeAttendanceByTime(NativeTimeBasedAttendanceRequest request);
}