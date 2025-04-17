package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class AttendancePercentageRequest {
    private String fingerprintId;
    private String startDate;
    private String endDate;
}
