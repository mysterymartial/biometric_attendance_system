package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class AttendanceMessage {
    private String fingerprintId;
    private String time;
    private String date;
    private String topic;

    @Override
    public String toString() {
        return "AttendanceMessage{" +
                "fingerprintId='" + fingerprintId + '\'' +
                ", time='" + time + '\'' +
                ", date='" + date + '\'' +
                ", topic='" + topic + '\'' +
                '}';
    }
}
