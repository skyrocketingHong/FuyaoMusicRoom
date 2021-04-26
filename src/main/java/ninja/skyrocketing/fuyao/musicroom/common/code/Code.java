package ninja.skyrocketing.fuyao.musicroom.common.code;

import java.io.Serializable;

/**
 * @author skyrocketing Hong
 */
public interface Code extends Serializable {
    /**
     * code
     *
     * @return String
     */
    String code();

    /**
     * message
     *
     * @return String
     */
    String message();

}
