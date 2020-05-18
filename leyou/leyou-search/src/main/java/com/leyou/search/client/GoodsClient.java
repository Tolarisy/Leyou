package com.leyou.search.client;

import com.leyou.item.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 21:49
 */
@FeignClient("item-service")
public interface GoodsClient extends GoodsApi {

}
