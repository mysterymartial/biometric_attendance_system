package africa.pk.attendance.utils;

import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.models.Native;
import africa.pk.attendance.dtos.request.AddAttendanceRequest;
import africa.pk.attendance.dtos.request.RegisterNativeRequest;
import africa.pk.attendance.dtos.response.AttendanceHistoryResponse;
import africa.pk.attendance.dtos.response.FindNativeByFingerprintResponse;
import africa.pk.attendance.dtos.response.RegisterNativeResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mapper {

    public static Native map(RegisterNativeRequest registerNativeRequest) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId(registerNativeRequest.getFingerprintId());
        nativePerson.setFirstName(registerNativeRequest.getFirstName());
        nativePerson.setLastName(registerNativeRequest.getLastName());
        nativePerson.setEmail(registerNativeRequest.getEmail());
        nativePerson.setCohort(registerNativeRequest.getCohort());
        return nativePerson;
    }

        public static RegisterNativeResponse map(Native nativePerson) {
            RegisterNativeResponse response = new RegisterNativeResponse();
            response.setFingerprintId(nativePerson.getFingerprintId());
            response.setFirstName(nativePerson.getFirstName());
            response.setLastName(nativePerson.getLastName());
            response.setEmail(nativePerson.getEmail());
            response.setCohort(nativePerson.getCohort());
            response.setMessage("Native registered successfully");
            response.setSuccess(true);
            return response;
        }

    public static FindNativeByFingerprintResponse findNativeMapper(Native nativePerson) {
        FindNativeByFingerprintResponse response = new FindNativeByFingerprintResponse();
        response.setFingerprintId(nativePerson.getFingerprintId());
        response.setFirstName(nativePerson.getFirstName());
        response.setLastName(nativePerson.getLastName());
        response.setEmail(nativePerson.getEmail());
        response.setCohort(nativePerson.getCohort());
        response.setSuccess(true);
        return response;
    }

    public static Attendance map(AddAttendanceRequest addAttendanceRequest) {
        Attendance attendance = new Attendance();
        attendance.setNativeId(addAttendanceRequest.getFingerprintId());
        attendance.setNativeName(addAttendanceRequest.getNativeName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime attendanceDateTime = LocalDateTime.parse(addAttendanceRequest.getAttendanceDate(), formatter);
        attendance.setAttendanceDate(attendanceDateTime);
        attendance.setAttendanceTime(addAttendanceRequest.getAttendanceTime());
        return attendance;
    }

    public static AttendanceHistoryResponse attendanceHistoryResponseMapper(Attendance attendance) {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId(attendance.getNativeId()); // Fixed: Use getNativeId instead of getFingerprintId
        response.setNativeName(attendance.getNativeName());  // Already available in Attendance model
        response.setAttendanceDate(attendance.getAttendanceDate());
        response.setAttendanceTime(attendance.getAttendanceTime());
        return response;
    }
}