package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class RegisterNativeResponse {
    private String fingerprintId;
    private String firstName;
    private String lastName;
    private String email;
    private String cohort;
    private String message;
    private boolean isSuccess;
}
