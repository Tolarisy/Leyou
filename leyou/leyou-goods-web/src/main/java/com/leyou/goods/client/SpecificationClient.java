package com.leyou.goods.client;

import com.leyou.item.api.SpecificationApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:42
 */
@FeignClient("item-service")
public interface SpecificationClient extends SpecificationApi {
}
