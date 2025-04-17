package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class AttendanceHistoryRequest {
    private String fingerprintId;
    private String startDate;
    private String endDate;
}
