package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.models.Native;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.data.repositories.NativeRepository;
import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;
import africa.pk.attendance.expections.NativeExpection;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.NativeService;
import africa.pk.attendance.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NativeServiceImpl implements NativeService {

    private final NativeRepository nativeRepository;


    private final AttendanceRepository attendanceRepository;


    private  final AttendanceMessageHandler attendanceMessageHandler;

    private final String responseTopic = "response";

    @Override
    public RegisterNativeResponse registerNative(RegisterNativeRequest registerNativeRequest) {
        try {
            if (registerNativeRequest.getFingerprintId() == null || registerNativeRequest.getFingerprintId().isEmpty()) {
                throw new NativeExpection("Fingerprint ID cannot be empty");
            }
            if (registerNativeRequest.getFirstName() == null || registerNativeRequest.getFirstName().isEmpty()) {
                throw new NativeExpection("First name cannot be empty");
            }
            if (registerNativeRequest.getLastName() == null || registerNativeRequest.getLastName().isEmpty()) {
                throw new NativeExpection("Last name cannot be empty");
            }
            if (registerNativeRequest.getEmail() == null || registerNativeRequest.getEmail().isEmpty()) {
                throw new NativeExpection("Email cannot be empty");
            }
            if (registerNativeRequest.getCohort() == null || registerNativeRequest.getCohort().isEmpty()) {
                throw new NativeExpection("Cohort cannot be empty");
            }

            Optional<Native> existingNative = nativeRepository.findByFingerprintId(registerNativeRequest.getFingerprintId());
            if (existingNative.isPresent()) {
                throw new NativeExpection("Native with fingerprint ID " + registerNativeRequest.getFingerprintId() + " already exists");
            }

            Native nativePerson = Mapper.map(registerNativeRequest);
            Native savedNative = nativeRepository.save(nativePerson);
            return Mapper.map(savedNative);
        } catch (NativeExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public FindNativeByFingerprintResponse findNativeByFingerprintId(FindNativeByFingerprintRequest findNativeByFingerprintIdRequest) {
        try {
            if (findNativeByFingerprintIdRequest.getFingerprintId() == null || findNativeByFingerprintIdRequest.getFingerprintId().isEmpty()) {
                throw new NativeExpection("Fingerprint ID cannot be empty");
            }

            Optional<Native> foundNative = nativeRepository.findByFingerprintId(findNativeByFingerprintIdRequest.getFingerprintId());
            if (foundNative.isPresent()) {
                Native nativePerson = foundNative.get();
                return Mapper.findNativeMapper(nativePerson);
            } else {
                throw new NativeExpection("Native not found for fingerprint ID: " + findNativeByFingerprintIdRequest.getFingerprintId());
            }
        } catch (NativeExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    public FindNativeByFingerprintResponse findNativeByFingerprintId(String fingerprintId) {
        FindNativeByFingerprintRequest request = new FindNativeByFingerprintRequest();
        request.setFingerprintId(fingerprintId);
        return findNativeByFingerprintId(request);
    }

    @Override
    public List<AttendanceHistoryResponse> getNativeAttendanceHistory(AttendanceHistoryRequest attendanceHistoryRequest) {
        try {
            if (attendanceHistoryRequest.getFingerprintId() == null || attendanceHistoryRequest.getFingerprintId().isEmpty()) {
                throw new NativeExpection("Fingerprint ID cannot be empty");
            }

            Optional<Native> foundNative = nativeRepository.findByFingerprintId(attendanceHistoryRequest.getFingerprintId());
            if (foundNative.isEmpty()) {
                throw new NativeExpection("Native not found for fingerprint ID: " + attendanceHistoryRequest.getFingerprintId());
            }

            if (attendanceHistoryRequest.getStartDate() == null || attendanceHistoryRequest.getStartDate().isEmpty()) {
                throw new NativeExpection("Start date cannot be empty");
            }
            if (attendanceHistoryRequest.getEndDate() == null || attendanceHistoryRequest.getEndDate().isEmpty()) {
                throw new NativeExpection("End date cannot be empty");
            }

            List<Attendance> attendanceList = attendanceRepository.findByNativeId(attendanceHistoryRequest.getFingerprintId());
            List<AttendanceHistoryResponse> responses = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(attendanceHistoryRequest.getStartDate(), formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(attendanceHistoryRequest.getEndDate(), formatter);

            if (startDateTime.isAfter(endDateTime)) {
                throw new NativeExpection("Start date cannot be after end date");
            }

            for (Attendance attendance : attendanceList) {
                LocalDateTime attendanceDateTime = attendance.getAttendanceDate();
                if ((attendanceDateTime.isEqual(startDateTime) || attendanceDateTime.isAfter(startDateTime)) &&
                        (attendanceDateTime.isEqual(endDateTime) || attendanceDateTime.isBefore(endDateTime))) {
                    responses.add(Mapper.attendanceHistoryResponseMapper(attendance));
                }
            }
            if (responses.isEmpty()) {
                throw new NativeExpection("No attendance records found for native with fingerprint ID: " +
                        attendanceHistoryRequest.getFingerprintId() + " between " +
                        attendanceHistoryRequest.getStartDate() + " and " + attendanceHistoryRequest.getEndDate());
            }
            return responses;
        } catch (NativeExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public TotalNumberOfAttendanceResponse nativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest) {
        try {
            Optional<Native> foundNative = nativeRepository.findByFingerprintId(totalNumberOfAttendanceRequest.getFingerprintId());
            if (foundNative.isEmpty()) {
                throw new NativeExpection("Native not found for fingerprint ID: " + totalNumberOfAttendanceRequest.getFingerprintId());
            }

            if (totalNumberOfAttendanceRequest.getStartDate() == null || totalNumberOfAttendanceRequest.getStartDate().isEmpty()) {
                throw new NativeExpection("Start date cannot be empty");
            }
            if (totalNumberOfAttendanceRequest.getEndDate() == null || totalNumberOfAttendanceRequest.getEndDate().isEmpty()) {
                throw new NativeExpection("End date cannot be empty");
            }

            List<Attendance> attendanceList = attendanceRepository.findByNativeId(totalNumberOfAttendanceRequest.getFingerprintId());
            int count = 0;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime startDateTime = LocalDateTime.parse(totalNumberOfAttendanceRequest.getStartDate(), formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(totalNumberOfAttendanceRequest.getEndDate(), formatter);

            if (startDateTime.isAfter(endDateTime)) {
                throw new NativeExpection("Start date cannot be after end date");
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
        } catch (NativeExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public List<AttendanceHistoryResponse> viewNativeAttendance(NativeAttendanceRequest request) {
        try {
            if (request.getFingerprintId() == null || request.getFingerprintId().isEmpty()) {
                throw new NativeExpection("Fingerprint ID cannot be empty");
            }

            List<Attendance> attendanceList = attendanceRepository.findByNativeId(request.getFingerprintId());
            if (attendanceList.isEmpty()) {
                throw new NativeExpection("No attendance records found for native with fingerprint ID: " + request.getFingerprintId());
            }
            return attendanceList.stream()
                    .map(Mapper::attendanceHistoryResponseMapper)
                    .collect(Collectors.toList());
        } catch (NativeExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public List<AttendanceHistoryResponse> viewNativeAttendanceByTime(NativeTimeBasedAttendanceRequest request) {
        if (request.getFingerprintId() == null || request.getFingerprintId().isEmpty()) {
            attendanceMessageHandler.publishMessage("Error: Fingerprint ID cannot be empty", "response");
            throw new NativeExpection("Fingerprint ID cannot be empty");
        }

        AttendanceHistoryRequest historyRequest = new AttendanceHistoryRequest();
        historyRequest.setFingerprintId(request.getFingerprintId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        historyRequest.setStartDate(request.getStartDate().format(formatter));
        historyRequest.setEndDate(request.getEndDate().format(formatter));

        return getNativeAttendanceHistory(historyRequest);
    }
}