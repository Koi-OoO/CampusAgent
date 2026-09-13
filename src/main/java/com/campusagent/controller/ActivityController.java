package com.campusagent.controller;

import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.request.ActivityCreateRequest;
import com.campusagent.dto.request.ActivityUpdateRequest;
import com.campusagent.entity.Activity;
import com.campusagent.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动发布控制器，提供草稿创建、提交审核、修改、删除及当前用户的活动列表接口。
 *
 * <p>所有接口沿用登录拦截器，用户主键从已验证的请求属性中获取。
 * 请求 DTO 仅包含可编辑字段，转换为活动实体后交由服务层执行业务校验。</p>
 */
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    /**
     * 活动服务，通过构造器注入。
     */
    private final ActivityService activityService;

    /**
     * 创建活动发布控制器。
     *
     * @param activityService 活动服务
     */
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 为当前登录用户创建活动草稿。
     *
     * @param request 活动创建请求，字段合法性由框架统一校验
     * @param userId 登录拦截器写入的当前用户主键
     * @return 创建成功响应
     */
    @PostMapping("/draft")
    public Result<Void> createDraft(@Valid @RequestBody ActivityCreateRequest request,
                                    @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activityService.createDraft(activity, userId);
        return Result.success();
    }

    /**
     * 将当前用户发布的活动提交审核。
     *
     * @param id 待提交审核的活动主键
     * @param userId 登录拦截器写入的当前用户主键
     * @return 提交成功响应
     */
    @PostMapping("/submit/{id}")
    public Result<Void> submitForAudit(@PathVariable("id") Long id,
                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        activityService.submitForAudit(id, userId);
        return Result.success();
    }

    /**
     * 修改当前用户发布的草稿或已驳回活动。
     *
     * @param id 待修改的活动主键
     * @param request 活动修改请求，字段合法性由框架统一校验
     * @param userId 登录拦截器写入的当前用户主键
     * @return 修改成功响应
     */
    @PutMapping("/{id}")
    public Result<Void> updateActivity(@PathVariable("id") Long id,
                                       @Valid @RequestBody ActivityUpdateRequest request,
                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activityService.updateActivity(id, activity, userId);
        return Result.success();
    }

    /**
     * 逻辑删除当前用户发布的草稿活动。
     *
     * @param id 待删除的活动主键
     * @param userId 登录拦截器写入的当前用户主键
     * @return 删除成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable("id") Long id,
                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        activityService.deleteActivity(id, userId);
        return Result.success();
    }

    /**
     * 查询当前用户发布的全部未删除活动，按创建时间倒序返回。
     *
     * @param userId 登录拦截器写入的当前用户主键
     * @return 当前用户发布的活动列表
     */
    @GetMapping("/my-published")
    public Result<List<Activity>> getMyPublished(@RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        return Result.success(activityService.getMyPublished(userId));
    }
}
