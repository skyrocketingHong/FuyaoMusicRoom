package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicBlackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author skyrocketing Hong
 */
@Repository
@Slf4j
public class MusicBlackRepositoryImpl implements MusicBlackRepository {
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getBlackSet() + houseId);
    }

    @Override
    public boolean isMember(String id, String houseId) {
        return redisTemplate.opsForSet().isMember(redisKeys.getBlackSet() + houseId, id);
    }

    @Override
    public Long add(String value, String houseId) {
        return redisTemplate.opsForSet().add(redisKeys.getBlackSet() + houseId, value);
    }

    @Override
    public Long remove(String id, String houseId) {
        return redisTemplate.opsForSet().remove(redisKeys.getBlackSet() + houseId, id);
    }

    @Override
    public Set showBlackList(String houseId) {
        return redisTemplate.opsForSet().members(redisKeys.getBlackSet() + houseId);
    }
}
