package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * @anthor Tolaris
 * @date 2020/4/23 - 16:57
 */
public interface CategoryMapper extends Mapper<Category>,
        SelectByIdListMapper<Category, Long> {
}
