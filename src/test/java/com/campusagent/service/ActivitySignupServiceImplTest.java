package com.campusagent.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.entity.Activity;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.entity.User;
import com.campusagent.enums.ActivityStatusEnum;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.mapper.ActivitySignupMapper;
import com.campusagent.service.impl.ActivitySignupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动报名服务单元测试，验证签到状态流转、乐观更新、名单权限校验及分页查询条件。
 *
 * <p>测试使用 Mock Mapper 和 Mock Service 隔离数据库，只关注业务方法构造的规则和调用。</p>
 */
class ActivitySignupServiceImplTest {

    /** 活动报名记录 Mapper Mock。 */
    private ActivitySignupMapper activitySignupMapper;

    /** 活动服务 Mock。 */
    private ActivityService activityService;

    /** 用户服务 Mock。 */
    private UserService userService;

    /** 被测活动报名服务。 */
    private ActivitySignupServiceImpl activitySignupService;

    /**
     * 初始化 MyBatis-Plus 实体元数据，使单元测试可以解析 Lambda Wrapper 的字段引用。
     */
    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "activitySignupTestMapper"),
                ActivitySignup.class);
    }

    /**
     * 初始化被测服务及依赖，并向 MyBatis-Plus 父类注入 Mock Mapper。
     */
    @BeforeEach
    void setUp() {
        activitySignupMapper = mock(ActivitySignupMapper.class);
        activityService = mock(ActivityService.class);
        userService = mock(UserService.class);
        activitySignupService = new ActivitySignupServiceImpl(activitySignupMapper, activityService, userService);
        setBaseMapper(activitySignupService, activitySignupMapper);
    }

    /**
     * 用户没有报名记录时，签到应返回数据不存在。
     */
    @Test
    void checkinThrowsDataNotFoundWhenSignupMissing() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(null).when(spy).getOne(any());

        assertThatThrownBy(() -> spy.checkin(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
        verify(activityService, never()).getById(any(Long.class));
    }

    /**
     * 报名状态不是待签到时，签到应被拒绝。
     */
    @Test
    void checkinRejectsNonPendingSignup() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(signup(SignupStatusEnum.CHECKED_IN)).when(spy).getOne(any());

        assertThatThrownBy(() -> spy.checkin(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("当前状态不可签到");
                });
        verify(activityService, never()).getById(any(Long.class));
    }

    /**
     * 活动不存在时，签到应返回数据不存在。
     */
    @Test
    void checkinThrowsDataNotFoundWhenActivityMissing() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(signup(SignupStatusEnum.PENDING)).when(spy).getOne(any());
        when(activityService.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> spy.checkin(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    /**
     * 活动不是进行中状态时，签到应被拒绝。
     */
    @Test
    void checkinRejectsActivityThatIsNotInProgress() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(signup(SignupStatusEnum.PENDING)).when(spy).getOne(any());
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.NOT_STARTED));

        assertThatThrownBy(() -> spy.checkin(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("活动未开始或已结束");
                });
    }

    /**
     * 待签到记录在进行中活动内签到时，应写入已签到状态和当前签到时间。
     */
    @Test
    void checkinUpdatesPendingSignupWithCheckinTimeWhenActivityInProgress() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(signup(SignupStatusEnum.PENDING)).when(spy).getOne(any());
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        doReturn(true).when(spy).update(any(LambdaUpdateWrapper.class));

        LocalDateTime before = LocalDateTime.now();
        spy.checkin(1L, 7L);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LambdaUpdateWrapper<ActivitySignup>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(spy).update(captor.capture());
        LambdaUpdateWrapper<ActivitySignup> updateWrapper = captor.getValue();
        assertThat(updateWrapper.getSqlSegment()).contains("id", "status");
        assertThat(updateWrapper.getSqlSet()).contains("status", "checkin_time");
        Map<String, Object> params = updateWrapper.getParamNameValuePairs();
        assertThat(params).containsValue(SignupStatusEnum.CHECKED_IN);
        assertThat(params.values().stream()
                .filter(LocalDateTime.class::isInstance)
                .map(LocalDateTime.class::cast))
                .anySatisfy(checkinTime -> assertThat(checkinTime).isBetween(before, after));
    }

    /**
     * 乐观更新影响行数为零时，签到应提示当前状态不可签到。
     */
    @Test
    void checkinRejectsWhenOptimisticUpdateFails() {
        ActivitySignupServiceImpl spy = spy(activitySignupService);
        doReturn(signup(SignupStatusEnum.PENDING)).when(spy).getOne(any());
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        doReturn(false).when(spy).update(any(LambdaUpdateWrapper.class));

        assertThatThrownBy(() -> spy.checkin(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("当前状态不可签到");
                });
    }

    /**
     * 报名名单页码小于一时，应返回参数错误。
     */
    @Test
    void getSignupListRejectsInvalidPage() {
        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 报名名单每页条数小于一时，应返回参数错误。
     */
    @Test
    void getSignupListRejectsInvalidSize() {
        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 1, 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 报名名单每页条数超过上限时，应返回参数错误。
     */
    @Test
    void getSignupListRejectsOversizedPage() {
        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 1, 101))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询报名名单时活动不存在，应返回数据不存在。
     */
    @Test
    void getSignupListThrowsDataNotFoundWhenActivityMissing() {
        when(activityService.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    /**
     * 查询报名名单时操作人不存在，应返回无权限。
     */
    @Test
    void getSignupListThrowsForbiddenWhenOperatorMissing() {
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        when(userService.getById(7L)).thenReturn(null);

        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    /**
     * 普通用户不是活动发布者时，不能查看报名名单。
     */
    @Test
    void getSignupListRejectsNormalUserWhoIsNotPublisher() {
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        when(userService.getById(8L)).thenReturn(user(8L, UserRoleEnum.USER));

        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 8L, null, 1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    /**
     * 被禁用的发布者即使持有有效身份标识，也不能查看报名名单。
     */
    @Test
    void getSignupListRejectsDisabledPublisher() {
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        User disabledPublisher = user(7L, UserRoleEnum.USER);
        disabledPublisher.setStatus(0);
        when(userService.getById(7L)).thenReturn(disabledPublisher);

        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, null, 1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    /**
     * 发布者本人可以查看活动报名名单。
     */
    @Test
    void getSignupListAllowsPublisher() {
        stubSignupListDependencies(7L, UserRoleEnum.USER);

        IPage<ActivitySignup> result = activitySignupService.getSignupList(1L, 7L, null, 1, 10);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }

    /**
     * 管理员可以查看非本人发布活动的报名名单。
     */
    @Test
    void getSignupListAllowsAdmin() {
        stubSignupListDependencies(8L, UserRoleEnum.ADMIN);

        IPage<ActivitySignup> result = activitySignupService.getSignupList(1L, 8L, null, 1, 10);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }

    /**
     * 超级管理员可以查看非本人发布活动的报名名单。
     */
    @Test
    void getSignupListAllowsSuperAdmin() {
        stubSignupListDependencies(9L, UserRoleEnum.SUPER_ADMIN);

        IPage<ActivitySignup> result = activitySignupService.getSignupList(1L, 9L, null, 1, 10);

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }

    /**
     * 非法报名状态编码不能用于报名名单筛选。
     */
    @Test
    void getSignupListRejectsUnknownSignupStatus() {
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        when(userService.getById(7L)).thenReturn(user(7L, UserRoleEnum.USER));

        assertThatThrownBy(() -> activitySignupService.getSignupList(1L, 7L, 99, 1, 10))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("报名状态不合法");
                });
    }

    /**
     * 报名名单查询应按活动、状态、报名时间升序和主键升序构造分页查询。
     */
    @Test
    void getSignupListFiltersByStatusAndOrdersBySignupTimeThenId() {
        stubSignupListDependencies(7L, UserRoleEnum.USER);

        activitySignupService.getSignupList(1L, 7L, SignupStatusEnum.CHECKED_IN.getCode(), 2, 20);

        ArgumentCaptor<Page<ActivitySignup>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper<ActivitySignup>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(activitySignupMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("activity_id", "status");
        assertThat(sqlSegment).contains("ORDER BY signup_time ASC,id ASC");
    }

    /**
     * 为允许访问的名单查询准备活动、操作人和分页 Mapper 桩。
     *
     * @param operatorId 操作人主键
     * @param role 操作人角色
     */
    private void stubSignupListDependencies(Long operatorId, UserRoleEnum role) {
        when(activityService.getById(1L)).thenReturn(activity(1L, 7L, ActivityStatusEnum.IN_PROGRESS));
        when(userService.getById(operatorId)).thenReturn(user(operatorId, role));
        when(activitySignupMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * 构造指定状态的报名记录。
     *
     * @param status 报名状态
     * @return 报名记录
     */
    private static ActivitySignup signup(SignupStatusEnum status) {
        ActivitySignup signup = new ActivitySignup();
        signup.setId(100L);
        signup.setActivityId(1L);
        signup.setUserId(7L);
        signup.setStatus(status);
        return signup;
    }

    /**
     * 构造活动实体。
     *
     * @param activityId 活动主键
     * @param publisherId 发布者主键
     * @param status 活动状态
     * @return 活动实体
     */
    private static Activity activity(Long activityId, Long publisherId, ActivityStatusEnum status) {
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setPublisherId(publisherId);
        activity.setStatus(status);
        return activity;
    }

    /**
     * 构造用户实体。
     *
     * @param userId 用户主键
     * @param role 用户角色
     * @return 用户实体
     */
    private static User user(Long userId, UserRoleEnum role) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    /**
     * 通过反射把 Mock Mapper 注入 MyBatis-Plus ServiceImpl 的 baseMapper 字段。
     *
     * @param service 被测服务
     * @param mapper Mock 数据访问接口
     */
    private static void setBaseMapper(ActivitySignupServiceImpl service, ActivitySignupMapper mapper) {
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
            throw new IllegalStateException("无法注入活动报名 Mapper", exception);
        }
    }
}
