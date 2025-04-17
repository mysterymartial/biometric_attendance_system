package africa.pk.attendance.expections;

public class AdminExpection extends RuntimeException {
    public AdminExpection(String message) {
        super(message);
    }
}
