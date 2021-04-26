package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.common.page.HulkPage;
import ninja.skyrocketing.fuyao.musicroom.common.page.Page;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.*;
import ninja.skyrocketing.fuyao.musicroom.service.ConfigService;
import ninja.skyrocketing.fuyao.musicroom.service.MusicService;
import ninja.skyrocketing.fuyao.musicroom.service.SessionService;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author skyrocketing Hong
 */
@Controller
@Slf4j
public class MusicController {
    private static final List<String> roles = new ArrayList<String>() {{
        add("root");
        add("admin");
    }};
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private MusicService musicService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ConfigService configService;

    @MessageMapping("/music/pick")
    public void pick(Music music, StompHeaderAccessor accessor) {
        log.info("收到点歌请求: {},{}", music.getName(), music.getId());
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String nickName = sessionService.getNickName(sessionId, houseId);
        Chat chat = new Chat();
        chat.setContent("点歌 " + music.getName());
        chat.setNickName(nickName);
        chat.setSendTime(System.currentTimeMillis());
        chat.setSessionId(sessionId);
        sessionService.send(MessageType.CHAT, Response.success(chat), houseId);
        String role = sessionService.getRole(sessionId, houseId);
        Boolean enableSearch = configService.getEnableSearch(houseId);
        if (enableSearch != null && !enableSearch && !roles.contains(role) && !role.contains("picker")) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "当前禁止点歌"), houseId);
            return;
        }
        Music pick;
        String name = music.getName();
        // 点歌结果反馈
        if ("QQMusic".equals(music.getSource())) {
            if (music.getId() != null && !"".equals(music.getId())) {
                pick = musicService.getQQMusic(music.getId());
            } else {
                pick = musicService.getQQMusic(name);
            }
            pick.setSource("QQMusic");
        } else {
            if (music.getId() != null && !"".equals(music.getId())) {
                pick = musicService.getNeteaseMusic(music.getId());
            } else {
                pick = musicService.getNeteaseMusic(name);
            }
            pick.setSource("Netease");
        }
        String pickUrl = pick.getUrl();
        Long pickDuration = pick.getDuration();
        if (pickUrl.isEmpty()) {
            log.info("点歌失败, 可能音乐不存在, 关键字: {}, 即将向客户端反馈点歌失败消息", name);
            sessionService.send(MessageType.NOTICE, Response.failure((Object) null, "点歌失败,暂无此音乐或须单独购买"), houseId);
        } else if (musicService.isBlack(music.getId(), houseId)) {
            log.info("点歌失败, 音乐: {} 已被拉黑, 关键字: {}, 即将向客户端反馈点歌失败消息", music.getId(), name);
            sessionService.send(MessageType.NOTICE, Response.failure((Object) null, "点歌失败, 音乐已被拉黑"), houseId);
        } else if (musicService.isPicked(music.getId(), houseId)) {
            log.info("点歌失败, 音乐: {} 已在播放列表中, 关键字: {}, 即将向客户端反馈点歌失败消息", music.getId(), name);
            sessionService.send(MessageType.NOTICE, Response.failure((Object) null, "点歌失败, 已在播放列表"), houseId);
        } else {
            log.info("点歌成功, 音乐: {}, 时长: {}, 链接: {}, 即将向客户端广播消息以及列表", pick.getName(), pickDuration, pickUrl);
            musicService.toPick(sessionId, pick, houseId, music.getSource());
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "点歌成功"), houseId);
            log.info("点歌成功");
            sessionService.send(MessageType.PICK, Response.success(pickList, "点歌列表"), houseId);
            log.info("向客户端发送点歌列表, 共 {} 首, 列表: {}", pickList.size(), pickList);
        }
    }

    @MessageMapping("/music/good/{musicId}")
    public void good(@DestinationVariable String musicId, StompHeaderAccessor accessor) {
        // 点歌消息反馈
        String sessionId = accessor.getHeader("simpSessionId").toString();
        accessor.getSessionAttributes().get("remoteAddress");
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String nickName = sessionService.getNickName(sessionId, houseId);
        Chat chat = new Chat();
        Boolean goodModel = configService.getGoodModel(houseId);
        if (goodModel == null || !goodModel) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "当前不是点赞模式"), houseId);
            return;
        }
        String ip = (String) (accessor.getSessionAttributes().get("remoteAddress"));
        Object[] musics = musicService.getMusicById(musicId, houseId);
        if (musics == null) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "点歌列表未发现此歌"), houseId);
            return;
        }
        Music music = (Music) musics[0];
        HashSet<String> ips = music.getIps();
        if (ips.size() == 0) {
            ips.add(ip);
            music.setGoodTime(System.currentTimeMillis());
            music.setIps(ips);
        } else {
            if (ips.contains(ip)) {
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "已点赞过" + music.getName() + "，总票数" + ips.size()), houseId);
                return;
            }
            music.setGoodTime(System.currentTimeMillis());
            ips.add(ip);
        }
        chat.setContent(music.getName() + "点赞数" + ips.size());
        chat.setNickName(nickName);
        chat.setSessionId(sessionId);
        sessionService.send(MessageType.CHAT, Response.success(chat), houseId);
        // 点歌结果反馈
        LinkedList pickList = musicService.getSortedPickList((List<Music>) musics[1], houseId);
        sessionService.send(MessageType.PICK, Response.success(pickList, "点歌列表"), houseId);
    }

    @MessageMapping("/music/volumn/{volumn}")
    public void voice(@DestinationVariable Double volumn, StompHeaderAccessor accessor) {
        // 点歌消息反馈
        log.info("收到调整音量请求: {}", volumn);
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "调整音量成功"), houseId);
            sessionService.send(MessageType.VOLUMN, Response.success(volumn, "调整后的音量"), houseId);
        }
    }

    @MessageMapping("/music/vote/{voteRate}")
    public void voteRate(@DestinationVariable Float voteRate, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (voteRate == null || voteRate > 1 || voteRate <= 0) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "投票率在(0,1]区间"), houseId);
            return;
        }
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            configService.setVoteRate(voteRate, houseId);
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "投票率被修改为" + voteRate), houseId);
        }
    }

    @MessageMapping("/music/skip/vote")
    public void skip(StompHeaderAccessor accessor) {
        // 投票消息反馈
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String nickName = sessionService.getNickName(sessionId, houseId);
        Chat chat = new Chat();
        chat.setSessionId(sessionId);
        chat.setSendTime(System.currentTimeMillis());
        chat.setContent("投票切歌");
        chat.setNickName(nickName);
        String role = sessionService.getRole(sessionId, houseId);
        Boolean enableSwitch = configService.getEnableSwitch(houseId);
        sessionService.send(MessageType.CHAT, Response.success(chat), houseId);
        if (enableSwitch != null && !enableSwitch && !roles.contains(role) && !role.contains("voter")) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "禁止切歌"), houseId);
            return;
        }
        Long voteCount = 0L;
        Long vote = 0L;
        Integer size = fuyaoMusicRoomProperties.getSessions(houseId).size();
        if (roles.contains(role)) {
            // 管理员
            configService.setPushSwitch(true, houseId);
            log.info("推送开关开启, 原因: 投票通过 - 管理员参与投票");
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "切歌成功"), houseId);
        } else {
            // 投票
            vote = musicService.vote(sessionId, houseId);
            voteCount = musicService.getVoteCount(houseId);
            if (vote == 0) {
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你已经投过票了,当前状态：" + voteCount + "/" + size), houseId);
                log.info("你已经投过票了");
            } else {
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, voteCount + "/" + size + " 投票成功"), houseId);
            }
        }
        log.info("投票成功");
        Float voteRate = configService.getVoteRate(houseId);
        if (voteCount == 1 && vote != 0 && voteCount < size * voteRate) {
            sessionService.send(MessageType.NOTICE, Response.success((Object) null, "有人希望切歌, 如果支持请发送“投票切歌”"), houseId);
            log.info("有人希望切歌, 如果支持请发送“投票切歌”");
        }
    }

    /**
     * 调整音乐顺序
     *
     * @return 调整后的点歌列表
     */
    @MessageMapping("/music/order")
    @SendTo("/topic/music/order")
    public Response modifyPickOrder(LinkedList<Music> musicList, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            return Response.failure((Object) null, "你没有权限");
        }

        musicService.modifyPickOrder(musicList, houseId);
        return Response.success(musicList, "调整成功");
    }

    @MessageMapping("/music/clear")
    public void clearPlayerList(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            musicService.clearPlayList(houseId);
            LinkedList<Music> pickList = new LinkedList<>();
            Music music = musicService.getPlaying(houseId);
            pickList.add(music);
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "清空列表成功"), houseId);
            sessionService.send(MessageType.PICK, Response.success(pickList, "清空后的播放列表"), houseId);

        }
    }

    @MessageMapping("/music/clearDefaultPlayList")
    public void clearDefaultPlayerList(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            musicService.clearDefaultPlayList(houseId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "清空列表成功"), houseId);
        }
    }

    /**
     * 删除播放列表的音乐
     *
     * @param music    music
     * @param accessor accessor
     */
    @MessageMapping("/music/delete")
    public void deletePickMusic(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        music.setName(music.getId());
        if (!roles.contains(role)) {
            music.setSessionId(sessionId);
        }
        if (musicService.deletePickMusic(music, houseId)) {
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            log.info("session: {} 删除音乐: {} 已成功, 即将广播删除后的播放列表", sessionId, music.getName());
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "删除成功"), houseId);
            sessionService.send(MessageType.PICK, Response.success(pickList, "删除后的播放列表"), houseId);
        } else {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "本人暂未点播此歌或请输入完整歌名"), houseId);
        }
    }

    /**
     * 置顶播放列表的音乐
     *
     * @param music    music
     * @param accessor accessor
     */
    @MessageMapping("/music/top")
    public void topPickMusic(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试置顶音乐但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            musicService.topPickMusic(music, houseId);
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            log.info("session: {} 置顶音乐: {} 已成功, 即将广播置顶操作后的播放列表", sessionId, music.getName());
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "置顶成功"), houseId);
            sessionService.send(MessageType.PICK, Response.success(pickList, "置顶后的播放列表"), houseId);
        }
    }

    @MessageMapping("/music/black")
    public void black(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试拉黑音乐: {}, 没有权限, 已被阻止", sessionId, music.getId());
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            Long black = musicService.black(music.getId(), houseId);
            if (black > 0) {
                log.info("session: {} 尝试拉黑音乐: {} 已成功", sessionId, music.getName());
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "音乐拉黑成功"), houseId);
            } else {
                log.info("session: {} 尝试拉黑音乐: {} 已成功, 重复拉黑", sessionId, music.getName());
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "音乐重复拉黑"), houseId);
            }
        }
    }

    /**
     * 禁止切歌
     *
     * @param accessor
     */
    @MessageMapping("/music/banswitch/{ban}")
    public void banswitch(@DestinationVariable boolean ban, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试禁止切歌, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            configService.setEnableSwitch(!ban, houseId);
            if (ban) {
                log.info("session: {} 禁止切歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "禁止切歌功能成功"), houseId);
            } else {
                log.info("session: {} 启用切歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "启用切歌功能成功"), houseId);
            }
        }
    }

    /**
     * 点赞模式的切换
     *
     * @param accessor
     */
    @MessageMapping("/music/goodmodel/{good}")
    public void goodModel(@DestinationVariable boolean good, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试点赞模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            configService.setGoodModel(good, houseId);
            if (good) {
                log.info("session: {} 进入点赞模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入点赞模式"), houseId);
                sessionService.send(MessageType.GOODMODEL, Response.success("GOOD", "goodlist"), houseId);

            } else {
                log.info("session: {} 退出点赞模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出点赞模式"), houseId);
                sessionService.send(MessageType.GOODMODEL, Response.success("EXITGOOD", "goodlist"), houseId);
            }
        }
    }

    @MessageMapping("/music/randommodel/{random}")
    public void randomModel(@DestinationVariable boolean random, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试随机模式, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            configService.setRandomModel(random, houseId);
            if (random) {
                log.info("session: {} 进入随机模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "进入随机模式"), houseId);
            } else {
                log.info("session: {} 退出随机模式已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "退出随机模式"), houseId);
            }
        }
    }

    @MessageMapping("/music/banchoose/{ban}")
    public void banchoose(@DestinationVariable boolean ban, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试禁止点歌, 没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            configService.setEnableSearch(!ban, houseId);
            if (ban) {
                log.info("session: {} 禁止点歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "禁止点歌功能成功"), houseId);
            } else {
                log.info("session: {} 启用点歌功能已成功", sessionId);
                sessionService.send(MessageType.NOTICE, Response.success((Object) null, "启用点歌功能成功"), houseId);
            }
        }
    }

    @MessageMapping("/music/unblack")
    public void unblack(Music music, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试拉黑音乐但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            Long unblack = musicService.unblack(music.getId(), houseId);
            if (unblack > 0) {
                log.info("session: {} 尝试漂白音乐: {} 已成功", sessionId, music.getName());
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "音乐漂白成功"), houseId);
            } else {
                log.info("session: {} 尝试漂白音乐: {} 漂白异常, 可能不在黑名单中", sessionId, music.getName());
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "音乐漂白异常, 可能不在黑名单中"), houseId);
            }
        }
    }

    @MessageMapping("/music/setDefaultPlaylist")
    public void setDefaultPlaylist(SongList songList, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试设置默认播放列表但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            String id = songList.getId();
            if (id.isEmpty()) {
                sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "歌单id不能为空"), houseId);
            } else {
                if (StringUtils.isPlayListIds(id)) {
                    String[] idsStr = StringUtils.splitPlayListIds(id);
                    long count = musicService.addDefaultPlayList(houseId, idsStr, songList.getSource());
                    sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "已添加" + count + "首歌至默认播放列表"), houseId);
                } else {
                    sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "多个歌单id以逗号或者空格隔开"), houseId);
                }
            }
        }
    }

    @MessageMapping("/music/blackmusic")
    public void blackmusic(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            log.info("session: {} 尝试展示黑名单但没有权限, 已被阻止", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            String blackMusic = musicService.showBlackMusic(houseId);
            if (blackMusic.isEmpty()) {
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, blackMusic), houseId);
            } else {
                sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "暂无拉黑列表"), houseId);
            }
        }
    }

    @MessageMapping("/music/playlistSize")
    public void playlistSize(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String role = sessionService.getRole(sessionId, houseId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"), houseId);
        } else {
            Long size = musicService.playlistSize(houseId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.success((Object) null, "默认列表歌曲数" + size), houseId);
        }
    }

    @MessageMapping("/music/search")
    public void search(Music music, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String keyword = music.getName();
        String source = music.getSource();
        if (keyword.isBlank()) {
            log.info("session: {} 尝试搜索音乐, 但关键字为空", sessionId);
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "请输入要搜索的关键字"), houseId);
            return;
        }
        Page<List<Music>> page = musicService.searchMusic(keyword, source, hulkPage);
        log.info("session: {} 尝试搜索音乐, 关键字: {}, 即将向该用户推送结果", accessor.getHeader("simpSessionId"), keyword);
        sessionService.send(sessionId, MessageType.SEARCH, Response.success(page, "搜索结果"), houseId);
    }

    @MessageMapping("/music/searchsonglist")
    public void searchsonglist(SongList songList, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String keyword = songList.getName();
        String source = songList.getSource();
        if (keyword.isEmpty()) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "请输入要搜索的用户帐号"), houseId);
            return;
        }
        Page<List<SongList>> page = musicService.searchSonglist(keyword, source, hulkPage);
        log.info("session: {} 尝试搜索歌单, 关键字: {},{}, 即将向该用户推送结果", accessor.getHeader("simpSessionId"), keyword, source);
        sessionService.send(sessionId, MessageType.SEARCH_SONGLIST, Response.success(page, "搜索结果"), houseId);
    }

    @MessageMapping("/music/searchuser")
    public void searchuser(MusicUser musicUser, HulkPage hulkPage, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String houseId = (String) accessor.getSessionAttributes().get("houseId");
        String keyword = musicUser.getNickname();
        if (keyword.isEmpty()) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "请输入要搜索的用户昵称"), houseId);
            return;
        }
        String source = musicUser.getSource();
        Page<List<MusicUser>> page = musicService.searchUser(keyword, source, hulkPage);
        log.info("session: {} 尝试搜索用户, 关键字: {},{}, 即将向该用户推送结果", accessor.getHeader("simpSessionId"), musicUser.getNickname(), musicUser.getSource());
        sessionService.send(sessionId, MessageType.SEARCH_USER, Response.success(page, "搜索结果"), houseId);
    }
}
