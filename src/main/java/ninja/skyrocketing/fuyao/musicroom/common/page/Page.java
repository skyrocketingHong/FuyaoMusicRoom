package ninja.skyrocketing.fuyao.musicroom.common.page;

/**
 * @author skyrocketing Hong
 */
public interface Page<T> {

    /**
     * get page index
     *
     * @return page index
     */
    int getPageIndex();

    /**
     * get page size
     *
     * @return page size
     */
    int getPageSize();

    /**
     * set total size
     *
     * @param totalSize total size
     */
    void setTotalSize(int totalSize);

    /**
     * get data
     *
     * @return data
     */
    T getData();

    /**
     * set data
     *
     * @param data data
     */
    void setData(T data);
}
