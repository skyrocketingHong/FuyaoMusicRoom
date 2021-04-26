package ninja.skyrocketing.fuyao.musicroom.service;

import ninja.skyrocketing.fuyao.musicroom.common.page.HulkPage;
import ninja.skyrocketing.fuyao.musicroom.common.page.Page;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import ninja.skyrocketing.fuyao.musicroom.model.MusicUser;
import ninja.skyrocketing.fuyao.musicroom.model.SongList;

import java.util.LinkedList;
import java.util.List;

/**
 * @author skyrocketing Hong
 */
public interface MusicService {
    /**
     * 接收点歌请求，推送点歌信息
     *
     * @param sessionId session id
     * @param request   music info
     * @return music info
     */
    Music toPick(String sessionId, Music request, String houseId, String source);

    /**
     * 切歌
     *
     * @return 将要播放的音乐
     */
    Music musicSwitch(String houseId);

    /**
     * get pick list
     *
     * @return linked list
     */
    LinkedList<Music> getPickList(String houseId);

    LinkedList<Music> getSortedPickList(List<Music> musicList, String houseId);

    Music getPlaying(String houseId);

    /**
     * 修改点歌列表顺序
     *
     * @param musicList -
     * @return -
     */
    Long modifyPickOrder(LinkedList<Music> musicList, String houseId);

    /**
     * 投票
     *
     * @param sessionId session id
     * @return 0：投票失败，已经参与过。1：投票成功
     */
    Long vote(String sessionId, String houseId);

    /**
     * 从集合中获取参与投票的人数
     *
     * @return 参与投票的人数
     */
    Long getVoteCount(String houseId);

    Music getQQMusic(String keyword);

    Music getNeteaseMusic(String keyword);

    /**
     * 删除音乐
     *
     * @param music music
     */
    boolean deletePickMusic(Music music, String houseId);

    /**
     * top pick music
     *
     * @param music -
     */
    void topPickMusic(Music music, String houseId);

    /**
     * black
     *
     * @param id music id
     * @return -
     */
    Long black(String id, String houseId);

    /**
     * un black
     *
     * @param id music id
     * @return -
     */
    Long unblack(String id, String houseId);

    /**
     * is black?
     *
     * @param id music id
     * @return -
     */
    boolean isBlack(String id, String houseId);

    /**
     * is picked ?
     *
     * @param id music id
     * @return
     */
    boolean isPicked(String id, String houseId);

    Object[] getMusicById(String id, String houseId);

    Page<List<Music>> searchMusic(String keyword, String source, HulkPage hulkPage);

    boolean clearPlayList(String houseId);

    String showBlackMusic(String houseId);

    Page<List<SongList>> searchSonglist(String keyword, String source, HulkPage hulkPage);

    Page<List<MusicUser>> searchUser(String keyword, String source, HulkPage hulkPage);

    boolean clearDefaultPlayList(String houseId);

    Integer addDefaultPlayList(String houseId, String[] playlistIds, String source);

    Long playlistSize(String houseId);

    void updateMusicUrl(Music result);
}
