package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @anthor Tolaris
 * @date 2020/5/1 - 20:26
 */
@SuppressWarnings("ALL")
@Service
public class GoodsService {


    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    /**
     * 根据分页查询spu
     *
     * @param key
     * @param saleable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<SpuBo> querySpuByPage(String key, Boolean saleable, Integer page, Integer rows) {

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //添加查询条件
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //添加上下架的过滤条件
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //添加分页
        PageHelper.startPage(page, rows);

        //执行查询，获取spu集合
        List<Spu> spus = spuMapper.selectByExample(example);
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spus);

        //spu集合转换成spuBo集合
        List<SpuBo> spuBos = spus.stream().map(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu, spuBo);
            //查询品牌名称
            Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());
            //查询分类名称
            List<String> names = categoryService.queryNamseByIds(Arrays.asList(spu.getCid1(),
                    spu.getCid2(), spu.getCid3()));
            spuBo.setCname(StringUtils.join(names, "-"));
            return spuBo;
        }).collect(Collectors.toList());
        //返回pageResult<spuBo>
        return new PageResult<>(spuPageInfo.getTotal(), spuBos);
    }

    /**
     * 新增商品
     *
     * @param spuBo
     */
    @Transactional
    public void saveGoods(SpuBo spuBo) {
        //新增spu
        spuBo.setId(null);
        spuBo.setValid(true);
        spuBo.setSaleable(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        spuMapper.insertSelective(spuBo);
        //新增spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        spuDetailMapper.insertSelective(spuDetail);

        saveSkuAndStock(spuBo);
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku -> {
            //新增sku
            sku.setId(null);
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insertSelective(sku);
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insertSelective(stock);
        });
    }

    /**
     * 根据spuId查询spuDetail
     *
     * @param spuId
     * @return
     */
    public SpuDetail queryDetailBySpuId(Long spuId) {
        return spuDetailMapper.selectByPrimaryKey(spuId);
    }

    /**
     * 根据spuId查询sku集合
     *
     * @param spuId
     * @return
     */
    public List<Sku> querySkusBySpuId(Long spuId) {
        Sku record = new Sku();
        record.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(record);
        skus.forEach(sku -> {
            Stock stock = stockMapper.selectByPrimaryKey(sku.getId());
            sku.setStock(stock.getStock());
        });
        return skus;
    }

    /**
     * 更新商品
     *
     * @param spuBo
     */
    @Transactional
    public void updateGoods(SpuBo spuBo) {
        Sku record = new Sku();
        record.setSpuId(spuBo.getId());
        List<Sku> skus = skuMapper.select(record);
        //删除stock
        skus.forEach(sku -> {
            stockMapper.deleteByPrimaryKey(sku.getId());
        });
        //删除sku
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        skuMapper.delete(sku);
        //新增sku
        //新增stock
        saveSkuAndStock(spuBo);
        //更新spu和spuDetail
        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        spuBo.setValid(null);
        spuBo.setSaleable(null);
        spuMapper.updateByPrimaryKeySelective(spuBo);
        spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());

    }

    /**
     * 根据id查询spu
     *
     * @param id
     * @return
     */
    public Spu querySpuById(Long id) {
        return spuMapper.selectByPrimaryKey(id);
    }
}
