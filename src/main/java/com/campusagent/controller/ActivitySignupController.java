package com.campusagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.request.ActivitySignupRequest;
import com.campusagent.dto.response.MySignupResponse;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.service.ActivityService;
import com.campusagent.service.ActivitySignupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动报名控制器，提供活动报名、取消报名、我的报名列表及报名状态查询接口。
 *
 * <p>全部接口位于登录拦截器范围内，用户主键从已验证的请求属性中获取；
 * 我的报名列表由服务层分页查询报名记录后，在本层批量关联活动信息组装响应。</p>
 */
@RestController
@RequestMapping("/api/activity/signup")
public class ActivitySignupController {

    /**
     * 活动报名服务，通过构造器注入。
     */
    private final ActivitySignupService activitySignupService;

    /**
     * 活动服务，用于组装我的报名列表时批量查询活动信息，通过构造器注入。
     */
    private final ActivityService activityService;

    /**
     * 创建活动报名控制器。
     *
     * @param activitySignupService 活动报名服务
     * @param activityService 活动服务
     */
    public ActivitySignupController(ActivitySignupService activitySignupService, ActivityService activityService) {
        this.activitySignupService = activitySignupService;
        this.activityService = activityService;
    }

    /**
     * 报名活动接口。
     *
     * @param request 报名请求，活动主键必填，表单数据可选
     * @param userId 登录拦截器写入的当前用户主键
     * @return 报名成功响应
     */
    @PostMapping
    public Result<Void> signup(@Valid @RequestBody ActivitySignupRequest request,
                               @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        activitySignupService.signup(request.getActivityId(), userId, request.getFormData());
        return Result.success();
    }

    /**
     * 取消报名接口。
     *
     * @param activityId 待取消报名的活动主键
     * @param userId 登录拦截器写入的当前用户主键
     * @return 取消成功响应
     */
    @DeleteMapping("/{activityId}")
    public Result<Void> cancelSignup(@PathVariable Long activityId,
                                     @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        activitySignupService.cancelSignup(activityId, userId);
        return Result.success();
    }

    /**
     * 我的报名列表接口，分页返回报名记录及对应的活动信息。
     *
     * <p>活动信息按当前页的活动主键批量查询一次后填充，避免逐条查询造成 N+1；
     * 活动已被删除的报名记录从结果中跳过。</p>
     *
     * @param status 报名状态编码，可选
     * @param page 页码，从 1 开始，默认查询第一页
     * @param size 每页条数，默认为 10
     * @param userId 登录拦截器写入的当前用户主键
     * @return 我的报名分页结果，记录为列表响应 DTO
     */
    @GetMapping("/my")
    public Result<IPage<MySignupResponse>> getMySignup(@RequestParam(required = false) Integer status,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        IPage<ActivitySignup> signupPage = activitySignupService.getMySignup(userId, status, page, size);
        // 复用 IPage 的转换能力生成响应分页，随后批量填充活动信息并剔除已删除活动的记录。
        IPage<MySignupResponse> responsePage = signupPage.convert(this::buildMySignupResponse);
        fillActivityInfo(responsePage.getRecords());
        return Result.success(responsePage);
    }

    /**
     * 查询当前用户对指定活动的报名状态。
     *
     * @param activityId 活动主键
     * @param userId 登录拦截器写入的当前用户主键
     * @return 报名状态编码，未报名时 data 为 null
     */
    @GetMapping("/status/{activityId}")
    public Result<Integer> getUserSignupStatus(@PathVariable Long activityId,
                                               @RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        SignupStatusEnum status = activitySignupService.getUserSignupStatus(activityId, userId);
        return Result.success(status == null ? null : status.getCode());
    }

    /**
     * 批量查询当前页报名记录对应的活动信息，并填充到响应记录中。
     *
     * <p>活动主键集合一次查询完成关联，活动已被删除的记录从结果中移除。</p>
     *
     * @param records 待填充活动信息的响应记录列表，可为空
     */
    private void fillActivityInfo(List<MySignupResponse> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 汇总当前页涉及的活动主键，批量查询一次活动信息。
        Set<Long> activityIds = records.stream()
                .map(MySignupResponse::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Activity> activityMap = loadActivityMap(activityIds);
        // 活动已被删除时跳过该记录，其余记录填充活动信息。
        List<MySignupResponse> validRecords = new ArrayList<>();
        for (MySignupResponse response : records) {
            Activity activity = activityMap.get(response.getActivityId());
            if (activity == null) {
                continue;
            }
            fillActivityFields(response, activity);
            validRecords.add(response);
        }
        records.clear();
        records.addAll(validRecords);
    }

    /**
     * 批量加载活动主键到活动实体的映射，一次查询避免逐条查询造成 N+1。
     *
     * @param activityIds 待查询的活动主键集合，为空时直接返回空映射
     * @return 活动主键到活动实体的映射，已被删除或查不到的活动不在映射中
     */
    private Map<Long, Activity> loadActivityMap(Set<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Activity> activities = activityService.listByIds(activityIds);
        Map<Long, Activity> activityMap = new HashMap<>();
        for (Activity activity : activities) {
            activityMap.put(activity.getId(), activity);
        }
        return activityMap;
    }

    /**
     * 组装我的报名列表响应 DTO，仅填充报名记录字段，活动信息随后批量填充。
     *
     * @param signup 报名记录
     * @return 我的报名列表响应 DTO
     */
    private MySignupResponse buildMySignupResponse(ActivitySignup signup) {
        MySignupResponse response = new MySignupResponse();
        response.setSignupId(signup.getId());
        response.setActivityId(signup.getActivityId());
        response.setSignupStatus(signup.getStatus() == null ? null : signup.getStatus().getCode());
        response.setSignupTime(signup.getSignupTime());
        response.setCheckinTime(signup.getCheckinTime());
        return response;
    }

    /**
     * 将活动信息填充到响应 DTO 的活动字段中。
     *
     * @param response 待填充的响应 DTO
     * @param activity 报名记录对应的活动
     */
    private void fillActivityFields(MySignupResponse response, Activity activity) {
        response.setActivityTitle(activity.getTitle());
        response.setActivityLocation(activity.getLocation());
        response.setActivityStartTime(activity.getStartTime());
        response.setActivityEndTime(activity.getEndTime());
    }
}
