package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class RegisterNativeRequest {
    private String fingerprintId;
    private String firstName;
    private String lastName;
    private String email;
    private String cohort;
}
