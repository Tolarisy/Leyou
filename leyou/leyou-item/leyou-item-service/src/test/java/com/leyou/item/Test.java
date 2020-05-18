package com.leyou.item;

import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.pojo.Sku;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @anthor Tolaris
 * @date 2020/5/12 - 14:16
 */
@SuppressWarnings("ALL")
@SpringBootTest
@RunWith(SpringRunner.class)
public class Test {

    @Autowired
    private SkuMapper skuMapper;

    @org.junit.Test
    public void test() {
        List<Sku> skus = skuMapper.selectAll();
        skus.forEach(sku -> {
            //http://image.leyou.com/images/9/15/1524297313793.jpg
            String images = sku.getImages();
            String oldImages = images.substring(0, 22);
            String oldImages2 = images.substring(22);
            String str = oldImages + ":8080";
            String newImages = str + oldImages2;
            sku.setImages(newImages);
            skuMapper.updateByPrimaryKey(sku);
        });
    }
}
