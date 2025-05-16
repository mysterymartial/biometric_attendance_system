package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.dtos.request.AddAttendanceRequest;
import africa.pk.attendance.dtos.request.AttendanceHistoryRequest;
import africa.pk.attendance.dtos.request.TotalNumberOfAttendanceRequest;
import africa.pk.attendance.dtos.response.AddAttendanceResponse;
import africa.pk.attendance.dtos.response.AttendanceHistoryResponse;
import africa.pk.attendance.dtos.response.FindNativeByFingerprintResponse;
import africa.pk.attendance.dtos.response.TotalNumberOfAttendanceResponse;
import africa.pk.attendance.expections.AttendanceExpection;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.NativeService;
import africa.pk.attendance.utils.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private NativeService nativeService;

    @Mock
    private AttendanceMessageHandler attendanceMessageHandler;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private AddAttendanceRequest addAttendanceRequest;
    private AttendanceHistoryRequest attendanceHistoryRequest;
    private TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest;

    @BeforeEach
    public void setUp() {
        addAttendanceRequest = new AddAttendanceRequest();
        addAttendanceRequest.setFingerprintId("FP1");
        addAttendanceRequest.setNativeName("John Doe");
        addAttendanceRequest.setAttendanceDate("2025-04-13T10:00:00");
        addAttendanceRequest.setAttendanceTime("10:00:00");

        attendanceHistoryRequest = new AttendanceHistoryRequest();
        attendanceHistoryRequest.setFingerprintId("FP1");
        attendanceHistoryRequest.setStartDate("2025-04-01T00:00:00");
        attendanceHistoryRequest.setEndDate("2025-04-30T23:59:59");

        totalNumberOfAttendanceRequest = new TotalNumberOfAttendanceRequest();
        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate("2025-04-01T00:00:00");
        totalNumberOfAttendanceRequest.setEndDate("2025-04-30T23:59:59");
    }

    @Test
    public void testAddAttendance_Success() {
        AddAttendanceRequest addAttendanceRequest = new AddAttendanceRequest();
        addAttendanceRequest.setFingerprintId("FP1");
        addAttendanceRequest.setNativeName("John Doe");
        addAttendanceRequest.setAttendanceDate("2025-04-13T10:15:30");
        addAttendanceRequest.setAttendanceTime("10:15:30");

        FindNativeByFingerprintResponse findResponse = new FindNativeByFingerprintResponse();
        findResponse.setSuccess(true);
        findResponse.setMessage("Native found");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(findResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setAttendanceId(1L);
            attendance.setStatus("Present");
            return attendance;
        });

        AddAttendanceResponse response = attendanceService.addAttendance(addAttendanceRequest);

        assertTrue(response.isSuccess());
        assertEquals("Attendance recorded successfully for John Doe", response.getMessage());
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(attendanceMessageHandler, never()).publishMessage(anyString(), anyString());
    }

    @Test
    public void testAddAttendance_NullFingerprintId() {
        addAttendanceRequest.setFingerprintId(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_EmptyFingerprintId() {
        addAttendanceRequest.setFingerprintId("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_NullNativeName() {
        addAttendanceRequest.setNativeName(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native name field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_EmptyNativeName() {
        addAttendanceRequest.setNativeName("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native name field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_NullAttendanceDate() {
        addAttendanceRequest.setAttendanceDate(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Attendance date field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_EmptyAttendanceDate() {
        addAttendanceRequest.setAttendanceDate("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Attendance date field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_NullAttendanceTime() {
        addAttendanceRequest.setAttendanceTime(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Attendance time field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_EmptyAttendanceTime() {
        addAttendanceRequest.setAttendanceTime("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Attendance time field is empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testAddAttendance_NativeNotFound() {
        doThrow(new AttendanceExpection("Native not found")).when(nativeService).findNativeByFingerprintId("FP1"); // Fix: Use doThrow for void method
        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found", "response");
        verify(attendanceRepository, never()).findByNativeId(anyString());
    }

    @Test
    public void testAddAttendance_AlreadyRecordedToday() {
        Attendance existingAttendance = new Attendance();
        existingAttendance.setNativeId("FP1");
        existingAttendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T08:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        existingAttendance.setStatus("Present");

        FindNativeByFingerprintResponse findResponse = new FindNativeByFingerprintResponse();
        findResponse.setSuccess(true);
        findResponse.setMessage("Native found");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(findResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(existingAttendance));

        assertThrows(AttendanceExpection.class, () -> attendanceService.addAttendance(addAttendanceRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: John Doe has already recorded attendance for today", "response");
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    public void testAddAttendance_DifferentDay_Success() {
        Attendance existingAttendance = new Attendance();
        existingAttendance.setNativeId("FP1");
        existingAttendance.setAttendanceDate(LocalDateTime.parse("2025-04-12T08:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        existingAttendance.setStatus("Present");

        FindNativeByFingerprintResponse findResponse = new FindNativeByFingerprintResponse();
        findResponse.setSuccess(true);
        findResponse.setMessage("Native found");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(findResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(existingAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setStatus("Present");
            return attendance;
        });
        AddAttendanceResponse response = attendanceService.addAttendance(addAttendanceRequest);

        assertTrue(response.isSuccess());
        assertEquals("Attendance recorded successfully for John Doe", response.getMessage());
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    public void testAddAttendance_PublishMessageThrowsException() {
        addAttendanceRequest.setFingerprintId(null);
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID field is empty", "response");
        assertThrows(RuntimeException.class, () -> attendanceService.addAttendance(addAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID field is empty", "response");
    }

    @Test
    public void testAddAttendance_InvalidDateFormat() {
        addAttendanceRequest.setAttendanceDate("2025-04-13"); // Invalid format - missing time part

        assertThrows(DateTimeParseException.class, () -> attendanceService.addAttendance(addAttendanceRequest));
    }

    @Test
    public void testGetNativeAttendanceHistory_Success() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance.setAttendanceTime("10:00:00");
        attendance.setStatus("Present");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        response.setAttendanceTime("10:00:00");
        response.setStatus("Present");

        attendanceHistoryRequest.setFingerprintId("FP1");
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        List<AttendanceHistoryResponse> responses = attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest);

        assertEquals(1, responses.size());
        assertEquals("FP1", responses.get(0).getFingerprintId());
        assertEquals("John Doe", responses.get(0).getNativeName());
        assertTrue(responses.get(0).getAttendanceDate().toString().contains("2025-04-13"));
        assertEquals("10:00:00", responses.get(0).getAttendanceTime());
        assertEquals("Present", responses.get(0).getStatus());
    }

    @Test
    public void testGetNativeAttendanceHistory_NullFingerprintId() {
        attendanceHistoryRequest.setFingerprintId(null);

        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: null between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");

        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testGetNativeAttendanceHistory_EmptyFingerprintId() {
        attendanceHistoryRequest.setFingerprintId("");

        when(nativeService.findNativeByFingerprintId("")).thenThrow(new AttendanceExpection("Fingerprint ID cannot be empty"));

        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");

        verify(nativeService, times(1)).findNativeByFingerprintId("");
    }

    @Test
    public void testGetNativeAttendanceHistory_NativeNotFound() {
        doThrow(new AttendanceExpection("Native not found")).when(nativeService).findNativeByFingerprintId("FP1"); // Fix: Use doThrow for void method
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_NoRecords() {
        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(null);
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1 between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_RecordsOutsideDateRange() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance.setStatus("Present");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(null);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1 between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_NullStartDate() {
        attendanceHistoryRequest.setStartDate(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_EmptyStartDate() {
        attendanceHistoryRequest.setStartDate("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_NullEndDate() {
        attendanceHistoryRequest.setEndDate(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_EmptyEndDate() {
        attendanceHistoryRequest.setEndDate("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_InvalidDates() {
        AttendanceHistoryRequest request = new AttendanceHistoryRequest();
        request.setFingerprintId("FP1");
        request.setStartDate("2025-04-30T23:59:59");
        request.setEndDate("2025-04-01T00:00:00");

        FindNativeByFingerprintResponse response = new FindNativeByFingerprintResponse();
        response.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(response);

        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(request));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be after end date", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_PublishMessageThrowsException() {
        attendanceHistoryRequest.setFingerprintId(null);

        attendanceHistoryRequest.setStartDate("2025-04-01T00:00:00");
        attendanceHistoryRequest.setEndDate("2025-04-30T23:59:59");

        String expectedErrorMessage = "Error: No attendance records found for native with fingerprint ID: null between 2025-04-01T00:00:00 and 2025-04-30T23:59:59";

        doThrow(new RuntimeException("Message handler failed"))
                .when(attendanceMessageHandler)
                .publishMessage(expectedErrorMessage, "response");

        assertThrows(RuntimeException.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage(expectedErrorMessage, "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_InvalidStartDateFormat() {
        attendanceHistoryRequest.setStartDate("2025-04-01");

        FindNativeByFingerprintResponse response = new FindNativeByFingerprintResponse();
        response.setFingerprintId("FP1");
        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(response);

        assertThrows(AttendanceExpection.class, () -> attendanceService.getNativeAttendanceHistory(attendanceHistoryRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage(eq("Error: Text '2025-04-01' could not be parsed at index 10"), eq("response"));
    }

    @Test
    public void testNativeAttendanceCount_Success() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance.setStatus("Present");

        FindNativeByFingerprintResponse nativeResponse = new FindNativeByFingerprintResponse();
        nativeResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(nativeResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        TotalNumberOfAttendanceResponse response = attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(1, response.getTotalNumberOfAttendance());
    }

    @Test
    public void testNativeAttendanceCount_NullFingerprintId() {
        totalNumberOfAttendanceRequest.setFingerprintId(null);

        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");

        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testNativeAttendanceCount_EmptyFingerprintId() {
        totalNumberOfAttendanceRequest.setFingerprintId("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        verify(nativeService, never()).findNativeByFingerprintId(anyString());
    }

    @Test
    public void testNativeAttendanceCount_NativeNotFound() {
        doThrow(new AttendanceExpection("Native not found")).when(nativeService).findNativeByFingerprintId("FP1"); // Fix: Use doThrow for void method
        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found", "response");
    }

    @Test
    public void testNativeAttendanceCount_NoRecords() {
        FindNativeByFingerprintResponse mockResponse = new FindNativeByFingerprintResponse();
        mockResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(mockResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());
        TotalNumberOfAttendanceResponse response = attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(0, response.getTotalNumberOfAttendance());
    }

    @Test
    public void testNativeAttendanceCount_RecordsOutsideDateRange() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance.setStatus("Present");

        FindNativeByFingerprintResponse mockResponse = new FindNativeByFingerprintResponse();
        mockResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(mockResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));
        TotalNumberOfAttendanceResponse response = attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(0, response.getTotalNumberOfAttendance());
    }

    @Test
    public void testNativeAttendanceCount_BoundaryDates() {
        Attendance attendance1 = new Attendance();
        attendance1.setNativeId("FP1");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance1.setStatus("Present");

        Attendance attendance2 = new Attendance();
        attendance2.setNativeId("FP1");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-30T23:59:59",
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance2.setStatus("Present");

        FindNativeByFingerprintResponse mockResponse = new FindNativeByFingerprintResponse();
        mockResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(mockResponse);

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Arrays.asList(attendance1, attendance2));

        TotalNumberOfAttendanceResponse response = attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(2, response.getTotalNumberOfAttendance());
    }

    @Test
    public void testNativeAttendanceCount_NullStartDate() {
        totalNumberOfAttendanceRequest.setStartDate(null);

        AttendanceExpection exception = assertThrows(AttendanceExpection.class,
                () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));

        assertEquals("Start date cannot be empty", exception.getMessage());
    }

    @Test
    public void testNativeAttendanceCount_EmptyStartDate() {
        totalNumberOfAttendanceRequest.setStartDate("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_NullEndDate() {
        totalNumberOfAttendanceRequest.setEndDate(null);
        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_EmptyEndDate() {
        totalNumberOfAttendanceRequest.setEndDate("");
        assertThrows(AttendanceExpection.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_InvalidDates() {
        totalNumberOfAttendanceRequest.setStartDate("2025-04-30T23:59:59");
        totalNumberOfAttendanceRequest.setEndDate("2025-04-01T00:00:00");

        totalNumberOfAttendanceRequest.setFingerprintId("FP1");

        FindNativeByFingerprintResponse mockResponse = new FindNativeByFingerprintResponse();
        mockResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(mockResponse);

        assertThrows(AttendanceExpection.class, () ->
                attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest)
        );
    }

    @Test
    public void testNativeAttendanceCount_PublishMessageThrowsException() {
        totalNumberOfAttendanceRequest.setFingerprintId(null);
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        assertThrows(RuntimeException.class, () -> attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_InvalidStartDateFormat() {
        totalNumberOfAttendanceRequest.setStartDate("2025-04-01");
        totalNumberOfAttendanceRequest.setFingerprintId("FP1");

        FindNativeByFingerprintResponse mockResponse = new FindNativeByFingerprintResponse();
        mockResponse.setFingerprintId("FP1");

        when(nativeService.findNativeByFingerprintId("FP1")).thenReturn(mockResponse);

        assertThrows(AttendanceExpection.class, () ->
                attendanceService.nativeAttendanceCount(totalNumberOfAttendanceRequest)
        );

        verify(attendanceMessageHandler, times(1))
                .publishMessage(
                        contains("2025-04-01"),
                        eq("response")
                );
    }

    @Test
    void testViewAllAttendance_Success() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        attendance.setAttendanceTime("10:00:00");
        attendance.setStatus("Present");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        response.setAttendanceTime("10:00:00");
        response.setStatus("Present");

        when(attendanceRepository.findAll()).thenReturn(Collections.singletonList(attendance));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class)))
                    .thenReturn(response);

            List<AttendanceHistoryResponse> responses = attendanceService.viewAllAttendance();

            assertEquals(1, responses.size());
            assertEquals("FP1", responses.get(0).getFingerprintId());
            assertEquals("John Doe", responses.get(0).getNativeName());
            assertEquals(LocalDateTime.parse("2025-04-13T10:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                    responses.get(0).getAttendanceDate());
            assertEquals("10:00:00", responses.get(0).getAttendanceTime());
            assertEquals("Present", responses.get(0).getStatus());
        }
    }

    @Test
    void testViewAllAttendance_NoRecords() {
        when(attendanceRepository.findAll()).thenReturn(Collections.emptyList());

        AttendanceExpection exception = assertThrows(AttendanceExpection.class,
                () -> attendanceService.viewAllAttendance());

        assertEquals("No attendance records found", exception.getMessage());
    }

    @Test
    public void testViewAllAttendance_Of_Empty_Attendance_Record() {
        when(attendanceRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> attendanceService.viewAllAttendance());
    }
}