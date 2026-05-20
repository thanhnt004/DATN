package dto;

import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {

    private String content;

    private String type;

    private List<Attachment> attachments;

    private List<String> memberIds;

    private String conversationType; // PRIVATE | GROUP 🔥
}