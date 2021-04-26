package ninja.skyrocketing.fuyao.musicroom.util;

import org.springframework.web.socket.WebSocketSession;

/**
 * @author skyrocketing Hong
 * @create 2020-05-20 23:07
 */
public class SessionUtils {
    public static String getAttributeValue(WebSocketSession session, String key, String defaultValue) {
        Object keyValue = session.getAttributes().get(key);
        if (keyValue == null) {
            return defaultValue;
        } else {
            String keyValueStr = (String) keyValue;
            return keyValueStr == "" ? defaultValue : keyValueStr;
        }
    }
}
