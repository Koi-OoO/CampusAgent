package com.campusagent.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.TemplateCreateRequest;
import com.campusagent.dto.request.TemplateUpdateRequest;
import com.campusagent.entity.SignupFormTemplate;
import com.campusagent.mapper.SignupFormTemplateMapper;
import com.campusagent.service.impl.SignupFormTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;

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
 * 报名表单模板服务单元测试，通过 Mock Mapper 验证名称唯一性、存在性和列表排序规则。
 *
 * <p>测试通过反射把 Mock Mapper 注入 ServiceImpl 的 baseMapper 字段，
 * 复用 MyBatis-Plus 真实的查询条件构建逻辑，不依赖数据库。</p>
 */
class SignupFormTemplateServiceImplTest {

    /** Mock 的模板数据访问接口。 */
    private SignupFormTemplateMapper signupFormTemplateMapper;

    /** 被测服务，构造器注入 Mock Mapper 后由测试手动装配 baseMapper。 */
    private SignupFormTemplateServiceImpl signupFormTemplateService;

    @BeforeEach
    void setUp() {
        signupFormTemplateMapper = mock(SignupFormTemplateMapper.class);
        signupFormTemplateService = new SignupFormTemplateServiceImpl(signupFormTemplateMapper);
        // ServiceImpl 的增删改查均通过受保护的 baseMapper 字段执行，此处用反射注入 Mock。
        setBaseMapper(signupFormTemplateService, signupFormTemplateMapper);
    }

    @Test
    void rejectsDuplicateNameWhenCreatingTemplate() {
        when(signupFormTemplateMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> signupFormTemplateService.createTemplate(validCreateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("模板名称已存在");
                });
        // 名称重复时不应执行插入。
        verify(signupFormTemplateMapper, never()).insert(any());
    }

    @Test
    void savesTemplateWithRequestFieldsWhenNameIsUnique() {
        when(signupFormTemplateMapper.selectCount(any())).thenReturn(0L);
        when(signupFormTemplateMapper.insert(any())).thenReturn(1);

        signupFormTemplateService.createTemplate(validCreateRequest());

        ArgumentCaptor<SignupFormTemplate> captor = ArgumentCaptor.forClass(SignupFormTemplate.class);
        verify(signupFormTemplateMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("报名信息收集");
        assertThat(captor.getValue().getConfig()).isEqualTo("{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}");
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void throwsDataNotFoundWhenUpdatingMissingTemplate() {
        when(signupFormTemplateMapper.selectById(9)).thenReturn(null);

        assertThatThrownBy(() -> signupFormTemplateService.updateTemplate(9, validUpdateRequest(9)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
        // 模板不存在时不应执行更新。
        verify(signupFormTemplateMapper, never()).updateById(any());
    }

    @Test
    void rejectsUpdatingToAnotherTemplateName() {
        when(signupFormTemplateMapper.selectById(1)).thenReturn(existingTemplate(1, "报名信息收集"));
        when(signupFormTemplateMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> signupFormTemplateService.updateTemplate(1, validUpdateRequest(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("模板名称已存在");
                });
        verify(signupFormTemplateMapper, never()).updateById(any());
    }

    @Test
    void updatesTemplateKeepingItsOwnName() {
        when(signupFormTemplateMapper.selectById(1)).thenReturn(existingTemplate(1, "报名信息收集"));
        when(signupFormTemplateMapper.selectCount(any())).thenReturn(0L);
        when(signupFormTemplateMapper.updateById(any())).thenReturn(1);

        signupFormTemplateService.updateTemplate(1, validUpdateRequest(1));

        ArgumentCaptor<SignupFormTemplate> captor = ArgumentCaptor.forClass(SignupFormTemplate.class);
        verify(signupFormTemplateMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1);
        assertThat(captor.getValue().getName()).isEqualTo("报名信息收集");
        assertThat(captor.getValue().getConfig()).isEqualTo("{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}");
    }

    @Test
    void throwsDataNotFoundWhenDeletingMissingTemplate() {
        when(signupFormTemplateMapper.selectById(9)).thenReturn(null);

        assertThatThrownBy(() -> signupFormTemplateService.deleteTemplate(9))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
        verify(signupFormTemplateMapper, never()).deleteById(9);
    }

    @Test
    void deletesExistingTemplateById() {
        when(signupFormTemplateMapper.selectById(1)).thenReturn(existingTemplate(1, "报名信息收集"));
        // 用 spy 桩掉继承自 IService 的 removeById，隔离 MyBatis-Plus 对逻辑删除的底层解析。
        SignupFormTemplateServiceImpl spy = spy(signupFormTemplateService);
        doReturn(true).when(spy).removeById(1);

        spy.deleteTemplate(1);

        verify(spy).removeById(1);
    }

    @Test
    void throwsDataNotFoundWhenLogicalDeleteReturnsFalse() {
        when(signupFormTemplateMapper.selectById(1)).thenReturn(existingTemplate(1, "报名信息收集"));
        SignupFormTemplateServiceImpl spy = spy(signupFormTemplateService);
        doReturn(false).when(spy).removeById(1);

        assertThatThrownBy(() -> spy.deleteTemplate(1))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void listsTemplatesByCreateTimeDescending() {
        SignupFormTemplate oldTemplate = existingTemplate(2, "旧模板");
        SignupFormTemplate newTemplate = existingTemplate(1, "新模板");
        when(signupFormTemplateMapper.selectList(any())).thenReturn(List.of(newTemplate, oldTemplate));

        assertThat(signupFormTemplateService.listAll())
                .containsExactly(newTemplate, oldTemplate);
    }

    /**
     * 构建字段合法的模板创建请求，作为各测试的基础数据。
     *
     * @return 名称和配置均通过校验的创建请求
     */
    private static TemplateCreateRequest validCreateRequest() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setName("报名信息收集");
        request.setConfig("{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}");
        return request;
    }

    /**
     * 构建字段合法的模板修改请求，主键按入参填写。
     *
     * @param id 请求体中的模板主键
     * @return 主键、名称和配置均通过校验的修改请求
     */
    private static TemplateUpdateRequest validUpdateRequest(Integer id) {
        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setId(id);
        request.setName("报名信息收集");
        request.setConfig("{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}");
        return request;
    }

    /**
     * 构建指定主键和名称的模板实体，供存在性校验桩返回。
     *
     * @param id 模板主键
     * @param name 模板名称
     * @return 已存在的模板实体
     */
    private static SignupFormTemplate existingTemplate(Integer id, String name) {
        SignupFormTemplate template = new SignupFormTemplate();
        template.setId(id);
        template.setName(name);
        template.setConfig("{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}");
        return template;
    }

    /**
     * 通过反射把 Mock Mapper 注入 ServiceImpl 的受保护 baseMapper 字段。
     *
     * @param service 被测服务
     * @param mapper 注入的 Mock 数据访问接口
     */
    private static void setBaseMapper(SignupFormTemplateServiceImpl service, SignupFormTemplateMapper mapper) {
        Class<?> declaringClass = null;
        Field baseMapperField = null;
        // 沿类继承链向上查找 baseMapper 字段的声明类，兼容 MyBatis-Plus 版本间的层级差异。
        for (Class<?> type = service.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("baseMapper");
                if (BaseMapper.class.isAssignableFrom(field.getType())) {
                    declaringClass = type;
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
            throw new IllegalStateException("无法向 " + declaringClass.getName() + " 注入 baseMapper", exception);
        }
    }
}
