package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AionServiceLauncher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AionServiceLauncher.class);

    private final AionServicesProperties services;

    public AionServiceLauncher(AionServicesProperties services) {
        this.services = services;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] sourceArgs = args.getSourceArgs();
        boolean loginEnabled = services.getLogin().isEnabled();
        boolean chatEnabled = services.getChat().isEnabled();
        boolean gameEnabled = services.getGame().isEnabled();

        log.info("Aion service startup: login={}, chat={}, game={}", loginEnabled, chatEnabled, gameEnabled);
        if (gameEnabled && !loginEnabled) {
            log.warn("Game service is enabled while login service is disabled; game will still use its configured login-server connector.");
        }

        if (loginEnabled) {
            startService("login", () -> com.aionemu.loginserver.LoginServer.start(sourceArgs));
        }
        if (chatEnabled) {
            startService("chat", () -> com.aionemu.chatserver.ChatServer.start(sourceArgs));
        } else {
            log.info("Chat service is disabled by boot configuration.");
        }
        if (gameEnabled) {
            startService("game", () -> com.aionemu.gameserver.GameServer.start(sourceArgs, chatEnabled));
        }
    }

    private void startService(String name, ServiceStartAction action) throws Exception {
        log.info("Starting {} service...", name);
        action.start();
        log.info("{} service startup returned.", name);
    }

    @FunctionalInterface
    private interface ServiceStartAction {
        void start() throws Exception;
    }
}
