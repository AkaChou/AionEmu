package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class LoginServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private boolean started;

    public LoginServiceLifecycle(AionServicesProperties services) {
        this.services = services;
    }

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
            com.aionemu.loginserver.LoginServer.start(args.getSourceArgs());
            started = true;
        } catch (RuntimeException | Error e) {
            com.aionemu.loginserver.Shutdown.getInstance().shutdown(false);
            throw e;
        }
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        com.aionemu.loginserver.Shutdown.getInstance().shutdown(false);
        started = false;
    }
}
