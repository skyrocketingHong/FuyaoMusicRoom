package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.*;
import ninja.skyrocketing.fuyao.musicroom.repository.ConfigRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicPlayingRepository;
import ninja.skyrocketing.fuyao.musicroom.service.ConfigService;
import ninja.skyrocketing.fuyao.musicroom.service.MusicService;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author skyrocketing Hong
 * @create 2020-05-21 2:28
 */
@Controller
@Slf4j
public class HouseController {
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RoomContainer houseContainer;
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;

    @MessageMapping("/house/add")
    public void addHouse(House house, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        if (house.getName() == null || "".equals(house.getName())) {
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "????????????????????????"), houseId);
            return;
        }
        if (house.getNeedPwd() != null && house.getNeedPwd()) {
            if (house.getPassword() == null || "".equals(house.getPassword().trim())) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "????????????????????????"), houseId);
                return;
            } else if (StringUtils.isUrlSpecialCharacter(house.getPassword())) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "???????????????????????????????????????????%???#???&???=???+"), houseId);
                return;
            }
            house.setPassword(house.getPassword().trim());
        }
        if (houseContainer.contains(sessionId)) {
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "?????????????????????????????????????????????????????????????????????"), houseId);
            return;
        }
        if (houseContainer.size() >= fuyaoMusicRoomProperties.getHouseSize()) {
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "????????????????????????????????????????????????????????????????????????"), houseId);
            return;
        }
        String ip = (String) (accessor.getSessionAttributes().get("remoteAddress"));
        if (houseContainer.isBeyondIpHouse(ip, fuyaoMusicRoomProperties.getIpHouse())) {
            sessionService.send(sessionId,
                    MessageType.ADD_HOUSE,
                    Response.failure((Object) null, "?????????????????????????????????????????????????????????????????????????????????"), houseId);
            return;
        }
        if (house.getEnableStatus() != null && house.getEnableStatus()) {
            if (house.getRetainKey() == null || "".equals(house.getRetainKey().trim())) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "????????????????????????"), houseId);
                return;
            }
            RetainKey key = houseContainer.getRetainKey(house.getRetainKey());
            if (key == null) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "??????????????????????????????3?????????????????????????????????3?????????????????????????????????QQ???3055823718"), houseId);
                return;
            } else if (key.getIsUsed()) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "?????????????????????"), houseId);
                return;

            } else if (key.getExpireTime() != null && key.getExpireTime() < System.currentTimeMillis()) {
                sessionService.send(sessionId,
                        MessageType.ADD_HOUSE,
                        Response.failure((Object) null, "??????????????????"), houseId);
                return;
            }
            key.setHouseId(sessionId);
            key.setRemoteAddress(ip);
            key.setUsedTime(System.currentTimeMillis());
            key.setIsUsed(true);
            houseContainer.updateRetainKey(key);
        }
        WebSocketSession oldSession = sessionService.clearSession(sessionId, houseId);
        sessionService.send(oldSession,
                MessageType.ADD_HOUSE_START,
                Response.success((Object) null, "??????????????????"));
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
        house.setSessionId(sessionId);
        house.setRemoteAddress(ip);
        houseContainer.add(house);
        oldSession.getAttributes().put("houseId", sessionId);
        sessionService.putSession(oldSession, sessionId);
        // 1. send online
        Online online = new Online();
        online.setCount(fuyaoMusicRoomProperties.getSessions(sessionId).size());
        sessionService.send(MessageType.ONLINE, Response.success(online), sessionId);

        int oldHouseCount = fuyaoMusicRoomProperties.getSessions(houseId).size();
        online.setCount(oldHouseCount);
        if (oldHouseCount != 0) {
            sessionService.send(MessageType.ONLINE, Response.success(online), houseId);
        }
        // 4.??????????????????????????????
        sessionService.send(oldSession,
                MessageType.AUTH_ADMIN,
                Response.success((Object) null, "????????????????????????"));
        //???????????????????????????
        sessionService.send(MessageType.GOODMODEL, Response.success("GOOD", "goodlist"), sessionId);

        // 5.?????????????????????????????????
        if (oldHouseCount == 0 && !houseId.equals(FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID) && (houseContainer.get(houseId).getEnableStatus() == null || !houseContainer.get(houseId).getEnableStatus())) {
            houseContainer.destroy(houseId);
        }
        sessionService.send(oldSession,
                MessageType.ADD_HOUSE,
                Response.success(sessionId, "??????????????????"));
    }

    @MessageMapping("/house/enter")
    public void enterHouse(House house, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        if (houseId.equals(house.getId())) {
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.success((Object) null, "??????????????????"), houseId);
            return;
        }
        if (!houseContainer.contains(house.getId())) {
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.failure((Object) null, "?????????????????????"), houseId);
            return;
        }
        House matchHouse = houseContainer.get(house.getId());
        if (matchHouse.getNeedPwd() && !matchHouse.getPassword().equals(house.getPassword())) {
            sessionService.send(sessionId,
                    MessageType.ENTER_HOUSE,
                    Response.failure((Object) null, "??????????????????????????????"), houseId);
            return;
        }
        WebSocketSession oldSession = sessionService.clearSession(sessionId, houseId);
        sessionService.send(oldSession,
                MessageType.ENTER_HOUSE_START,
                Response.success((Object) null, "??????????????????"));
        oldSession.getAttributes().put("houseId", house.getId());
        User user = sessionService.putSession(oldSession, house.getId());
        // 1. send online
        Online online = new Online();
        online.setCount(fuyaoMusicRoomProperties.getSessions(house.getId()).size());
        sessionService.send(MessageType.ONLINE, Response.success(online), house.getId());
        //???????????????????????????????????????????????????????????????????????????
        int oldHouseCount = fuyaoMusicRoomProperties.getSessions(houseId).size();
        online.setCount(oldHouseCount);
        if (oldHouseCount != 0) {
            sessionService.send(MessageType.ONLINE, Response.success(online), houseId);
        }
        // 2. send playing
        Music playing = musicPlayingRepository.getPlaying(house.getId());
        if (playing != null) {
            musicService.updateMusicUrl(playing);
            playing.setIps(null);
            sessionService.send(oldSession, MessageType.MUSIC, Response.success(playing, "????????????"));
            // 3. send pick list
            LinkedList<Music> pickList = musicService.getPickList(house.getId());
            if (configService.getGoodModel(house.getId()) != null && configService.getGoodModel(house.getId())) {
                sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "goodlist"));
                sessionService.send(oldSession, MessageType.GOODMODEL, Response.success("GOOD", "goodlist"));
            } else {
                sessionService.send(oldSession, MessageType.PICK, Response.success(pickList, "????????????"));
                sessionService.send(oldSession, MessageType.GOODMODEL, Response.success("EXITGOOD", "goodlist"));

            }
        }
        // 4.????????????????????????
        if (user.getRole().equals("admin")) {
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.success((Object) null, "????????????"));
        } else {
            sessionService.send(oldSession,
                    MessageType.AUTH_ADMIN,
                    Response.failure((Object) null, "??????????????????"));
        }
        // 5.?????????????????????????????????
        if (oldHouseCount == 0 && !houseId.equals(FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID) && (houseContainer.get(houseId).getEnableStatus() == null || !houseContainer.get(houseId).getEnableStatus())) {
            houseContainer.destroy(houseId);
        }
        if (matchHouse.getAnnounce() != null && matchHouse.getAnnounce().getContent() != null && !"".equals(matchHouse.getAnnounce().getContent().trim())) {
            sessionService.send(oldSession, MessageType.ANNOUNCEMENT, Response.success(matchHouse.getAnnounce(), "????????????"));
        } else {
            sessionService.send(oldSession, MessageType.ANNOUNCEMENT, Response.success("", "????????????"));
        }
        sessionService.send(oldSession,
                MessageType.ENTER_HOUSE,
                Response.success(house.getId(), "??????????????????"));
    }


    @MessageMapping("/house/search")
    public void searchHouse(StompHeaderAccessor accessor) {
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        ArrayList<House> housesSimple = new ArrayList<>();
        for (House house : houses) {
            House houseSimple = new House();
            houseSimple.setName(house.getName());
            houseSimple.setId(house.getId());
            houseSimple.setDesc(house.getDesc());
            houseSimple.setCreateTime(house.getCreateTime());
            houseSimple.setNeedPwd(house.getNeedPwd());
            houseSimple.setPopulation(fuyaoMusicRoomProperties.getSessions(house.getId()).size());
            housesSimple.add(houseSimple);
        }
        String sessionId = accessor.getHeader("simpSessionId").toString();
        sessionService.send(sessionId, MessageType.SEARCH_HOUSE, Response.success(housesSimple, "????????????"), houseId);
    }

    /**
     * ??????????????????
     *
     * @param accessor
     */
    @MessageMapping("/house/retain/{retain}")
    public void houseRetain(@DestinationVariable boolean retain, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!"root".equals(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "???????????????"), houseId);
        } else {
            houseContainer.get(houseId).setEnableStatus(retain);
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "??????????????????????????????"), houseId);
        }
    }

    @MessageMapping("/house/houseuser")
    public void houseuser(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        List<User> users = sessionService.getSession(houseId);
        users.forEach(user -> {
            user.setRemoteAddress(StringUtils.desensitizeIPV4(user.getRemoteAddress()));
        });
        sessionService.send(sessionId, MessageType.HOUSE_USER, Response.success(users, "????????????????????????"), houseId);
    }
}
