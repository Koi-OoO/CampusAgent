package com.campusagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.dto.response.ActivityDetailResponse;
import com.campusagent.dto.response.ActivityListResponse;
import com.campusagent.entity.Activity;

import java.util.List;

/**
 * 活动服务接口，在基础增删改查能力上声明发布、审核、查询等业务方法。
 *
 * <p>本阶段使用 Activity 承载创建和修改请求中的可编辑资料，操作人身份单独传入。
 * 主键、发布人、状态、统计值及审计字段由服务端管理，不能直接信任请求中的这些值。
 * 当前提供活动发布、管理员审核与取消、待审核列表查询及用户端公开列表与详情查询。</p>
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
     * 查询公开活动的详情，并累加浏览次数。
     *
     * <p>仅公开状态（未开始、进行中、已结束）的活动可见；
     * currentUserId 非空时查询该用户的报名状态，报名功能落地前该字段暂为 null。</p>
     *
     * @param activityId 待查询的活动主键
     * @param currentUserId 当前登录用户主键，未登录时为 null
     * @return 活动详情，包含分类名称、发布人姓名及当前用户报名状态
     * @throws BusinessException 活动不存在或处于非公开状态
     */
    ActivityDetailResponse getActivityDetail(Long activityId, Long currentUserId);

    /**
     * 查询当前用户发布的全部未删除活动，按创建时间倒序返回。
     *
     * @param userId 当前登录用户的主键
     * @return 当前用户发布的活动列表，没有活动时返回空列表
     */
    List<Activity> getMyPublished(Long userId);

    /**
     * 查询全部待审核活动，按创建时间升序返回。
     *
     * @return 等待审核的活动列表，没有活动时返回空列表
     */
    List<Activity> getAuditList();

    /**
     * 分页查询待审核活动，按创建时间升序返回。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 包含待审核活动记录、总数及分页信息的结果
     * @throws BusinessException 页码或每页条数为空或不大于零
     */
    IPage<Activity> getAuditListPage(Integer page, Integer size);

    /**
     * 审核活动，记录审核结果及审核说明。
     *
     * @param activityId 待审核的活动主键
     * @param result 审核结果编码，表示审核通过或驳回
     * @param reason 审核说明，驳回时填写驳回理由
     * @param operatorId 当前执行审核的操作人主键
     * @throws BusinessException 活动不存在、状态或审核参数不合法、并发状态冲突或审核记录保存失败
     */
    void auditActivity(Long activityId, Integer result, String reason, Long operatorId);

    /**
     * 取消活动并记录取消理由。
     *
     * @param activityId 待取消的活动主键
     * @param reason 活动取消理由
     * @param operatorId 当前执行取消操作的用户主键
     * @throws BusinessException 活动不存在、状态或取消理由不合法，或者发生并发状态冲突
     */
    void cancelActivity(Long activityId, String reason, Long operatorId);

    /**
     * 分页查询用户端公开活动列表，支持分类、状态及标题关键词筛选。
     *
     * <p>仅返回未开始、进行中、已结束的活动，按开始时间升序排列，
     * 分类名称通过批量查询一次性关联，避免逐条查询。</p>
     *
     * @param categoryId 活动分类主键，为 null 时不限定分类
     * @param keyword 活动标题关键词，为 null 或空白时不限定关键词
     * @param status 活动状态编码（3/4/5），为 null 时查询全部公开状态
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 公开活动分页结果，记录为列表响应 DTO
     * @throws BusinessException 页码、每页条数非法或状态编码不是公开状态
     */
    IPage<ActivityListResponse> getPublicList(Integer categoryId, String keyword, Integer status,
                                              Integer page, Integer size);
}
