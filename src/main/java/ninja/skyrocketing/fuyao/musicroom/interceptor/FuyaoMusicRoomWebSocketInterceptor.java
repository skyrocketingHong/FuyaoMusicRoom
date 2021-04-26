package ninja.skyrocketing.fuyao.musicroom.interceptor;

import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.servlet.annotation.WebFilter;

/**
 * @author skyrocketing Hong
 */
@Component
public class FuyaoMusicRoomWebSocketInterceptor implements ChannelInterceptor {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RoomContainer houseContainer;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String ip = (String) (accessor.getSessionAttributes().get("remoteAddress"));
        User black = sessionService.getBlack(sessionId, ip, houseId);
        if (houseId == null || houseContainer.get(houseId) == null) {
            return null;
        }
        if (null != black) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你已被管理员拉黑"), houseId);
            return null;
        }
        return message;
    }
}
