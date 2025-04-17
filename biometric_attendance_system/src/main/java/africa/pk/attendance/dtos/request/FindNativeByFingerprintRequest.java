package africa.pk.attendance.dtos.request;

import lombok.Data;

@Data
public class FindNativeByFingerprintRequest {
    private String fingerprintId;
}
