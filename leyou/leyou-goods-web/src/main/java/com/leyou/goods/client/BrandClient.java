package com.leyou.goods.client;

import com.leyou.item.api.BrandApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:39
 */
@FeignClient("item-service")
public interface BrandClient extends BrandApi {
}
