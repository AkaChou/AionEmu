package com.aionemu.loginserver.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.aionemu.testutil.ConfigSnapshot;

/**
 * 登录服配置绑定验证：Binder 侧与遗留装配必须收敛到同一组静态门面字段。
 * Login-server configuration binding check: Binder binding and the legacy assembly must converge
 * onto the same set of static facade fields.
 */
class LoginConfigBindingTest {

    @TempDir
    Path tempDir;

    private final ConfigSnapshot snapshot = ConfigSnapshot.of(Config.class,
        "LOGIN_PORT", "ACCOUNT_AUTO_CREATION", "MAINTENANCE_MOD", "MAINTENANCE_MOD_GMLEVEL",
        "ENABLE_PINGPONG", "PINGPONG_DELAY", "EXCLUDED_IP");

    @AfterEach
    void resetConfig() {
        System.clearProperty("aion.config.dir");
        Config.setBootOverrides(null);
        snapshot.restore();
    }

    /**
     * Binder 侧绑定镜像键时必须经嵌套属性写入静态门面字段。
     * Binding mirrored keys through the Binder must reach the static facade fields via the
     * nested properties.
     */
    @Test
    void binderBindingWritesStaticFacadeFields() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "loginserver.network.client.port", "2206",
            "loginserver.accounts.autocreate", "false",
            "loginserver.server.maintenance.value", "true",
            "loginserver.server.maintenance.gmlevel", "5",
            "loginserver.server.pingpong.value", "false",
            "loginserver.server.pingpong.delay", "4500",
            "loginserver.excluded.ips", "1.2.3.4"
        )));

        Binder.get(environment).bind("loginserver", Bindable.ofInstance(new Config()));

        assertEquals(2206, Config.LOGIN_PORT);
        assertFalse(Config.ACCOUNT_AUTO_CREATION);
        assertTrue(Config.MAINTENANCE_MOD);
        assertEquals(5, Config.MAINTENANCE_MOD_GMLEVEL);
        assertFalse(Config.ENABLE_PINGPONG);
        assertEquals(4500, Config.PINGPONG_DELAY);
        assertEquals("1.2.3.4", Config.EXCLUDED_IP);
    }

    /**
     * VIP 等级越界必须被 JSR-380 约束捕获（Binder 路径的声明式校验；
     * 遗留路径由 validate() 守护）。
     * Out-of-range VIP levels must be caught by the JSR-380 constraint (the Binder path's
     * declarative validation; the legacy path stays guarded by validate()).
     */
    @Test
    void validatorRejectsOutOfRangeVipLevel() {
        try (org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
            new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            com.aionemu.loginserver.configs.VipConfig config = new com.aionemu.loginserver.configs.VipConfig();
            config.getAutoEnable().setValue(true);
            config.getAutoEnable().setLevel(9);

            var violations = validator.validate(config);
            org.junit.jupiter.api.Assertions.assertFalse(violations.isEmpty());
            org.junit.jupiter.api.Assertions.assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("level")));

            config.getAutoEnable().setLevel(3);
            org.junit.jupiter.api.Assertions.assertTrue(validator.validate(config).isEmpty());
        }
    }

    /**
     * 遗留装配沿 `.value` 规范化子键取值，不得让 defaultValue 覆盖文件值。
     * The legacy assembly must read the normalized `.value` sub-keys instead of letting
     * defaultValues overwrite the file values.
     */
    @Test
    void legacyLoadReadsNormalizedValueSubKeys() throws Exception {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir.resolve("login"));
        Files.createDirectories(configDir.resolve("network"));
        Files.createDirectories(configDir.resolve("main"));
        Files.writeString(configDir.resolve("network/network.properties"), "");
        Files.writeString(configDir.resolve("main/vip.properties"), "");
        Files.writeString(configDir.resolve("login/loginserver.properties"), """
            loginserver.accounts.autocreate = false
            loginserver.server.maintenance.value = true
            loginserver.server.maintenance.gmlevel = 5
            loginserver.server.pingpong.value = false
            loginserver.server.pingpong.delay = 4500
            loginserver.excluded.ips = 1.2.3.4
            """);
        System.setProperty("aion.config.dir", configDir.toString());

        Config.load();

        assertFalse(Config.ACCOUNT_AUTO_CREATION);
        assertTrue(Config.MAINTENANCE_MOD);
        assertEquals(5, Config.MAINTENANCE_MOD_GMLEVEL);
        assertFalse(Config.ENABLE_PINGPONG);
        assertEquals(4500, Config.PINGPONG_DELAY);
        assertEquals("1.2.3.4", Config.EXCLUDED_IP);
    }
}
