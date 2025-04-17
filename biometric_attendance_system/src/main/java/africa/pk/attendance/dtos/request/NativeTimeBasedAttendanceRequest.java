package africa.pk.attendance.dtos.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class NativeTimeBasedAttendanceRequest {
    private String fingerprintId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
