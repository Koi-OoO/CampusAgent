package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.enums.ActivityStatusEnum;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.mapper.ActivitySignupMapper;
import com.campusagent.service.ActivityService;
import com.campusagent.service.ActivitySignupService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 活动报名服务实现，负责报名、取消报名、我的报名列表及报名状态查询。
 *
 * <p>报名名额通过数据库原子更新扣减（UPDATE ... WHERE current_participants < max_participants），
 * 避免先查再改在并发下超卖；重复报名由唯一索引 (activity_id, user_id) 兜底拦截；
 * 扣名额与写入报名记录在同一事务中完成，保证两者原子生效。</p>
 */
@Service
public class ActivitySignupServiceImpl extends ServiceImpl<ActivitySignupMapper, ActivitySignup>
        implements ActivitySignupService {

    /** 不限制报名人数时使用的人数上限值。 */
    private static final int UNLIMITED_PARTICIPANTS = 0;

    /** 活动服务，用于查询活动信息及原子更新报名人数。 */
    private final ActivityService activityService;

    /**
     * 创建活动报名服务，通过构造器注入数据访问接口及活动服务。
     *
     * @param activitySignupMapper 活动报名记录数据访问接口
     * @param activityService 活动服务
     */
    public ActivitySignupServiceImpl(ActivitySignupMapper activitySignupMapper, ActivityService activityService) {
        this.baseMapper = activitySignupMapper;
        this.activityService = activityService;
    }

    /**
     * 报名活动：校验活动及报名时间后，原子扣减名额并写入报名记录。
     *
     * <p>活动存在（未被逻辑删除）、状态为未开始且未过报名截止时间时开放报名；
     * 不限人数（max_participants = 0）的活动跳过名额扣减，直接写入报名记录；
     * 并发下的重复报名由唯一索引兜底，DuplicateKeyException 统一转换为业务提示。</p>
     *
     * @param activityId 待报名的活动主键
     * @param userId 当前登录用户的主键
     * @param formData 报名表单数据，JSON 字符串，可为空
     * @throws BusinessException 活动不存在、活动不可报名、报名已截止、名额已满或重复报名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signup(Long activityId, Long userId, String formData) {
        // 活动不存在（含已逻辑删除）时直接拒绝。
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 只有审核通过的未开始活动才开放报名。
        if (activity.getStatus() != ActivityStatusEnum.NOT_STARTED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前活动不可报名");
        }
        // 报名时间必须不晚于报名截止时间。
        LocalDateTime now = LocalDateTime.now();
        if (activity.getSignupDeadline() == null || now.isAfter(activity.getSignupDeadline())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "报名已截止");
        }

        // 不限人数（max_participants = 0）时跳过名额扣减，直接写入报名记录。
        Integer maxParticipants = activity.getMaxParticipants();
        boolean unlimited = maxParticipants != null && maxParticipants == UNLIMITED_PARTICIPANTS;
        if (!unlimited) {
            // 原子扣减名额，等价于以下 SQL，任一条件不满足时影响行数为 0：
            // UPDATE activity SET current_participants = current_participants + 1
            // WHERE id = ? AND current_participants < max_participants
            //   AND max_participants > 0 AND signup_deadline > NOW() AND status = 3
            LambdaUpdateWrapper<Activity> consumeWrapper = new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, activityId)
                    .eq(Activity::getStatus, ActivityStatusEnum.NOT_STARTED)
                    .gt(Activity::getSignupDeadline, now)
                    .gt(Activity::getMaxParticipants, UNLIMITED_PARTICIPANTS)
                    .lt(Activity::getCurrentParticipants, maxParticipants)
                    // 列名为硬编码的固定值，不存在 SQL 注入风险。
                    .setSql("current_participants = current_participants + 1");
            if (!activityService.update(consumeWrapper)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "名额已满或报名已截止");
            }
        }

        // 写入报名记录，初始状态为待签到。
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setFormData(formData);
        signup.setStatus(SignupStatusEnum.PENDING);
        try {
            if (!save(signup)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "报名失败");
            }
        } catch (DuplicateKeyException exception) {
            // 唯一索引 (activity_id, user_id) 兜底并发重复报名，事务回滚已扣减的名额。
            throw new BusinessException(ResultCode.PARAM_ERROR, "请勿重复报名");
        }
    }

    /**
     * 取消报名：校验报名状态及活动状态后，更新报名记录并原子释放名额。
     *
     * <p>只有待签到状态的报名可以取消，活动已开始（状态不再是未开始）后不允许取消；
     * 释放名额时要求当前人数大于 0，保证人数不会减为负数。</p>
     *
     * @param activityId 待取消报名的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 报名记录不存在、报名状态不可取消或活动已开始
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long activityId, Long userId) {
        // 查询当前用户对指定活动的未删除报名记录。
        LambdaQueryWrapper<ActivitySignup> queryWrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId);
        ActivitySignup signup = getOne(queryWrapper);
        if (signup == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 只有待签到状态可以取消。
        if (signup.getStatus() != SignupStatusEnum.PENDING) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可取消");
        }
        // 活动已开始或已取消时不允许取消报名。
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (activity.getStatus() != ActivityStatusEnum.NOT_STARTED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动已开始，不可取消");
        }

        // 条件更新报名状态，避免并发重复取消导致名额被重复释放。
        LambdaUpdateWrapper<ActivitySignup> cancelWrapper = new LambdaUpdateWrapper<ActivitySignup>()
                .eq(ActivitySignup::getId, signup.getId())
                .eq(ActivitySignup::getUserId, userId)
                .eq(ActivitySignup::getStatus, SignupStatusEnum.PENDING)
                .set(ActivitySignup::getStatus, SignupStatusEnum.CANCELLED);
        if (!update(cancelWrapper)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可取消");
        }

        // 原子释放名额，当前人数大于 0 时才递减，保证人数不为负。
        LambdaUpdateWrapper<Activity> releaseWrapper = new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .gt(Activity::getCurrentParticipants, 0)
                // 列名为硬编码的固定值，不存在 SQL 注入风险。
                .setSql("current_participants = current_participants - 1");
        // 人数数据异常为 0 时影响行数为 0，此时报名记录已取消，忽略释放结果。
        activityService.update(releaseWrapper);
    }

    /**
     * 分页查询当前用户的报名记录，按报名时间倒序返回。
     *
     * @param userId 当前登录用户的主键
     * @param status 报名状态编码，为 null 时查询全部状态
     * @param page 页码，从 1 开始
     * @param size 每页条数，必须大于零
     * @return 当前用户的报名记录分页结果
     * @throws BusinessException 页码、每页条数非法或状态编码未定义
     */
    @Override
    public IPage<ActivitySignup> getMySignup(Long userId, Integer status, Integer page, Integer size) {
        // 分页参数非法时提前终止，避免向分页插件传入无效页码。
        if (page == null || page < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于 0");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页条数必须大于 0");
        }
        // 状态编码仅在枚举中已定义时参与筛选，非法值直接拒绝。
        SignupStatusEnum statusEnum = null;
        if (status != null) {
            statusEnum = SignupStatusEnum.fromCode(status);
            if (statusEnum == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "报名状态不合法");
            }
        }

        LambdaQueryWrapper<ActivitySignup> queryWrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getUserId, userId)
                .eq(statusEnum != null, ActivitySignup::getStatus, statusEnum)
                // 报名时间相同时间点时按主键倒序，保持分页顺序稳定。
                .orderByDesc(ActivitySignup::getSignupTime)
                .orderByDesc(ActivitySignup::getId);
        return page(new Page<>(page, size), queryWrapper);
    }

    /**
     * 查询当前用户对指定活动的报名状态。
     *
     * <p>唯一索引 (activity_id, user_id) 保证同一用户对同一活动至多一条未删除记录。</p>
     *
     * @param activityId 活动主键
     * @param userId 当前登录用户的主键
     * @return 报名状态枚举，未报名时返回 null
     */
    @Override
    public SignupStatusEnum getUserSignupStatus(Long activityId, Long userId) {
        LambdaQueryWrapper<ActivitySignup> queryWrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId);
        ActivitySignup signup = getOne(queryWrapper);
        return signup == null ? null : signup.getStatus();
    }
}
