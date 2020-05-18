package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @anthor Tolaris
 * @date 2020/5/14 - 16:17
 */
@SuppressWarnings("ALL")
@Service
public class GoodsService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private SpecificationClient specificationClient;

    public Map<String, Object> loadData(Long spuId) {
        Map<String, Object> model = new HashMap<>();

        Spu spu = goodsClient.querySpuById(spuId);
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spuId);
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(),
                spu.getCid3());
        List<String> names = categoryClient.queryNamesByIds(cids);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        List<Sku> skus = goodsClient.querySkusBySpuId(spuId);
        List<SpecGroup> groups = specificationClient.queryGroupsWithParam(spu.getCid3());
        List<SpecParam> params = specificationClient.queryParams(null, spu.getCid3(), false, null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });


        model.put("spu", spu);
        model.put("spuDetail", spuDetail);
        model.put("categories", categories);
        model.put("brand", brand);
        model.put("skus", skus);
        model.put("groups", groups);
        model.put("paramMap", paramMap);
        return model;
    }
}
