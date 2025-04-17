package africa.pk.attendance.service.interfaces;

public interface AttendanceMessageHandler {
    void getMessageFromAttendanceHandler(String message, String topicToSendMessageTo);
    void publishMessage(String message, String topic);
}