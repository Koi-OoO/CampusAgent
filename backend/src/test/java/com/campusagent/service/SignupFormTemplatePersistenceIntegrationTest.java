package com.campusagent.service;

import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.TemplateCreateRequest;
import com.campusagent.dto.request.TemplateUpdateRequest;
import com.campusagent.entity.SignupFormTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 报名表单模板数据访问集成测试，验证真实 MySQL 上的 JSON 配置映射、查询排序和逻辑删除。
 *
 * <p>连接地址固定为本地 campus 库，通过系统属性 campus.mysql.integration=true 启用。
 * 每个测试方法完成后自动回滚事务，不影响库中已有模板数据。</p>
 */
@EnabledIfSystemProperty(named = "campus.mysql.integration", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/campus?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
        "logging.level.com.campusagent.mapper=INFO",
        // knife4j 自动配置与无 Web 环境的测试上下文冲突，测试中禁用。
        "knife4j.enable=false"
})
@ActiveProfiles("dev")
@Transactional
class SignupFormTemplatePersistenceIntegrationTest {

    // 由 Spring 容器提供的真实模板服务，验证完整的数据访问调用链。
    @Autowired
    private SignupFormTemplateService signupFormTemplateService;

    // 直接读取数据库原始字段，用于确认 JSON 存储内容和逻辑删除标记。
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 解析 JSON 配置内容，MySQL 会规范化 JSON 文本，比较时以解析后的节点为准。
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证模板保存后 JSON 配置以文本形式正确往返，时间字段由数据库自动生成。
     */
    @Test
    void persistsTemplateAndReadsBackJsonConfig() {
        SignupFormTemplate template = createTemplate(uniqueName(), validConfig());

        SignupFormTemplate persisted = signupFormTemplateService.getById(template.getId());

        assertThat(persisted).isNotNull();
        assertThat(persisted.getName()).isEqualTo(template.getName());
        assertThat(jsonTree(persisted.getConfig())).isEqualTo(jsonTree(validConfig()));
        assertThat(persisted.getIsDeleted()).isZero();
        assertThat(persisted.getCreateTime()).isNotNull();
        assertThat(persisted.getUpdateTime()).isNotNull();
    }

    /**
     * 验证模板列表按创建时间倒序返回，先创建的模板排在后面。
     */
    @Test
    void listsTemplatesByCreateTimeDescending() {
        SignupFormTemplate earlier = createTemplate(uniqueName(), validConfig());
        // 在回滚事务内改写创建时间为较早的基准值，避免依赖等待一秒等不稳定验证方式。
        LocalDateTime originalTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        jdbcTemplate.update("UPDATE signup_form_template SET create_time = ?, update_time = ? WHERE id = ?",
                originalTime, originalTime, earlier.getId());

        SignupFormTemplate later = createTemplate(uniqueName(), validConfig());

        List<SignupFormTemplate> templates = signupFormTemplateService.listAll();

        // 库中可能已有其他模板，只校验本次创建的两个模板的相对顺序。
        int earlierIndex = indexOfById(templates, earlier.getId());
        int laterIndex = indexOfById(templates, later.getId());
        assertThat(earlierIndex).isGreaterThan(laterIndex);
    }

    /**
     * 验证修改模板后名称和配置被更新，创建时间保持不变，更新时间由数据库自动维护。
     */
    @Test
    void updatesTemplateAndLetsDatabaseMaintainTimestamps() {
        SignupFormTemplate template = createTemplate(uniqueName(), validConfig());
        LocalDateTime originalTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        jdbcTemplate.update("UPDATE signup_form_template SET create_time = ?, update_time = ? WHERE id = ?",
                originalTime, originalTime, template.getId());

        signupFormTemplateService.updateTemplate(template.getId(), updateRequest(template.getId(), uniqueName()));

        SignupFormTemplate updated = signupFormTemplateService.getById(template.getId());
        assertThat(updated.getName()).isNotEqualTo(template.getName());
        assertThat(jsonTree(updated.getConfig())).isEqualTo(jsonTree(validConfig()));
        assertThat(updated.getCreateTime()).isEqualTo(originalTime);
        assertThat(updated.getUpdateTime()).isAfter(originalTime);
    }

    /**
     * 验证创建模板时名称重复被拒绝，并返回参数错误状态码和统一提示。
     */
    @Test
    void rejectsDuplicateNameOnCreate() {
        String name = uniqueName();
        createTemplate(name, validConfig());

        assertThatThrownBy(() -> signupFormTemplateService.createTemplate(createRequest(name)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(businessException.getMessage()).isEqualTo("模板名称已存在");
                });
    }

