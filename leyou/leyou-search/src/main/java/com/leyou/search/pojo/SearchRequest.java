package com.leyou.search.pojo;

import java.util.Map;

/**
 * @anthor Tolaris
 * @date 2020/5/7 - 22:24
 */
public class SearchRequest {

    private String key;//搜索条件

    private Integer page;//当前页

    private Map<String, Object> filter;

    public Map<String, Object> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filter) {
        this.filter = filter;
    }

    private static final Integer DEFAULT_SIZE = 20;//每页大小
    private static final Integer DEFAULT_PAGE = 1;//默认页码

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(DEFAULT_PAGE, page);
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return DEFAULT_SIZE;
    }
}
