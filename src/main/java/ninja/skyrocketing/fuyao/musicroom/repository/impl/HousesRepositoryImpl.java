package ninja.skyrocketing.fuyao.musicroom.repository.impl;

import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.repository.HousesRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author skyrocketing Hong
 * @create 2020-06-21 22:18
 */
@Repository
public class HousesRepositoryImpl implements HousesRespository {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private FuyaoMusicRoomProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(List houses) {
        this.reset();
        this.rightPushAll(houses.toArray());
        return true;
    }

    @Override
    public CopyOnWriteArrayList<House> initialize() {
        if (this.size() == 0) {
            CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<House>();
            houses.add(defaultHouse());
            return houses;
        } else {
            CopyOnWriteArrayList<House> houses = this.get();
            House house = defaultHouse();
            if (houses.contains(house)) {
                return houses;
            } else {
                CopyOnWriteArrayList<House> newHouses = new CopyOnWriteArrayList<>();
                newHouses.add(house);
                newHouses.addAll(houses);
                return newHouses;
            }
        }
    }

    private House defaultHouse() {
        House house = new House();
        house.setEnableStatus(true);
        house.setName(FuyaoMusicRoomProperties.HOUSE_DEFAULT_NAME);
        house.setId(FuyaoMusicRoomProperties.HOUSE_DEFAULT_ID);
        house.setDesc(FuyaoMusicRoomProperties.HOUSE_DEFAULT_DESC);
        house.setCreateTime(System.currentTimeMillis());
        house.setNeedPwd(false);
        house.setRemoteAddress("127.0.0.1");
        return house;
    }

    @Override
    public Long add(Object... value) {
        return redisTemplate.opsForList().rightPush(redisKeys.getHouses(), value);
    }


    @Override
    public Long size() {
        return redisTemplate.opsForList().size(redisKeys.getHouses());
    }

    @Override
    public void reset() {
        redisTemplate.opsForList().trim(redisKeys.getHouses(), 1, 0);
    }

    @Override
    public Long rightPushAll(Object... value) {
        return redisTemplate.opsForList().rightPushAll(redisKeys.getHouses(), value);
    }

    @Override
    public CopyOnWriteArrayList<House> get() {
        Long size = this.size();
        size = size == null ? 0 : size;
        List<House> houseOrigin = (List<House>) redisTemplate.opsForList().range(redisKeys.getHouses(), 0, size);
        return new CopyOnWriteArrayList<>(houseOrigin);
    }
}
