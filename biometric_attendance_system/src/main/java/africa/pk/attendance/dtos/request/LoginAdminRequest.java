package africa.pk.attendance.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginAdminRequest {
    @NotBlank(message = "Username cannot be empty")
    private String userName;
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 50, message = "password must be between 8 and 50 characters")
    private String password;

}
