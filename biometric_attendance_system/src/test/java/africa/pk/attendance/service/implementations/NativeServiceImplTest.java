package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.models.Native;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.data.repositories.NativeRepository;
import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;
import africa.pk.attendance.expections.NativeExpection;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.utils.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NativeServiceImplTest {

    @Mock
    private NativeRepository nativeRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceMessageHandler attendanceMessageHandler;

    @InjectMocks
    private NativeServiceImpl nativeService;

    private RegisterNativeRequest registerNativeRequest;
    private FindNativeByFingerprintRequest findNativeByFingerprintRequest;
    private AttendanceHistoryRequest attendanceHistoryRequest;
    private TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest;
    private NativeAttendanceRequest nativeAttendanceRequest;
    private NativeTimeBasedAttendanceRequest nativeTimeBasedRequest;

    private DateTimeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        registerNativeRequest = new RegisterNativeRequest();
        registerNativeRequest.setFingerprintId("FP1");
        registerNativeRequest.setFirstName("John");
        registerNativeRequest.setLastName("Doe");
        registerNativeRequest.setEmail("john.doe@example.com");
        registerNativeRequest.setCohort("CohortA");

        findNativeByFingerprintRequest = new FindNativeByFingerprintRequest();
        findNativeByFingerprintRequest.setFingerprintId("FP1");

        attendanceHistoryRequest = new AttendanceHistoryRequest();
        attendanceHistoryRequest.setFingerprintId("FP1");
        attendanceHistoryRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        attendanceHistoryRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).toString());

        totalNumberOfAttendanceRequest = new TotalNumberOfAttendanceRequest();
        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).toString());

        nativeAttendanceRequest = new NativeAttendanceRequest();
        nativeAttendanceRequest.setFingerprintId("FP1");

        nativeTimeBasedRequest = new NativeTimeBasedAttendanceRequest();
        nativeTimeBasedRequest.setFingerprintId("FP1");
        nativeTimeBasedRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        nativeTimeBasedRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter));
    }

    @Test
    public void testRegisterNative_Success() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());
        when(nativeRepository.save(any(Native.class))).thenAnswer(invocation -> {
            Native nativePerson = invocation.getArgument(0);
            nativePerson.setId(1L);
            return nativePerson;
        });

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            Native mappedNative = new Native();
            mappedNative.setFingerprintId("FP1");
            mappedNative.setFirstName("John");
            mappedNative.setLastName("Doe");
            mappedNative.setEmail("john.doe@example.com");
            mappedNative.setCohort("CohortA");
            mockedMapper.when(() -> Mapper.map(registerNativeRequest)).thenReturn(mappedNative);

            RegisterNativeResponse mappedResponse = new RegisterNativeResponse();
            mappedResponse.setFingerprintId("FP1");
            mappedResponse.setFirstName("John");
            mappedResponse.setLastName("Doe");
            mappedResponse.setEmail("john.doe@example.com");
            mappedResponse.setCohort("CohortA");
            mappedResponse.setSuccess(true);
            mappedResponse.setMessage("Native registered successfully");
            mockedMapper.when(() -> Mapper.map(any(Native.class))).thenReturn(mappedResponse);

            RegisterNativeResponse response = nativeService.registerNative(registerNativeRequest);

            assertTrue(response.isSuccess());
            assertEquals("Native registered successfully", response.getMessage());
            assertEquals("FP1", response.getFingerprintId());
            assertEquals("John", response.getFirstName());
            assertEquals("Doe", response.getLastName());
            assertEquals("john.doe@example.com", response.getEmail());
            assertEquals("CohortA", response.getCohort());
            verify(nativeRepository, times(1)).save(any(Native.class));
        }
    }

    @Test
    public void testRegisterNative_AlreadyExists() {
        Native existingNative = new Native();
        existingNative.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(existingNative));

        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native with fingerprint ID FP1 already exists", "response");
        verify(nativeRepository, never()).save(any(Native.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRegisterNative_InvalidFingerprintId(String fingerprintId) {
        registerNativeRequest.setFingerprintId(fingerprintId);
        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        verify(nativeRepository, never()).findByFingerprintId(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRegisterNative_InvalidFirstName(String firstName) {
        registerNativeRequest.setFirstName(firstName);
        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: First name cannot be empty", "response");
        verify(nativeRepository, never()).findByFingerprintId(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRegisterNative_InvalidLastName(String lastName) {
        registerNativeRequest.setLastName(lastName);
        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Last name cannot be empty", "response");
        verify(nativeRepository, never()).findByFingerprintId(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRegisterNative_InvalidEmail(String email) {
        registerNativeRequest.setEmail(email);
        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Email cannot be empty", "response");
        verify(nativeRepository, never()).findByFingerprintId(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testRegisterNative_InvalidCohort(String cohort) {
        registerNativeRequest.setCohort(cohort);
        assertThrows(NativeExpection.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Cohort cannot be empty", "response");
        verify(nativeRepository, never()).findByFingerprintId(any());
    }

    @Test
    public void testRegisterNative_PublishMessageThrowsException() {
        registerNativeRequest.setFingerprintId(null);
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        assertThrows(RuntimeException.class, () -> nativeService.registerNative(registerNativeRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }


    @Test
    public void testFindNativeByFingerprintId_Success() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setFirstName("John");
        nativePerson.setLastName("Doe");
        nativePerson.setEmail("john.doe@example.com");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        FindNativeByFingerprintResponse mappedResponse = new FindNativeByFingerprintResponse();
        mappedResponse.setFingerprintId("FP1");
        mappedResponse.setFirstName("John");
        mappedResponse.setLastName("Doe");
        mappedResponse.setEmail("john.doe@example.com");
        mappedResponse.setCohort("CohortA");
        mappedResponse.setSuccess(true);
        mappedResponse.setMessage("Native found successfully");

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.findNativeMapper(any(Native.class))).thenReturn(mappedResponse);

            FindNativeByFingerprintResponse response = nativeService.findNativeByFingerprintId(findNativeByFingerprintRequest);

            assertTrue(response.isSuccess());
            assertEquals("Native found successfully", response.getMessage());
            assertEquals("FP1", response.getFingerprintId());
            assertEquals("John", response.getFirstName());
            assertEquals("Doe", response.getLastName());
            assertEquals("john.doe@example.com", response.getEmail());
            assertEquals("CohortA", response.getCohort());
        }
    }

    @Test
    public void testFindNativeByFingerprintId_NotFound() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(NativeExpection.class, () -> nativeService.findNativeByFingerprintId(findNativeByFingerprintRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: FP1", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testFindNativeByFingerprintId_InvalidFingerprintId(String fingerprintId) {
        findNativeByFingerprintRequest.setFingerprintId(fingerprintId);
        assertThrows(NativeExpection.class, () -> nativeService.findNativeByFingerprintId(findNativeByFingerprintRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    void testFindNativeByFingerprintId_PublishMessageThrowsException() {
        findNativeByFingerprintRequest.setFingerprintId(null);
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        assertThrows(RuntimeException.class, () -> nativeService.findNativeByFingerprintId(findNativeByFingerprintRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }


    @Test
    public void testFindNativeByFingerprintId_String_Success() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            FindNativeByFingerprintResponse mappedResponse = new FindNativeByFingerprintResponse();
            mappedResponse.setFingerprintId("FP1");
            mappedResponse.setSuccess(true);
            mockedMapper.when(() -> Mapper.findNativeMapper(any(Native.class))).thenReturn(mappedResponse);

            nativeService.findNativeByFingerprintId("FP1");

            verify(nativeRepository, times(1)).findByFingerprintId("FP1");
        }
    }

    @Test
    public void testFindNativeByFingerprintId_String_NotFound() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(NativeExpection.class, () -> nativeService.findNativeByFingerprintId("FP1"));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: FP1", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testFindNativeByFingerprintId_String_InvalidFingerprintId(String fingerprintId) {
        assertThrows(NativeExpection.class, () -> nativeService.findNativeByFingerprintId(fingerprintId));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_Success() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        attendanceHistoryRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        attendanceHistoryRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class))).thenReturn(response);

            List<AttendanceHistoryResponse> responses = nativeService.getNativeAttendanceHistory(attendanceHistoryRequest);

            assertEquals(1, responses.size());
            assertEquals("FP1", responses.get(0).getFingerprintId());
            assertEquals("John Doe", responses.get(0).getNativeName());
            assertEquals("2025-04-13T10:00:00", responses.get(0).getAttendanceDate().format(formatter));
            assertEquals("10:00:00", responses.get(0).getAttendanceTime());
        }
    }

    @Test
    public void testGetNativeAttendanceHistory_NativeNotFound() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: FP1", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_NoRecords() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");

        attendanceHistoryRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        attendanceHistoryRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1 between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_RecordsOutsideDateRange() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00", formatter));

        attendanceHistoryRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        attendanceHistoryRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1 between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_BoundaryDates() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");

        Attendance attendance1 = new Attendance();
        attendance1.setNativeId("FP1");
        attendance1.setNativeName("John Doe");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        attendance1.setAttendanceTime("00:00:00");

        Attendance attendance2 = new Attendance();
        attendance2.setNativeId("FP1");
        attendance2.setNativeName("John Doe");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter));
        attendance2.setAttendanceTime("23:59:59");

        AttendanceHistoryResponse response1 = new AttendanceHistoryResponse();
        response1.setFingerprintId("FP1");
        response1.setNativeName("John Doe");
        response1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        response1.setAttendanceTime("00:00:00");

        AttendanceHistoryResponse response2 = new AttendanceHistoryResponse();
        response2.setFingerprintId("FP1");
        response2.setNativeName("John Doe");
        response2.setAttendanceDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter));
        response2.setAttendanceTime("23:59:59");

        attendanceHistoryRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        attendanceHistoryRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Arrays.asList(attendance1, attendance2));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(attendance1)).thenReturn(response1);
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(attendance2)).thenReturn(response2);

            List<AttendanceHistoryResponse> responses = nativeService.getNativeAttendanceHistory(attendanceHistoryRequest);

            assertEquals(2, responses.size());
            assertEquals("2025-04-01T00:00:00", formatter.format(responses.get(0).getAttendanceDate()));
            assertEquals("2025-04-30T23:59:59", formatter.format(responses.get(1).getAttendanceDate()));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetNativeAttendanceHistory_InvalidFingerprintId(String fingerprintId) {
        attendanceHistoryRequest.setFingerprintId(fingerprintId);
        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetNativeAttendanceHistory_InvalidStartDate(String startDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        attendanceHistoryRequest.setStartDate(startDate);
        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be empty", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetNativeAttendanceHistory_InvalidEndDate(String endDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        attendanceHistoryRequest.setEndDate(endDate);
        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testGetNativeAttendanceHistory_InvalidDates() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        AttendanceHistoryRequest request = new AttendanceHistoryRequest();
        request.setFingerprintId("FP1");
        request.setStartDate("2025-04-30T23:59:59");
        request.setEndDate("2025-04-01T00:00:00");

        assertThrows(NativeExpection.class, () -> nativeService.getNativeAttendanceHistory(request));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be after end date", "response");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-date", "2025-13-01T00:00:00"})
    public void testGetNativeAttendanceHistory_InvalidDateFormat(String invalidDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        attendanceHistoryRequest.setStartDate(invalidDate);
        assertThrows(DateTimeParseException.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, never())
                .publishMessage(anyString(), eq("response"));
    }

    @Test
    public void testGetNativeAttendanceHistory_PublishMessageThrowsException() {
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        attendanceHistoryRequest.setFingerprintId(null);

        assertThrows(RuntimeException.class, () -> nativeService.getNativeAttendanceHistory(attendanceHistoryRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_Success() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        Attendance attendance1 = new Attendance();
        attendance1.setNativeId("FP1");
        attendance1.setNativeName("John Doe");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-15T12:00:00", formatter));
        attendance1.setAttendanceTime("12:00:00");

        Attendance attendance2 = new Attendance();
        attendance2.setNativeId("FP1");
        attendance2.setNativeName("John Doe");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-20T14:00:00", formatter));
        attendance2.setAttendanceTime("14:00:00");

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Arrays.asList(attendance1, attendance2));

        TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest = new TotalNumberOfAttendanceRequest();
        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        Long count = nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest).getTotalNumberOfAttendance();

        assertEquals(2L, count);
    }
    @Test
    public void testNativeAttendanceCount_NoRecords() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        TotalNumberOfAttendanceResponse response = nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(0, response.getTotalNumberOfAttendance());
    }
    @Test
    public void testNativeAttendanceCount_RecordsOutsideDateRange() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00", formatter));
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        TotalNumberOfAttendanceResponse response = nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(0, response.getTotalNumberOfAttendance());
    }

    @Test
    public void testNativeAttendanceCount_BoundaryDates() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        Attendance attendance1 = new Attendance();
        attendance1.setNativeId("FP1");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        Attendance attendance2 = new Attendance();
        attendance2.setNativeId("FP1");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter));

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Arrays.asList(attendance1, attendance2));

        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).format(formatter));
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-30T23:59:59", formatter).format(formatter));

        TotalNumberOfAttendanceResponse response = nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest);

        assertEquals(2, response.getTotalNumberOfAttendance());
    }
    @Test
    public void testNativeAttendanceCount_NativeNotFound() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(NativeExpection.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: FP1", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testNativeAttendanceCount_InvalidFingerprintId(String fingerprintId) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId(fingerprintId)).thenReturn(Optional.empty());

        totalNumberOfAttendanceRequest.setFingerprintId(fingerprintId);
        assertThrows(NativeExpection.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        String expectedMessage = fingerprintId == null ?
                "Error: Native not found for fingerprint ID: null" :
                "Error: Native not found for fingerprint ID: ";
        verify(attendanceMessageHandler, times(1))
                .publishMessage(expectedMessage, "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testNativeAttendanceCount_InvalidStartDate(String startDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        totalNumberOfAttendanceRequest.setStartDate(startDate);
        assertThrows(NativeExpection.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be empty", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testNativeAttendanceCount_InvalidEndDate(String endDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        totalNumberOfAttendanceRequest.setEndDate(endDate);
        assertThrows(NativeExpection.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: End date cannot be empty", "response");
    }

    @Test
    public void testNativeAttendanceCount_InvalidDates() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        totalNumberOfAttendanceRequest.setStartDate("2025-04-30T23:59:59");
        totalNumberOfAttendanceRequest.setEndDate("2025-04-01T00:00:00");

        assertThrows(NativeExpection.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Start date cannot be after end date", "response");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-date", "2025-13-01T00:00:00"})
    public void testNativeAttendanceCount_InvalidDateFormat(String invalidDate) {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        totalNumberOfAttendanceRequest.setStartDate(invalidDate);
        assertThrows(DateTimeParseException.class, () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));
    }

    @Test
    public void testNativeAttendanceCount_PublishMessageThrowsException() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setFingerprintId(null);
        when(nativeRepository.findByFingerprintId(null)).thenReturn(Optional.empty());

        NativeExpection exception = assertThrows(NativeExpection.class,
                () -> nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest));

        assertEquals("Native not found for fingerprint ID: null", exception.getMessage());

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: null", "response");
    }


    @Test
    public void testViewNativeAttendance_Success() {
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class))).thenReturn(response);

            List<AttendanceHistoryResponse> responses = nativeService.viewNativeAttendance(nativeAttendanceRequest);

            assertEquals(1, responses.size());
            assertEquals("FP1", responses.get(0).getFingerprintId());
            assertEquals("John Doe", responses.get(0).getNativeName());
            assertEquals("2025-04-13T10:00:00", responses.get(0).getAttendanceDate().format(formatter));
            assertEquals("10:00:00", responses.get(0).getAttendanceTime());
        }
    }

    @Test
    public void testViewNativeAttendance_NoRecords() {
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());
        nativeAttendanceRequest.setFingerprintId("FP1");

        NativeExpection exception = assertThrows(NativeExpection.class,
                () -> nativeService.viewNativeAttendance(nativeAttendanceRequest));

        assertEquals("No attendance records found for native with fingerprint ID: FP1", exception.getMessage());
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1", "response");
    }


    @ParameterizedTest
    @NullAndEmptySource
    public void testViewNativeAttendance_InvalidFingerprintId(String fingerprintId) {
        nativeAttendanceRequest.setFingerprintId(fingerprintId);
        assertThrows(NativeExpection.class, () -> nativeService.viewNativeAttendance(nativeAttendanceRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    public void testViewNativeAttendance_PublishMessageThrowsException() {
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
        nativeAttendanceRequest.setFingerprintId(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> nativeService.viewNativeAttendance(nativeAttendanceRequest));

        assertEquals("Message handler failed", exception.getMessage());
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

    @Test
    public void testViewNativeAttendanceByTime_Success() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00"));
        attendance.setAttendanceTime("10:00:00");

        NativeTimeBasedAttendanceRequest request = new NativeTimeBasedAttendanceRequest();
        request.setFingerprintId("FP1");

        LocalDateTime startDate = LocalDateTime.of(2025, 4, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 4, 30, 23, 59, 59);

        request.setStartDate(startDate);
        request.setEndDate(endDate);

        NativeServiceImpl serviceSpy = spy(nativeService);

        List<AttendanceHistoryResponse> expectedResponses = new ArrayList<>();
        AttendanceHistoryResponse expectedResponse = new AttendanceHistoryResponse();
        expectedResponse.setFingerprintId("FP1");
        expectedResponse.setNativeName("John Doe");
        expectedResponse.setAttendanceDate(LocalDateTime.parse("2025-04-13T10:00:00"));
        expectedResponse.setAttendanceTime("10:00:00");
        expectedResponses.add(expectedResponse);

        doReturn(expectedResponses).when(serviceSpy).getNativeAttendanceHistory(any(AttendanceHistoryRequest.class));

        List<AttendanceHistoryResponse> responses = serviceSpy.viewNativeAttendanceByTime(request);

        assertEquals(1, responses.size());
        assertEquals("FP1", responses.get(0).getFingerprintId());
        assertEquals("John Doe", responses.get(0).getNativeName());
        assertEquals(LocalDateTime.parse("2025-04-13T10:00:00"), responses.get(0).getAttendanceDate());
        assertEquals("10:00:00", responses.get(0).getAttendanceTime());

        ArgumentCaptor<AttendanceHistoryRequest> captor = ArgumentCaptor.forClass(AttendanceHistoryRequest.class);
        verify(serviceSpy).getNativeAttendanceHistory(captor.capture());

        AttendanceHistoryRequest capturedRequest = captor.getValue();
        assertEquals("FP1", capturedRequest.getFingerprintId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        assertEquals(startDate.format(formatter), capturedRequest.getStartDate());
        assertEquals(endDate.format(formatter), capturedRequest.getEndDate());
    }




    @Test
    public void testViewNativeAttendanceByTime_NativeNotFound() {
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(NativeExpection.class, () -> nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Native not found for fingerprint ID: FP1", "response");
    }

    @Test
    public void testViewNativeAttendanceByTime_NoRecords() {
        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(NativeExpection.class, () -> nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest));
        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: No attendance records found for native with fingerprint ID: FP1 between 2025-04-01T00:00:00 and 2025-04-30T23:59:59", "response");
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testViewNativeAttendanceByTime_InvalidFingerprintId(String fingerprintId) {
        NativeTimeBasedAttendanceRequest request = new NativeTimeBasedAttendanceRequest();
        request.setFingerprintId(fingerprintId);

        LocalDateTime startDate = LocalDateTime.of(2025, 4, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 4, 30, 23, 59, 59);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        assertThrows(NativeExpection.class, () -> nativeService.viewNativeAttendanceByTime(request));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }


    @Test
    public void testViewNativeAttendanceByTime_PublishMessageThrowsException() {
        doThrow(new RuntimeException("Message handler failed")).when(attendanceMessageHandler)
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");

        nativeTimeBasedRequest.setFingerprintId(null);

        assertThrows(RuntimeException.class, () -> nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest));

        verify(attendanceMessageHandler, times(1))
                .publishMessage("Error: Fingerprint ID cannot be empty", "response");
    }

}