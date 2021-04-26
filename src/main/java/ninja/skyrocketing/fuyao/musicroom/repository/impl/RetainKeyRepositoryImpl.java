package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.RetainKey;
import ninja.skyrocketing.fuyao.musicroom.repository.RetainKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author skyrocketing Hong
 * @create 2020-07-06 16:27
 */
@Repository
@Slf4j
public class RetainKeyRepositoryImpl implements RetainKeyRepository {

    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List showRetainKey() {
        return redisTemplate.opsForHash().values(redisKeys.getRetainKeyHash());
    }

    @Override
    public void addRetainKey(RetainKey retainKey) {
        redisTemplate.opsForHash().put(redisKeys.getRetainKeyHash(), retainKey.getKey(), retainKey);
    }

    @Override
    public void removeRetainKey(String retainKey) {
        redisTemplate.opsForHash().delete(redisKeys.getRetainKeyHash(), retainKey);
    }

    @Override
    public RetainKey getRetainKey(String retainKey) {
        return (RetainKey) redisTemplate.opsForHash().get(redisKeys.getRetainKeyHash(), retainKey);
    }

    @Override
    public void updateRetainKey(RetainKey retainKey) {
        this.addRetainKey(retainKey);
    }
}
