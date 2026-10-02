package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.response.ActivityDetailResponse;
import com.campusagent.dto.response.ActivityListResponse;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivityAuditLog;
import com.campusagent.entity.ActivityCategory;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.entity.User;
import com.campusagent.enums.ActivityStatusEnum;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.mapper.ActivityAuditLogMapper;
import com.campusagent.mapper.ActivityMapper;
import com.campusagent.mapper.ActivitySignupMapper;
import com.campusagent.service.ActivityCategoryService;
import com.campusagent.service.ActivityService;
import com.campusagent.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动服务实现，负责活动发布、管理员审核与取消，以及用户端公开列表与详情查询。
 *
 * <p>活动归属、状态、时间和人数限制统一在服务层校验；
 * 公开查询仅暴露未开始、进行中、已结束三种状态，浏览次数通过数据库原子自增维护。</p>
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    /** 活动开始时间相对当前时间必须超过的小时数。 */
    private static final long MIN_START_LEAD_HOURS = 1L;

    /** 活动持续时间必须超过的小时数。 */
    private static final long MIN_DURATION_HOURS = 1L;

    /** 报名截止时间相对活动开始时间至少提前的小时数。 */
    private static final long MIN_SIGNUP_LEAD_HOURS = 2L;

    /** 不限制报名人数时使用的上限值。 */
    private static final int UNLIMITED_PARTICIPANTS = 0;

    /** 审核通过的结果编码。 */
    private static final int AUDIT_APPROVED = 1;

    /** 审核驳回的结果编码。 */
    private static final int AUDIT_REJECTED = 2;

    /** 审核理由及取消理由允许的最大字符数。 */
    private static final int MAX_REASON_LENGTH = 200;

    /** 审核记录数据访问接口，与活动状态更新使用同一事务。 */
    private final ActivityAuditLogMapper activityAuditLogMapper;

    /** 活动分类服务，用于公开列表和详情中批量关联分类名称。 */
    private final ActivityCategoryService activityCategoryService;

    /** 用户服务，用于详情中查询发布人姓名及报名状态。 */
    private final UserService userService;

    /** 活动报名数据访问接口，用于详情中查询当前用户报名状态。 */
    private final ActivitySignupMapper activitySignupMapper;

    /**
     * 创建活动服务，通过构造器注入数据访问接口、分类服务、用户服务及报名数据访问接口。
     *
     * @param activityMapper 活动数据访问接口
     * @param activityAuditLogMapper 审核记录数据访问接口
     * @param activityCategoryService 活动分类服务
     * @param userService 用户服务
     * @param activitySignupMapper 活动报名数据访问接口
     */
    public ActivityServiceImpl(ActivityMapper activityMapper, ActivityAuditLogMapper activityAuditLogMapper,
                               ActivityCategoryService activityCategoryService, UserService userService,
                               ActivitySignupMapper activitySignupMapper) {
        this.baseMapper = activityMapper;
        this.activityAuditLogMapper = activityAuditLogMapper;
        this.activityCategoryService = activityCategoryService;
        this.userService = userService;
        this.activitySignupMapper = activitySignupMapper;
    }

    /**
     * 校验活动资料并创建草稿，发布人、状态及统计值由服务端初始化。
     *
     * @param request 活动创建请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键，用于确定活动发布人
     * @throws BusinessException 时间或人数不合法，或者草稿保存失败
     */
    @Override
    public void createDraft(Activity request, Long userId) {
        validateTimeRules(request);
        validateParticipants(request);

        // 只复制可编辑资料，避免请求中的主键、删除标记或审核信息参与新建。
        Activity activity = new Activity();
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setCoverImage(request.getCoverImage());
        activity.setCategoryId(request.getCategoryId());
        activity.setLocation(request.getLocation());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setSignupDeadline(request.getSignupDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setSignupFormConfig(request.getSignupFormConfig());
        activity.setPublisherId(userId);
        activity.setStatus(ActivityStatusEnum.DRAFT);
        activity.setCurrentParticipants(0);
        activity.setViewCount(0);

        if (!save(activity)) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 将当前发布人的草稿或已驳回活动提交审核，重新提交时清除驳回理由。
     *
     * @param activityId 待提交审核的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或活动状态不可提交
     */
    @Override
    public void submitForAudit(Long activityId, Long userId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (userId == null || !userId.equals(activity.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        ActivityStatusEnum status = activity.getStatus();
        if (status != ActivityStatusEnum.DRAFT && status != ActivityStatusEnum.REJECTED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可提交审核");
        }

        // 将读取时的状态加入更新条件，避免重复提交覆盖已发生的状态变化。
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .eq(Activity::getPublisherId, userId)
                .eq(Activity::getStatus, status)
                .set(Activity::getStatus, ActivityStatusEnum.PENDING_AUDIT)
                // 显式写入 NULL，避免实体更新策略忽略空值而保留旧的驳回理由。
                .set(status == ActivityStatusEnum.REJECTED, Activity::getRejectReason, null);
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动状态已变化，请刷新后重试");
        }
    }

    /**
     * 修改当前发布人的草稿或已驳回活动，仅更新可编辑资料并校验报名人数上限。
     *
     * @param activityId 待修改的活动主键，以此参数确定目标活动
     * @param request 活动修改请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或状态、时间、人数不符合规则
     */
    @Override
    public void updateActivity(Long activityId, Activity request, Long userId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (userId == null || !userId.equals(activity.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        ActivityStatusEnum status = activity.getStatus();
        if (status != ActivityStatusEnum.DRAFT && status != ActivityStatusEnum.REJECTED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可修改");
        }
        validateTimeRules(request);
        validateParticipants(request);

        int oldLimit = activity.getMaxParticipants();
        int newLimit = request.getMaxParticipants();
        // 0 表示无限容量；从有限人数改为不限属于扩大，反向修改属于缩小。
        if (activity.getCurrentParticipants() > 0 && newLimit != UNLIMITED_PARTICIPANTS
                && (oldLimit == UNLIMITED_PARTICIPANTS || newLimit < oldLimit)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已有人报名，人数上限只能增大");
        }

        // 显式列出可编辑字段，使可空资料可以清除，同时保护发布人、状态及统计值。
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .eq(Activity::getPublisherId, userId)
                .eq(Activity::getStatus, status)
                .eq(Activity::getMaxParticipants, oldLimit)
                .eq(Activity::getCurrentParticipants, activity.getCurrentParticipants())
                .set(Activity::getTitle, request.getTitle())
                .set(Activity::getDescription, request.getDescription())
                .set(Activity::getCoverImage, request.getCoverImage())
                .set(Activity::getCategoryId, request.getCategoryId())
                .set(Activity::getLocation, request.getLocation())
                .set(Activity::getStartTime, request.getStartTime())
                .set(Activity::getEndTime, request.getEndTime())
                .set(Activity::getSignupDeadline, request.getSignupDeadline())
                .set(Activity::getMaxParticipants, newLimit)
                .set(Activity::getSignupFormConfig, request.getSignupFormConfig());
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动信息已变化，请刷新后重试");
        }
    }

    /**
     * 在事务中锁定活动记录，校验归属和草稿状态后执行逻辑删除。
     *
     * <p>行锁覆盖状态检查与删除，防止并发提交审核后仍将活动删除。</p>
     *
     * @param activityId 待删除的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 活动不存在、用户不是发布人或活动不是草稿
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId, Long userId) {
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .last("FOR UPDATE");
        Activity activity = getOne(queryWrapper);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (userId == null || !userId.equals(activity.getPublisherId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (activity.getStatus() != ActivityStatusEnum.DRAFT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能删除草稿状态的活动");
        }
        if (!removeById(activityId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 查询公开活动详情，浏览次数通过数据库原子自增，避免并发读取覆盖。
     *
     * <p>活动不存在或处于非公开状态（草稿、待审核、已驳回、已取消）时统一返回数据不存在；
     * 当前用户未登录或未报名时，当前用户报名状态返回 null。</p>
     *
     * @param activityId 待查询的活动主键
     * @param currentUserId 当前登录用户主键，未登录时为 null
     * @return 活动详情，包含分类名称、发布人姓名及当前用户报名状态
     * @throws BusinessException 活动不存在或处于非公开状态
     */
    @Override
    public ActivityDetailResponse getActivityDetail(Long activityId, Long currentUserId) {
        Activity activity = getById(activityId);
        // 非公开状态的活动对外与不存在同等对待，不暴露内部状态信息。
        if (activity == null || !isPublicStatus(activity.getStatus())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 使用数据库原子自增维护浏览次数，避免先查再改在并发下丢失计数；
        // 列名为硬编码的固定值，不存在 SQL 注入风险。
        LambdaUpdateWrapper<Activity> incrementWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .setSql("view_count = view_count + 1");
        if (!update(incrementWrapper)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 自增后的最新值在响应中体现，避免返回读取时的旧计数。
        activity.setViewCount(activity.getViewCount() == null ? 1 : activity.getViewCount() + 1);

        ActivityDetailResponse response = new ActivityDetailResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setCoverImage(activity.getCoverImage());
        response.setLocation(activity.getLocation());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setSignupDeadline(activity.getSignupDeadline());
        response.setStatus(activity.getStatus().getCode());
        response.setCategoryId(activity.getCategoryId());
        response.setMaxParticipants(activity.getMaxParticipants());
        response.setCurrentParticipants(activity.getCurrentParticipants());
        response.setViewCount(activity.getViewCount());
        response.setSignupFormConfig(activity.getSignupFormConfig());
        response.setPublisherId(activity.getPublisherId());
        response.setRejectReason(activity.getRejectReason());
        response.setCancelReason(activity.getCancelReason());
        response.setCreateTime(activity.getCreateTime());

        // 分类名称和发布人姓名缺失时允许为空，不影响详情主体返回。
        response.setCategoryName(resolveCategoryName(activity.getCategoryId()));
        response.setPublisherName(resolvePublisherName(activity.getPublisherId()));

        SignupStatusEnum signupStatus = resolveCurrentUserSignupStatus(activityId, currentUserId);
        response.setCurrentUserSignupStatus(signupStatus == null ? null : signupStatus.getCode());
        return response;
    }

    /**
     * 查询当前用户发布的全部未删除活动，按创建时间倒序返回。
     *
     * @param userId 当前登录用户的主键
     * @return 当前用户发布的活动列表，没有活动时返回空列表
     */
    @Override
    public List<Activity> getMyPublished(Long userId) {
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getPublisherId, userId)
                .orderByDesc(Activity::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 查询全部未删除的待审核活动，按创建时间升序返回。
     *
     * @return 等待审核的活动列表，没有活动时返回空列表
     */
    @Override
    public List<Activity> getAuditList() {
        return list(buildAuditQueryWrapper());
    }

    /**
     * 分页查询未删除的待审核活动，按创建时间升序返回并统计总数。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 待审核活动的分页结果
     * @throws BusinessException 页码或每页条数为空或不大于零
     */
    @Override
    public IPage<Activity> getAuditListPage(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于 0");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页条数必须大于 0");
        }
        return page(new Page<Activity>(page, size), buildAuditQueryWrapper());
    }

    /**
     * 审核待审核活动，并在同一事务中写入审核记录。
     *
     * <p>更新时要求活动仍为待审核状态，防止并发审核重复写入结果或记录。</p>
     *
     * @param activityId 待审核的活动主键
     * @param result 审核结果编码，表示审核通过或驳回
     * @param reason 审核说明，驳回时填写驳回理由
     * @param operatorId 当前执行审核的操作人主键
     * @throws BusinessException 活动不存在、状态或审核参数不合法、并发状态冲突或审核记录保存失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditActivity(Long activityId, Integer result, String reason, Long operatorId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (activity.getStatus() != ActivityStatusEnum.PENDING_AUDIT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可审核");
        }
        if (result == null || (result != AUDIT_APPROVED && result != AUDIT_REJECTED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果不合法");
        }
        if (result == AUDIT_REJECTED && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "驳回时必须填写理由");
        }
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核理由长度不能超过 200 个字符");
        }
        if (operatorId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核人不能为空");
        }

        boolean approved = result == AUDIT_APPROVED;
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .eq(Activity::getStatus, ActivityStatusEnum.PENDING_AUDIT)
                .set(Activity::getStatus, approved ? ActivityStatusEnum.NOT_STARTED : ActivityStatusEnum.REJECTED)
                // 显式写入 NULL，确保通过审核后清除已有的驳回理由。
                .set(Activity::getRejectReason, approved ? null : reason);
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动状态已变化，请刷新后重试");
        }

        ActivityAuditLog auditLog = new ActivityAuditLog();
        auditLog.setActivityId(activityId);
        auditLog.setOperatorId(operatorId);
        auditLog.setResult(result);
        auditLog.setReason(reason);
        if (activityAuditLogMapper.insert(auditLog) != 1) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "审核记录保存失败");
        }
    }

    /**
     * 在事务中取消未开始或进行中的活动，并保存取消理由。
     *
     * <p>更新条件包含读取时的原状态，避免并发状态变化被取消操作覆盖。</p>
     *
     * @param activityId 待取消的活动主键
     * @param reason 活动取消理由
     * @param operatorId 当前执行取消操作的用户主键
     * @throws BusinessException 活动不存在、状态或取消理由不合法，或者发生并发状态冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long activityId, String reason, Long operatorId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        ActivityStatusEnum status = activity.getStatus();
        if (status != ActivityStatusEnum.NOT_STARTED && status != ActivityStatusEnum.IN_PROGRESS) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可取消");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "取消理由不能为空");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "取消理由长度不能超过 200 个字符");
        }

        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .eq(Activity::getStatus, status)
                .set(Activity::getStatus, ActivityStatusEnum.CANCELLED)
                .set(Activity::getCancelReason, reason);
        if (!update(updateWrapper)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动状态已变化，请刷新后重试");
        }
    }

    /**
     * 分页查询用户端公开活动列表，支持分类、状态及标题关键词筛选。
     *
     * <p>分类名称按当前页分类主键集合批量查询一次后填充，避免逐条查询造成 N+1；
     * 列表按开始时间升序返回，最近要开始的活动排在前面。</p>
     *
     * @param categoryId 活动分类主键，为 null 时不限定分类
     * @param keyword 活动标题关键词，为 null 或空白时不限定关键词
     * @param status 活动状态编码（3/4/5），为 null 时查询全部公开状态
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 公开活动分页结果，记录为列表响应 DTO
     * @throws BusinessException 页码、每页条数非法或状态编码不是公开状态
     */
    @Override
    public IPage<ActivityListResponse> getPublicList(Integer categoryId, String keyword, Integer status,
                                                     Integer page, Integer size) {
        // 分页参数非法时提前终止，避免向分页插件传入无效页码。
        if (page == null || page < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于 0");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页条数必须大于 0");
        }

        // 状态编码只允许公开状态的三种取值，非法值直接拒绝。
        ActivityStatusEnum statusEnum = null;
        if (status != null) {
            statusEnum = ActivityStatusEnum.fromCode(status);
            if (statusEnum == null || !isPublicStatus(statusEnum)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "活动状态不合法");
            }
        }

        // 构造公开活动查询条件，状态、分类和关键词按参数动态拼接。
        LambdaQueryWrapper<Activity> queryWrapper = buildPublicQueryWrapper(categoryId, keyword, statusEnum);
        IPage<Activity> activityPage = page(new Page<Activity>(page, size), queryWrapper);

        // 批量查询当前页涉及的分类名称，构建主键到名称的映射供逐条填充。
        Map<Integer, String> categoryNameMap = loadCategoryNameMap(toCategoryIdSet(activityPage.getRecords()));
        // 复用 IPage 的转换能力生成响应分页，保留 total、pages 等分页元数据。
        return activityPage.convert(activity -> buildListResponse(activity, categoryNameMap));
    }

    /**
     * 构建待审核活动查询条件，按创建时间及主键升序保持分页顺序稳定。
     *
     * @return 仅匹配待审核状态的活动查询条件，逻辑删除条件由框架自动追加
     */
    private LambdaQueryWrapper<Activity> buildAuditQueryWrapper() {
        return new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, ActivityStatusEnum.PENDING_AUDIT)
                .orderByAsc(Activity::getCreateTime)
                .orderByAsc(Activity::getId);
    }

    /**
     * 判断状态是否属于用户端可见的公开状态。
     *
     * @param status 待判断的活动状态，允许为 null
     * @return 状态为未开始、进行中或已结束时返回 true
     */
    private boolean isPublicStatus(ActivityStatusEnum status) {
        return status == ActivityStatusEnum.NOT_STARTED
                || status == ActivityStatusEnum.IN_PROGRESS
                || status == ActivityStatusEnum.ENDED;
    }

    /**
     * 构建用户端公开活动查询条件，按开始时间及主键升序保持分页顺序稳定。
     *
     * @param categoryId 活动分类主键，为 null 时不限定分类
     * @param keyword 活动标题关键词，为 null 或空白时不限定关键词
     * @param statusEnum 公开活动状态，为 null 时查询全部公开状态
     * @return 匹配公开状态及筛选条件的活动查询条件
     */
    private LambdaQueryWrapper<Activity> buildPublicQueryWrapper(Integer categoryId, String keyword,
                                                                 ActivityStatusEnum statusEnum) {
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<Activity>();
        if (statusEnum != null) {
            queryWrapper.eq(Activity::getStatus, statusEnum);
        } else {
            queryWrapper.in(Activity::getStatus,
                    ActivityStatusEnum.NOT_STARTED, ActivityStatusEnum.IN_PROGRESS, ActivityStatusEnum.ENDED);
        }
        queryWrapper.eq(categoryId != null, Activity::getCategoryId, categoryId);
        // 关键词先去除首尾空白，避免仅含空格的参数被当作有效条件。
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        queryWrapper.like(trimmedKeyword != null, Activity::getTitle, trimmedKeyword);
        queryWrapper.orderByAsc(Activity::getStartTime).orderByAsc(Activity::getId);
        return queryWrapper;
    }

    /**
     * 汇总活动记录中非空的分类主键集合。
     *
     * @param activities 活动列表，允许为空
     * @return 去重后的分类主键集合，没有分类主键时返回空集合
     */
    private Set<Integer> toCategoryIdSet(List<Activity> activities) {
        return activities.stream()
                .map(Activity::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 批量加载分类主键到名称的映射，一次查询避免逐条查询造成 N+1。
     *
     * @param categoryIds 待查询的分类主键集合，为空时直接返回空映射
     * @return 分类主键到名称的映射，被删除或查不到的分类不在映射中
     */
    private Map<Integer, String> loadCategoryNameMap(Set<Integer> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<ActivityCategory> queryWrapper = new LambdaQueryWrapper<ActivityCategory>()
                .in(ActivityCategory::getId, categoryIds)
                .select(ActivityCategory::getId, ActivityCategory::getName);
        List<ActivityCategory> categories = activityCategoryService.list(queryWrapper);
        Map<Integer, String> nameMap = new HashMap<>();
        for (ActivityCategory category : categories) {
            nameMap.put(category.getId(), category.getName());
        }
        return nameMap;
    }

    /**
     * 组装列表响应 DTO，分类名称由批量加载的映射填充。
     *
     * @param activity 活动实体
     * @param categoryNameMap 分类主键到名称的映射
     * @return 活动列表响应 DTO
     */
    private ActivityListResponse buildListResponse(Activity activity, Map<Integer, String> categoryNameMap) {
        ActivityListResponse response = new ActivityListResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setCoverImage(activity.getCoverImage());
        response.setLocation(activity.getLocation());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setStatus(activity.getStatus().getCode());
        response.setCategoryId(activity.getCategoryId());
        // 未设置分类或分类已被删除时，分类名称为 null。
        response.setCategoryName(categoryNameMap.get(activity.getCategoryId()));
        response.setMaxParticipants(activity.getMaxParticipants());
        response.setCurrentParticipants(activity.getCurrentParticipants());
        return response;
    }

    /**
     * 根据分类主键查询分类名称。
     *
     * @param categoryId 活动分类主键，允许为 null
     * @return 分类名称，分类主键为 null 或分类不存在时返回 null
     */
    private String resolveCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        ActivityCategory category = activityCategoryService.getById(categoryId);
        return category == null ? null : category.getName();
    }

    /**
     * 根据发布人主键查询对外展示的发布人姓名。
     *
     * <p>优先使用真实姓名，未填写时回退为用户名，保证响应字段不为空。</p>
     *
     * @param publisherId 发布人主键，允许为 null
     * @return 发布人姓名，发布人主键为 null 或用户不存在时返回 null
     */
    private String resolvePublisherName(Long publisherId) {
        if (publisherId == null) {
            return null;
        }
        User publisher = userService.getById(publisherId);
        if (publisher == null) {
            return null;
        }
        return StringUtils.hasText(publisher.getRealName()) ? publisher.getRealName() : publisher.getUsername();
    }

    /**
     * 查询当前用户对活动的报名状态，未登录或未报名时返回 null。
     *
     * @param activityId 活动主键
     * @param currentUserId 当前登录用户主键，未登录时为 null
     * @return 当前用户报名状态
     */
    private SignupStatusEnum resolveCurrentUserSignupStatus(Long activityId, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        LambdaQueryWrapper<ActivitySignup> queryWrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, currentUserId);
        ActivitySignup signup = activitySignupMapper.selectOne(queryWrapper);
        return signup == null ? null : signup.getStatus();
    }

    /**
     * 校验活动时间：开始时间晚于当前时间一小时，持续时间超过一小时，报名至少提前两小时截止。
     *
     * @param activity 待校验的活动资料
     * @throws BusinessException 活动资料或必填时间为空，或者时间关系不符合规则
     */
    private void validateTimeRules(Activity activity) {
        if (activity == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动信息不能为空");
        }
        LocalDateTime startTime = activity.getStartTime();
        LocalDateTime endTime = activity.getEndTime();
        LocalDateTime signupDeadline = activity.getSignupDeadline();
        if (startTime == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间不能为空");
        }
        if (endTime == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束时间不能为空");
        }
        if (signupDeadline == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "报名截止时间不能为空");
        }
        if (!startTime.isAfter(LocalDateTime.now().plusHours(MIN_START_LEAD_HOURS))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间必须晚于当前时间 1 小时");
        }
        if (!endTime.isAfter(startTime.plusHours(MIN_DURATION_HOURS))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间 1 小时");
        }
        if (signupDeadline.isAfter(startTime.minusHours(MIN_SIGNUP_LEAD_HOURS))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "报名截止时间必须早于开始时间 2 小时");
        }
    }

    /**
     * 校验报名人数上限必须提供且不小于零，零表示不限人数。
     *
     * @param activity 已通过时间校验的活动资料
     * @throws BusinessException 人数上限为空或为负数
     */
    private void validateParticipants(Activity activity) {
        Integer maxParticipants = activity.getMaxParticipants();
        if (maxParticipants == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限不能为空");
        }
        if (maxParticipants < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限不能为负数");
        }
    }
}
