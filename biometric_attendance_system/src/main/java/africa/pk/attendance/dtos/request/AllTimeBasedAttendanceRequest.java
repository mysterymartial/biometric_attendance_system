package africa.pk.attendance.dtos.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
public class AllTimeBasedAttendanceRequest {
    private String startDate;
    private String endDate;
}
