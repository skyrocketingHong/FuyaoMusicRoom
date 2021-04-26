package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicVoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author skyrocketing Hong
 */
@Repository
@Slf4j
public class MusicVoteRepositoryImpl implements MusicVoteRepository {

    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getSkipSet() + houseId);
    }

    @Override
    public Long add(String houseId, Object... value) {
        return redisTemplate.opsForSet().add(redisKeys.getSkipSet() + houseId, value);
    }

    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForSet().size(redisKeys.getSkipSet() + houseId);
    }

    @Override
    public void reset(String houseId) {
        Set members = this.members(houseId);
        if (null != members && members.size() > 0) {
            redisTemplate.opsForSet().remove(redisKeys.getSkipSet() + houseId, members.toArray());
        }
    }

    @Override
    public Set members(String houseId) {
        return redisTemplate.opsForSet().members(redisKeys.getSkipSet() + houseId);
    }

    @Override
    public Long remove(String sessionId, String houseId) {
        return redisTemplate.opsForSet().remove(redisKeys.getSkipSet() + houseId, sessionId);
    }
}
