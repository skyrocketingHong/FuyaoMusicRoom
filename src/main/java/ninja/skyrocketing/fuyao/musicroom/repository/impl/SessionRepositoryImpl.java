package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author skyrocketing Hong
 */
@Repository
@Slf4j
public class SessionRepositoryImpl implements SessionRepository {

    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getSessionHash() + houseId);
    }

    @Override
    public User getSession(String sessionId, String houseId) {
        return (User) redisTemplate.opsForHash().get(redisKeys.getSessionHash() + houseId, sessionId);
    }

    @Override
    public List<Object> getSession(String houseId) {
        return redisTemplate.opsForHash().values(redisKeys.getSessionHash() + houseId);
    }

    @Override
    public void setSession(User user, String houseId) {
        redisTemplate.opsForHash().put(redisKeys.getSessionHash() + houseId, user.getSessionId(), user);
    }

    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForHash().size(redisKeys.getSessionHash() + houseId);
    }

    @Override
    public Long removeSession(String sessionId, String houseId) {
        return redisTemplate.opsForHash().delete(redisKeys.getSessionHash() + houseId, sessionId);
    }

}
