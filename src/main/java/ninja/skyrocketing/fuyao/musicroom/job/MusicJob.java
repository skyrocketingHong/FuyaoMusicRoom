package ninja.skyrocketing.fuyao.musicroom.job;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.model.MessageType;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import ninja.skyrocketing.fuyao.musicroom.repository.ConfigRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicPlayingRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicVoteRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionRepository;
import ninja.skyrocketing.fuyao.musicroom.service.MusicService;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * @author skyrocketing Hong
 */
@Component
@Slf4j
public class MusicJob {

    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private RoomContainer houseContainer;
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;

    /**
     * 广播条件：第一次启动时 playing 为空、音乐播放完毕、投票切歌
     */
    @Scheduled(fixedRate = 500)
    private void sendIfSufficient() {
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        for (House house : houses) {
            //没人就不推送
            if (fuyaoMusicRoomProperties.getSessions(house.getId()).size() > 0) {
                try {
                    if (this.isPlayingNull(house.getId())) {
                        configRepository.setPushSwitch(true, house.getId());
                        log.info("推送开关开启, 原因: 首次启动" + house.getName());
                    } else if (this.isPlayingOver(house.getId())) {
                        configRepository.setPushSwitch(true, house.getId());
                        log.info("推送开关开启, 原因: 上一首播放完毕");
                    } else if (this.isPlayingSkip(house.getId())) {
                        configRepository.setPushSwitch(true, house.getId());
                        log.info("推送开关开启, 原因: 投票通过");
                    }
                    if (this.isPushSwitchOpen(house.getId())) {
                        log.info("检测到推送开关已开启");
                        Music music = musicService.musicSwitch(house.getId());
                        long pushTime = System.currentTimeMillis();
                        if (music.getDuration() == null) {
                            music.setDuration(300000L);
                        }
                        configRepository.setLastMusicPushTimeAndDuration(pushTime, music.getDuration(), house.getId());
                        music.setPushTime(pushTime);
                        sessionService.send(MessageType.MUSIC, Response.success(music, "正在播放"), house.getId());
                        //更新music pushTime
                        musicPlayingRepository.leftPush(music, house.getId());
                        musicPlayingRepository.keepTheOne(house.getId());
                        log.info("已保存推送时间和音乐时长" + house.getName());
                        configRepository.setPushSwitch(false, house.getId());
                        log.info("已关闭音乐推送开关" + house.getName());
                        musicVoteRepository.reset(house.getId());
                        log.info("已重置投票");
                        log.info("已向所有客户端推送音乐, 音乐: {}, 时长: {}, 推送时间: {}, 链接: {}", music.getName(), music.getDuration(), pushTime, music.getUrl());
                        LinkedList<Music> result = musicService.getPickList(house.getId());
                        sessionService.send(MessageType.PICK, Response.success(result, "播放列表"), house.getId());
                        log.info("已向客户端推送播放列表, 共 {} 首, 列表: {}", result.size(), result);
                    }
                } catch (Exception e) {
                    try {
                        configRepository.destroy(house.getId());
                        musicPlayingRepository.destroy(house.getId());
                    } catch (Exception e2) {
                        log.error("定时任务销毁config及playing异常[{}]", e2.getMessage());
                    }
                    log.error("houseName:{},houseId:{},message:[{}]", house.getName(), house.getId(), e.getMessage());
                }
            }
        }
    }

    private boolean isPushSwitchOpen(String houseId) {
        Boolean pushSwitch = configRepository.getPushSwitch(houseId);
        return pushSwitch != null && pushSwitch;
    }

    private boolean isPlayingSkip(String houseId) {
        Long voteSize = musicVoteRepository.size(houseId);
        Integer sessionSize = fuyaoMusicRoomProperties.getSessions(houseId).size();
        Float voteRate = configRepository.getVoteRate(houseId);
        if (voteSize != null && voteSize != 0 && voteRate != null) {
            return voteSize >= sessionSize * voteRate;
        }
        return false;
    }

    private boolean isPlayingOver(String houseId) {
        Long lastMusicDuration = configRepository.getLastMusicDuration(houseId);
        Long lastMusicPushTime = configRepository.getLastMusicPushTime(houseId);
        if (null == lastMusicDuration || null == lastMusicPushTime) {
            return true;
        }
        return (lastMusicPushTime + lastMusicDuration) - System.currentTimeMillis() <= 0;
    }

    private boolean isPlayingNull(String houseId) {
        return null == musicPlayingRepository.getPlaying(houseId);
    }

}
