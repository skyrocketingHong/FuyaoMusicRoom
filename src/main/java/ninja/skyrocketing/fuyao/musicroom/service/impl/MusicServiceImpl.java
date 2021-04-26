package ninja.skyrocketing.fuyao.musicroom.service.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.page.HulkPage;
import ninja.skyrocketing.fuyao.musicroom.common.page.Page;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.*;
import ninja.skyrocketing.fuyao.musicroom.repository.*;
import ninja.skyrocketing.fuyao.musicroom.service.MusicService;
import ninja.skyrocketing.fuyao.musicroom.util.MusicSearchUtils;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author skyrocketing Hong
 */
@Service
@Slf4j
public class MusicServiceImpl implements MusicService {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private MusicPickRepository musicPickRepository;
    @Autowired
    private MusicDefaultRepository musicDefaultRepository;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private MusicBlackRepository musicBlackRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 把音乐放进点歌列表
     */
    @Override
    public Music toPick(String sessionId, Music music, String houseId, String source) {
        music.setSessionId(sessionId);
        music.setPickTime(System.currentTimeMillis());
        music.setSource(source);
        User user = sessionRepository.getSession(sessionId, houseId);
        music.setNickName(user == null ? "" : user.getNickName());
        musicPickRepository.leftPush(music, houseId);
        log.info("点歌成功, 音乐: {}, 已放入点歌列表", music.getName());
        return music;
    }

    /**
     * 音乐切换
     */
    @Override
    public Music musicSwitch(String houseId) {
        Music result;
        if (musicPickRepository.size(houseId) < 1) {
            String defaultPlayListHouse = houseId;
            if (musicDefaultRepository.size(houseId) == 0) {
                defaultPlayListHouse = "";
            }
            String keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
            log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
            result = StringUtils.isQQMusicId(keyword) ? this.getQQMusic(keyword) : this.getNeteaseMusic(keyword);
            while (result == null || result.getUrl() == null) {
                musicDefaultRepository.remove(keyword, defaultPlayListHouse);
                log.info("该歌曲url为空:{}", keyword);
                if (musicDefaultRepository.size(houseId) == 0) {
                    defaultPlayListHouse = "";
                }
                keyword = musicDefaultRepository.randomMember(defaultPlayListHouse);
                log.info("选歌列表为空, 已从默认列表中随机选择一首: {}", keyword);
                result = StringUtils.isQQMusicId(keyword) ? this.getQQMusic(keyword) : this.getNeteaseMusic(keyword);
            }
            result.setPickTime(System.currentTimeMillis());
            result.setNickName("system");
            musicPlayingRepository.leftPush(result, houseId);
        } else {
            if (configRepository.getRandomModel(houseId) == null || !configRepository.getRandomModel(houseId)) {
                result = musicPlayingRepository.pickToPlaying(houseId);
            } else {
                result = musicPlayingRepository.randomToPlaying(houseId);
            }
            result.setIps(null);
        }
        updateMusicUrl(result);
        musicPlayingRepository.keepTheOne(houseId);
        return result;
    }

    @Override
    public void updateMusicUrl(Music result) {
        // 防止选歌的时间超过音乐链接的有效时长
        if (result.getPickTime() + fuyaoMusicRoomProperties.getMusicExpireTime() <= System.currentTimeMillis()) {
            String musicUrl;
            if ("QQMusic".equals(result.getSource())) {
                musicUrl = MusicSearchUtils.getQQMusicLinkById(
                        fuyaoMusicRoomProperties.getMusicServeDomainQq(),
                        fuyaoMusicRoomProperties.getRetryCount(),
                        result.getId()
                );
            } else {
                musicUrl = MusicSearchUtils.getNeteaseMusicUrlById(
                        fuyaoMusicRoomProperties.getMusicServeDomain(),
                        fuyaoMusicRoomProperties.getRetryCount(),
                        result.getId()
                );
            }
            if (Objects.nonNull(musicUrl)) {
                result.setUrl(musicUrl);
                log.info("音乐链接已超时, 已更新链接");
            } else {
                log.info("音乐链接更新失败, 接下来客户端音乐链接可能会失效, 请检查音乐服务");
            }
        }
    }

