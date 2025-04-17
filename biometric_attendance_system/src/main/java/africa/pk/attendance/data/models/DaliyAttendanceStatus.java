package africa.pk.attendance.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
@Data
@Entity
public class DaliyAttendanceStatus {
    @Id
    private Long id;
    private String fingerprintId;
    private LocalDate date;
    private String status;
}
