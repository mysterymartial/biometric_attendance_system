package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class AttendanceProcessingResult {
    private String message;
    private String topic;
    private boolean isSuccess;
}
