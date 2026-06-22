package com.aionemu.boot.lifecycle;

import java.nio.file.Path;

final class AionServicePaths {

    private AionServicePaths() {
    }

    static void configureLogin() {
        configure("aion.login.config.dir", "AL-Login/config");
        configure("aion.login.data.dir", "AL-Login/data");
    }

    static void configureChat() {
        configure("aion.chat.config.dir", "AL-Chat/config");
    }

    static void configureGame() {
        configure("aion.game.config.dir", "AL-Game/config");
        configure("aion.game.data.dir", "AL-Game/data");
        configure("aion.game.cache.dir", "AL-Game/cache");
    }

    private static void configure(String property, String defaultPath) {
        if (System.getProperty(property) != null) {
            return;
        }

        String home = System.getProperty("aion.home", ".");
        System.setProperty(property, Path.of(home).resolve(defaultPath).normalize().toString());
    }
}
