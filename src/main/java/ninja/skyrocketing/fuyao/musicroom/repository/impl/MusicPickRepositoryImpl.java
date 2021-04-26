package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author skyrocketing Hong
 */
@Repository
public class MusicPickRepositoryImpl implements MusicPickRepository {
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getPickList() + houseId);
    }

    @Override
    public Long leftPush(Music pick, String houseId) {
        return redisTemplate.opsForList().leftPush(redisKeys.getPickList() + houseId, pick);
    }

    @Override
    public Long leftPushAll(String houseId, Object... value) {
        return redisTemplate.opsForList().leftPushAll(redisKeys.getPickList() + houseId, value);
    }

    @Override
    public Long rightPushAll(String houseId, Object... value) {
        return redisTemplate.opsForList().rightPushAll(redisKeys.getPickList() + houseId, value);
    }

    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForList().size(redisKeys.getPickList() + houseId);
    }

    /**
     * clear the pick list.
     */
    @Override
    public void reset(String houseId) {
        redisTemplate.opsForList().trim(redisKeys.getPickList() + houseId, 1, 0);
    }

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    @Override
    public List<Music> getPickMusicList(String houseId) {
        Long size = this.size(houseId);
        size = size == null ? 0 : size;
        return (List<Music>) redisTemplate.opsForList().range(redisKeys.getPickList() + houseId, 0, size);
    }

}
