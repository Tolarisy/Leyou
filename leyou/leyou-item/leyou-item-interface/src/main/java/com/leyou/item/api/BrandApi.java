package com.leyou.item.api;

import com.leyou.item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:32
 */
@RequestMapping("brand")
public interface BrandApi {

    /**
     * 根据id查品牌
     *
     * @param id
     * @return
     */
    @GetMapping("{id}")
    Brand queryBrandById(@PathVariable("id") Long id);
}
