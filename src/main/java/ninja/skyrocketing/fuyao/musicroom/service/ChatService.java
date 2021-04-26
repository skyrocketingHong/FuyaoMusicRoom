package ninja.skyrocketing.fuyao.musicroom.service;

import ninja.skyrocketing.fuyao.musicroom.common.page.HulkPage;
import ninja.skyrocketing.fuyao.musicroom.common.page.Page;

import java.util.List;

/**
 * @author skyrocketing Hong
 */
public interface ChatService {

    /**
     * picture search
     * https://www.52doutu.cn
     *
     * @param content  keyword
     * @param hulkPage page
     * @return page
     */
    Page<List> pictureSearch(String content, HulkPage hulkPage);
}
