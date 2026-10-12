package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyLoginConfigOverrides;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.lifecycle.LoginStartupSequence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * 内嵌登录服生命周期：配置路径、应用遗留覆盖并驱动登录服启动序列。
 * Embedded login-service lifecycle: configures paths, applies legacy overrides, and drives the
 * login-server startup sequence.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyLoginConfigOverrides legacyConfigOverrides;
    /**
     * 启动序列提供者：以 {@link ObjectProvider} 延迟解析，避免非 Boot 上下文在无数据源时被迫实例化整条持久层链。
     * Startup-sequence provider: resolved lazily through {@link ObjectProvider} so non-Boot contexts without a
     * DataSource do not force the whole persistence chain to instantiate.
     */
    private final ObjectProvider<LoginStartupSequence> loginStartupSequenceProvider;
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
     * @param args 应用启动参数 / application arguments
     */
    @Override
    public void start(ApplicationArguments args) {
        AionServicePaths.configureLogin();
        try {
            legacyConfigOverrides.applyToLoginConfig();
            LoginStartupSequence startupSequence = loginStartupSequenceProvider.getObject();
            startupSequence.start();
            log.info(I18n.get("log.23ddf7057872", startupSequence.getLoadTimeMillis() / 1000));
            started = true;
        } catch (RuntimeException | Error e) {
            loginStartupSequenceProvider.ifAvailable(LoginStartupSequence::stop);
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
        loginStartupSequenceProvider.ifAvailable(LoginStartupSequence::stop);
        started = false;
    }
}
