package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @anthor Tolaris
 * @date 2020/4/30 - 11:21
 */
@SuppressWarnings("ALL")
@Service
public class SpecificationService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据分类ID查询组
     *
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGropusByCid(Long cid) {
        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        return specGroupMapper.select(record);
    }

    /**
     * 根据条件查询规格参数
     *
     * @param gid
     * @param cid
     * @param generic
     * @param searching
     * @return
     */
    public List<SpecParam> queryParams(Long gid, Long cid, Boolean generic,
                                       Boolean searching) {
        SpecParam record = new SpecParam();
        record.setGroupId(gid);
        record.setCid(cid);
        record.setGeneric(generic);
        record.setSearching(searching);
        return specParamMapper.select(record);
    }

    /**
     * 根据id查询组和规格参数
     *
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupsWithParam(Long cid) {
        List<SpecGroup> specGroups = queryGropusByCid(cid);
        specGroups.forEach(specGroup -> {
            List<SpecParam> params = queryParams(specGroup.getId(), null, null, null);
            specGroup.setParams(params);
        });
        return specGroups;
    }
}
