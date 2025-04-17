package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class TotalNumberOfAttendanceRequest {
    private String fingerprintId;
    private String startDate;
    private String endDate;
}
