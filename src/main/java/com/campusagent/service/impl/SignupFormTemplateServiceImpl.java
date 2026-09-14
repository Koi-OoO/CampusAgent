package com.campusagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.TemplateCreateRequest;
import com.campusagent.dto.request.TemplateUpdateRequest;
import com.campusagent.entity.SignupFormTemplate;
import com.campusagent.mapper.SignupFormTemplateMapper;
import com.campusagent.service.SignupFormTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报名表单模板服务实现类，负责模板的业务校验和持久化操作。
 */
@Service
public class SignupFormTemplateServiceImpl extends ServiceImpl<SignupFormTemplateMapper, SignupFormTemplate>
        implements SignupFormTemplateService {

    /** 模板名称重复时的统一提示。 */
    private static final String TEMPLATE_NAME_EXISTS_MESSAGE = "模板名称已存在";

    /**
     * 创建报名表单模板服务，通过构造器注入父类所需的数据访问接口。
     *
     * @param signupFormTemplateMapper 报名表单模板数据访问接口
     */
    public SignupFormTemplateServiceImpl(SignupFormTemplateMapper signupFormTemplateMapper) {
        this.baseMapper = signupFormTemplateMapper;
    }

    @Override
    public List<SignupFormTemplate> listAll() {
        LambdaQueryWrapper<SignupFormTemplate> queryWrapper = new LambdaQueryWrapper<SignupFormTemplate>()
                .orderByDesc(SignupFormTemplate::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public void createTemplate(TemplateCreateRequest request) {
        // 名称唯一性校验
        checkNameUnique(request.getName(), null);

        SignupFormTemplate template = new SignupFormTemplate();
        template.setName(request.getName());
        template.setConfig(request.getConfig());

        if (!save(template)) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
    }

    @Override
    public void updateTemplate(Integer id, TemplateUpdateRequest request) {
        // 模板存在性校验
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 名称唯一性校验（排除自己）
        checkNameUnique(request.getName(), id);

        SignupFormTemplate template = new SignupFormTemplate();
        template.setId(id);
        template.setName(request.getName());
        template.setConfig(request.getConfig());

        if (!updateById(template)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    @Override
    public void deleteTemplate(Integer id) {
        // 模板存在性校验
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 实体配置了逻辑删除，removeById 实际执行的是逻辑删除。
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 校验名称是否与其他未删除模板重复。
     *
     * @param name 待校验的模板名称
     * @param excludedId 需要排除的模板主键，创建时传入 null
     */
    private void checkNameUnique(String name, Integer excludedId) {
        LambdaQueryWrapper<SignupFormTemplate> queryWrapper = new LambdaQueryWrapper<SignupFormTemplate>()
                .eq(SignupFormTemplate::getName, name)
                .ne(excludedId != null, SignupFormTemplate::getId, excludedId);
        if (count(queryWrapper) > 0L) {
            throw new BusinessException(ResultCode.PARAM_ERROR, TEMPLATE_NAME_EXISTS_MESSAGE);
        }
    }
}
