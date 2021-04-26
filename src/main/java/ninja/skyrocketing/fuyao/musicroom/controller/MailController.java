package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.Chat;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.service.MailService;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * @author skyrocketing Hong
 */
@Controller
@Slf4j
public class MailController {
    @Autowired
    private MailService mailService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RoomContainer houseContainer;

    @MessageMapping("/mail/send")
    public void send(Chat chat, StompHeaderAccessor stompHeaderAccessor) {
        String sessionId = stompHeaderAccessor.getHeader("simpSessionId").toString();
        String houseId = (String) stompHeaderAccessor.getSessionAttributes().get("houseId");
        User user = sessionService.getUser(sessionId, houseId);
        long currentTime = System.currentTimeMillis();
        if (null != user.getLastMessageTime() && currentTime - user.getLastMessageTime() < 2000) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "发言时间间隔太短"), houseId);
        } else {
            chat.setSessionId(user.getSessionId());
            chat.setNickName(user.getNickName());
            chat.setContent("@管理员 " + chat.getContent());
            sessionService.send(MessageType.CHAT, Response.success(chat, "@管理员"), houseId);
            sessionService.setLastMessageTime(user, System.currentTimeMillis(), houseId);
            String content = user +
                    "\n\n\n\n" +
                    houseContainer.get(user.getHouseId()) +
                    "\n\n\n\n" +
                    chat.getContent();
            boolean result = mailService.sendSimpleMail("music.scoder.club@管理员[" + user.getRemoteAddress() + "]", content);
            if (result) {
                log.info("session id: {}, @管理员, 邮件发送成功", sessionId);
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "@管理员 成功"), houseId);
            } else {
                log.info("session id: {}, @管理员, 邮件发送失败", sessionId);
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "@管理员 失败"), houseId);
            }
        }
    }
}
