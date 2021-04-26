package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.Music;
import ninja.skyrocketing.fuyao.musicroom.repository.MusicPlayingRepository;
import ninja.skyrocketing.fuyao.musicroom.util.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author skyrocketing Hong
 */
@Repository
public class MusicPlayingRepositoryImpl implements MusicPlayingRepository {
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getPlayingList() + houseId);
    }

    @Override
    public Long leftPush(Music pick, String houseId) {
        return redisTemplate.opsForList().leftPush(redisKeys.getPlayingList() + houseId, pick);
    }

    @Override
    public Music pickToPlaying(String houseId) {
        return (Music) redisTemplate.opsForList().rightPopAndLeftPush(redisKeys.getPickList() + houseId, redisKeys.getPlayingList() + houseId);
    }

    @Override
    public Music randomToPlaying(String houseId) {
        int size = redisTemplate.opsForList().size(redisKeys.getPickList() + houseId).intValue();
        if (size == 1) {
            return this.pickToPlaying(houseId);
        }
        int index = RandomUtils.getRandNumber(size);
        Music music = (Music) redisTemplate.opsForList().index(redisKeys.getPickList() + houseId, index);
        this.leftPush(music, houseId);
        redisTemplate.opsForList().remove(redisKeys.getPickList() + houseId, 1, music);
        return music;
    }

    @Override
    public void keepTheOne(String houseId) {
        redisTemplate.opsForList().trim(redisKeys.getPlayingList() + houseId, 0, 0);
    }

    @Override
    public Music getPlaying(String houseId) {
        return (Music) redisTemplate.opsForList().index(redisKeys.getPlayingList() + houseId, 0);
    }

}
