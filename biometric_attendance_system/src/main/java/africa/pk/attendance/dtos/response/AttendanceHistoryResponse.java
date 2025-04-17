package africa.pk.attendance.dtos.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceHistoryResponse {
    private String fingerprintId;
    private String nativeName;
    private String status;
    private LocalDateTime attendanceDate;
    private String attendanceTime;
}
