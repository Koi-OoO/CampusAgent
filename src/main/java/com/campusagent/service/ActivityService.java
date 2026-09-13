package com.campusagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.entity.Activity;

import java.util.List;

/**
 * 活动服务接口，在基础增删改查能力上声明发布、审核及查询等业务方法。
 *
 * <p>本阶段使用 Activity 承载创建和修改请求中的可编辑资料，操作人身份单独传入。
 * 主键、发布人、状态、统计值及审计字段由服务端管理，不能直接信任请求中的这些值。
 * 当前提供草稿创建、提交审核、修改、删除及发布列表查询，其余业务方法保留占位实现。</p>
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 为当前用户创建活动草稿。
     *
     * @param request 活动创建请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键，用于确定活动发布人
     * @throws BusinessException 时间或人数不合法，或者草稿保存失败
     */
    void createDraft(Activity request, Long userId);

    /**
     * 将当前用户发布的活动提交审核。
     *
     * @param activityId 待提交审核的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或活动状态不可提交
     */
    void submitForAudit(Long activityId, Long userId);

    /**
     * 修改当前用户发布的活动资料。
     *
     * @param activityId 待修改的活动主键，以此参数确定目标活动
     * @param request 活动修改请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或状态、时间、人数不符合规则
     */
    void updateActivity(Long activityId, Activity request, Long userId);

    /**
     * 逻辑删除当前用户发布的活动。
     *
     * @param activityId 待删除的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或活动不是草稿
     */
    void deleteActivity(Long activityId, Long userId);

    /**
     * 根据活动主键查询活动详情。
     *
     * @param activityId 待查询的活动主键
     * @return 活动详情
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    Activity getActivityDetail(Long activityId);

    /**
     * 查询当前用户发布的全部未删除活动，按创建时间倒序返回。
     *
     * @param userId 当前登录用户的主键
     * @return 当前用户发布的活动列表，没有活动时返回空列表
     */
    List<Activity> getMyPublished(Long userId);

    /**
     * 查询待审核的活动列表。
     *
     * @return 等待审核的活动列表
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    List<Activity> getAuditList();

    /**
     * 审核活动，记录审核结果及审核说明。
     *
     * @param activityId 待审核的活动主键
     * @param result 审核结果编码，表示审核通过或驳回
     * @param reason 审核说明，驳回时填写驳回理由
     * @param operatorId 当前执行审核的操作人主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    void auditActivity(Long activityId, Integer result, String reason, Long operatorId);

    /**
     * 取消活动并记录取消理由。
     *
     * @param activityId 待取消的活动主键
     * @param reason 活动取消理由
     * @param operatorId 当前执行取消操作的用户主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    void cancelActivity(Long activityId, String reason, Long operatorId);

    /**
     * 按分类和活动标题关键词查询公开活动列表。
     *
     * @param categoryId 活动分类主键，为 null 时不限定分类
     * @param keyword 活动标题关键词，为 null 或空白时不限定关键词
     * @return 符合筛选条件的公开活动列表
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    List<Activity> getPublicList(Integer categoryId, String keyword);
}
