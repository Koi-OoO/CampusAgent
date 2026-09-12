package com.campusagent.controller;

import com.campusagent.annotation.RequireRole;
import com.campusagent.common.result.Result;
import com.campusagent.dto.request.CategoryCreateRequest;
import com.campusagent.dto.request.CategoryUpdateRequest;
import com.campusagent.entity.ActivityCategory;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.ActivityCategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动分类控制器，提供分类列表查询、创建、修改和逻辑删除接口。
 *
 * <p>业务逻辑统一由 ActivityCategoryService 处理，控制器只负责参数接收和结果封装。</p>
 */
@RestController
@RequestMapping("/api/category")
public class ActivityCategoryController {

    private final ActivityCategoryService activityCategoryService;

    public ActivityCategoryController(ActivityCategoryService activityCategoryService) {
        this.activityCategoryService = activityCategoryService;
    }

    /**
     * 查询所有未删除的活动分类，按排序值升序返回。
     */
    @GetMapping("/list")
    public Result<List<ActivityCategory>> list() {
        return Result.success(activityCategoryService.listAll());
    }

    /**
     * 创建活动分类。
     */
    @PostMapping
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> create(@Valid @RequestBody CategoryCreateRequest request) {
        activityCategoryService.createCategory(request);
        return Result.success();
    }

    /**
     * 修改指定分类。
     */
    @PutMapping("/{id}")
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> update(@PathVariable("id") Integer id,
                               @Valid @RequestBody CategoryUpdateRequest request) {
        activityCategoryService.updateCategory(id, request);
        return Result.success();
    }

    /**
     * 逻辑删除指定分类。
     */
    @DeleteMapping("/{id}")
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> delete(@PathVariable("id") Integer id) {
        activityCategoryService.deleteCategory(id);
        return Result.success();
    }
}