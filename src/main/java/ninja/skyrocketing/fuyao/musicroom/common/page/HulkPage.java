package ninja.skyrocketing.fuyao.musicroom.common.page;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author skyrocketing Hong
 */
public class HulkPage<T> implements Page<T> {

    @JsonProperty("pageIndex")
    private final int pageIndex = 1;
    @JsonProperty("pageSize")
    private final int pageSize = 10;
    private int totalSize;
    private T data;

    @Override
    public int getPageIndex() {
        return this.pageIndex;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public void setData(T data) {
        this.data = data;
    }

}
