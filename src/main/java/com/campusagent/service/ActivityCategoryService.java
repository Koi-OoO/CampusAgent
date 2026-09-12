package com.campusagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.dto.request.CategoryCreateRequest;
import com.campusagent.dto.request.CategoryUpdateRequest;
import com.campusagent.entity.ActivityCategory;

import java.util.List;

/**
 * 活动分类服务接口，在基础增删改查能力上提供分类列表查询和分类管理。
 */
public interface ActivityCategoryService extends IService<ActivityCategory> {

    /**
     * 按排序值升序查询所有未被逻辑删除的活动分类。
     *
     * @return 活动分类列表，没有分类时返回空列表
     */
    List<ActivityCategory> listAll();

    /**
     * 创建活动分类，名称重复时拒绝保存。
     *
     * @param request 分类创建请求体
     */
    void createCategory(CategoryCreateRequest request);

    /**
     * 修改活动分类，校验分类存在性和名称唯一性。
     *
     * @param id 分类主键
     * @param request 分类修改请求体
     */
    void updateCategory(Integer id, CategoryUpdateRequest request);

    /**
     * 逻辑删除活动分类。
     *
     * @param id 分类主键
     */
    void deleteCategory(Integer id);
}