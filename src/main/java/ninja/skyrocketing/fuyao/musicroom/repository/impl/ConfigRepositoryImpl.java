package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * @author skyrocketing Hong
 */
@Repository
public class ConfigRepositoryImpl implements ConfigRepository {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getConfigHash() + houseId);
    }

    @Override
    public void initialize(String houseId) {
        this.put(redisKeys.getRedisRoleRoot(), fuyaoMusicRoomProperties.getRoleRootPassword(), houseId);
        this.put(redisKeys.getRedisRoleAdmin(), fuyaoMusicRoomProperties.getRoleAdminPassword(), houseId);
        this.put(redisKeys.getVoteSkipRate(), fuyaoMusicRoomProperties.getVoteRate(), houseId);
        this.put(redisKeys.getGoodModel(), fuyaoMusicRoomProperties.getGoodModel(), houseId);
        this.put(redisKeys.getRandomModel(), fuyaoMusicRoomProperties.getRandomModel(), houseId);
    }

    @Override
    public Object get(Object hashKey, String houseId) {
        return redisTemplate.opsForHash().get(redisKeys.getConfigHash() + houseId, hashKey);
    }

    @Override
    public void put(Object hashKey, Object value, String houseId) {
        redisTemplate.opsForHash().put(redisKeys.getConfigHash() + houseId, hashKey, value);
    }

    @Override
    public void putAll(Map<String, Object> map, String houseId) {
        redisTemplate.opsForHash().putAll(redisKeys.getConfigHash() + houseId, map);
    }

    @Override
    public String getPassword(String role, String houseId) {
        return (String) this.get(role, houseId);
    }

    @Override
    public void setPassword(String role, String password, String houseId) {
        this.put(role, password, houseId);
    }

    @Override
    public void setAdminPassword(String password, String houseId) {
        this.put(redisKeys.getRedisRoleAdmin(), password, houseId);
    }

    @Override
    public void setRootPassword(String password, String houseId) {
        this.put(redisKeys.getRedisRoleRoot(), password, houseId);
    }

    @Override
    public void initRootPassword(String houseId) {
        this.setPassword(redisKeys.getRedisRoleRoot(), fuyaoMusicRoomProperties.getRoleRootPassword(), houseId);
    }

    @Override
    public void initAdminPassword(String houseId) {
        this.setPassword(redisKeys.getRedisRoleAdmin(), fuyaoMusicRoomProperties.getRoleAdminPassword(), houseId);
    }

    @Override
    public String getRootPassword(String houseId) {
        return this.getPassword(redisKeys.getRedisRoleRoot(), houseId);
    }

    @Override
    public String getAdminPassword(String houseId) {
        return this.getPassword(redisKeys.getRedisRoleAdmin(), houseId);
    }

    @Override
    public Long getLastMusicDuration(String houseId) {
        return (Long) this.get(redisKeys.getLastMusicDuration(), houseId);
    }

    @Override
    public void setLastMusicDuration(Long duration, String houseId) {
        this.put(redisKeys.getLastMusicDuration(), duration, houseId);
    }

    @Override
    public Long getLastMusicPushTime(String houseId) {
        return (Long) this.get(redisKeys.getLastMusicPushTime(), houseId);
    }

    @Override
    public void setLastMusicPushTime(Long pushTime, String houseId) {
        this.put(redisKeys.getLastMusicPushTime(), pushTime, houseId);
    }

    @Override
    public void setLastMusicPushTimeAndDuration(Long pushTime, Long duration, String houseId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(redisKeys.getLastMusicPushTime(), pushTime);
        map.put(redisKeys.getLastMusicDuration(), duration);
        this.putAll(map, houseId);
    }

    @Override
    public Boolean getPushSwitch(String houseId) {
        return (Boolean) this.get(redisKeys.getSwitchMusicPush(), houseId);
    }

    @Override
    public void setPushSwitch(boolean pushSwitch, String houseId) {
        this.put(redisKeys.getSwitchMusicPush(), pushSwitch, houseId);
    }

    @Override
    public Float getVoteRate(String houseId) {
        Object voteRateObj = this.get(redisKeys.getVoteSkipRate(), houseId);
        return voteRateObj == null ? fuyaoMusicRoomProperties.getVoteRate() : (Float) voteRateObj;
    }

    @Override
    public void setVoteRate(Float voteRate, String houseId) {
        this.put(redisKeys.getVoteSkipRate(), voteRate, houseId);
    }

    @Override
    public Boolean getEnableSwitch(String houseId) {
        return (Boolean) this.get(redisKeys.getSwitchMusicEnable(), houseId);
    }

    @Override
    public void setEnableSwitch(boolean enableSwitch, String houseId) {
        this.put(redisKeys.getSwitchMusicEnable(), enableSwitch, houseId);
    }

    @Override
    public Boolean getEnableSearch(String houseId) {
        return (Boolean) this.get(redisKeys.getSearchMusicEnable(), houseId);
    }

    @Override
    public void setEnableSearch(boolean enableSearch, String houseId) {
        this.put(redisKeys.getSearchMusicEnable(), enableSearch, houseId);
    }

    @Override
    public Boolean getGoodModel(String houseId) {
        return (Boolean) this.get(redisKeys.getGoodModel(), houseId);
    }

    @Override
    public void setGoodModel(boolean goodModel, String houseId) {
        this.put(redisKeys.getGoodModel(), goodModel, houseId);
    }

    @Override
    public Boolean getRandomModel(String houseId) {
        return (Boolean) this.get(redisKeys.getRandomModel(), houseId);
    }

    @Override
    public void setRandomModel(boolean randomModel, String houseId) {
        this.put(redisKeys.getRandomModel(), randomModel, houseId);
    }
}
