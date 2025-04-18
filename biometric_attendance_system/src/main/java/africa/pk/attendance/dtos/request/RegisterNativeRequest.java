package africa.pk.attendance.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterNativeRequest {
    @NotBlank(message = "Fingerprint ID cannot be blank")
    private String fingerprintId;
    @NotBlank(message = "First name cannot be blank")
    private String firstName;
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address", regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private String email;
    @NotBlank(message = "Cohort cannot be blank")
    private String cohort;
}
