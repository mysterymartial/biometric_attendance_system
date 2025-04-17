package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class RegisterAdminRequest {
    private String userName;
    private String firstName;
    private String lastName;
    private String password;
}
