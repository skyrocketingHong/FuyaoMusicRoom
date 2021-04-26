package ninja.skyrocketing.fuyao.musicroom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author skyrocketing Hong
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class Chat extends Message {
    private String type = "chat";
    private String sessionId;
}
