package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionBlackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author skyrocketing Hong
 */
@Repository
@Slf4j
public class SessionBlackRepositoryImpl implements SessionBlackRepository {
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getSessionBlackHash() + houseId);
    }

    @Override
    public User getSession(String sessionId, String ip, String houseId) {
        User user = (User) redisTemplate.opsForHash().get(redisKeys.getSessionBlackHash() + houseId, sessionId);
        if (user == null) {
            user = (User) redisTemplate.opsForHash().get(redisKeys.getSessionBlackHash() + houseId, ip);
        }
        return user;
    }

    @Override
    public void setSession(User user, String houseId) {
        redisTemplate.opsForHash().put(redisKeys.getSessionBlackHash() + houseId, user.getSessionId(), user);
        redisTemplate.opsForHash().put(redisKeys.getSessionBlackHash() + houseId, user.getRemoteAddress(), user);
    }

    @Override
    public Long removeSession(String sessionId, String houseId) {
        User user = (User) redisTemplate.opsForHash().get(redisKeys.getSessionBlackHash() + houseId, sessionId);
        if (user != null) {
            redisTemplate.opsForHash().delete(redisKeys.getSessionBlackHash() + houseId, user.getRemoteAddress());
        }
        return redisTemplate.opsForHash().delete(redisKeys.getSessionBlackHash() + houseId, sessionId);
    }

    @Override
    public Set showBlackList(String houseId) {
        return redisTemplate.opsForHash().keys(redisKeys.getSessionBlackHash() + houseId);
    }
}
