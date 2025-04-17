package africa.pk.attendance.dtos.response;

import lombok.Data;

@Data
public class MessageToBeReturned {
    private String message;
    private String topicToPublishTo;

}
