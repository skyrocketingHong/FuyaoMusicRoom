package ninja.skyrocketing.fuyao.musicroom.model;

import lombok.*;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;

import java.io.Serializable;

/**
 * @author skyrocketing Hong
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = -5508341219684417455L;

    private String houseId;
    /**
     * WebSocketServerSockJsSession 中的 session id
     */
    private String sessionId;
    /**
     * 用户名
     */
    private String name = "";
    /**
     * 昵称
     */
    private String nickName = "";
    /**
     * ip 地址
     */
    private String remoteAddress = "";
    /**
     * 角色
     */
    private String role = "default";
    /**
     * 最后在线时间
     */
    private Long lastMessageTime;

    public String getNickName() {
        StringBuilder nickName = new StringBuilder();
        nickName.append(this.name)
                .append("(")
                .append(StringUtils.desensitizeIPV4(this.remoteAddress))
                .append(")");
        return nickName.toString();
    }
}
