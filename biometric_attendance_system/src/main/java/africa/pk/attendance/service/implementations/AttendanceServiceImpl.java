package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.dtos.request.AddAttendanceRequest;
import africa.pk.attendance.dtos.request.AttendanceHistoryRequest;
import africa.pk.attendance.dtos.request.TotalNumberOfAttendanceRequest;
import africa.pk.attendance.dtos.response.AddAttendanceResponse;
import africa.pk.attendance.dtos.response.AttendanceHistoryResponse;
import africa.pk.attendance.dtos.response.TotalNumberOfAttendanceResponse;
import africa.pk.attendance.expections.AttendanceExpection;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.AttendanceService;
import africa.pk.attendance.service.interfaces.NativeService;
import africa.pk.attendance.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    @Lazy
    private AttendanceRepository attendanceRepository;

    @Lazy
    private NativeService nativeService;

    @Lazy
    private AttendanceMessageHandler attendanceMessageHandler;

    private final String responseTopic = "response";

    @Override
    public AddAttendanceResponse addAttendance(AddAttendanceRequest addAttendanceRequest) {
        try {
            if (addAttendanceRequest.getFingerprintId() == null || addAttendanceRequest.getFingerprintId().isEmpty()) {
                throw new AttendanceExpection("Fingerprint ID field is empty");
            }
            if (addAttendanceRequest.getNativeName() == null || addAttendanceRequest.getNativeName().isEmpty()) {
                throw new AttendanceExpection("Native name field is empty");
            }
            if (addAttendanceRequest.getAttendanceDate() == null || addAttendanceRequest.getAttendanceDate().isEmpty()) {
                throw new AttendanceExpection("Attendance date field is empty");
            }
            if (addAttendanceRequest.getAttendanceTime() == null || addAttendanceRequest.getAttendanceTime().isEmpty()) {
                throw new AttendanceExpection("Attendance time field is empty");
            }

            nativeService.findNativeByFingerprintId(addAttendanceRequest.getFingerprintId());

            List<Attendance> existingAttendance = attendanceRepository.findByNativeId(addAttendanceRequest.getFingerprintId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime requestDateTime = LocalDateTime.parse(addAttendanceRequest.getAttendanceDate(), formatter);

            for (Attendance attendance : existingAttendance) {
                LocalDateTime existingDateTime = attendance.getAttendanceDate();
                if (existingDateTime.toLocalDate().equals(requestDateTime.toLocalDate())) {
                    throw new AttendanceExpection(addAttendanceRequest.getNativeName() + " has already recorded attendance for today");
                }
            }

            Attendance attendance = Mapper.map(addAttendanceRequest);
            attendance.setStatus("Present"); // Adding status field
            attendanceRepository.save(attendance);

            AddAttendanceResponse response = new AddAttendanceResponse();
            response.setMessage("Attendance recorded successfully for " + addAttendanceRequest.getNativeName());
            response.setIssuccess(true);
            return response;
        } catch (AttendanceExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public List<AttendanceHistoryResponse> getNativeAttendanceHistory(AttendanceHistoryRequest attendanceHistoryRequest) {
        try {
            nativeService.findNativeByFingerprintId(attendanceHistoryRequest.getFingerprintId());

            if (attendanceHistoryRequest.getStartDate() == null || attendanceHistoryRequest.getStartDate().isEmpty()) {
                throw new AttendanceExpection("Start date cannot be empty");
            }
            if (attendanceHistoryRequest.getEndDate() == null || attendanceHistoryRequest.getEndDate().isEmpty()) {
                throw new AttendanceExpection("End date cannot be empty");
            }

            List<Attendance> attendanceList = attendanceRepository.findByNativeId(attendanceHistoryRequest.getFingerprintId());
            List<AttendanceHistoryResponse> responses = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(attendanceHistoryRequest.getStartDate(), formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(attendanceHistoryRequest.getEndDate(), formatter);

            if (startDateTime.isAfter(endDateTime)) {
                throw new AttendanceExpection("Start date cannot be after end date");
            }

            for (Attendance attendance : attendanceList) {
                LocalDateTime attendanceDateTime = attendance.getAttendanceDate();
                if ((attendanceDateTime.isEqual(startDateTime) || attendanceDateTime.isAfter(startDateTime)) &&
                        (attendanceDateTime.isEqual(endDateTime) || attendanceDateTime.isBefore(endDateTime))) {
                    AttendanceHistoryResponse response = Mapper.attendanceHistoryResponseMapper(attendance);
                    response.setStatus(attendance.getStatus() != null ? attendance.getStatus() : "Unknown"); // Adding status to response
                    responses.add(response);
                }
            }

            if (responses.isEmpty()) {
                throw new AttendanceExpection("No attendance records found for native with fingerprint ID: " +
                        attendanceHistoryRequest.getFingerprintId() + " between " +
                        attendanceHistoryRequest.getStartDate() + " and " + attendanceHistoryRequest.getEndDate());
            }
            return responses;
        } catch (Exception e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw new AttendanceExpection(e.getMessage());
        }
    }

    @Override
    public TotalNumberOfAttendanceResponse nativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest) {
        try {
            if (totalNumberOfAttendanceRequest.getFingerprintId() == null ||
                    totalNumberOfAttendanceRequest.getFingerprintId().trim().isEmpty()) {
                attendanceMessageHandler.publishMessage("Error: Fingerprint ID cannot be empty", responseTopic);
                throw new AttendanceExpection("Fingerprint ID cannot be empty");
            }

            try {
                nativeService.findNativeByFingerprintId(totalNumberOfAttendanceRequest.getFingerprintId());
            } catch (AttendanceExpection e) {
                attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
                throw e;
            }

            if (totalNumberOfAttendanceRequest.getStartDate() == null || totalNumberOfAttendanceRequest.getStartDate().isEmpty()) {
                attendanceMessageHandler.publishMessage("Error: Start date cannot be empty", responseTopic);
                throw new AttendanceExpection("Start date cannot be empty");
            }
            if (totalNumberOfAttendanceRequest.getEndDate() == null || totalNumberOfAttendanceRequest.getEndDate().isEmpty()) {
                attendanceMessageHandler.publishMessage("Error: End date cannot be empty", responseTopic);
                throw new AttendanceExpection("End date cannot be empty");
            }

            List<Attendance> attendanceList = attendanceRepository.findByNativeId(totalNumberOfAttendanceRequest.getFingerprintId());
            int count = 0;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(totalNumberOfAttendanceRequest.getStartDate(), formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(totalNumberOfAttendanceRequest.getEndDate(), formatter);

            if (startDateTime.isAfter(endDateTime)) {
                attendanceMessageHandler.publishMessage("Error: Start date cannot be after end date", responseTopic);
                throw new AttendanceExpection("Start date cannot be after end date");
            }

            for (Attendance attendance : attendanceList) {
                LocalDateTime attendanceDateTime = attendance.getAttendanceDate();
                if ((attendanceDateTime.isEqual(startDateTime) || attendanceDateTime.isAfter(startDateTime)) &&
                        (attendanceDateTime.isEqual(endDateTime) || attendanceDateTime.isBefore(endDateTime))) {
                    count++;
                }
            }

            TotalNumberOfAttendanceResponse response = new TotalNumberOfAttendanceResponse();
            response.setTotalNumberOfAttendance(count);
            return response;
        } catch (DateTimeParseException e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw new AttendanceExpection(e.getMessage());
        } catch (Exception e) {
            if (!(e instanceof AttendanceExpection)) {
                attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            }
            throw new AttendanceExpection(e.getMessage());
        }
    }

    @Override
    public List<AttendanceHistoryResponse> viewAllAttendance() {
        List<Attendance> attendances = attendanceRepository.findAll();
        if (attendances.isEmpty()) {
            throw new AttendanceExpection("No attendance records found");
        }
        return attendances.stream()
                .map(attendance -> {
                    AttendanceHistoryResponse response = Mapper.attendanceHistoryResponseMapper(attendance);
                    response.setStatus(attendance.getStatus() != null ? attendance.getStatus() : "Unknown"); // Adding status to response
                    return response;
                })
                .collect(Collectors.toList());
    }
}