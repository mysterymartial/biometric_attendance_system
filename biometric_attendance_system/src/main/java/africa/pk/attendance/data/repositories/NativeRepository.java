package africa.pk.attendance.data.repositories;

import africa.pk.attendance.data.models.Native;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NativeRepository extends JpaRepository<Native, Long> {
    Optional<Native> findByFingerprintId(String fingerprintId);
    List<Native> findByCohort(String cohort);
    List<Native> findAll();
}