package africa.pk.attendance.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Attendance {
    private String nativeName;
    private String nativeId;
    private String status;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long attendanceId;
    private LocalDateTime attendanceDate;
    private String attendanceTime;
}
