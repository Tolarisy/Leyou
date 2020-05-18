package com.leyou.search;

import com.alibaba.fastjson.JSON;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.search.SearchService;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @anthor Tolaris
 * @date 2020/5/6 - 10:56
 */
@SuppressWarnings("ALL")
@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticSearchTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private SearchService searchService;

    @Autowired
    private GoodsClient goodsClient;

    @Test
    public void test() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("goods");
        CreateIndexResponse indexResponse = restHighLevelClient.indices().create(request,
                RequestOptions.DEFAULT);
        Integer page = 1;
        Integer rows = 100;
        do {
            PageResult<SpuBo> result = goodsClient.querySpuByPage(null, null, page,
                    rows);
            List<SpuBo> items = result.getItems();
            List<Goods> goodsList = items.stream().map(spuBo -> {
                try {
                    return searchService.buildGoods(spuBo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());

            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.timeout("15s");
            for (int i = 0; i < goodsList.size(); i++) {
                bulkRequest.add(new IndexRequest("goods")
                        .id("" + (i + (page - 1) * 100 + 1)).source(JSON.toJSONString(goodsList.get(i)), XContentType.JSON));
            }
            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

            rows = items.size();
            page++;
        } while (rows == 100);
    }


    @Test
    public void search() throws IOException {
        com.leyou.search.pojo.SearchRequest request =
                new com.leyou.search.pojo.SearchRequest();
        request.setKey("手机");
        request.setPage(1);
        if (StringUtils.isBlank(request.getKey())) {
            System.out.println("!!!!!!!!!!!!!!!!!!!");
        }
        //自定义查询构建器
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //添加查询条件
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("all",
                request.getKey()).operator(Operator.AND);
        //分页
        sourceBuilder.from(request.getPage());
        sourceBuilder.size(request.getSize());
        //添加结果集过滤
        sourceBuilder.fetchSource(new String[]{"id", "skus", "subTitle"}, null);
        //执行查询，获取结果集
        sourceBuilder.query(matchQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Goods goods = new Goods();
            goods.setId(Long.parseLong(hit.getSourceAsMap().get("id").toString()));
            goods.setSkus(hit.getSourceAsMap().get("skus").toString());
            goods.setSubTitle(hit.getSourceAsMap().get("subTitle").toString());
            goodsList.add(goods);
        }
        PageResult<Goods> result = new PageResult<>(Long.parseLong(request.getSize().toString()),
                request.getPage(),
                goodsList);
    }

}
