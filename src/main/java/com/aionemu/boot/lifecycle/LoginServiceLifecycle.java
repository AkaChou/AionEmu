package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyLoginConfigOverrides;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * 内嵌登录服生命周期：配置路径、应用遗留覆盖并委托网关启停。
 * Embedded login-service lifecycle: configures paths, applies legacy overrides, and delegates start/stop.
 */
@Component
@RequiredArgsConstructor
public class LoginServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyLoginConfigOverrides legacyConfigOverrides;
    private final LoginServerLifecycleGateway loginServerLifecycleGateway;
    private boolean started;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "login";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhase() {
        return 100;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return services.getLogin().isEnabled();
    }

    /**
     * 配置登录路径与遗留配置后启动；失败时立即 stop 清理。
     * Configures login paths and legacy settings, then starts; stops immediately on failure.
     *
     * @param args 应用启动参数 / application arguments
     */
    @Override
    public void start(ApplicationArguments args) {
        AionServicePaths.configureLogin();
        try {
            legacyConfigOverrides.applyToLoginConfig();
            loginServerLifecycleGateway.start(args.getSourceArgs());
            started = true;
        } catch (RuntimeException | Error e) {
            loginServerLifecycleGateway.stop();
            throw e;
        }
    }

    /**
     * 若已成功启动则停止登录服。
     * Stops the login server when it was successfully started.
     */
    @Override
    public void stop() {
        if (!started) {
            return;
        }
        loginServerLifecycleGateway.stop();
        started = false;
    }
}
