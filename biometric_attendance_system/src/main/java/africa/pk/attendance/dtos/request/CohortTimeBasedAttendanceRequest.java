package africa.pk.attendance.dtos.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CohortTimeBasedAttendanceRequest {
    private String cohort;
    private String startDate;
    private String endDate;
}
