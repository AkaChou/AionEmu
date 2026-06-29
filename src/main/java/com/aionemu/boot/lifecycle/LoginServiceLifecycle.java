package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyLoginConfigOverrides;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyLoginConfigOverrides legacyConfigOverrides;
    private final LoginServerLifecycleGateway loginServerLifecycleGateway;
    private boolean started;

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public int getPhase() {
        return 100;
    }

    @Override
    public boolean isEnabled() {
        return services.getLogin().isEnabled();
    }

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

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        loginServerLifecycleGateway.stop();
        started = false;
    }
}
