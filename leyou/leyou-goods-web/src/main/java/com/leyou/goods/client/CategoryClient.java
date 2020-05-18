package com.leyou.goods.client;

import com.leyou.item.api.CategoryApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:41
 */
@FeignClient("item-service")
public interface CategoryClient extends CategoryApi {
}
