package com.aionemu;

import com.aionemu.boot.callback.CallbackWeavingBootstrap;
import com.aionemu.boot.config.AionGameProperties;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatProperties;
import com.aionemu.boot.config.LegacyGameProperties;
import com.aionemu.boot.config.LegacyLoginProperties;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Aion 统一 Boot 启动入口，以非 Web 模式拉起内嵌游戏/登录/聊天服务。
 * Unified Boot entry that starts the embedded game/login/chat services without a web container.
 */
@SpringBootApplication
@EnableConfigurationProperties({
    AionServicesProperties.class,
    AionGameProperties.class,
    LegacyGameProperties.class,
    LegacyLoginProperties.class,
    LegacyChatProperties.class
})
public class AionBootApplication {

    /**
     * 启用内嵌运行模式、按需织入回调字节码后启动 Spring 应用。
     * Enables embedded runtime mode, weaves callback bytecode if needed, then starts Spring.
     *
     * @param args 命令行参数 / command-line arguments
     */
    public static void main(String[] args) {
        AionRuntimeMode.enableBootEmbeddedMode();
        CallbackWeavingBootstrap.weaveExplodedClassesIfNeeded(AionBootApplication.class);
        new SpringApplicationBuilder(AionBootApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }
}
