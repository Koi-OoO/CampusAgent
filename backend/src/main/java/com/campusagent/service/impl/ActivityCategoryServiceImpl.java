package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.CategoryCreateRequest;
import com.campusagent.dto.request.CategoryUpdateRequest;
import com.campusagent.entity.ActivityCategory;
import com.campusagent.mapper.ActivityCategoryMapper;
import com.campusagent.service.ActivityCategoryService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动分类服务实现类，负责分类的业务校验和持久化操作。
 */
@Service
public class ActivityCategoryServiceImpl extends ServiceImpl<ActivityCategoryMapper, ActivityCategory>
        implements ActivityCategoryService {

    /** 创建分类时的默认排序值。 */
    private static final int DEFAULT_SORT = 0;

    /** 分类名称重复时的统一提示。 */
    private static final String CATEGORY_NAME_EXISTS_MESSAGE = "分类名称已存在";

    /**
     * 创建活动分类服务，通过构造器注入父类所需的数据访问接口。
     *
     * @param activityCategoryMapper 活动分类数据访问接口
     */
    public ActivityCategoryServiceImpl(ActivityCategoryMapper activityCategoryMapper) {
        this.baseMapper = activityCategoryMapper;
    }

    @Override
    public List<ActivityCategory> listAll() {
        LambdaQueryWrapper<ActivityCategory> queryWrapper = new LambdaQueryWrapper<ActivityCategory>()
                .orderByAsc(ActivityCategory::getSort);
        return list(queryWrapper);
    }

    @Override
    public void createCategory(CategoryCreateRequest request) {
        // 名称唯一性校验
        checkNameUnique(request.getName(), null);

        ActivityCategory category = new ActivityCategory();
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? DEFAULT_SORT : request.getSort());

        try {
            if (!save(category)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            // 并发创建时由数据库唯一索引兜底
            throw new BusinessException(ResultCode.PARAM_ERROR, CATEGORY_NAME_EXISTS_MESSAGE);
        }
    }

    @Override
    public void updateCategory(Integer id, CategoryUpdateRequest request) {
        // 路径 ID 与请求体 ID 一致性校验
        if (!id.equals(request.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "路径分类编号与请求体编号不一致");
        }
        // 分类存在性校验
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 名称唯一性校验（排除自己）
        checkNameUnique(request.getName(), id);

        ActivityCategory category = new ActivityCategory();
        category.setId(id);
        category.setName(request.getName());
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }

        try {
            if (!updateById(category)) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.PARAM_ERROR, CATEGORY_NAME_EXISTS_MESSAGE);
        }
    }

    @Override
    public void deleteCategory(Integer id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // TODO Phase 2-2 建立活动表后，删除前检查该分类下是否存在活动。
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验名称是否与其他未删除分类重复。
     *
     * @param name 待校验的分类名称
     * @param excludedId 需要排除的分类主键，创建时传入 null
     */
    private void checkNameUnique(String name, Integer excludedId) {
        LambdaQueryWrapper<ActivityCategory> queryWrapper = new LambdaQueryWrapper<ActivityCategory>()
                .eq(ActivityCategory::getName, name)
                .ne(excludedId != null, ActivityCategory::getId, excludedId);
        if (count(queryWrapper) > 0L) {
            throw new BusinessException(ResultCode.PARAM_ERROR, CATEGORY_NAME_EXISTS_MESSAGE);
        }
    }
}