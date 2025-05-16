package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class AddAttendanceRequest {
    private String fingerprintId;
    private String nativeName;
    private String attendanceDate;
    private String attendanceTime;
    private String status;
}
