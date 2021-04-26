package ninja.skyrocketing.fuyao.musicroom.model;

import lombok.*;

/**
 * @author skyrocketing Hong
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Setting extends Message {
    private String name;
}
