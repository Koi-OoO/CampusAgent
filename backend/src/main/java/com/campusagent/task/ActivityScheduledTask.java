package com.campusagent.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.enums.ActivityStatusEnum;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.mapper.ActivityMapper;
import com.campusagent.mapper.ActivitySignupMapper;
import com.campusagent.service.ActivityService;
import com.campusagent.service.ActivitySignupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动生命周期定时任务，负责自动推进活动状态并在活动结束后批量标记缺席报名记录。
 *
 * <p>定时任务只处理可批量更新的状态变更，不逐条加载活动后循环更新；
 * 每个任务方法内部捕获异常并写入日志，避免单次执行失败影响后续调度。</p>
 */
@Slf4j
@Component
public class ActivityScheduledTask {

    /**
     * 缺席判定延迟小时数，活动结束超过该时长后仍未签到的报名记录会被标记为已缺席。
     */
    private static final long ABSENT_DELAY_HOURS = 24L;

    /**
     * 活动服务，通过构造器注入，保留业务层依赖入口。
     */
    private final ActivityService activityService;

    /**
     * 活动报名服务，通过构造器注入，保留业务层依赖入口。
     */
    private final ActivitySignupService activitySignupService;

    /**
     * 活动 Mapper，用于批量更新活动状态并获取影响行数。
     */
    private final ActivityMapper activityMapper;

    /**
     * 活动报名 Mapper，用于批量更新报名状态并获取影响行数。
     */
    private final ActivitySignupMapper activitySignupMapper;

    /**
     * 创建活动生命周期定时任务。
     *
     * @param activityService 活动服务
     * @param activitySignupService 活动报名服务
     * @param activityMapper 活动数据访问接口
     * @param activitySignupMapper 活动报名数据访问接口
     */
    public ActivityScheduledTask(ActivityService activityService, ActivitySignupService activitySignupService,
                                 ActivityMapper activityMapper, ActivitySignupMapper activitySignupMapper) {
        this.activityService = activityService;
        this.activitySignupService = activitySignupService;
        this.activityMapper = activityMapper;
        this.activitySignupMapper = activitySignupMapper;
    }

    /**
     * 每 10 分钟自动流转活动状态。
     *
     * <p>未开始且开始时间已到的活动批量改为进行中；进行中且结束时间已到的活动批量改为已结束。</p>
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void updateActivityStatus() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int startedCount = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getStatus, ActivityStatusEnum.NOT_STARTED)
                    .le(Activity::getStartTime, now)
                    .set(Activity::getStatus, ActivityStatusEnum.IN_PROGRESS));
            int endedCount = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getStatus, ActivityStatusEnum.IN_PROGRESS)
                    .le(Activity::getEndTime, now)
                    .set(Activity::getStatus, ActivityStatusEnum.ENDED));
            log.info("活动状态自动流转完成，未开始转进行中 {} 条，进行中转已结束 {} 条", startedCount, endedCount);
        } catch (Exception exception) {
            log.error("活动状态自动流转任务执行失败", exception);
        }
    }

    /**
     * 每天凌晨 2 点批量标记缺席报名记录。
     *
     * <p>查询已结束且结束时间超过 24 小时的活动，将这些活动下仍处于待签到状态的报名记录批量更新为已缺席。</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateAbsentStatus() {
        try {
            LocalDateTime absentDeadline = LocalDateTime.now().minusHours(ABSENT_DELAY_HOURS);
            List<Long> activityIds = activityService.list(new LambdaQueryWrapper<Activity>()
                            .select(Activity::getId)
                            .eq(Activity::getStatus, ActivityStatusEnum.ENDED)
                            .le(Activity::getEndTime, absentDeadline))
                    .stream()
                    .map(Activity::getId)
                    .toList();
            if (activityIds.isEmpty()) {
                log.info("缺席状态自动更新完成，更新报名记录 0 条");
                return;
            }

            int absentCount = activitySignupMapper.update(null, new LambdaUpdateWrapper<ActivitySignup>()
                    .in(ActivitySignup::getActivityId, activityIds)
                    .eq(ActivitySignup::getStatus, SignupStatusEnum.PENDING)
                    .set(ActivitySignup::getStatus, SignupStatusEnum.ABSENT));
            log.info("缺席状态自动更新完成，更新报名记录 {} 条", absentCount);
        } catch (Exception exception) {
            log.error("缺席状态自动更新任务执行失败", exception);
        }
    }
}
