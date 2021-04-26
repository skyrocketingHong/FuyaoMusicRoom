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
public class Online extends Message {
    /**
     * 在线人数
     */
    private Integer count;
}
