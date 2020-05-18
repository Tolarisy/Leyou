package com.leyou.item.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:34
 */
@RequestMapping("category")
public interface CategoryApi {

    @GetMapping
    List<String> queryNamesByIds(@RequestParam("ids") List<Long> ids);
}
