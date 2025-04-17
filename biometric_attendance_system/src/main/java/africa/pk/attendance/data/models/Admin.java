package africa.pk.attendance.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Locale;

@Data
@Entity
public class Admin {
    private String firstName;
    private String lastName;

    @Id
    private String userName;
    private String password;
    private LocalDateTime dateOfCreation = LocalDateTime.now();
    private Boolean isLoggedIn;

    public String toString() {
        return "ADMIN [firstname=" + firstName + ", " +
                "lastname=" + lastName +
                ", username=" + userName +
                ", password=" + password +
                ", dateOfCreation=" + dateOfCreation +
                ", isLoggedIn=" + isLoggedIn + "]";
    }



}
