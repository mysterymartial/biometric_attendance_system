package africa.pk.attendance.controller;


import africa.pk.attendance.dtos.request.*;
import africa.pk.attendance.dtos.response.*;
import africa.pk.attendance.service.interfaces.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterAdminResponse>> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        RegisterAdminResponse response = adminService.registerAdmin(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Admin registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginAdminResponse>> loginAdmin(@Valid @RequestBody LoginAdminRequest request) {
        LoginAdminResponse response = adminService.loginAdmin(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Admin logged in successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutAdminResponse>> logoutAdmin(@Valid @RequestBody LogoutAdminRequest request) {
        LogoutAdminResponse response = adminService.logoutAdmin(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Admin logged out successfully", response));
    }

    @PostMapping("/native")
    public ResponseEntity<ApiResponse<RegisterNativeResponse>> addNative(@Valid @RequestBody RegisterNativeRequest request) {
        RegisterNativeResponse response = adminService.addNative(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Native added successfully", response));
    }

    @GetMapping("/attendance/all")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewAllAttendance() {
        List<AttendanceHistoryResponse> response = adminService.viewAllAttendance();
        return ResponseEntity.ok(new ApiResponse<>("success", "All attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/time-based")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewAllAttendanceByTime(@Valid @RequestBody AllTimeBasedAttendanceRequest request) {
        List<AttendanceHistoryResponse> response = adminService.viewAllAttendanceByTime(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Time-based attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/cohort")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewCohortAttendance(@Valid @RequestBody CohortAttendanceRequest request) {
        List<AttendanceHistoryResponse> response = adminService.viewCohortAttendance(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Cohort attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/cohort/time-based")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewCohortAttendanceByTime(@Valid @RequestBody CohortTimeBasedAttendanceRequest request) {
        List<AttendanceHistoryResponse> response = adminService.viewCohortAttendanceByTime(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Cohort time-based attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/native")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewNativeAttendance(@Valid @RequestBody NativeAttendanceRequest request) {
        List<AttendanceHistoryResponse> response = adminService.viewNativeAttendance(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Native attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/native/time-based")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> viewNativeAttendanceByTime(@Valid @RequestBody NativeTimeBasedAttendanceRequest request) {
        List<AttendanceHistoryResponse> response = adminService.viewNativeAttendanceByTime(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Native time-based attendance retrieved successfully", response));
    }

    @PostMapping("/attendance/native/count")
    public ResponseEntity<ApiResponse<TotalNumberOfAttendanceResponse>> viewNativeAttendanceCount(@Valid @RequestBody TotalNumberOfAttendanceRequest request) {
        TotalNumberOfAttendanceResponse response = adminService.viewNativeAttendanceCount(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Native attendance count retrieved successfully", response));
    }

    @GetMapping("/attendance/export/excel")
    public ResponseEntity<byte[]> viewAllAttendanceAsExcel() {
        byte[] excelData = adminService.viewAllAttendanceAsExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "all_attendance.xlsx");
        return new ResponseEntity<>(excelData, headers, 200);
    }

    @PostMapping("/attendance/percentage")
    public ResponseEntity<ApiResponse<Double>> getAttendancePercentage(@Valid @RequestBody AttendancePercentageRequest request) {
        Double response = adminService.getAttendancePercentage(request);
        return ResponseEntity.ok(new ApiResponse<>("success", "Attendance percentage retrieved successfully", response));
    }
}