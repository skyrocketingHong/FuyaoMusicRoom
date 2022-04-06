package ninja.skyrocketing.fuyao.musicroom.configuration;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.model.RetainKey;
import ninja.skyrocketing.fuyao.musicroom.repository.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author skyrocketing Hong
 * @create 2020-05-20 23:28
 */
@Component
@Slf4j
public class RoomContainer {
    private final ConfigRepository configRepository;
    private final SessionRepository sessionRepository;
    private final MusicDefaultRepository musicDefaultRepository;
    private final MusicPlayingRepository musicPlayingRepository;
    private final MusicPickRepository musicPickRepository;
    private final MusicVoteRepository musicVoteRepository;
    private final MusicBlackRepository musicBlackRepository;
    private final SessionBlackRepository sessionBlackRepository;
    private final FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    private final HousesRespository housesRespository;
    private final RetainKeyRepository retainKeyRepository;
    private CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<>();

    public RoomContainer(ConfigRepository configRepository, SessionRepository sessionRepository, MusicDefaultRepository musicDefaultRepository, MusicPlayingRepository musicPlayingRepository, MusicPickRepository musicPickRepository, MusicVoteRepository musicVoteRepository, MusicBlackRepository musicBlackRepository, SessionBlackRepository sessionBlackRepository, FuyaoMusicRoomProperties fuyaoMusicRoomProperties, HousesRespository housesRespository, RetainKeyRepository retainKeyRepository) {
        this.configRepository = configRepository;
        this.sessionRepository = sessionRepository;
        this.musicDefaultRepository = musicDefaultRepository;
        this.musicPlayingRepository = musicPlayingRepository;
        this.musicPickRepository = musicPickRepository;
        this.musicVoteRepository = musicVoteRepository;
        this.musicBlackRepository = musicBlackRepository;
        this.sessionBlackRepository = sessionBlackRepository;
        this.fuyaoMusicRoomProperties = fuyaoMusicRoomProperties;
        this.housesRespository = housesRespository;
        this.retainKeyRepository = retainKeyRepository;
    }

    public int size() {
        return houses.size();
    }

    public boolean isBeyondIpHouse(String ip, int limit) {
        int count = 0;
        for (House house : houses) {
            if (house.getRemoteAddress().equals(ip)) {
                if (++count >= limit) {
                    return true;
                }
            }
        }
        return false;
    }

    public CopyOnWriteArrayList<House> getHouses() {
        return this.houses;
    }

    public void setHouses(CopyOnWriteArrayList<House> houses) {
        this.houses = houses;
    }

    public House get(String id) {
        House house = new House();
        house.setId(id);
        int indexOf = houses.indexOf(house);
        if (indexOf != -1) {
            return houses.get(indexOf);
        }
        return null;
    }

    public Boolean contains(String id) {
        House house = new House();
        house.setId(id);
        return houses.contains(house);
    }

    public void add(House house) {
        configRepository.initialize(house.getId());
        houses.add(house);
        if (house.getEnableStatus() != null && house.getEnableStatus()) {
            this.refreshHouses();
        }
    }

    public void refreshHouses() {
        CopyOnWriteArrayList<House> retainHouse = new CopyOnWriteArrayList<>();
        for (House house : houses) {
            if (house.getEnableStatus() != null && house.getEnableStatus()) {
                retainHouse.add(house);
            }
        }
        housesRespository.destroy(retainHouse);
    }

    public void destroy(String id) {
        try {
            this.remove(id);
            fuyaoMusicRoomProperties.removeSessions(id);
            sessionRepository.destroy(id);
            configRepository.destroy(id);
            musicPlayingRepository.destroy(id);
            musicPickRepository.destroy(id);
            musicVoteRepository.destroy(id);
            musicBlackRepository.destroy(id);
            sessionBlackRepository.destroy(id);
            musicDefaultRepository.destroy(id);
        } catch (Exception e) {
            log.error("houseId{},message:[{}]", id, e.getMessage());
        }
    }

    public void destroy() {
        musicDefaultRepository.destroy("");
        Iterator<House> iterator = houses.iterator();
        while (iterator.hasNext()) {
            House house = iterator.next();
            if (!FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID.equals(house.getId()) && (house.getEnableStatus() == null || !house.getEnableStatus())) {
                sessionRepository.destroy(house.getId());
                configRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
                musicPickRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
                musicBlackRepository.destroy(house.getId());
                sessionBlackRepository.destroy(house.getId());
                musicDefaultRepository.destroy(house.getId());
                iterator.remove();
            } else {
                sessionRepository.destroy(house.getId());
                musicPlayingRepository.destroy(house.getId());
                musicVoteRepository.destroy(house.getId());
            }
        }
        housesRespository.destroy(houses);
    }

    public boolean remove(String id) {
        House house = new House();
        house.setId(id);
        return houses.remove(house);
    }

    /**
     * 清理 session
     * 清理 config
     * 清理 default
     * 清理 playing
     * 清理 pick
     */
    public CopyOnWriteArrayList<House> clearSurvive() {
        log.info("清理工作开始");
        CopyOnWriteArrayList<House> housesRedis = (CopyOnWriteArrayList<House>) housesRespository.initialize();
        musicDefaultRepository.destroy("");
        for (House house : housesRedis) {
            sessionRepository.destroy(house.getId());
            musicPlayingRepository.destroy(house.getId());
            musicVoteRepository.destroy(house.getId());
        }
        log.info("清理工作完成");
        return housesRedis;
    }

    public void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        configRepository.initialize(houses.get(0).getId());
        musicDefaultRepository.initialize("");
        this.setHouses(houses);
    }

    public RetainKey getRetainKey(String retainKey) {
        return retainKeyRepository.getRetainKey(retainKey);
    }

    public void removeRetainKey(String retainKey) {
        retainKeyRepository.removeRetainKey(retainKey);
    }

    public void addRetainKey(RetainKey retainKey) {
        retainKeyRepository.addRetainKey(retainKey);
    }

    public void updateRetainKey(RetainKey retainKey) {
        retainKeyRepository.updateRetainKey(retainKey);
    }

    public List showRetainKey() {
        return retainKeyRepository.showRetainKey();
    }
}
