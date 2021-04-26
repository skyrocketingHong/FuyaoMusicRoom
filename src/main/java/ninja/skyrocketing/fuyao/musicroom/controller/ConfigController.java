package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.Setting;
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
public class ConfigController {
    @Autowired
    private SessionService sessionService;

    @MessageMapping("/setting/name")
    public void settingName(Setting setting, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String name = setting.getName();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        if (name == null || "".equals(name)) {
            sessionService.send(sessionId, MessageType.SETTING_NAME, Response.failure((Object) null, "昵称设置失败"), houseId);
        } else {
            log.info("设置用户名: {}", name);
            sessionService.settingName(sessionId, name, houseId);
            sessionService.send(sessionId, MessageType.SETTING_NAME, Response.success(setting, "昵称设置成功"), houseId);
        }
    }
}
