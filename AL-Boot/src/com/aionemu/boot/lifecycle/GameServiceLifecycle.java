package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class GameServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;

    public GameServiceLifecycle(AionServicesProperties services) {
        this.services = services;
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public int getPhase() {
        return 300;
    }

    @Override
    public boolean isEnabled() {
        return services.getGame().isEnabled();
    }

    @Override
    public void start(ApplicationArguments args) {
        com.aionemu.gameserver.GameServer.start(args.getSourceArgs(), services.getChat().isEnabled());
    }
}
