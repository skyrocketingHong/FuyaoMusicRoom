package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author skyrocketing Hong
 */
@Repository
public class MusicDefaultRepositoryImpl implements MusicDefaultRepository {
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getDefaultSet() + "_" + houseId);
    }

    @Override
    public Long initialize(String houseId) {
        return redisTemplate.opsForSet().add(redisKeys.getDefaultSet() + "_" + houseId, FuyaoMusicRoomProperties.getDefaultListForRepository().toArray());
    }

    /**
     * set
     *
     * @return long
     */
    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForSet().size(redisKeys.getDefaultSet() + "_" + houseId);
    }

    @Override
    public String randomMember(String houseId) {
        return (String) redisTemplate.opsForSet().randomMember(redisKeys.getDefaultSet() + "_" + houseId);
    }

    @Override
    public void remove(String id, String houseId) {
        redisTemplate.opsForSet().remove(redisKeys.getDefaultSet() + "_" + houseId, id);
    }

    @Override
    public Long add(String[] value, String houseId) {
        return redisTemplate.opsForSet().add(redisKeys.getDefaultSet() + "_" + houseId, value);
    }
}
