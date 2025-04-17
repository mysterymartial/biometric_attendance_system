package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class AddAttendanceResponse {
    private String message;
    private Boolean issuccess;
    private String status;

    public boolean isSuccess() {
        return issuccess;
    }
}


