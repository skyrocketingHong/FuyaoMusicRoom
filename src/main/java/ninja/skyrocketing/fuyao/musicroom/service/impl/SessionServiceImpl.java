package ninja.skyrocketing.fuyao.musicroom.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicVoteRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionBlackRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionRepository;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author skyrocketing Hong
 */
@Service
@Slf4j
public class SessionServiceImpl implements SessionService {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionBlackRepository sessionBlackRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;

    @Override
    public void settingName(String sessionId, String name, String houseId) {
        if (null != name) {
            log.info("setting name: {}", name);
            User user = sessionRepository.getSession(sessionId, houseId);
            user.setName(name);
            user.setNickName(user.getName() + "(" + StringUtils.desensitizeIPV4(user.getRemoteAddress()) + ")");
            sessionRepository.setSession(user, houseId);
        }
    }

    @Override
    public String getRole(String sessionId, String houseId) {
        User session = sessionRepository.getSession(sessionId, houseId);
        if (session != null) {
            return session.getRole();
        } else {
            return "";
        }
    }

    @Override
    public User putSession(WebSocketSession session, String houseId) {
        fuyaoMusicRoomProperties.getSessions(houseId).put(session.getId(), session);
        User user = User.builder()
                .sessionId(session.getId())
                .name("")
                .nickName("")
                .remoteAddress(session.getAttributes().get("remoteAddress").toString())
                .role(session.getId().equals(houseId) ? "admin" : "default").houseId(session.getAttributes().get("houseId").toString())
                .build();
        sessionRepository.setSession(user, houseId);
        return user;
    }

    @Override
    public void clearSession(WebSocketSession session, String houseId) {
        log.info("Clear Session: {}", session.getId());
        fuyaoMusicRoomProperties.getSessions(houseId).remove(session.getId());
        sessionRepository.removeSession(session.getId(), houseId);
    }

    @Override
    public WebSocketSession clearSession(String sessionId, String houseId) {
        log.info("Clear Session: {}", sessionId);
        sessionRepository.removeSession(sessionId, houseId);
        musicVoteRepository.remove(sessionId, houseId);
        return fuyaoMusicRoomProperties.getSessions(houseId).remove(sessionId);
    }

    @Override
    public void send(Object payload, String houseId) {
        this.send(MessageType.NOTICE, payload, houseId);
    }

    @Override
    public void send(MessageType messageType, Object payload, String houseId) {
        Map<String, WebSocketSession> sessions = fuyaoMusicRoomProperties.getSessions(houseId);
        sessions.forEach((key, session) -> {
            if (session.isOpen()) {
                this.send(session, messageType, payload);
            }
        });
    }

    @Override
    public void send(MessageType messageType, Object payload) {
        FuyaoMusicRoomProperties.SessionContainer sessions = fuyaoMusicRoomProperties.getSessions();
        Map<String, ConcurrentHashMap<String, WebSocketSession>> housesSession = sessions.get();
        Set<String> houseIdSet = housesSession.keySet();
        for (String s : houseIdSet) {
            ConcurrentHashMap<String, WebSocketSession> houseSession = housesSession.get(s);
            houseSession.forEach((key, session) -> {
                if (session.isOpen()) {
                    this.send(session, messageType, payload);
                }
            });
        }
    }

    @Override
    public void send(String sessionId, Object payload, String houseId) {
        this.send(sessionId, MessageType.NOTICE, payload, houseId);
    }

    @Override
    public void send(String sessionId, MessageType messageType, Object payload, String houseId) {
        Map<String, WebSocketSession> sessions = fuyaoMusicRoomProperties.getSessions(houseId);
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            this.send(session, messageType, payload);
        }
    }

    @Override
    public void send(WebSocketSession session, Object payload) {
        this.send(session, MessageType.NOTICE, payload);
    }

    @Override
    public void send(WebSocketSession session, MessageType messageType, Object payload) {
        TextMessage textMessage = new TextMessage(this.getPayload(messageType.type(), payload));
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            log.error("send exception.");
        }
    }

    @Override
    public String getNickName(String sessionId, String houseId) {
        User session = sessionRepository.getSession(sessionId, houseId);
        if (session != null) {
            return session.getNickName();
        } else {
            return "";
        }
    }

    @Override
    public void setLastMessageTime(User user, Long time, String houseId) {
        user.setLastMessageTime(time);
        sessionRepository.setSession(user, houseId);
    }

    @Override
    public User getUser(String sessionId, String houseId) {
        return sessionRepository.getSession(sessionId, houseId);
    }

    @Override
    public void black(User user, String houseId) {
        sessionBlackRepository.setSession(user, houseId);
    }

    @Override
    public String showBlackUser(String houseId) {
        Set blackList = sessionBlackRepository.showBlackList(houseId);
        if (blackList != null && blackList.size() > 0) {
            return String.join(",", blackList);
        }
        return null;
    }


    @Override
    public User getBlack(String sessionId, String ip, String houseId) {
        return sessionBlackRepository.getSession(sessionId, ip, houseId);
    }

    @Override
    public void unblack(String sessionId, String houseId) {
        sessionBlackRepository.removeSession(sessionId, houseId);
    }

    @Override
    public Long size(String houseId) {
        return sessionRepository.size(houseId);
    }

    @Override
    public List getSession(String houseId) {
        return sessionRepository.getSession(houseId);
    }

    /**
     * ?????????????????????????????? stomp ?????????
     * </p>
     * ?????????http://stomp.github.io/stomp-specification-1.2.html#MESSAGE
     *
     * @param content payload ????????????????????????
     * @return ???????????????????????????????????????????????????????????????
     */
    private String getPayload(String messageType, Object content) {
        StringBuilder payload = new StringBuilder();
        String jsonString = JSON.toJSONString(content);
        payload.append(messageType)
                .append("\n")
                .append("content-type:application/json");
        try {
            payload.append("\n")
                    .append("content-length:").append(StringUtils.getLength(jsonString, StringUtils.ENCODE_UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        payload.append("\n")
                .append("\n")
                .append(jsonString);
        return payload.toString();
    }
}
