package africa.pk.attendance.data.repositories;

import africa.pk.attendance.data.models.DaliyAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyAttendanceStatusRepository extends JpaRepository<DaliyAttendanceStatus, Long> {
    Optional<DaliyAttendanceStatus> findByFingerprintIdAndDate(String fingerprintId, LocalDate date);
    List<DaliyAttendanceStatus> findByFingerprintIdAndDateBetween(String fingerprintId, LocalDate startDate, LocalDate endDate);
    List<DaliyAttendanceStatus> findByDate(LocalDate date);
}