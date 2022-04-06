package ninja.skyrocketing.fuyao.musicroom.handler;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import ninja.skyrocketing.fuyao.musicroom.model.Online;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicPlayingRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicVoteRepository;
import ninja.skyrocketing.fuyao.musicroom.service.ConfigService;
import ninja.skyrocketing.fuyao.musicroom.service.MusicService;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;

/**
 * @author skyrocketing Hong
 */
@Component
@Slf4j
public class FuyaoMusicRoomWebSocketHandlerAsync {

    @Autowired
    private FuyaoMusicRoomProperties musicBar;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private RoomContainer houseContainer;
    @Autowired
    private MusicVoteRepository musicVoteRepository;

    @Async
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String houseId = (String) session.getAttributes().get("houseId");
        House house = houseContainer.get(houseId);
        if (house == null) {
            houseId = houseContainer.getHouses().get(0).getId();
            session.getAttributes().put("houseId", houseId);
        }
        sessionService.putSession(session, houseId);
        int size = musicBar.getSessions(houseId).size();
        log.info("Connection established: {}, ip: {}, and now online: {}", session.getId(), session.getAttributes().get("remoteAddress").toString(), size);
        //要睡一下，不然报错
        Thread.sleep(500);
        sessionService.send(session, MessageType.NOTICE, Response.success((Object) null, "连接到服务器成功！"));

        // 1. send online
        Online online = new Online();
        online.setCount(size);
        sessionService.send(MessageType.ONLINE, Response.success(online), houseId);

        Object connectType = session.getAttributes().get("connectType");

        // 2. send playing
        Music playing = musicPlayingRepository.getPlaying(houseId);
        if (FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID.equals(houseId) || (connectType != null && !"".equals(connectType))) {
            if (house.getAnnounce() != null && house.getAnnounce().getContent() != null && !"".equals(house.getAnnounce().getContent().trim())) {
                sessionService.send(session, MessageType.ANNOUNCEMENT, Response.success(house.getAnnounce(), "房间公告"));
            } else {
                sessionService.send(session, MessageType.ANNOUNCEMENT, Response.success("", "房间公告"));
            }
        }
        while (playing == null || playing.getPushTime() == null || playing.getDuration() == null || ((playing.getPushTime() + playing.getDuration()) - System.currentTimeMillis() <= 0)) {
            Thread.sleep(500);
            playing = musicPlayingRepository.getPlaying(houseId);
        }
        musicService.updateMusicUrl(playing);
        sessionService.send(session, MessageType.MUSIC, Response.success(playing, "正在播放"));
        // 3. send pick list
        LinkedList<Music> pickList = musicService.getPickList(houseId);
        if (configService.getGoodModel(houseId) != null && configService.getGoodModel(houseId)) {
            sessionService.send(session, MessageType.PICK, Response.success(pickList, "goodlist"));
        } else {
            sessionService.send(session, MessageType.PICK, Response.success(pickList, "播放列表"));
        }
        log.info("发现有客户端连接, 已向该客户端: {} 发送正在播放的音乐: {}, 以及播放列表, 共 {} 首", session.getId(), playing.getName(), pickList.size());

    }


    @Async
    public void afterConnectionClosed(WebSocketSession session) {
        String houseId = (String) session.getAttributes().get("houseId");
        sessionService.clearSession(session, houseId);
        int size = musicBar.getSessions(houseId).size();
        log.info("Connection closed: {}, and now online: {}", session.getId(), size);
        Online online = new Online();
        online.setCount(size);
        if (size != 0) {
            musicVoteRepository.remove(session.getId(), houseId);
            sessionService.send(MessageType.ONLINE, Response.success(online, null), houseId);
        } else {
            House house = houseContainer.get(houseId);
            if (!FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID.equals(houseId) && house != null && (house.getEnableStatus() == null || !house.getEnableStatus())) {
                houseContainer.destroy(houseId);
            }
        }
    }

}
