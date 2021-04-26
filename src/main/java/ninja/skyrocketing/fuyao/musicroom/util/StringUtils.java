package ninja.skyrocketing.fuyao.musicroom.util;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String utils
 *
 * @author skyrocketing Hong
 */
public class StringUtils {
    public static final String ENCODE_UTF_8 = "UTF-8";

    /**
     * 根据给定编码方式获取长度
     *
     * @param str    字符串
     * @param encode 给定编码方式
     * @return 长度
     * @throws UnsupportedEncodingException 异常
     */
    public static int getLength(String str, String encode) throws UnsupportedEncodingException {
        return str.getBytes(encode).length;
    }

    /**
     * ipv4 脱敏处理
     *
     * @param ipv4 待脱敏的 ip
     * @return 127.0.*.*
     */
    public static String desensitizeIPV4(String ipv4) {
        String[] split = ipv4.split("\\.");
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i >= split.length / 2) {
                ip.append("*");
            } else {
                ip.append(split[i]);
            }
            if (i != split.length - 1) {
                ip.append(".");
            }
        }
        return ip.toString();
    }

    public static boolean isQQMusicId(String id) {
        String regEx = "^[a-zA-Z0-9]{14}$";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        return m.find();
    }

    public static boolean isNeteaseMusicId(String id) {
        String regEx = "^[0-9]{6,}$";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        return m.find();
    }

    public static boolean isUserId(String id) {
        String regEx = "^[0-9]+$";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        boolean result = m.find();
        return result;
    }

    public static boolean isPlayListIds(String id) {
        String regEx = "^[,\\s，]*\\d+([,\\s，]+\\d*[,\\s，]*)*$";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        boolean result = m.find();
        return result || id.startsWith("*");
    }

    public static String[] splitPlayListIds(String id) {
        String regEx = "[,\\s，]+";
        String[] ids = id.split(regEx);
        if ("".equals(ids[0])) {
            String[] newIds = new String[ids.length - 1];
            System.arraycopy(ids, 1, newIds, 0, ids.length - 1);
            return newIds;
        } else {
            return ids;
        }
    }

    public static boolean isSessionId(String id) {
        String regEx = "[a-zA-Z0-9]{8,}";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        return m.find();
    }

    public static String getSessionId(String id) {
        String regEx = "@[a-zA-Z0-9]{8,}";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(id);
        if (m.find()) {
            return m.group().substring(1);
        } else {
            return null;
        }
    }

    public static String encodeString(String param) {
        if (param != null && param != "") {
            return param.replaceAll("\\s+", "%20").replaceAll("\\?", "%3F").replaceAll("%", "%25").
                    replaceAll("#", "%23").replaceAll("&", "%26").replaceAll("=", "%3D").
                    replaceAll("/", "%2F").replaceAll("\\+", "%2B");
        } else {
            return param;
        }
    }

    public static boolean isUrlSpecialCharacter(String str) {
        String regEx = "[\\s\\?%#&=/\\+]+";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }
}
