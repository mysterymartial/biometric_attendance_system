package africa.pk.attendance.service.implementations;

import africa.pk.attendance.dtos.request.AddAttendanceRequest;
import africa.pk.attendance.dtos.request.AttendanceMessage;
import africa.pk.attendance.dtos.request.FindNativeByFingerprintRequest;
import africa.pk.attendance.dtos.response.AddAttendanceResponse;
import africa.pk.attendance.dtos.response.AttendanceProcessingResult;
import africa.pk.attendance.dtos.response.FindNativeByFingerprintResponse;
import africa.pk.attendance.service.interfaces.AttendanceMessageService;
import africa.pk.attendance.service.interfaces.AttendanceService;
import africa.pk.attendance.service.interfaces.NativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttendanceMessageServiceImpl implements AttendanceMessageService {

    @Lazy
    private final AttendanceService attendanceService;

    private final NativeService nativeService;

    @Override
    public AttendanceProcessingResult addMessage(AttendanceMessage attendanceMessage) {
        try {
            FindNativeByFingerprintRequest findNativeRequest = new FindNativeByFingerprintRequest();
            findNativeRequest.setFingerprintId(attendanceMessage.getFingerprintId());

            FindNativeByFingerprintResponse findNativeResponse = nativeService.findNativeByFingerprintId(findNativeRequest);

            if (!findNativeResponse.isSuccess()) {
                AttendanceProcessingResult result = new AttendanceProcessingResult();
                result.setMessage("Error: " + findNativeResponse.getMessage());
                result.setTopic(attendanceMessage.getTopic());
                result.setSuccess(false);
                return result;
            }

            AddAttendanceRequest addAttendanceRequest = new AddAttendanceRequest();
            addAttendanceRequest.setFingerprintId(attendanceMessage.getFingerprintId());
            addAttendanceRequest.setNativeName(findNativeResponse.getFirstName() + " " + findNativeResponse.getLastName());
            addAttendanceRequest.setAttendanceDate(attendanceMessage.getDate() + "T" + attendanceMessage.getTime());
            addAttendanceRequest.setAttendanceTime(attendanceMessage.getTime());

            AddAttendanceResponse addAttendanceResponse = attendanceService.addAttendance(addAttendanceRequest);

            if (!addAttendanceResponse.isSuccess()) {
                AttendanceProcessingResult result = new AttendanceProcessingResult();
                result.setMessage("Error: " + addAttendanceResponse.getMessage());
                result.setTopic(attendanceMessage.getTopic());
                result.setSuccess(false);
                return result;
            }

            AttendanceProcessingResult result = new AttendanceProcessingResult();
            result.setMessage(addAttendanceResponse.getMessage());
            result.setTopic(attendanceMessage.getTopic());
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            AttendanceProcessingResult result = new AttendanceProcessingResult();
            result.setMessage("Error: " + e.getMessage());
            result.setTopic(attendanceMessage.getTopic());
            result.setSuccess(false);
            return result;
        }
    }
}