package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class RegisterAdminResponse {
    private String userName;
    private String firstName;
    private String lastName;
    private String message;
    private boolean isSuccess;
}
