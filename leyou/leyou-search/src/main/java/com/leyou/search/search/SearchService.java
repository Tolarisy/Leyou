package com.leyou.search.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @anthor Tolaris
 * @date 2020/5/4 - 22:45
 */
@SuppressWarnings("ALL")
@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {

        //根据分类的id查询分类名称
        List<String> names = categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(),
                spu.getCid2(), spu.getCid3()));

        //根据品牌id查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());

        //根据spuId查询所有的sku
        List<Sku> skus = goodsClient.querySkusBySpuId(spu.getId());
        //初始化一个价格集合，收集所有的sku的价格
        List<Long> prices = new ArrayList<>();
        //收集sku的必要字段信息
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            //获取sku中的图片
            map.put("image", StringUtils.isBlank(sku.getImages()) ? "" :
                    StringUtils.split(sku.getImages(), ",")[0]);
            skuMapList.add(map);

        });

        //根据spu中的cid3查询出所有的搜索规格参数
        List<SpecParam> params = specificationClient.queryParams(null, spu.getCid3(), null, true);

        //根据spuId查询spuDetail
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spu.getId());
        //把通用的规格参数进行反序列化
        Map<String, Object> genericSpecMap =
                OBJECT_MAPPER.readValue(spuDetail.getGenericSpec(),
                        new TypeReference<Map<String, Object>>() {
                        });
        //把特殊的规格参数，进行反序列化
        Map<String, List<Object>> specialSpecMap =
                OBJECT_MAPPER.readValue(spuDetail.getSpecialSpec(),
                        new TypeReference<Map<String, List<Object>>>() {
                        });

        Map<String, Object> specs = new HashMap<>();
        params.forEach(param -> {
            //判断规格参数的类型，是否是通规格参数参数
            if (param.getGeneric()) {
                //如果是通用类型的参数，从genericSpecMap中获取规格参数值
                String value = genericSpecMap.get(param.getId().toString()).toString();
                //判断是否是数值类型，如果是数值类型，返回一个区间
                if (param.getNumeric()) {
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            } else {
                //如果是特殊类型的参数
                List<Object> value = specialSpecMap.get(param.getId().toString());
                specs.put(param.getName(), value);
            }
        });


        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //拼接all字段，需要分类名称以及名牌名称
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " ") + " " + brand.getName());
        //获取spu下的所有sku价格
        goods.setPrice(prices);
        //获取spu下的所有sku,并转化为json字符串
        goods.setSkus(OBJECT_MAPPER.writeValueAsString(skuMapList));
        //获取所有的查询规格参数
        goods.setSpecs(specs);
        return goods;
    }

    private String chooseSegment(String value, SpecParam param) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        for (String segment : param.getSegments().split(",")) {
            String[] segs = segment.split("-");
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + param.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + param.getUnit() + "以下";
                } else {
                    result = segment + param.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(com.leyou.search.pojo.SearchRequest request) throws IOException {
        if (StringUtils.isBlank(request.getKey())) {
            return null;
        }
        //自定义查询构建器
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //添加查询条件
        //QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("all",
        //        request.getKey()).operator(Operator.AND);
        BoolQueryBuilder matchQueryBuilder = buildBoolQueryBuilder(request);
        //分页
        sourceBuilder.from(request.getPage());
        sourceBuilder.size(request.getSize());
        //添加结果集过滤
        sourceBuilder.fetchSource(new String[]{"id", "skus", "subTitle"}, null);
        //添加分类和品牌的聚合
        String categoryAggName = "categories";
        String brandAddName = "brands";
        TermsAggregationBuilder aggregationBuilder =
                AggregationBuilders.terms(categoryAggName).field("cid3");
        TermsAggregationBuilder aggregationBuilder1 =
                AggregationBuilders.terms(brandAddName).field("brandId");
        //执行查询，获取结果集
        sourceBuilder.aggregation(aggregationBuilder);
        sourceBuilder.aggregation(aggregationBuilder1);
        sourceBuilder.query(matchQueryBuilder);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取集合结果集并解析
        List<Map<String, Object>> categories =
                getCategoryAggResult(searchResponse.getAggregations().get(categoryAggName));
        List<Brand> brands =
                getBrandAggResult(searchResponse.getAggregations().get(brandAddName));
        //对规格参数进行聚合
        List<Map<String, Object>> specs = null;
        if (!CollectionUtils.isEmpty(categories) && categories.size() == 1) {
            specs = getParamAggResult((Long) categories.get(0).get("id"), matchQueryBuilder);
        }
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Goods goods = new Goods();
            goods.setId(Long.parseLong(hit.getSourceAsMap().get("id").toString()));
            goods.setSkus(hit.getSourceAsMap().get("skus").toString());
            goods.setSubTitle(hit.getSourceAsMap().get("subTitle").toString());
            goodsList.add(goods);
        }
        TotalHits totalHits = searchResponse.getHits().getTotalHits();
        Integer page = (int) totalHits.value / request.getSize() + 1;
        return new SearchResult(Long.valueOf(totalHits.value),
                10, goodsList, categories, brands, specs);
    }

    /**
     * 构建布尔查询
     *
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBoolQueryBuilder(com.leyou.search.pojo.SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",
                request.getKey()).operator(Operator.AND));
        //添加过滤条件
        Map<String, Object> filter = request.getFilter();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.equals("品牌", key)) {
                key = "brandId";
            } else if (StringUtils.equals("分类", key)) {
                key = "cid3";
            } else {
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key,
                    entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 根据查询条件聚合参数
     *
     * @param id
     * @param matchQueryBuilder
     * @return
     */
    private List<Map<String, Object>> getParamAggResult(Long id, QueryBuilder matchQueryBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(matchQueryBuilder);
        List<SpecParam> params = specificationClient.queryParams(null, id, null, true);
        //添加规格参数的聚合
        params.forEach(param -> {
            sourceBuilder.aggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        sourceBuilder.fetchSource(new String[]{}, null);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse =
                restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> specs = new ArrayList<>();
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("k", entry.getKey());
            List<String> options = new ArrayList<>();
            ParsedStringTerms terms = (ParsedStringTerms) entry.getValue();
            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析名牌的聚合结果集
     *
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        ParsedLongTerms terms = (ParsedLongTerms) aggregation;

        return terms.getBuckets().stream().map(bucket -> {
            return brandClient.queryBrandById(bucket.getKeyAsNumber().longValue());
        }).collect(Collectors.toList());
    }

    /**
     * 解析分类的聚合结果集
     *
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        ParsedLongTerms terms = (ParsedLongTerms) aggregation;

        return terms.getBuckets().stream().map(bucket -> {
            Map<String, Object> map = new HashMap<>();
            long id = bucket.getKeyAsNumber().longValue();
            List<String> names = categoryClient.queryNamesByIds(Arrays.asList(id));
            map.put("id", id);
            map.put("name", names.get(0));
            return map;
        }).collect(Collectors.toList());
    }
}
