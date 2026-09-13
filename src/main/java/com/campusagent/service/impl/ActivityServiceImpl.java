package com.campusagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.entity.Activity;
import com.campusagent.mapper.ActivityMapper;
import com.campusagent.service.ActivityService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 活动服务骨架，通过构造器注入活动 Mapper，并继承 MyBatis-Plus 基础服务能力。
 *
 * <p>本类声明的业务方法仅保留签名，统一抛出未实现异常，具体逻辑在 Phase 2-3 补充。</p>
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    /**
     * 创建活动服务，通过构造器注入父类所需的数据访问接口。
     *
     * @param activityMapper 活动数据访问接口
     */
    public ActivityServiceImpl(ActivityMapper activityMapper) {
        this.baseMapper = activityMapper;
    }

    /**
     * 创建活动草稿的占位实现。
     *
     * @param request 活动创建请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键，用于确定活动发布人
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void createDraft(Activity request, Long userId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 提交活动审核的占位实现。
     *
     * @param activityId 待提交审核的活动主键
     * @param userId 当前登录用户的主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void submitForAudit(Long activityId, Long userId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 修改活动资料的占位实现。
     *
     * @param activityId 待修改的活动主键，以此参数确定目标活动
     * @param request 活动修改请求，仅使用可编辑的活动资料
     * @param userId 当前登录用户的主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void updateActivity(Long activityId, Activity request, Long userId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 逻辑删除活动的占位实现。
     *
     * @param activityId 待删除的活动主键
     * @param userId 当前登录用户的主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void deleteActivity(Long activityId, Long userId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 查询活动详情的占位实现。
     *
     * @param activityId 待查询的活动主键
     * @return 活动详情，当前阶段统一抛出未实现异常
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public Activity getActivityDetail(Long activityId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 查询用户发布活动列表的占位实现。
     *
     * @param userId 当前登录用户的主键
     * @return 当前用户发布的活动列表，当前阶段统一抛出未实现异常
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public List<Activity> getMyPublished(Long userId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 查询待审核活动列表的占位实现。
     *
     * @return 等待审核的活动列表，当前阶段统一抛出未实现异常
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public List<Activity> getAuditList() {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 审核活动的占位实现。
     *
     * @param activityId 待审核的活动主键
     * @param result 审核结果编码，表示审核通过或驳回
     * @param reason 审核说明，驳回时填写驳回理由
     * @param operatorId 当前执行审核的操作人主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void auditActivity(Long activityId, Integer result, String reason, Long operatorId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 取消活动的占位实现。
     *
     * @param activityId 待取消的活动主键
     * @param reason 活动取消理由
     * @param operatorId 当前执行取消操作的用户主键
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public void cancelActivity(Long activityId, String reason, Long operatorId) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }

    /**
     * 查询公开活动列表的占位实现。
     *
     * @param categoryId 活动分类主键，为 null 时不限定分类
     * @param keyword 活动标题关键词，为 null 或空白时不限定关键词
     * @return 符合筛选条件的公开活动列表，当前阶段统一抛出未实现异常
     * @throws UnsupportedOperationException 当前阶段尚未实现该方法
     */
    @Override
    public List<Activity> getPublicList(Integer categoryId, String keyword) {
        throw new UnsupportedOperationException("待 Phase 2-3 实现");
    }
}
