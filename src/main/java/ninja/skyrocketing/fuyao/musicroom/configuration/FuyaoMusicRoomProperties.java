package ninja.skyrocketing.fuyao.musicroom.configuration;

import lombok.Data;
import lombok.ToString;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author skyrocketing Hong
 */
@Component
@ConfigurationProperties(prefix = "fuyaomusicroom")
@Data
@ToString
public class FuyaoMusicRoomProperties {
    public static final String HOUSE_DEFAULT_ID = "DEFAULT";
    public static final String HOUSE_DEFAULT_NAME = "一起来听歌";
    public static final String HOUSE_DEFAULT_DESC = "默认房间，欢迎试用";
    /**
     * springboot 在启动的时候将会初始化这个列表，从 defaultListFile 文件中逐行读取
     */
    private static List<String> defaultList = new LinkedList<>();
    /**
     * 自定义容器，用来装载 session
     */
    private final SessionContainer sessions = new SessionContainer();
    /**
     * 音乐到期时间，每首音乐的链接都会有一个失效时间
     */
    private Long musicExpireTime = 1200000L;
    /**
     * 重试次数，从音乐服务那里获取音乐失败的重试机会
     */
    private Integer retryCount = 3;
    /**
     * 投票通过率
     */
    private Float voteRate = 0.3F;
    /**
     * yml file 文件路径名，这是一个默认列表文件
     */
    private String defaultMusicFile;
    /**
     * root 密码
     */
    private String roleRootPassword = "";
    /**
     * admin 密码
     */
    private String roleAdminPassword = "";
    /**
     * 网易热门歌曲url
     */
    private String neteaseTopId = "";
    private String qqMusicTopId="";
    /**
     * 音乐服务
     */
    private String musicServeDomain = "";
    private String musicServeDomainQq = "";
    /**
     * mail send from
     */
    private String mailSendFrom = "";
    /**
     * mail send to
     */
    private String mailSendTo = "";
    private Integer houseSize;
    private Integer ipHouse;
    private Boolean goodModel = false;
    private Boolean randomModel = false;

    public static void setDefaultListByJob(ArrayList<String> list) {
        if (list != null && list.size() > 0) {
            defaultList.clear();
            defaultList.addAll(list);
        }
    }

    public static List<String> getDefaultListForRepository() {
        return defaultList;
    }

    public ConcurrentHashMap<String, WebSocketSession> getSessions(String houseId) {
        return sessions.getHouseSession(houseId);
    }

    public SessionContainer getSession() {
        return sessions;
    }

    public void removeSessions(String houseId) {
        sessions.remove(houseId);
    }

    /**
     * redis keys
     */
    @Component
    @ConfigurationProperties(prefix = "fuyaomusicroom.redis.keys")
    @Data
    @ToString
    public static class RedisKeys {
        private final FuyaoMusicRoomEnvironment fuyaoMusicRoomEnvironment;
        /**
         * 配置
         */
        private String configHash = "fuyaomusicroom_config";
        /**
         * 存放在线用户
         */
        private String sessionHash = "fuyaomusicroom_session";
        /**
         * 留存码
         */
        private String retainKeyHash = "fuyaomusicroom_retain_key";
        /**
         * 黑名单
         */
        private String sessionBlackHash = "fuyaomusicroom_session_black";
        /**
         * 默认播放列表，如果点歌列表为空则会从这里选出一首推到点歌列表的。
         * 这里存放的全部是 keyword，需要处理一下，拿到 Music info 再存到点歌列表
         */
        private String defaultSet = "fuyaomusicroom_default";
        /**
         * 点歌列表，存放 {@link Music} 对象
         */
        private String pickList = "fuyaomusicroom_pick";
        private String houses = "fuyaomusicroom_houses";
        /**
         * 播放列表，存放 {@link Music} 对象
         */
        private String playingList = "fuyaomusicroom_playing";
        /**
         * 音乐黑名单 id
         */
        private String blackSet = "fuyaomusicroom_black";
        /**
         * 投票集合，用来临时投票切换音乐，存放每个 session 的唯一 id
         */
        private String skipSet = "fuyaomusicroom_skip";
        /**
         * root key, config 子键名
         */
        private String redisRoleRoot = "role_root_password";
        /**
         * admin key, config 子键名
         */
        private String redisRoleAdmin = "role_admin_password";
        /**
         * 音乐最后推送时间, config 子键名
         */
        private String lastMusicPushTime = "last_music_push_time";
        /**
         * 正在播放的音乐时长, config 子键名
         */
        private String lastMusicDuration = "last_music_duration";
        /**
         * 音乐推送开关, config 子键名
         */
        private String switchMusicPush = "switch_music_push";
        private String switchMusicEnable = "switch_music_enable";
        private String searchMusicEnable = "search_music_enable";
        private String goodModel = "good_model";
        private String randomModel = "random_model";
        /**
         * 投票通过率, config 子键名
         */
        private String voteSkipRate = "vote_skip_rate";

        public RedisKeys(FuyaoMusicRoomEnvironment fuyaoMusicRoomEnvironment) {
            this.fuyaoMusicRoomEnvironment = fuyaoMusicRoomEnvironment;
        }

        public String getConfigHash() {
            return this.configHash + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getSessionHash() {
            return this.sessionHash + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getRetainKeyHash() {
            return this.retainKeyHash + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getSessionBlackHash() {
            return this.sessionBlackHash + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getDefaultSet() {
            return this.defaultSet + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getPickList() {
            return this.pickList + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getHouses() {
            return this.houses + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getPlayingList() {
            return this.playingList + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getBlackSet() {
            return blackSet + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }

        public String getSkipSet() {
            return this.skipSet + "_" + fuyaoMusicRoomEnvironment.getServerPort() + "_";
        }
    }

    public static class SessionContainer {
        private final Map<String, ConcurrentHashMap<String, WebSocketSession>> sessionContainer = new ConcurrentHashMap<>();

        public void remove(String houseId) {
            sessionContainer.remove(houseId);
        }

        public ConcurrentHashMap<String, WebSocketSession> getHouseSession(String houseId) {
            ConcurrentHashMap<String, WebSocketSession> houseSession;
            if (sessionContainer.containsKey(houseId)) {
                houseSession = sessionContainer.get(houseId);
                if (houseSession == null) {
                    houseSession = new ConcurrentHashMap<>();
                    sessionContainer.put(houseId, houseSession);
                }
            } else {
                houseSession = new ConcurrentHashMap<>();
                sessionContainer.put(houseId, houseSession);
            }
            return houseSession;
        }

        public Map<String, ConcurrentHashMap<String, WebSocketSession>> get() {
            return sessionContainer;
        }
    }

}
