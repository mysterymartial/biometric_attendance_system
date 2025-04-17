package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class ViewCohortAttendanceRequest {
    private String cohort;
    private String startDate;
    private String endDate;
}
