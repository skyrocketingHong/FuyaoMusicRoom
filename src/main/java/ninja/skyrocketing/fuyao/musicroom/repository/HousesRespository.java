package ninja.skyrocketing.fuyao.musicroom.repository;

import ninja.skyrocketing.fuyao.musicroom.model.House;

import java.util.List;

/**
 * @author skyrocketing Hong
 * @create 2020-06-21 22:03
 */
public interface HousesRespository {

    Boolean destroy(List<House> houses);

    List<House> initialize();

    Long rightPushAll(Object... value);

    Long size();


    void reset();

    Long add(Object... value);

    List<House> get();
}
