package ninja.skyrocketing.fuyao.musicroom.repository;

import ninja.skyrocketing.fuyao.musicroom.model.RetainKey;

import java.util.List;

/**
 * @author skyrocketing Hong
 * @create 2020-07-06 16:23
 */
public interface RetainKeyRepository {

    List showRetainKey();

    void addRetainKey(RetainKey retainKey);

    void removeRetainKey(String retainKey);

    RetainKey getRetainKey(String retainKey);

    void updateRetainKey(RetainKey retainKey);

}
