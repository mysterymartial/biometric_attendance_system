package africa.pk.attendance.data.repositories;

import africa.pk.attendance.data.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByNativeId(String nativeId);
    List<Attendance> findByNativeName(String nativeName);

    Object findByNativeIdAndAttendanceDateBetween(String fp1, LocalDateTime any, LocalDateTime any1);
}
