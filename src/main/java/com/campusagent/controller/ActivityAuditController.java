package com.campusagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusagent.annotation.RequireRole;
import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.request.ActivityAuditRequest;
import com.campusagent.dto.request.ActivityCancelRequest;
import com.campusagent.entity.Activity;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动审核控制器，提供待审核活动分页查询、活动审核及管理员取消活动接口。
 *
 * <p>全部接口要求管理员权限，超级管理员沿用现有权限切面的放行规则。
 * 操作人主键仅从登录拦截器验证后写入的请求属性中获取。</p>
 */
@RestController
@RequestMapping("/api/activity/audit")
@RequireRole(UserRoleEnum.ADMIN)
public class ActivityAuditController {

    /**
     * 活动服务，负责审核状态、事务及审核记录的业务处理。
     */
    private final ActivityService activityService;

    /**
     * 创建活动审核控制器，通过构造器注入活动服务。
     *
     * @param activityService 活动服务
     */
    public ActivityAuditController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 分页查询待审核活动，按创建时间升序返回。
     *
     * @param page 页码，从 1 开始，默认查询第一页
     * @param size 每页条数，默认为 10
     * @return 待审核活动分页结果，包含记录及总数
     */
    @GetMapping("/list")
    public Result<IPage<Activity>> getAuditList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                               @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.success(activityService.getAuditListPage(page, size));
    }

    /**
     * 审核指定活动，审核通过和驳回均保存审核记录。
     *
     * @param request 活动审核请求，基础字段由框架统一校验
     * @param operatorId 登录拦截器写入的当前管理员主键
     * @return 审核成功响应
     */
    @PostMapping({"", "/"})
    public Result<Void> auditActivity(@Valid @RequestBody ActivityAuditRequest request,
                                      @RequestAttribute(RequestAttributeConstants.USER_ID) Long operatorId) {
        activityService.auditActivity(request.getActivityId(), request.getResult(), request.getReason(), operatorId);
        return Result.success();
    }

    /**
     * 取消未开始或进行中的活动，并保存取消理由。
     *
     * @param request 活动取消请求，基础字段由框架统一校验
     * @param operatorId 登录拦截器写入的当前管理员主键
     * @return 取消成功响应
     */
    @PostMapping("/cancel")
    public Result<Void> cancelActivity(@Valid @RequestBody ActivityCancelRequest request,
                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long operatorId) {
        activityService.cancelActivity(request.getActivityId(), request.getReason(), operatorId);
        return Result.success();
    }
}
