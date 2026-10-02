package com.campusagent.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.campusagent.dto.response.ActivityDetailResponse;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivityCategory;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.entity.User;
import com.campusagent.enums.ActivityStatusEnum;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.mapper.ActivityAuditLogMapper;
import com.campusagent.mapper.ActivityMapper;
import com.campusagent.mapper.ActivitySignupMapper;
import com.campusagent.service.impl.ActivityServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动服务单元测试，验证活动详情响应中的当前用户报名状态填充规则。
 */
class ActivityServiceImplTest {

    /** 活动 Mapper Mock。 */
    private ActivityMapper activityMapper;

    /** 活动分类服务 Mock。 */
    private ActivityCategoryService activityCategoryService;

    /** 用户服务 Mock。 */
    private UserService userService;

    /** 活动报名 Mapper Mock。 */
    private ActivitySignupMapper activitySignupMapper;

    /** 被测活动服务。 */
    private ActivityServiceImpl activityService;

    /**
     * 初始化 MyBatis-Plus 实体元数据，使单元测试可以解析 Lambda Wrapper 的字段引用。
     */
    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "activityTestMapper"),
                Activity.class);
    }

    /**
     * 初始化被测服务及依赖，并向 MyBatis-Plus 父类注入 Mock Mapper。
     */
    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        ActivityAuditLogMapper activityAuditLogMapper = mock(ActivityAuditLogMapper.class);
        activityCategoryService = mock(ActivityCategoryService.class);
        userService = mock(UserService.class);
        activitySignupMapper = mock(ActivitySignupMapper.class);
        activityService = new ActivityServiceImpl(
                activityMapper,
                activityAuditLogMapper,
                activityCategoryService,
                userService,
                activitySignupMapper);
        setBaseMapper(activityService, activityMapper);
    }

    /**
     * 未登录访问公开活动详情时，报名状态为空且不查询报名服务。
     */
    @Test
    void getActivityDetailLeavesSignupStatusNullWhenUserAnonymous() {
        stubPublicActivity();

        ActivityDetailResponse response = activityService.getActivityDetail(1L, null);

        assertThat(response.getCurrentUserSignupStatus()).isNull();
        verify(activitySignupMapper, never()).selectOne(any());
    }

    /**
     * 登录用户未报名时，活动详情报名状态为空，前端可展示立即报名。
     */
    @Test
    void getActivityDetailLeavesSignupStatusNullWhenUserNotSignedUp() {
        stubPublicActivity();
        when(activitySignupMapper.selectOne(any())).thenReturn(null);

        ActivityDetailResponse response = activityService.getActivityDetail(1L, 9L);

        assertThat(response.getCurrentUserSignupStatus()).isNull();
    }

    /**
     * 登录用户已报名待签到时，活动详情返回待签到状态编码。
     */
    @Test
    void getActivityDetailReturnsPendingSignupStatusForSignedUpUser() {
        stubPublicActivity();
        when(activitySignupMapper.selectOne(any())).thenReturn(signup(SignupStatusEnum.PENDING));

        ActivityDetailResponse response = activityService.getActivityDetail(1L, 9L);

        assertThat(response.getCurrentUserSignupStatus()).isEqualTo(SignupStatusEnum.PENDING.getCode());
    }

    /**
     * 登录用户已签到时，活动详情返回已签到状态编码。
     */
    @Test
    void getActivityDetailReturnsCheckedInSignupStatusForCheckedInUser() {
        stubPublicActivity();
        when(activitySignupMapper.selectOne(any())).thenReturn(signup(SignupStatusEnum.CHECKED_IN));

        ActivityDetailResponse response = activityService.getActivityDetail(1L, 9L);

        assertThat(response.getCurrentUserSignupStatus()).isEqualTo(SignupStatusEnum.CHECKED_IN.getCode());
    }

    /**
     * 为公开活动详情查询准备活动、分类、发布人和浏览量自增 Mapper 桩。
     */
    private void stubPublicActivity() {
        when(activityMapper.selectById(1L)).thenReturn(publicActivity());
        when(activityMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        ActivityCategory category = new ActivityCategory();
        category.setId(3);
        category.setName("校园活动");
        when(activityCategoryService.getById(3)).thenReturn(category);
        User publisher = new User();
        publisher.setId(7L);
        publisher.setUsername("publisher");
        publisher.setRealName("发布人");
        when(userService.getById(7L)).thenReturn(publisher);
    }

    /**
     * 构造公开可见的活动实体。
     *
     * @return 公开活动
     */
    private static Activity publicActivity() {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setTitle("活动详情状态测试");
        activity.setDescription("用于验证当前用户报名状态");
        activity.setLocation("操场");
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        activity.setSignupDeadline(LocalDateTime.now().plusHours(12));
        activity.setStatus(ActivityStatusEnum.NOT_STARTED);
        activity.setCategoryId(3);
        activity.setPublisherId(7L);
        activity.setMaxParticipants(20);
        activity.setCurrentParticipants(5);
        activity.setViewCount(10);
        activity.setCreateTime(LocalDateTime.now().minusDays(1));
        return activity;
    }

    /**
     * 构造指定状态的报名记录。
     *
     * @param status 报名状态
     * @return 报名记录
     */
    private static ActivitySignup signup(SignupStatusEnum status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(1L);
        signup.setUserId(9L);
        signup.setStatus(status);
        return signup;
    }

    /**
     * 通过反射把 Mock Mapper 注入 MyBatis-Plus ServiceImpl 的 baseMapper 字段。
     *
     * @param service 被测服务
     * @param mapper Mock 数据访问接口
     */
    private static void setBaseMapper(ActivityServiceImpl service, ActivityMapper mapper) {
        Field baseMapperField = null;
        for (Class<?> type = service.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("baseMapper");
                if (BaseMapper.class.isAssignableFrom(field.getType())) {
                    baseMapperField = field;
                    break;
                }
            } catch (NoSuchFieldException ignored) {
                // 该层级未声明 baseMapper，继续向上查找。
            }
        }
        try {
            baseMapperField.setAccessible(true);
            baseMapperField.set(service, mapper);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法注入活动 Mapper", exception);
        }
    }
}
