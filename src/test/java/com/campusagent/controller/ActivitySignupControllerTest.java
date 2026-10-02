package com.campusagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.response.ActivityStatisticsResponse;
import com.campusagent.dto.response.SignupListResponse;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.entity.User;
import com.campusagent.enums.SignupStatusEnum;
import com.campusagent.service.ActivityService;
import com.campusagent.service.ActivitySignupService;
import com.campusagent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 活动报名控制器单元测试，验证签到接口委托以及报名名单响应 DTO 的批量用户资料填充。
 */
class ActivitySignupControllerTest {

    /** 活动报名服务 Mock。 */
    private ActivitySignupService activitySignupService;

    /** 活动服务 Mock。 */
    private ActivityService activityService;

    /** 用户服务 Mock。 */
    private UserService userService;

    /** 被测控制器。 */
    private ActivitySignupController controller;

    /** 独立控制器测试客户端。 */
    private MockMvc mockMvc;

    /**
     * 初始化控制器及其 Mock 依赖。
     */
    @BeforeEach
    void setUp() {
        activitySignupService = mock(ActivitySignupService.class);
        activityService = mock(ActivityService.class);
        userService = mock(UserService.class);
        controller = new ActivitySignupController(activitySignupService, activityService, userService);
        mockMvc = standaloneSetup(controller).build();
    }

    /**
     * 签到接口应把路径活动主键和请求属性中的用户主键传递给服务层。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    void checkinEndpointDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/activity/signup/checkin/11")
                        .requestAttr(RequestAttributeConstants.USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"操作成功","data":null}
                        """, true));

        verify(activitySignupService).checkin(11L, 7L);
    }

    /**
     * 统计接口应从路径和请求属性读取参数，并将统计结果返回为统一响应。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    void statisticsEndpointDelegatesToService() throws Exception {
        ActivityStatisticsResponse statistics = new ActivityStatisticsResponse();
        statistics.setActivityId(11L);
        statistics.setActivityTitle("校园篮球赛");
        statistics.setTotalSignups(5L);
        statistics.setCheckedIn(2L);
        when(activitySignupService.getStatistics(11L, 7L)).thenReturn(statistics);

        mockMvc.perform(get("/api/activity/signup/statistics/11")
                        .requestAttr(RequestAttributeConstants.USER_ID, 7L))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"操作成功","data":{"activityId":11,"activityTitle":"校园篮球赛","totalSignups":5,"checkedIn":2}}
                        """, false));

        verify(activitySignupService).getStatistics(11L, 7L);
    }

    /**
     * 报名名单接口应批量查询用户资料，并将资料填充到响应 DTO。
     */
    @Test
    void signupListEndpointFillsUserInfoInBatch() {
        Page<ActivitySignup> signupPage = new Page<>(1, 10);
        signupPage.setRecords(List.of(
                signup(100L, 7L, SignupStatusEnum.PENDING, "{\"name\":\"张三\"}"),
                signup(101L, 8L, SignupStatusEnum.CHECKED_IN, "{\"name\":\"李四\"}")
        ));
        signupPage.setTotal(2);
        when(activitySignupService.getSignupList(1L, 99L, null, 1, 10)).thenReturn(signupPage);
        when(userService.listByIds(List.of(7L, 8L))).thenReturn(List.of(
                user(7L, "张三", "20230001", "计算机学院", "13800000001"),
                user(8L, "李四", "20230002", "软件学院", "13800000002")
        ));

        Result<IPage<SignupListResponse>> result = controller.getSignupList(1L, null, 1, 10, 99L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTotal()).isEqualTo(2);
        assertThat(result.getData().getRecords())
                .extracting(SignupListResponse::getSignupId)
                .containsExactly(100L, 101L);
        SignupListResponse first = result.getData().getRecords().get(0);
        assertThat(first.getUserId()).isEqualTo(7L);
        assertThat(first.getRealName()).isEqualTo("张三");
        assertThat(first.getStudentId()).isEqualTo("20230001");
        assertThat(first.getCollege()).isEqualTo("计算机学院");
        assertThat(first.getPhone()).isEqualTo("13800000001");
        assertThat(first.getFormData()).isEqualTo("{\"name\":\"张三\"}");
        assertThat(first.getSignupStatus()).isEqualTo(SignupStatusEnum.PENDING.getCode());
        assertThat(first.getSignupTime()).isEqualTo(LocalDateTime.of(2026, 9, 30, 9, 0));
        assertThat(first.getCheckinTime()).isNull();
    }

    /**
     * 报名用户已被逻辑删除或不存在时，名单仍保留报名记录，用户资料字段保持为空。
     */
    @Test
    void signupListKeepsRecordWhenUserInfoMissing() {
        Page<ActivitySignup> signupPage = new Page<>(1, 10);
        signupPage.setRecords(List.of(signup(100L, 7L, SignupStatusEnum.PENDING, "{}")));
        signupPage.setTotal(1);
        when(activitySignupService.getSignupList(1L, 99L, null, 1, 10)).thenReturn(signupPage);
        when(userService.listByIds(List.of(7L))).thenReturn(List.of());

        Result<IPage<SignupListResponse>> result = controller.getSignupList(1L, null, 1, 10, 99L);

        SignupListResponse response = result.getData().getRecords().get(0);
        assertThat(response.getSignupId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRealName()).isNull();
        assertThat(response.getStudentId()).isNull();
        assertThat(response.getCollege()).isNull();
        assertThat(response.getPhone()).isNull();
    }

    /**
     * 报名名单接口应去重用户主键后批量查询用户，避免 N+1 查询。
     */
    @Test
    void signupListLoadsDistinctUsersOnce() {
        Page<ActivitySignup> signupPage = new Page<>(1, 10);
        signupPage.setRecords(List.of(
                signup(100L, 7L, SignupStatusEnum.PENDING, "{}"),
                signup(101L, 7L, SignupStatusEnum.CHECKED_IN, "{}")
        ));
        when(activitySignupService.getSignupList(1L, 99L, null, 1, 10)).thenReturn(signupPage);
        when(userService.listByIds(List.of(7L))).thenReturn(List.of(user(7L, "张三", "20230001", "计算机学院", "1")));

        controller.getSignupList(1L, null, 1, 10, 99L);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).listByIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(7L);
    }

    /**
     * 构造报名记录。
     *
     * @param signupId 报名记录主键
     * @param userId 用户主键
     * @param status 报名状态
     * @param formData 原始报名表单 JSON 字符串
     * @return 报名记录实体
     */
    private static ActivitySignup signup(Long signupId, Long userId, SignupStatusEnum status, String formData) {
        ActivitySignup signup = new ActivitySignup();
        signup.setId(signupId);
        signup.setActivityId(1L);
        signup.setUserId(userId);
        signup.setStatus(status);
        signup.setFormData(formData);
        signup.setSignupTime(LocalDateTime.of(2026, 9, 30, 9, 0));
        signup.setCheckinTime(status == SignupStatusEnum.CHECKED_IN
                ? LocalDateTime.of(2026, 9, 30, 10, 0) : null);
        return signup;
    }

    /**
     * 构造用户实体。
     *
     * @param userId 用户主键
     * @param realName 真实姓名
     * @param studentId 学号
     * @param college 学院
     * @param phone 联系电话
     * @return 用户实体
     */
    private static User user(Long userId, String realName, String studentId, String college, String phone) {
        User user = new User();
        user.setId(userId);
        user.setRealName(realName);
        user.setStudentId(studentId);
        user.setCollege(college);
        user.setPhone(phone);
        return user;
    }
}