    /**
     * 验证修改模板时名称与其他模板重复被拒绝，保持自己名称时允许修改。
     */
    @Test
    void rejectsDuplicateNameOnUpdateButAllowsKeepingOwnName() {
        SignupFormTemplate first = createTemplate(uniqueName(), validConfig());
        SignupFormTemplate second = createTemplate(uniqueName(), validConfig());

        // 改成其他模板的名称时应被拒绝。
        assertThatThrownBy(() -> signupFormTemplateService.updateTemplate(
                second.getId(), updateRequest(second.getId(), first.getName())))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        // 保持自己的名称时允许修改配置。
        String newConfig = "{\"fields\":[{\"key\":\"remark\",\"label\":\"备注\"}]}";
        TemplateUpdateRequest keepName = updateRequest(second.getId(), second.getName());
        keepName.setConfig(newConfig);
        signupFormTemplateService.updateTemplate(second.getId(), keepName);

        assertThat(jsonTree(signupFormTemplateService.getById(second.getId()).getConfig()))
                .isEqualTo(jsonTree(newConfig));
    }

    /**
     * 验证修改不存在的模板时返回数据不存在错误码。
     */
    @Test
    void throwsDataNotFoundWhenUpdatingMissingTemplate() {
        assertThatThrownBy(() -> signupFormTemplateService.updateTemplate(
                Integer.MAX_VALUE, updateRequest(Integer.MAX_VALUE, uniqueName())))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    /**
     * 验证逻辑删除保留数据库记录，查询和列表不再返回，原始行标记为已删除。
     */
    @Test
    void logicallyDeletesTemplateAndHidesItFromQueries() {
        SignupFormTemplate template = createTemplate(uniqueName(), validConfig());

        signupFormTemplateService.deleteTemplate(template.getId());

        assertThat(signupFormTemplateService.getById(template.getId())).isNull();
        assertThat(indexOfById(signupFormTemplateService.listAll(), template.getId())).isNegative();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM signup_form_template WHERE id = ?", Integer.class, template.getId()))
                .isEqualTo(1);
    }

    /**
     * 验证删除不存在的模板时返回数据不存在错误码。
     */
    @Test
    void throwsDataNotFoundWhenDeletingMissingTemplate() {
        assertThatThrownBy(() -> signupFormTemplateService.deleteTemplate(Integer.MAX_VALUE))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode())
                                .isEqualTo(ResultCode.DATA_NOT_FOUND.getCode()));
    }

    /**
     * 通过服务创建模板，验证插入后的主键已回填。
     *
     * @param name 模板名称
     * @param config 模板配置
     * @return 已保存的模板实体
     */
    private SignupFormTemplate createTemplate(String name, String config) {
        TemplateCreateRequest request = createRequest(name);
        request.setConfig(config);
        signupFormTemplateService.createTemplate(request);

        // 通过随机名称反查本次插入的记录，名称唯一性由服务层保证。
        return signupFormTemplateService.listAll().stream()
                .filter(item -> item.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("测试模板保存后无法查询到记录"));
    }

    /**
     * 构建指定名称的模板创建请求，配置由调用方覆盖。
     *
     * @param name 模板名称
     * @return 待补全配置的创建请求
     */
    private static TemplateCreateRequest createRequest(String name) {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setName(name);
        return request;
    }

    /**
     * 构建指定主键和名称的模板修改请求，配置使用默认合法值。
     *
     * @param id 请求体中的模板主键
     * @param name 模板名称
     * @return 字段合法的修改请求
     */
    private static TemplateUpdateRequest updateRequest(Integer id, String name) {
        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setId(id);
        request.setName(name);
        request.setConfig(validConfig());
        return request;
    }

    /**
     * 生成随机模板名称，避免与本地已有数据冲突。
     *
     * @return 随机模板名称
     */
    private static String uniqueName() {
        return "模板集成测试_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 返回格式紧凑的合法 JSON 配置，MySQL 规范化后保持相同语义。
     *
     * @return JSON 配置字符串
     */
    private static String validConfig() {
        return "{\"fields\":[{\"key\":\"name\",\"label\":\"姓名\"}]}";
    }

    /**
     * 把 JSON 文本解析为节点树，忽略 MySQL 存储时的空白差异。
     *
     * @param config JSON 配置文本
     * @return 解析后的 JSON 节点
     */
    private JsonNode jsonTree(String config) {
        try {
            return objectMapper.readTree(config);
        } catch (Exception exception) {
            throw new IllegalStateException("无法解析模板配置 JSON：" + config, exception);
        }
    }

    /**
     * 返回指定主键模板在列表中的下标。
     *
     * @param templates 模板列表
     * @param id 目标模板主键
     * @return 匹配的下标，未找到时返回 -1
     */
    private static int indexOfById(List<SignupFormTemplate> templates, Integer id) {
        for (int index = 0; index < templates.size(); index++) {
            if (id.equals(templates.get(index).getId())) {
                return index;
            }
        }
        return -1;
    }
}
