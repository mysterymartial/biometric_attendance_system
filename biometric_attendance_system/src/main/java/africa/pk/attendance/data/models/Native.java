package africa.pk.attendance.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Native {
    @Id
    private long id;
    private String fingerprintId;
    private String firstName;
    private String lastName;
    private String email;
    private String cohort;
    private LocalDateTime dateOfCreation = LocalDateTime.now();
}
