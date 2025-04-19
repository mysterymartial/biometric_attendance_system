package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Admin;
import africa.pk.attendance.data.models.Attendance;
import africa.pk.attendance.data.models.Native;
import africa.pk.attendance.data.repositories.AdminRepository;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.data.repositories.NativeRepository;
import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;

import africa.pk.attendance.expections.AdminExpection;
import africa.pk.attendance.expections.AttendanceExpection;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.AttendanceService;
import africa.pk.attendance.service.interfaces.NativeService;
import africa.pk.attendance.utils.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Added for password hashing

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private NativeRepository nativeRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private DailyAttendanceStatusRepository dailyAttendanceStatusRepository;

    @Mock
    private NativeService nativeService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AttendanceMessageHandler attendanceMessageHandler;

    @Mock
    private BCryptPasswordEncoder passwordEncoder; // Added to fix NullPointerException

    @InjectMocks
    private AdminServiceImpl adminService;

    private RegisterAdminRequest registerAdminRequest;
    private LoginAdminRequest loginAdminRequest;
    private CohortAttendanceRequest cohortAttendanceRequest;
    private AllTimeBasedAttendanceRequest allTimeBasedRequest;
    private CohortTimeBasedAttendanceRequest cohortTimeBasedRequest;
    private NativeAttendanceRequest nativeAttendanceRequest;
    private NativeTimeBasedAttendanceRequest nativeTimeBasedRequest;
    private TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest;
    private AttendancePercentageRequest attendancePercentageRequest;
    private LogoutAdminRequest logoutAdminRequest;

    private DateTimeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        registerAdminRequest = new RegisterAdminRequest();
        registerAdminRequest.setUserName("admin1");
        registerAdminRequest.setFirstName("John");
        registerAdminRequest.setLastName("Doe");
        registerAdminRequest.setPassword("password123");

        loginAdminRequest = new LoginAdminRequest();
        loginAdminRequest.setUserName("admin1");
        loginAdminRequest.setPassword("password123");

        cohortAttendanceRequest = new CohortAttendanceRequest();
        cohortAttendanceRequest.setCohort("CohortA");
        cohortTimeBasedRequest = new CohortTimeBasedAttendanceRequest();
        cohortTimeBasedRequest.setCohort("CohortA");
        cohortTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        cohortTimeBasedRequest.setEndDate("2025-04-01T23:59:59");

        allTimeBasedRequest = new AllTimeBasedAttendanceRequest();
        allTimeBasedRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        allTimeBasedRequest.setEndDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter).toString());

        cohortTimeBasedRequest = new CohortTimeBasedAttendanceRequest();
        cohortTimeBasedRequest.setCohort("CohortA");
        cohortTimeBasedRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        cohortTimeBasedRequest.setEndDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter).toString());

        nativeAttendanceRequest = new NativeAttendanceRequest();
        nativeAttendanceRequest.setFingerprintId("FP1");

        nativeTimeBasedRequest = new NativeTimeBasedAttendanceRequest();
        nativeTimeBasedRequest.setFingerprintId("FP1");
        nativeTimeBasedRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        nativeTimeBasedRequest.setEndDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter));

        totalNumberOfAttendanceRequest = new TotalNumberOfAttendanceRequest();
        totalNumberOfAttendanceRequest.setFingerprintId("FP1");
        totalNumberOfAttendanceRequest.setStartDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        totalNumberOfAttendanceRequest.setEndDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter).toString());

        attendancePercentageRequest = new AttendancePercentageRequest();
        attendancePercentageRequest.setFingerprintId("FP1");
        attendancePercentageRequest.setStartDate("2025-04-01T00:00:00");
        attendancePercentageRequest.setEndDate("2025-04-07T23:59:59");

        logoutAdminRequest = new LogoutAdminRequest();
        logoutAdminRequest.setUsername("admin1");
    }

    @Test
    void testRegisterAdmin_Success() {
        // Mock passwordEncoder to return a hashed password
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$mockedHashedPassword");
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.empty());
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterAdminResponse response = adminService.registerAdmin(registerAdminRequest);

        assertTrue(response.isSuccess());
        assertEquals("Admin registered successfully", response.getMessage());
        assertEquals("admin1", response.getUserName());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(passwordEncoder, times(1)).encode("password123"); // Verify encoder was called
    }

    @Test
    public void testRegisterAdmin_UsernameExists() {
        Admin existingAdmin = new Admin();
        existingAdmin.setUserName("admin1");
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.of(existingAdmin));

        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testRegisterAdmin_NullUserName() {
        registerAdminRequest.setUserName(null);

        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_EmptyUserName() {
        registerAdminRequest.setUserName("");
        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_NullFirstName() {
        registerAdminRequest.setFirstName(null);
        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_EmptyFirstName() {
        registerAdminRequest.setFirstName("");
        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_NullLastName() {
        registerAdminRequest.setLastName(null);
        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_EmptyLastName() {
        registerAdminRequest.setLastName("");

        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_NullPassword() {
        registerAdminRequest.setPassword(null);

        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testRegisterAdmin_EmptyPassword() {
        registerAdminRequest.setPassword("");

        assertThrows(AdminExpection.class, () -> adminService.registerAdmin(registerAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testLoginAdmin_Success() {
        Admin admin = new Admin();
        admin.setUserName("admin1");
        admin.setPassword("$2a$10$mockedHashedPassword"); // Use hashed password
        admin.setFirstName("John");
        admin.setLastName("Doe");
        admin.setIsLoggedIn(false);
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password123", "$2a$10$mockedHashedPassword")).thenReturn(true); // Mock password match
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAdminResponse response = adminService.loginAdmin(loginAdminRequest);

        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("admin1", response.getUserName());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertTrue(admin.getIsLoggedIn());
        verify(adminRepository, times(1)).save(any(Admin.class));
        verify(passwordEncoder, times(1)).matches("password123", "$2a$10$mockedHashedPassword"); // Verify matches was called
    }

    @Test
    void testLoginAdmin_UserNotFound() {
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.empty());
        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testLoginAdmin_WrongPassword() {
        Admin admin = new Admin();
        admin.setUserName("admin1");
        admin.setPassword("$2a$10$mockedHashedPassword"); // Use hashed password
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password123", "$2a$10$mockedHashedPassword")).thenReturn(false); // Mock failed password match

        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));

        verify(adminRepository, never()).save(any(Admin.class));
        verify(passwordEncoder, times(1)).matches("password123", "$2a$10$mockedHashedPassword");
    }

    @Test
    void testLoginAdmin_NullUserName() {
        loginAdminRequest.setUserName(null);

        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testLoginAdmin_EmptyUserName() {
        loginAdminRequest.setUserName("");

        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));

        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testLoginAdmin_NullPassword() {
        loginAdminRequest.setPassword(null);
        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testLoginAdmin_EmptyPassword() {
        loginAdminRequest.setPassword("");
        assertThrows(AdminExpection.class, () -> adminService.loginAdmin(loginAdminRequest));
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testAddNative_Success() {
        RegisterNativeRequest request = new RegisterNativeRequest();
        request.setFingerprintId("FP1");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setCohort("CohortA");

        RegisterNativeResponse response = new RegisterNativeResponse();
        response.setFingerprintId("FP1");
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john.doe@example.com");
        response.setCohort("CohortA");
        response.setSuccess(true);
        response.setMessage("Native registered successfully");

        when(nativeService.registerNative(request)).thenReturn(response);

        RegisterNativeResponse result = adminService.addNative(request);
        assertTrue(result.isSuccess());
        assertEquals("Native registered successfully", result.getMessage());
        assertEquals("FP1", result.getFingerprintId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("CohortA", result.getCohort());
        verify(nativeService, times(1)).registerNative(request);
    }

    @Test
    void testAddNative_Failure() {
        RegisterNativeRequest request = new RegisterNativeRequest();
        when(nativeService.registerNative(request)).thenThrow(new AdminExpection("Invalid native data"));

        assertThrows(AdminExpection.class, () -> adminService.addNative(request));
        verify(nativeService, times(1)).registerNative(request);
    }

    @Test
    void testViewAllAttendance_Success() {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(response));

        List<AttendanceHistoryResponse> result = adminService.viewAllAttendance();

        assertEquals(1, result.size());
        assertEquals("FP1", result.get(0).getFingerprintId());
        assertEquals("John Doe", result.get(0).getNativeName());

        assertEquals(LocalDateTime.parse("2025-04-02T10:00"), result.get(0).getAttendanceDate());

        assertEquals("10:00:00", result.get(0).getAttendanceTime());
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendance_NoRecords() {
        when(attendanceService.viewAllAttendance()).thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendance());
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceByTime_Success() {
        AttendanceHistoryResponse attendance = new AttendanceHistoryResponse();
        attendance.setFingerprintId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(attendance));

        allTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        allTimeBasedRequest.setEndDate("2025-04-03T23:59:59");

        List<AttendanceHistoryResponse> result = adminService.viewAllAttendanceByTime(allTimeBasedRequest);

        assertEquals(1, result.size());
        assertEquals("FP1", result.get(0).getFingerprintId());
        assertEquals("John Doe", result.get(0).getNativeName());

        LocalDateTime expectedDateTime = LocalDateTime.parse("2025-04-02T10:00:00", formatter);
        assertEquals(expectedDateTime.toString().substring(0, 16), result.get(0).getAttendanceDate().toString());

        assertEquals("10:00:00", result.get(0).getAttendanceTime());
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceByTime_RecordsOutsideDateRange() {
        AttendanceHistoryResponse attendance = new AttendanceHistoryResponse();
        attendance.setFingerprintId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00", formatter));

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(attendance));

        allTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        allTimeBasedRequest.setEndDate("2025-04-30T23:59:59");

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceByTime_BoundaryDates() {
        AttendanceHistoryResponse attendance1 = new AttendanceHistoryResponse();
        attendance1.setFingerprintId("FP1");
        attendance1.setNativeName("John Doe");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        attendance1.setAttendanceTime("00:00:00");

        AttendanceHistoryResponse attendance2 = new AttendanceHistoryResponse();
        attendance2.setFingerprintId("FP2");
        attendance2.setNativeName("Jane Doe");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter));
        attendance2.setAttendanceTime("23:59:59");

        allTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        allTimeBasedRequest.setEndDate("2025-04-05T23:59:59");

        when(attendanceService.viewAllAttendance()).thenReturn(Arrays.asList(attendance1, attendance2));

        List<AttendanceHistoryResponse> result = adminService.viewAllAttendanceByTime(allTimeBasedRequest);

        assertEquals(2, result.size());

        assertEquals("2025-04-01T00:00", result.get(0).getAttendanceDate().toString());
        assertEquals("2025-04-05T23:59:59", result.get(1).getAttendanceDate().toString());

        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceByTime_NoRecords() {
        allTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        allTimeBasedRequest.setEndDate("2025-04-05T23:59:59");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));

        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceByTime_NullStartDate() {
        allTimeBasedRequest.setStartDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
    }

    @Test
    void testViewAllAttendanceByTime_EmptyStartDate() {
        allTimeBasedRequest.setStartDate("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
    }

    @Test
    void testViewAllAttendanceByTime_NullEndDate() {
        allTimeBasedRequest.setEndDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
    }

    @Test
    void testViewAllAttendanceByTime_EmptyEndDate() {
        allTimeBasedRequest.setEndDate("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
    }

    @Test
    void testViewAllAttendanceByTime_InvalidDates() {
        allTimeBasedRequest.setStartDate("2025-04-05T23:59:59");
        allTimeBasedRequest.setEndDate("2025-04-01T00:00:00");

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));
    }

    @Test
    void testViewAllAttendanceByTime_InvalidStartDateFormat() {
        allTimeBasedRequest.setStartDate("2025-04-01");
        allTimeBasedRequest.setEndDate("2025-04-05T23:59:59");

        Exception exception = assertThrows(DateTimeParseException.class,
                () -> adminService.viewAllAttendanceByTime(allTimeBasedRequest));

        assertTrue(exception.getMessage().contains("could not be parsed") ||
                exception.getMessage().contains("parse"));
    }

    @Test
    void testViewCohortAttendance_Success() {
        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Dan");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        cohortAttendanceRequest.setCohort("Cohort21");

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class)))
                    .thenReturn(response);

            List<AttendanceHistoryResponse> result = adminService.viewCohortAttendance(cohortAttendanceRequest);
            assertEquals(1, result.size());
            assertEquals("FP1", result.get(0).getFingerprintId());
            assertEquals("John Dan", result.get(0).getNativeName());

            LocalDateTime expectedDate = LocalDateTime.parse("2025-04-02T10:00:00", formatter);
            LocalDateTime actualDate = result.get(0).getAttendanceDate();

            assertEquals(expectedDate.getYear(), actualDate.getYear());
            assertEquals(expectedDate.getMonth(), actualDate.getMonth());
            assertEquals(expectedDate.getDayOfMonth(), actualDate.getDayOfMonth());
            assertEquals(expectedDate.getHour(), actualDate.getHour());
            assertEquals(expectedDate.getMinute(), actualDate.getMinute());

            assertEquals("10:00:00", result.get(0).getAttendanceTime());
        }
    }

    @Test
    void testViewCohortAttendance_NoNatives() {
        when(nativeRepository.findByCohort("CohortA")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendance(cohortAttendanceRequest));
    }

    @Test
    void testViewCohortAttendance_NoRecords() {
        Native anative = new Native();
        anative.setFingerprintId("FP1");
        when(nativeRepository.findByCohort("CohortA")).thenReturn(Collections.singletonList(anative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendance(cohortAttendanceRequest));
    }

    @Test
    void testViewCohortAttendance_NullCohort() {
        cohortAttendanceRequest.setCohort(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendance(cohortAttendanceRequest));
    }

    @Test
    void testViewCohortAttendance_EmptyCohort() {
        cohortAttendanceRequest.setCohort("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendance(cohortAttendanceRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_Success() {
        Native anative = new Native();
        anative.setFingerprintId("FP1");
        anative.setCohort("Cohort21");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Dan");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Dan");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        cohortTimeBasedRequest.setCohort("Cohort21");
        cohortTimeBasedRequest.setStartDate("2025-04-02T09:00:00");
        cohortTimeBasedRequest.setEndDate("2025-04-02T11:00:00");

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(anative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class)))
                    .thenReturn(response);
            List<AttendanceHistoryResponse> result = adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest);

            assertEquals(1, result.size());
            assertEquals("FP1", result.get(0).getFingerprintId());
            assertEquals("John Dan", result.get(0).getNativeName());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            assertEquals(
                    LocalDateTime.parse("2025-04-02T10:00:00", formatter).format(dateFormatter),
                    result.get(0).getAttendanceDate().format(dateFormatter)
            );

            assertEquals("10:00:00", result.get(0).getAttendanceTime());
        }
    }

    @Test
    void testViewCohortAttendanceByTime_NoNatives() {
        when(nativeRepository.findByCohort("CohortA")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_NoRecords() {
        cohortTimeBasedRequest.setCohort("Cohort21");
        cohortTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        cohortTimeBasedRequest.setEndDate("2025-04-01T23:59:59");

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_RecordsOutsideDateRange() {
        cohortTimeBasedRequest.setCohort("Cohort21");
        cohortTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        cohortTimeBasedRequest.setEndDate("2025-04-01T23:59:59");

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-03-01T10:00:00", formatter));

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_BoundaryDates() {
        cohortTimeBasedRequest.setCohort("Cohort21");
        cohortTimeBasedRequest.setStartDate("2025-04-01T00:00:00");
        cohortTimeBasedRequest.setEndDate("2025-04-05T23:59:59");

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        Attendance attendance1 = new Attendance();
        attendance1.setNativeId("FP1");
        attendance1.setNativeName("John Dan");
        attendance1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        attendance1.setAttendanceTime("00:00:00");

        Attendance attendance2 = new Attendance();
        attendance2.setNativeId("FP1");
        attendance2.setNativeName("John Dan");
        attendance2.setAttendanceDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter));
        attendance2.setAttendanceTime("23:59:59");

        AttendanceHistoryResponse response1 = new AttendanceHistoryResponse();
        response1.setFingerprintId("FP1");
        response1.setNativeName("John Dan");
        response1.setAttendanceDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter));
        response1.setAttendanceTime("00:00:00");

        AttendanceHistoryResponse response2 = new AttendanceHistoryResponse();
        response2.setFingerprintId("FP1");
        response2.setNativeName("John Dan");
        response2.setAttendanceDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter));
        response2.setAttendanceTime("23:59:59");

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Arrays.asList(attendance1, attendance2));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(attendance1)).thenReturn(response1);
            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(attendance2)).thenReturn(response2);

            List<AttendanceHistoryResponse> result = adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest);

            assertEquals(2, result.size());

            DateTimeFormatter compareFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

            assertEquals("2025-04-01T00:00", result.get(0).getAttendanceDate().format(compareFormatter));
            assertEquals("2025-04-05T23:59", result.get(1).getAttendanceDate().format(compareFormatter));
        }
    }

    @Test
    void testViewCohortAttendanceByTime_NullCohort() {
        cohortTimeBasedRequest.setCohort(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_EmptyCohort() {
        cohortTimeBasedRequest.setCohort("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_NullStartDate() {
        cohortTimeBasedRequest.setStartDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_EmptyStartDate() {
        cohortTimeBasedRequest.setStartDate("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_NullEndDate() {
        cohortTimeBasedRequest.setEndDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_EmptyEndDate() {
        cohortTimeBasedRequest.setEndDate("");
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_InvalidDates() {
        cohortTimeBasedRequest.setStartDate(LocalDateTime.parse("2025-04-05T23:59:59", formatter).toString());
        cohortTimeBasedRequest.setEndDate(LocalDateTime.parse("2025-04-01T00:00:00", formatter).toString());
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewCohortAttendanceByTime_InvalidStartDateFormat() {
        cohortTimeBasedRequest.setStartDate("2025-04-01");
        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTime(cohortTimeBasedRequest));
    }

    @Test
    void testViewNativeAttendance_Success() {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(nativeService.viewNativeAttendance(nativeAttendanceRequest)).thenReturn(Collections.singletonList(response));

        List<AttendanceHistoryResponse> result = adminService.viewNativeAttendance(nativeAttendanceRequest);

        assertEquals(1, result.size());
        assertEquals("FP1", result.get(0).getFingerprintId());
        assertEquals("John Doe", result.get(0).getNativeName());

        DateTimeFormatter compareFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        assertEquals("2025-04-02T10:00", result.get(0).getAttendanceDate().format(compareFormatter));

        assertEquals(LocalDateTime.parse("2025-04-02T10:00:00", formatter), result.get(0).getAttendanceDate());

        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        assertEquals("2025-04-02T10:00:00", result.get(0).getAttendanceDate().format(fullFormatter));

        assertEquals("10:00:00", result.get(0).getAttendanceTime());
        verify(nativeService, times(1)).viewNativeAttendance(nativeAttendanceRequest);
    }

    @Test
    void testViewNativeAttendance_NoRecords() {
        when(nativeService.viewNativeAttendance(nativeAttendanceRequest))
                .thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendance(nativeAttendanceRequest));
        verify(nativeService, times(1)).viewNativeAttendance(nativeAttendanceRequest);
    }

    @Test
    void testViewNativeAttendance_NullFingerprintId() {
        nativeAttendanceRequest.setFingerprintId(null);
        when(nativeService.viewNativeAttendance(nativeAttendanceRequest))
                .thenThrow(new AttendanceExpection("Fingerprint ID cannot be empty"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendance(nativeAttendanceRequest));
        verify(nativeService, times(1)).viewNativeAttendance(nativeAttendanceRequest);
    }

    @Test
    void testViewNativeAttendanceByTime_Success() {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest)).thenReturn(Collections.singletonList(response));

        List<AttendanceHistoryResponse> result = adminService.viewNativeAttendanceByTime(nativeTimeBasedRequest);

        assertEquals(1, result.size());
        assertEquals("FP1", result.get(0).getFingerprintId());
        assertEquals("John Doe", result.get(0).getNativeName());

        assertEquals(LocalDateTime.parse("2025-04-02T10:00:00", formatter), result.get(0).getAttendanceDate());

        assertEquals("10:00:00", result.get(0).getAttendanceTime());
        verify(nativeService, times(1)).viewNativeAttendanceByTime(nativeTimeBasedRequest);
    }

    @Test
    void testViewNativeAttendanceByTime_NoRecords() {
        when(nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest))
                .thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendanceByTime(nativeTimeBasedRequest));
        verify(nativeService, times(1)).viewNativeAttendanceByTime(nativeTimeBasedRequest);
    }

    @Test
    void testViewNativeAttendanceByTime_NullFingerprintId() {
        nativeTimeBasedRequest.setFingerprintId(null);
        when(nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest))
                .thenThrow(new AttendanceExpection("Fingerprint ID cannot be empty"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendanceByTime(nativeTimeBasedRequest));
        verify(nativeService, times(1)).viewNativeAttendanceByTime(nativeTimeBasedRequest);
    }

    @Test
    void testViewNativeAttendanceCount_Success() {
        TotalNumberOfAttendanceResponse response = new TotalNumberOfAttendanceResponse();
        response.setTotalNumberOfAttendance(5);
        when(nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest)).thenReturn(response);

        TotalNumberOfAttendanceResponse result = adminService.viewNativeAttendanceCount(totalNumberOfAttendanceRequest);
        assertEquals(5, result.getTotalNumberOfAttendance());
        verify(nativeService, times(1)).nativeAttendanceCount(totalNumberOfAttendanceRequest);
    }

    @Test
    void testViewNativeAttendanceCount_NoRecords() {
        TotalNumberOfAttendanceResponse response = new TotalNumberOfAttendanceResponse();
        response.setTotalNumberOfAttendance(0);
        when(nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest)).thenReturn(response);

        TotalNumberOfAttendanceResponse result = adminService.viewNativeAttendanceCount(totalNumberOfAttendanceRequest);
        assertEquals(0, result.getTotalNumberOfAttendance());
        verify(nativeService, times(1)).nativeAttendanceCount(totalNumberOfAttendanceRequest);
    }

    @Test
    void testViewNativeAttendanceCount_NullFingerprintId() {
        totalNumberOfAttendanceRequest.setFingerprintId(null);
        when(nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest))
                .thenThrow(new AttendanceExpection("Fingerprint ID cannot be empty"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendanceCount(totalNumberOfAttendanceRequest));
        verify(nativeService, times(1)).nativeAttendanceCount(totalNumberOfAttendanceRequest);
    }

    @Test
    void testViewAllAttendanceAsExcel_Success() throws Exception {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(response));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        byte[] result = adminService.viewAllAttendanceAsExcel();
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(attendanceService, times(1)).viewAllAttendance();
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewAllAttendanceAsExcel_NoRecords() throws Exception {
        when(attendanceService.viewAllAttendance()).thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceAsExcel());
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewAllAttendanceAsExcel_NativeNotFound() throws Exception {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(response));
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.empty());

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceAsExcel());
        verify(attendanceService, times(1)).viewAllAttendance();
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewAllAttendanceByTimeAsExcel_Success() throws Exception {
        LocalDateTime testDateTime = LocalDateTime.parse("2025-04-02T10:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(testDateTime);
        response.setAttendanceTime("10:00:00");

        AllTimeBasedAttendanceRequest mockRequest = mock(AllTimeBasedAttendanceRequest.class);
        when(mockRequest.getStartDate()).thenReturn("2025-04-01T00:00:00");
        when(mockRequest.getEndDate()).thenReturn("2025-04-03T23:59:59");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(response));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        byte[] result = adminService.viewAllAttendanceByTimeAsExcel(mockRequest);

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(attendanceService, times(1)).viewAllAttendance();
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewAllAttendanceByTimeAsExcel_NoRecords() throws Exception {
        AllTimeBasedAttendanceRequest mockRequest = mock(AllTimeBasedAttendanceRequest.class);
        when(mockRequest.getStartDate()).thenReturn("2025-04-01T00:00:00");
        when(mockRequest.getEndDate()).thenReturn("2025-04-03T23:59:59");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewAllAttendanceByTimeAsExcel(mockRequest));

        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testViewCohortAttendanceAsExcel_Success() throws Exception {
        CohortAttendanceRequest mockRequest = mock(CohortAttendanceRequest.class);
        when(mockRequest.getCohort()).thenReturn("CohortA");

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");

        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Doe");
        attendance.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        attendance.setAttendanceTime("10:00:00");

        when(nativeRepository.findByCohort("CohortA")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.singletonList(attendance));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        try (MockedStatic<Mapper> mockedMapper = mockStatic(Mapper.class)) {
            AttendanceHistoryResponse response = new AttendanceHistoryResponse();
            response.setFingerprintId("FP1");
            response.setNativeName("John Doe");
            response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
            response.setAttendanceTime("10:00:00");

            mockedMapper.when(() -> Mapper.attendanceHistoryResponseMapper(any(Attendance.class)))
                    .thenReturn(response);

            byte[] result = adminService.viewCohortAttendanceAsExcel(mockRequest);

            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        verify(nativeRepository, times(1)).findByCohort("CohortA");
        verify(attendanceRepository, times(1)).findByNativeId("FP1");
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewCohortAttendanceAsExcel_NoRecords() throws Exception {
        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        when(nativeRepository.findByCohort("CohortA")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceAsExcel(cohortAttendanceRequest));
        verify(nativeRepository, times(1)).findByCohort("CohortA");
        verify(attendanceRepository, times(1)).findByNativeId("FP1");
    }

    @Test
    void testViewCohortAttendanceByTimeAsExcel_Success() throws Exception {
        CohortTimeBasedAttendanceRequest mockRequest = mock(CohortTimeBasedAttendanceRequest.class);
        when(mockRequest.getCohort()).thenReturn("Cohort21");

        LocalDateTime startDateTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0);
        LocalDateTime endDateTime = LocalDateTime.now().withHour(17).withMinute(0).withSecond(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String startDateStr = startDateTime.format(formatter);
        String endDateStr = endDateTime.format(formatter);

        when(mockRequest.getStartDate()).thenReturn(startDateStr);
        when(mockRequest.getEndDate()).thenReturn(endDateStr);

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        List<Attendance> attendanceList = new ArrayList<>();
        Attendance attendance = new Attendance();
        attendance.setNativeId("FP1");
        attendance.setNativeName("John Dan");

        LocalDateTime attendanceDateTime = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0);
        attendance.setAttendanceDate(attendanceDateTime);
        attendance.setAttendanceTime("10:00:00");
        attendanceList.add(attendance);

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(attendanceList);

        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(aNative));

        byte[] result = adminService.viewCohortAttendanceByTimeAsExcel(mockRequest);

        assertNotNull(result);
        assertTrue(result.length > 0);

        verify(nativeRepository).findByCohort("Cohort21");
        verify(attendanceRepository).findByNativeId("FP1");
        verify(nativeRepository).findByFingerprintId("FP1");
    }

    @Test
    void testViewCohortAttendanceByTimeAsExcel_NoRecords() throws Exception {
        CohortTimeBasedAttendanceRequest mockRequest = mock(CohortTimeBasedAttendanceRequest.class);
        when(mockRequest.getCohort()).thenReturn("Cohort21");

        LocalDateTime startDateTime = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0);
        LocalDateTime endDateTime = LocalDateTime.now().withHour(17).withMinute(0).withSecond(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String startDateStr = startDateTime.format(formatter);
        String endDateStr = endDateTime.format(formatter);

        when(mockRequest.getStartDate()).thenReturn(startDateStr);
        when(mockRequest.getEndDate()).thenReturn(endDateStr);

        Native aNative = new Native();
        aNative.setFingerprintId("FP1");
        aNative.setCohort("Cohort21");

        when(nativeRepository.findByCohort("Cohort21")).thenReturn(Collections.singletonList(aNative));
        when(attendanceRepository.findByNativeId("FP1")).thenReturn(Collections.emptyList());

        assertThrows(AttendanceExpection.class, () -> adminService.viewCohortAttendanceByTimeAsExcel(mockRequest));

        verify(nativeRepository).findByCohort("Cohort21");
        verify(attendanceRepository).findByNativeId("FP1");
    }

    @Test
    void testViewNativeAttendanceAsExcel_Success() throws Exception {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(nativeService.viewNativeAttendance(nativeAttendanceRequest)).thenReturn(Collections.singletonList(response));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        byte[] result = adminService.viewNativeAttendanceAsExcel(nativeAttendanceRequest);
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(nativeService, times(1)).viewNativeAttendance(nativeAttendanceRequest);
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewNativeAttendanceAsExcel_NoRecords() throws Exception {
        when(nativeService.viewNativeAttendance(nativeAttendanceRequest))
                .thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendanceAsExcel(nativeAttendanceRequest));
        verify(nativeService, times(1)).viewNativeAttendance(nativeAttendanceRequest);
    }

    @Test
    void testViewNativeAttendanceByTimeAsExcel_Success() throws Exception {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest)).thenReturn(Collections.singletonList(response));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        byte[] result = adminService.viewNativeAttendanceByTimeAsExcel(nativeTimeBasedRequest);
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(nativeService, times(1)).viewNativeAttendanceByTime(nativeTimeBasedRequest);
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testViewNativeAttendanceByTimeAsExcel_NoRecords() throws Exception {
        when(nativeService.viewNativeAttendanceByTime(nativeTimeBasedRequest))
                .thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.viewNativeAttendanceByTimeAsExcel(nativeTimeBasedRequest));
        verify(nativeService, times(1)).viewNativeAttendanceByTime(nativeTimeBasedRequest);
    }

    @Test
    void testExportAttendanceToExcel_Success() throws Exception {
        AttendanceHistoryResponse response = new AttendanceHistoryResponse();
        response.setFingerprintId("FP1");
        response.setNativeName("John Doe");
        response.setAttendanceDate(LocalDateTime.parse("2025-04-02T10:00:00", formatter));
        response.setAttendanceTime("10:00:00");

        when(attendanceService.viewAllAttendance()).thenReturn(Collections.singletonList(response));

        Native nativePerson = new Native();
        nativePerson.setFingerprintId("FP1");
        nativePerson.setCohort("CohortA");
        when(nativeRepository.findByFingerprintId("FP1")).thenReturn(Optional.of(nativePerson));

        byte[] result = adminService.exportAttendanceToExcel();
        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(attendanceService, times(1)).viewAllAttendance();
        verify(nativeRepository, times(1)).findByFingerprintId("FP1");
    }

    @Test
    void testExportAttendanceToExcel_NoRecords() throws Exception {
        when(attendanceService.viewAllAttendance()).thenThrow(new AttendanceExpection("No attendance records found"));

        assertThrows(AttendanceExpection.class, () -> adminService.exportAttendanceToExcel());
        verify(attendanceService, times(1)).viewAllAttendance();
    }

    @Test
    void testLogoutAdmin_Success() {
        Admin admin = new Admin();
        admin.setUserName("admin1");
        admin.setIsLoggedIn(true);
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LogoutAdminResponse response = adminService.logoutAdmin(logoutAdminRequest);

        assertTrue(response.isSuccess());
        assertEquals("Logout successful", response.getMessage());
        assertFalse(admin.getIsLoggedIn());
        verify(adminRepository, times(1)).save(admin);
    }

    @Test
    void testLogoutAdmin_UserNotFound() {
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.empty());

        assertThrows(AdminExpection.class, () -> adminService.logoutAdmin(logoutAdminRequest),
                "Admin not found");
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testLogoutAdmin_AlreadyLoggedOut() {
        Admin admin = new Admin();
        admin.setUserName("admin1");
        admin.setIsLoggedIn(false);
        when(adminRepository.findByUserName("admin1")).thenReturn(Optional.of(admin));

        assertThrows(AdminExpection.class, () -> adminService.logoutAdmin(logoutAdminRequest),
                "Admin is already logged out");
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void testLogoutAdmin_NullUsername() {
        logoutAdminRequest.setUsername(null);
        assertThrows(AdminExpection.class, () -> adminService.logoutAdmin(logoutAdminRequest),
                "Username cannot be empty");
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testLogoutAdmin_EmptyUsername() {
        logoutAdminRequest.setUsername("");
        assertThrows(AdminExpection.class, () -> adminService.logoutAdmin(logoutAdminRequest),
                "Username cannot be empty");
        verify(adminRepository, never()).findByUserName(anyString());
    }

    @Test
    void testGetAttendancePercentage_Success() {
        attendancePercentageRequest.setStartDate("2025-04-07T00:00:00");
        attendancePercentageRequest.setEndDate("2025-04-11T23:59:59");
        attendancePercentageRequest.setFingerprintId("FP1");

        TotalNumberOfAttendanceResponse attendanceResponse = new TotalNumberOfAttendanceResponse();
        attendanceResponse.setTotalNumberOfAttendance(3);
        when(nativeService.nativeAttendanceCount(any(TotalNumberOfAttendanceRequest.class))).thenReturn(attendanceResponse);

        double result = adminService.getAttendancePercentage(attendancePercentageRequest);

        assertEquals(60.0, result, 0.01);
        verify(nativeService, times(1)).nativeAttendanceCount(any(TotalNumberOfAttendanceRequest.class));
    }

    @Test
    void testGetAttendancePercentage_NoWorkingDays() {
        attendancePercentageRequest.setStartDate("2025-04-12T00:00:00");
        attendancePercentageRequest.setEndDate("2025-04-13T23:59:59");
        attendancePercentageRequest.setFingerprintId("FP1");

        assertThrows(AttendanceExpection.class, () -> adminService.getAttendancePercentage(attendancePercentageRequest),
                "No working days in the specified date range");
    }

    @Test
    void testGetAttendancePercentage_NullFingerprintId() {
        attendancePercentageRequest.setFingerprintId(null);
        assertThrows(AttendanceExpection.class, () -> adminService.getAttendancePercentage(attendancePercentageRequest),
                "Fingerprint ID cannot be empty");
    }

    @Test
    void testGetAttendancePercentage_InvalidDates() {
        attendancePercentageRequest.setStartDate("2025-04-11T23:59:59");
        attendancePercentageRequest.setEndDate("2025-04-07T00:00:00");
        attendancePercentageRequest.setFingerprintId("FP1");

        assertThrows(AttendanceExpection.class, () -> adminService.getAttendancePercentage(attendancePercentageRequest),
                "Start date cannot be after end date");
    }

    @Test
    void testGetAttendancePercentage_NullStartDate() {
        attendancePercentageRequest.setStartDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.getAttendancePercentage(attendancePercentageRequest),
                "Start date cannot be empty");
    }

    @Test
    void testGetAttendancePercentage_NullEndDate() {
        attendancePercentageRequest.setEndDate(null);
        assertThrows(AttendanceExpection.class, () -> adminService.getAttendancePercentage(attendancePercentageRequest),
                "End date cannot be empty");
    }
}