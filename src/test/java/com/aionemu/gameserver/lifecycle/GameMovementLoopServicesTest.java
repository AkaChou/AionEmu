package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

class GameMovementLoopServicesTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void gameServerCodeUsesMovementLoopBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/MovementNotifyTask.java")))
                .filter(path -> !path.endsWith(Path.of("taskmanager/tasks/PlayerMoveTaskManager.java")))
                .filter(path -> !path.endsWith(Path.of("world/zone/ZoneUpdateService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMovementLoopServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMovementLoopGateway.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("MovementNotifyTask.getInstance()"), source.toString());
            assertFalse(content.contains("MoveTaskManager.getInstance()"), source.toString());
            assertFalse(content.contains("PlayerMoveTaskManager.getInstance()"), source.toString());
            assertFalse(content.contains("ZoneUpdateService.getInstance()"), source.toString());
        }
    }

    @Test
    void cachesResolvedMovementServices() {
        GameMovementLoopServices services = new GameMovementLoopServices(
            prototypeProvider(MovementNotifyTask.class),
            prototypeProvider(MoveTaskManager.class),
            prototypeProvider(PlayerMoveTaskManager.class),
            prototypeProvider(ZoneUpdateService.class)
        );

        try {
            assertSame(GameMovementLoopServices.movementNotifyTask(), GameMovementLoopServices.movementNotifyTask());
            assertSame(GameMovementLoopServices.moveTaskManager(), GameMovementLoopServices.moveTaskManager());
            assertSame(GameMovementLoopServices.playerMoveTaskManager(), GameMovementLoopServices.playerMoveTaskManager());
            assertSame(GameMovementLoopServices.zoneUpdateService(), GameMovementLoopServices.zoneUpdateService());
        } finally {
            services.destroy();
        }
    }

    private <T> ObjectProvider<T> prototypeProvider(Class<T> type) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        RootBeanDefinition definition = new RootBeanDefinition(type);
        definition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        definition.setInstanceSupplier(() -> objenesis.newInstance(type));
        beanFactory.registerBeanDefinition(type.getName(), definition);
        return beanFactory.getBeanProvider(type);
    }
}
