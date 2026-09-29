package com.campusagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.entity.ActivitySignup;
import com.campusagent.enums.SignupStatusEnum;

/**
 * 活动报名服务接口，在基础增删改查能力上声明报名、取消报名、我的报名列表及报名状态查询等业务方法。
 *
 * <p>报名名额通过数据库原子更新扣减，防止并发超卖；
 * 重复报名由 activity_signup 表的唯一索引兜底拦截。</p>
 */
public interface ActivitySignupService extends IService<ActivitySignup> {

    /**
     * 为当前用户报名活动，报名成功后占用一个名额并生成待签到记录。
     *
     * @param activityId 待报名的活动主键
     * @param userId 当前登录用户的主键
     * @param formData 报名表单数据，JSON 字符串，可为空
     * @throws BusinessException 活动不存在、活动不可报名、报名已截止、名额已满或重复报名
     */
    void signup(Long activityId, Long userId, String formData);

    /**
     * 取消当前用户对指定活动的报名，并释放已占用的名额。
     *
     * @param activityId 待取消报名的活动主键
     * @param userId 当前登录用户的主键
     * @throws BusinessException 报名记录不存在、报名状态不可取消或活动已开始
     */
    void cancelSignup(Long activityId, Long userId);

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
    IPage<ActivitySignup> getMySignup(Long userId, Integer status, Integer page, Integer size);

    /**
     * 查询当前用户对指定活动的报名状态。
     *
     * @param activityId 活动主键
     * @param userId 当前登录用户的主键
     * @return 报名状态枚举，未报名时返回 null
     */
    SignupStatusEnum getUserSignupStatus(Long activityId, Long userId);
}
