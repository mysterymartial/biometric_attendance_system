package africa.pk.attendance.service.implementations;

import africa.pk.attendance.data.models.Admin;
import africa.pk.attendance.data.models.Native;
import africa.pk.attendance.data.repositories.AdminRepository;
import africa.pk.attendance.data.repositories.AttendanceRepository;
import africa.pk.attendance.data.repositories.NativeRepository;
import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;
import africa.pk.attendance.expections.AdminExpection;
import africa.pk.attendance.expections.AttendanceExpection;
import africa.pk.attendance.service.interfaces.AdminService;
import africa.pk.attendance.service.interfaces.AttendanceMessageHandler;
import africa.pk.attendance.service.interfaces.AttendanceService;
import africa.pk.attendance.service.interfaces.NativeService;
import africa.pk.attendance.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final NativeRepository nativeRepository;
    private final AttendanceRepository attendanceRepository;
    private final NativeService nativeService;
    private final AttendanceService attendanceService;
    private final AttendanceMessageHandler attendanceMessageHandler;
    private final BCryptPasswordEncoder passwordEncoder;

    private final String responseTopic = "response";

    @Override
    public RegisterAdminResponse registerAdmin(RegisterAdminRequest registerAdminRequest) {
        try {
            checkIfAdminFieldsAreEmpty(registerAdminRequest);
            checkIfAdminExists(registerAdminRequest);
            Admin admin = new Admin();
            admin.setUserName(registerAdminRequest.getUserName());
            admin.setFirstName(registerAdminRequest.getFirstName());
            admin.setLastName(registerAdminRequest.getLastName());
            admin.setPassword(passwordEncoder.encode(registerAdminRequest.getPassword()));
            admin.setIsLoggedIn(false);
            adminRepository.save(admin);
            RegisterAdminResponse response = new RegisterAdminResponse();
            response.setUserName(admin.getUserName());
            response.setFirstName(admin.getFirstName());
            response.setLastName(admin.getLastName());
            response.setMessage("Admin registered successfully");
            response.setSuccess(true);
            return response;
        } catch (AdminExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    private void checkIfAdminExists(RegisterAdminRequest registerAdminRequest) {
        Optional<Admin> admin = adminRepository.findByUserName(registerAdminRequest.getUserName());
        if (admin.isPresent()) {
            throw new AdminExpection("Admin with username " + registerAdminRequest.getUserName() + " already exists");
        }
    }

    private void checkIfAdminFieldsAreEmpty(RegisterAdminRequest registerAdminRequest) {
        if (registerAdminRequest.getUserName() == null || registerAdminRequest.getUserName().isBlank()) {
            throw new AdminExpection("Username cannot be empty");
        }
        if (registerAdminRequest.getFirstName() == null || registerAdminRequest.getFirstName().isBlank()) {
            throw new AdminExpection("First name cannot be empty");
        }
        if (registerAdminRequest.getLastName() == null || registerAdminRequest.getLastName().isBlank()) {
            throw new AdminExpection("Last name cannot be empty");
        }
        if (registerAdminRequest.getPassword() == null || registerAdminRequest.getPassword().isBlank()) {
            throw new AdminExpection("Password cannot be empty");
        }
    }

    @Override
    public LoginAdminResponse loginAdmin(LoginAdminRequest loginAdminRequest) {
        try {
            if (loginAdminRequest.getUserName() == null || loginAdminRequest.getUserName().isBlank()) {
                throw new AdminExpection("Username cannot be empty");
            }
            if (loginAdminRequest.getPassword() == null || loginAdminRequest.getPassword().isBlank()) {
                throw new AdminExpection("Invalid username or password");
            }

            Optional<Admin> admin = adminRepository.findByUserName(loginAdminRequest.getUserName());
            if (admin.isPresent() && passwordEncoder.matches(loginAdminRequest.getPassword(), admin.get().getPassword())) {
                admin.get().setIsLoggedIn(true);
                adminRepository.save(admin.get());
                LoginAdminResponse response = new LoginAdminResponse();
                response.setUserName(admin.get().getUserName());
                response.setFirstName(admin.get().getFirstName());
                response.setLastName(admin.get().getLastName());
                response.setMessage("Login successful");
                response.setSuccess(true);
                return response;
            } else {
                throw new AdminExpection("Invalid username or password");
            }
        } catch (AdminExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }

    @Override
    public RegisterNativeResponse addNative(RegisterNativeRequest registerNativeRequest) {
        return nativeService.registerNative(registerNativeRequest);
    }

    @Override
    public List<AttendanceHistoryResponse> viewAllAttendance() {
        return attendanceService.viewAllAttendance();
    }

    @Override
    public List<AttendanceHistoryResponse> viewAllAttendanceByTime(AllTimeBasedAttendanceRequest request) {
        if (request.getStartDate() == null || request.getStartDate().isBlank()) {
            throw new AttendanceExpection("Start date cannot be empty");
        }
        if (request.getEndDate() == null || request.getEndDate().isBlank()) {
            throw new AttendanceExpection("End date cannot be empty");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime startDateTime = LocalDateTime.parse(request.getStartDate(), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(request.getEndDate(), formatter);

        if (startDateTime.isAfter(endDateTime)) {
            throw new AttendanceExpection("Start date cannot be after end date");
        }

        List<AttendanceHistoryResponse> allAttendance = attendanceService.viewAllAttendance();
        List<AttendanceHistoryResponse> filteredAttendance = allAttendance.stream()
                .filter(attendance -> {
                    LocalDateTime attendanceDateTime = attendance.getAttendanceDate();
                    return (attendanceDateTime.isEqual(startDateTime) || attendanceDateTime.isAfter(startDateTime)) &&
                            (attendanceDateTime.isEqual(endDateTime) || attendanceDateTime.isBefore(endDateTime));
                })
                .collect(Collectors.toList());

        if (filteredAttendance.isEmpty()) {
            throw new AttendanceExpection("No attendance records found between " +
                    request.getStartDate() + " and " + request.getEndDate());
        }

        return filteredAttendance;
    }

    @Override
    public List<AttendanceHistoryResponse> viewCohortAttendance(CohortAttendanceRequest request) {
        if (request.getCohort() == null || request.getCohort().isBlank()) {
            throw new AttendanceExpection("Cohort name cannot be empty");
        }

        List<Native> natives = nativeRepository.findByCohort(request.getCohort());
        if (natives.isEmpty()) {
            throw new AttendanceExpection("No natives found for cohort: " + request.getCohort());
        }

        List<AttendanceHistoryResponse> responses = natives.stream()
                .flatMap(nativePerson -> attendanceRepository.findByNativeId(nativePerson.getFingerprintId()).stream())
                .map(Mapper::attendanceHistoryResponseMapper)
                .collect(Collectors.toList());

        if (responses.isEmpty()) {
            throw new AttendanceExpection("No attendance records found for cohort: " + request.getCohort());
        }

        return responses;
    }

    @Override
    public List<AttendanceHistoryResponse> viewCohortAttendanceByTime(CohortTimeBasedAttendanceRequest request) {
        if (request.getCohort() == null || request.getCohort().isBlank()) {
            throw new AttendanceExpection("Cohort name cannot be empty");
        }
        if (request.getStartDate() == null || request.getStartDate().isBlank()) {
            throw new AttendanceExpection("Start date cannot be empty");
        }
        if (request.getEndDate() == null || request.getEndDate().isBlank()) {
            throw new AttendanceExpection("End date cannot be empty");
        }

        List<Native> natives = nativeRepository.findByCohort(request.getCohort());
        if (natives.isEmpty()) {
            throw new AttendanceExpection("No natives found for cohort: " + request.getCohort());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime startDateTime = LocalDateTime.parse(request.getStartDate(), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(request.getEndDate(), formatter);

        if (startDateTime.isAfter(endDateTime)) {
            throw new AttendanceExpection("Start date cannot be after end date");
        }

        List<AttendanceHistoryResponse> responses = natives.stream()
                .flatMap(nativePerson -> attendanceRepository.findByNativeId(nativePerson.getFingerprintId()).stream())
                .filter(attendance -> {
                    LocalDateTime attendanceDateTime = attendance.getAttendanceDate();
                    return (attendanceDateTime.isEqual(startDateTime) || attendanceDateTime.isAfter(startDateTime)) &&
                            (attendanceDateTime.isEqual(endDateTime) || attendanceDateTime.isBefore(endDateTime));
                })
                .map(Mapper::attendanceHistoryResponseMapper)
                .collect(Collectors.toList());

        if (responses.isEmpty()) {
            throw new AttendanceExpection("No attendance records found for cohort " + request.getCohort() +
                    " between " + request.getStartDate() + " and " + request.getEndDate());
        }

        return responses;
    }

    @Override
    public List<AttendanceHistoryResponse> viewNativeAttendance(NativeAttendanceRequest request) {
        return nativeService.viewNativeAttendance(request);
    }

    @Override
    public List<AttendanceHistoryResponse> viewNativeAttendanceByTime(NativeTimeBasedAttendanceRequest request) {
        return nativeService.viewNativeAttendanceByTime(request);
    }

    @Override
    public TotalNumberOfAttendanceResponse viewNativeAttendanceCount(TotalNumberOfAttendanceRequest totalNumberOfAttendanceRequest) {
        return nativeService.nativeAttendanceCount(totalNumberOfAttendanceRequest);
    }

    @Override
    public byte[] viewAllAttendanceAsExcel() {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewAllAttendance();
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] viewAllAttendanceByTimeAsExcel(AllTimeBasedAttendanceRequest request) {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewAllAttendanceByTime(request);
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] viewCohortAttendanceAsExcel(CohortAttendanceRequest request) {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewCohortAttendance(request);
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export cohort attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] viewCohortAttendanceByTimeAsExcel(CohortTimeBasedAttendanceRequest request) {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewCohortAttendanceByTime(request);
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export cohort attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] viewNativeAttendanceAsExcel(NativeAttendanceRequest request) {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewNativeAttendance(request);
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export native attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] viewNativeAttendanceByTimeAsExcel(NativeTimeBasedAttendanceRequest request) {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewNativeAttendanceByTime(request);
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export native attendance to Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportAttendanceToExcel() {
        try {
            List<AttendanceHistoryResponse> attendanceList = viewAllAttendance();
            return generateExcel(attendanceList);
        } catch (Exception e) {
            throw new AttendanceExpection("Failed to export attendance to Excel: " + e.getMessage());
        }
    }

    private byte[] generateExcel(List<AttendanceHistoryResponse> attendances) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Records");

        Row headerRow = sheet.createRow(0);
        String[] columns = {"Native Name", "Date", "Time", "Cohort"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (AttendanceHistoryResponse attendance : attendances) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(attendance.getNativeName());
            row.createCell(1).setCellValue(attendance.getAttendanceDate().toLocalDate().toString());
            row.createCell(2).setCellValue(attendance.getAttendanceTime());
            Optional<Native> nativeOptional = nativeRepository.findByFingerprintId(attendance.getFingerprintId());
            Native nativePerson = nativeOptional.orElseThrow(() -> new AttendanceExpection("Native not found for ID: " + attendance.getFingerprintId()));
            row.createCell(3).setCellValue(nativePerson.getCohort());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    public double getAttendancePercentage(AttendancePercentageRequest request) {
        if (request.getStartDate() == null || request.getStartDate().isBlank()) {
            throw new AttendanceExpection("Start date cannot be empty");
        }
        if (request.getEndDate() == null || request.getEndDate().isBlank()) {
            throw new AttendanceExpection("End date cannot be empty");
        }
        if (request.getFingerprintId() == null || request.getFingerprintId().isBlank()) {
            throw new AttendanceExpection("Fingerprint ID cannot be empty");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime startDateTime = LocalDateTime.parse(request.getStartDate(), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(request.getEndDate(), formatter);

        if (startDateTime.isAfter(endDateTime)) {
            throw new AttendanceExpection("Start date cannot be after end date");
        }

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        long workingDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> date.getDayOfWeek().getValue() <= 5)
                .count();

        if (workingDays == 0) {
            throw new AttendanceExpection("No working days in the specified date range");
        }

        TotalNumberOfAttendanceRequest attendanceRequest = new TotalNumberOfAttendanceRequest();
        attendanceRequest.setFingerprintId(request.getFingerprintId());
        attendanceRequest.setStartDate(request.getStartDate());
        attendanceRequest.setEndDate(request.getEndDate());
        TotalNumberOfAttendanceResponse attendanceResponse = nativeService.nativeAttendanceCount(attendanceRequest);

        long actualAttendance = attendanceResponse.getTotalNumberOfAttendance();

        double percentage = (double) actualAttendance / workingDays * 100;
        return Math.round(percentage * 100.0) / 100.0;
    }

    @Override
    public LogoutAdminResponse logoutAdmin(LogoutAdminRequest logoutAdminRequest) {
        try {
            if (logoutAdminRequest.getUsername() == null || logoutAdminRequest.getUsername().isBlank()) {
                throw new AdminExpection("Username cannot be empty");
            }

            Optional<Admin> adminOptional = adminRepository.findByUserName(logoutAdminRequest.getUsername());
            if (adminOptional.isEmpty()) {
                throw new AdminExpection("Admin with username " + logoutAdminRequest.getUsername() + " not found");
            }

            Admin admin = adminOptional.get();
            if (!admin.getIsLoggedIn()) {
                throw new AdminExpection("Admin is not logged in");
            }

            admin.setIsLoggedIn(false);
            adminRepository.save(admin);

            LogoutAdminResponse response = new LogoutAdminResponse();
            response.setUserName(admin.getUserName());
            response.setMessage("Logout successful");
            response.setSuccess(true);
            return response;
        } catch (AdminExpection e) {
            attendanceMessageHandler.publishMessage("Error: " + e.getMessage(), responseTopic);
            throw e;
        }
    }
}