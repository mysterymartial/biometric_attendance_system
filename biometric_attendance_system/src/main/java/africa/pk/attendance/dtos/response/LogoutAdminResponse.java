package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class LogoutAdminResponse {
    private String userName;
    private String message;
    private boolean success;
}