    /**
     * 获取点歌列表
     *
     * @return linked list
     */
    @Override
    public LinkedList<Music> getPickList(String houseId) {
        LinkedList<Music> result = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        Music playing = musicPlayingRepository.getPlaying(houseId);
        try {
            Collections.reverse(pickMusicList);
            result.add(playing);
            result.addAll(pickMusicList);
            result.forEach(m -> {
                m.setLyric("");
                m.setIps(null);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    public Music getPlaying(String houseId) {
        return musicPlayingRepository.getPlaying(houseId);
    }

    @Override
    public LinkedList<Music> getSortedPickList(List<Music> musicList, String houseId) {
        return sortPickedList(getPickList(houseId));
    }

    private LinkedList<Music> sortPickedList(LinkedList<Music> pickMusicList) {
        pickMusicList.sort(new MusicComparator());
        return pickMusicList;
    }

    @Override
    public Long modifyPickOrder(LinkedList<Music> musicList, String houseId) {
        musicPickRepository.reset(houseId);
        return musicPickRepository.leftPushAll(houseId, musicList);
    }

    /**
     * 投票
     *
     * @return 失败 = 0, 成功 >= 1
     */
    @Override
    public Long vote(String sessionId, String houseId) {
        return musicVoteRepository.add(houseId, sessionId);
    }

    /**
     * 从 redis set 中获取参与投票的人数
     *
     * @return 参与投票人数
     */
    @Override
    public Long getVoteCount(String houseId) {
        return musicVoteRepository.size(houseId);
    }

    @Override
    public Music getQQMusic(String keyword) {
        String api = fuyaoMusicRoomProperties.getMusicServeDomainQq();
        int failLimit = fuyaoMusicRoomProperties.getRetryCount();
        if (StringUtils.isQQMusicId(keyword)) {
            return MusicSearchUtils.getQQMusicByIdOrKeyword(api, failLimit, keyword, true);
        } else {
            return MusicSearchUtils.getQQMusicByIdOrKeyword(api, failLimit, keyword, false);
        }
    }

    @Override
    public Music getNeteaseMusic(String keyword) {
        String api = fuyaoMusicRoomProperties.getMusicServeDomain();
        int failLimit = fuyaoMusicRoomProperties.getRetryCount();
        if (StringUtils.isNeteaseMusicId(keyword)) {
            return MusicSearchUtils.getNeteaseMusicByIdOrKeyword(api, failLimit, keyword, true);
        } else {
            return MusicSearchUtils.getNeteaseMusicByIdOrKeyword(api, failLimit, keyword, false);
        }
    }

    @Override
    public boolean deletePickMusic(Music music, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        boolean isDeleted = false;
        for (int i = 0; i < pickMusicList.size(); i++) {
            if (music.getSessionId() != null) {
                if (pickMusicList.get(i).getName().equals(music.getName()) && music.getSessionId().equals(pickMusicList.get(i).getSessionId())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            } else {
                if (music.getId().equals(pickMusicList.get(i).getId()) || pickMusicList.get(i).getName().equals(music.getName())) {
                    pickMusicList.remove(pickMusicList.get(i));
                    isDeleted = true;
                    break;
                }
            }
        }
        if (isDeleted) {
            musicPickRepository.reset(houseId);
            if (pickMusicList.size() != 0) {
                musicPickRepository.rightPushAll(houseId, pickMusicList.toArray());
            }
        }
        return isDeleted;
    }

    @Override
    public void topPickMusic(Music music, String houseId) {
        List<Music> newPickMusicList = new LinkedList<>();
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (int i = 0; i < pickMusicList.size(); i++) {
            if (music.getId().equals(pickMusicList.get(i).getId())) {
                Music music2 = pickMusicList.get(i);
                music2.setTopTime(System.currentTimeMillis());
                newPickMusicList.add(music2);
                pickMusicList.remove(pickMusicList.get(i));
                break;
            }
        }
        pickMusicList.addAll(newPickMusicList);
        musicPickRepository.reset(houseId);
        musicPickRepository.rightPushAll(houseId, pickMusicList.toArray());
    }

    @Override
    public Long black(String id, String houseId) {
        return musicBlackRepository.add(id, houseId);
    }

    @Override
    public Long unblack(String id, String houseId) {
        return musicBlackRepository.remove(id, houseId);
    }

    @Override
    public boolean isBlack(String id, String houseId) {
        return musicBlackRepository.isMember(id, houseId);
    }

    @Override
    public boolean isPicked(String id, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return true;
            }
        }
        Music playing = musicPlayingRepository.getPlaying(houseId);
        return playing.getId().equals(id);
    }

    @Override
    public Object[] getMusicById(String id, String houseId) {
        List<Music> pickMusicList = musicPickRepository.getPickMusicList(houseId);
        for (Music music : pickMusicList) {
            if (music.getId().equals(id)) {
                return new Object[]{music, pickMusicList};
            }
        }
        return null;
    }

    @Override
    public HulkPage searchMusic(String keyword, String source, HulkPage hulkPage) {
        boolean isSonglist = keyword.matches("\\*.+");
        if ("QQMusic".equals(source)) {
            String api = fuyaoMusicRoomProperties.getMusicServeDomainQq();
            if (isSonglist) {
                keyword = keyword.replaceFirst("\\*", "");
                if ("热歌榜".equals(keyword)) {
                    return MusicSearchUtils.getQQMusicMusicSonglistById(api, "7217671025", hulkPage);
                }
                return MusicSearchUtils.getQQMusicMusicSonglistById(api, keyword, hulkPage);
            } else {
                return MusicSearchUtils.getQQMusicByKeyword(api, keyword, hulkPage);
            }
        } else if ("Netease".equals(source)) {
            String api = fuyaoMusicRoomProperties.getMusicServeDomain();
            int failLimit = fuyaoMusicRoomProperties.getRetryCount();
            if (isSonglist) {
                keyword = keyword.replaceFirst("\\*", "");
                if ("热歌榜".equals(keyword)) {
                    return MusicSearchUtils.getNeteaseMusicSonglistHulkPageById(api, fuyaoMusicRoomProperties.getNeteaseTopId(), failLimit, hulkPage);
                }
                return MusicSearchUtils.getNeteaseMusicSonglistHulkPageById(api, keyword, failLimit, hulkPage);
            } else {
                return MusicSearchUtils.getNeteaseMusicByKeyword(api, keyword, hulkPage);
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean clearPlayList(String houseId) {
        musicPickRepository.reset(houseId);
        return true;
    }

    @Override
    public String showBlackMusic(String houseId) {
        Set blackList = musicBlackRepository.showBlackList(houseId);
        if (blackList != null && blackList.size() > 0) {
            return String.join(",", blackList);
        }
        return null;
    }

    @Override
    public Page<List<SongList>> searchSonglist(String keyword, String source, HulkPage hulkPage) {
        String api;
        if ("QQMusicSonglist".equals(source)) {
            api = fuyaoMusicRoomProperties.getMusicServeDomainQq();
            return MusicSearchUtils.getQQMusicSonglistByKeyword(api, keyword, hulkPage);
        } else if ("QQUser".equals(source)) {
            api = fuyaoMusicRoomProperties.getMusicServeDomainQq();
            return MusicSearchUtils.getQQMusicSonglistByUser(api, keyword, hulkPage);
        } else if ("NeteaseUser".equals(source)) {
            api = fuyaoMusicRoomProperties.getMusicServeDomain();
            return MusicSearchUtils.getNeteaseSonglistByUser(api, keyword, hulkPage);
        } else if ("NeteaseSonglist".equals(source)){
            api = fuyaoMusicRoomProperties.getMusicServeDomain();
            return MusicSearchUtils.getNeteaseSonglistByKeyword(api, keyword, hulkPage);
        } else {
            return null;
        }
    }

    @Override
    public Page<List<MusicUser>> searchUser(String keyword, String source, HulkPage hulkPage) {
        String api = fuyaoMusicRoomProperties.getMusicServeDomain();
        return MusicSearchUtils.getNeteaseUserByKeyword(api, keyword, hulkPage);
    }

    @Override
    public boolean clearDefaultPlayList(String houseId) {
        musicDefaultRepository.destroy(houseId);
        return true;
    }

    @Override
    public Integer addDefaultPlayList(String houseId, String[] playlistIds, String source) {
        String api;
        int count = 0;
        if ("Netease".equals(source)) {
            api = fuyaoMusicRoomProperties.getMusicServeDomain();
            for (String id : playlistIds) {
                if (id != null && id.startsWith("*")) {
                    count++;
                    String[] songId = new String[1];
                    songId[0] = id.substring(1);
                    musicDefaultRepository.add(songId, houseId);
                } else {
                    String[] list = MusicSearchUtils.getNeteaseMusicSonglistStringArrayById(api, id);
                    if (list != null && list.length > 0) {
                        musicDefaultRepository.add(list, houseId);
                        count += list.length;
                    }
                }
            }
            return count;
        } else if ("QQMusic".equals(source)) {
            api = fuyaoMusicRoomProperties.getMusicServeDomainQq();
            for (String id : playlistIds) {
                if (id != null && id.startsWith("*")) {
                    count++;
                    String[] songId = new String[1];
                    songId[0] = id.substring(1);
                    musicDefaultRepository.add(songId, houseId);
                } else {
                    String[] list = MusicSearchUtils.getQQMusicSonglistByKeyword(api, id);
                    if (list != null && list.length > 0) {
                        musicDefaultRepository.add(list, houseId);
                        count += list.length;
                    }
                }
            }
            return count;
        } else {
            return null;
        }
    }

    @Override
    public Long playlistSize(String houseId) {
        return musicDefaultRepository.size(houseId);
    }
}