package africa.pk.attendance.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterAdminRequest {
    @NotBlank(message = "Username cannot be empty")
    private String userName;

    @NotBlank(message = "First name cannot be empty")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    private String lastName;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    private String password;
}