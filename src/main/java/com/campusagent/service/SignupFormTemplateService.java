package com.campusagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campusagent.dto.request.TemplateCreateRequest;
import com.campusagent.dto.request.TemplateUpdateRequest;
import com.campusagent.entity.SignupFormTemplate;

import java.util.List;

/**
 * 报名表单模板服务接口，在基础增删改查能力上提供模板列表查询和模板管理。
 */
public interface SignupFormTemplateService extends IService<SignupFormTemplate> {

    /**
     * 按创建时间倒序查询所有未被逻辑删除的报名表单模板。
     *
     * @return 报名表单模板列表，没有模板时返回空列表
     */
    List<SignupFormTemplate> listAll();

    /**
     * 创建报名表单模板，名称重复时拒绝保存。
     *
     * @param request 模板创建请求体
     */
    void createTemplate(TemplateCreateRequest request);

    /**
     * 修改报名表单模板，校验模板存在性和名称唯一性。
     *
     * @param id 模板主键
     * @param request 模板修改请求体
     */
    void updateTemplate(Integer id, TemplateUpdateRequest request);

    /**
     * 逻辑删除报名表单模板。
     *
     * @param id 模板主键
     */
    void deleteTemplate(Integer id);
}
