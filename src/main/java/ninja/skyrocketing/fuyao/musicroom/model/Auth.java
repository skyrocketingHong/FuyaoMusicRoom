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
public class Auth extends Message {
    private String password;
}
