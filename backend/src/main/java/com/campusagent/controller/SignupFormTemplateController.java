package com.campusagent.controller;

import com.campusagent.annotation.RequireRole;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.Result;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.TemplateCreateRequest;
import com.campusagent.dto.request.TemplateUpdateRequest;
import com.campusagent.entity.SignupFormTemplate;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.SignupFormTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报名表单模板控制器，提供模板列表查询、详情查询、创建、修改和逻辑删除接口。
 *
 * <p>业务逻辑统一由 SignupFormTemplateService 处理，控制器只负责参数接收和结果封装。</p>
 */
@RestController
@RequestMapping("/api/template")
public class SignupFormTemplateController {

    /**
     * 报名表单模板服务，通过构造器注入。
     */
    private final SignupFormTemplateService signupFormTemplateService;

    /**
     * 创建报名表单模板控制器。
     *
     * @param signupFormTemplateService 报名表单模板服务
     */
    public SignupFormTemplateController(SignupFormTemplateService signupFormTemplateService) {
        this.signupFormTemplateService = signupFormTemplateService;
    }

    /**
     * 查询所有未删除的报名表单模板，按创建时间倒序返回。
     *
     * @return 报名表单模板列表
     */
    @GetMapping("/list")
    public Result<List<SignupFormTemplate>> list() {
        return Result.success(signupFormTemplateService.listAll());
    }

    /**
     * 查询指定报名表单模板详情。
     *
     * @param id 模板主键
     * @return 匹配的报名表单模板
     */
    @GetMapping("/{id}")
    public Result<SignupFormTemplate> getById(@PathVariable("id") Integer id) {
        SignupFormTemplate template = signupFormTemplateService.getById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return Result.success(template);
    }

    /**
     * 创建报名表单模板。
     *
     * @param request 模板创建请求，字段合法性由框架统一校验
     * @return 创建成功响应
     */
    @PostMapping
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> create(@Valid @RequestBody TemplateCreateRequest request) {
        signupFormTemplateService.createTemplate(request);
        return Result.success();
    }

    /**
     * 修改指定报名表单模板。
     *
     * @param id 模板主键
     * @param request 模板修改请求，字段合法性由框架统一校验
     * @return 修改成功响应
     */
    @PutMapping("/{id}")
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> update(@PathVariable("id") Integer id,
                               @Valid @RequestBody TemplateUpdateRequest request) {
        signupFormTemplateService.updateTemplate(id, request);
        return Result.success();
    }

    /**
     * 逻辑删除指定报名表单模板。
     *
     * @param id 模板主键
     * @return 删除成功响应
     */
    @DeleteMapping("/{id}")
    @RequireRole(UserRoleEnum.ADMIN)
    public Result<Void> delete(@PathVariable("id") Integer id) {
        signupFormTemplateService.deleteTemplate(id);
        return Result.success();
    }
}
