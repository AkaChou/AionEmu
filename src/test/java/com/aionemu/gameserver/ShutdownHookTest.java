package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.world.World;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class ShutdownHookTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();
    private GameWorldBootstrapServices worldBootstrapServices;
    private boolean oldDespawnNpcs;

    @BeforeEach
    void rememberShutdownConfig() {
        oldDespawnNpcs = ShutdownConfig.DESPAWN_NPCS;
    }

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
        ShutdownConfig.DESPAWN_NPCS = oldDespawnNpcs;
        if (worldBootstrapServices != null) {
            worldBootstrapServices.destroy();
        }
    }

    @Test
    void embeddedShutdownRequestsBootShutdownAfterCountdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ShutdownHook.getInstance().doShutdown(0, 1, ShutdownMode.RESTART);

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }

    @Test
    void shutdownContinuesEarlyAfterLastPlayerLogsOut() throws Exception {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);
        ShutdownConfig.DESPAWN_NPCS = false;
        TestWorld world = objenesis.newInstance(TestWorld.class);
        world.npcs = List.of();
        world.players = new CopyOnWriteArrayList<>(List.of(objenesis.newInstance(Player.class)));
        worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));
        Thread logout = Thread.ofPlatform().start(() -> {
            try {
                Thread.sleep(100);
                world.players.clear();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        long start = System.nanoTime();
        new ShutdownHook().doShutdown(3, 1, ShutdownMode.SHUTDOWN);
        long elapsedMillis = java.time.Duration.ofNanos(System.nanoTime() - start).toMillis();
        logout.join();

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
        assertTrue(elapsedMillis < 2500, "shutdown should not wait for the remaining countdown after all players log out");
    }

    @Test
    void stopReportsFalseWhenGameServerWasNotStarted() {
        assertFalse(GameServer.stop());
    }

    @Test
    void shutdownStatusToleratesNpcRemovalFromWorldCollectionDuringDelete() {
        ShutdownConfig.DESPAWN_NPCS = true;
        List<Npc> liveNpcs = new ArrayList<Npc>();
        TestNpc firstNpc = npc(liveNpcs);
        TestNpc secondNpc = npc(liveNpcs);
        TestNpc thirdNpc = npc(liveNpcs);
        liveNpcs.add(firstNpc);
        liveNpcs.add(secondNpc);
        liveNpcs.add(thirdNpc);
        TestWorld world = objenesis.newInstance(TestWorld.class);
        world.npcs = liveNpcs;
        world.players = List.of();
        worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));

        assertDoesNotThrow(() -> invokeSendShutdownStatus(true));
        assertEquals(List.of(), liveNpcs);
    }

    @Test
    void finalShutdownUsesLifecycleBridgesInsteadOfDirectServiceSingletons() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ShutdownHook.java"));

        assertFalse(source.contains("import com.aionemu.commons.services.CronService;"));
        assertFalse(source.contains("import com.aionemu.gameserver.utils.ThreadPoolManager;"));
		assertFalse(source.contains("CronService.getInstance()"));
		assertFalse(source.contains("ThreadPoolManager.getInstance()"));
		assertTrue(source.contains("GameEngineServices.questEngine().shutdown()"));
	}

    private void invokeSendShutdownStatus(boolean status) throws ReflectiveOperationException {
        Method method = ShutdownHook.class.getDeclaredMethod("sendShutdownStatus", boolean.class);
        method.setAccessible(true);
        method.invoke(new ShutdownHook(), status);
    }

    private TestNpc npc(Collection<Npc> liveNpcs) {
        TestNpc npc = objenesis.newInstance(TestNpc.class);
        npc.controller = new RemovingNpcController(liveNpcs, npc);
        return npc;
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static final class TestWorld extends World {
        private Collection<Npc> npcs;
        private Collection<Player> players = List.of();

        @Override
        public Collection<Npc> getNpcs() {
            return npcs;
        }

        @Override
        public Iterator<Player> getPlayersIterator() {
            return players.iterator();
        }
    }

    private static final class TestNpc extends Npc {
        private NpcController controller;

        private TestNpc() {
            super(0, new NpcController(), null, (NpcTemplate) null);
        }

        @Override
        public NpcController getController() {
            return controller;
        }
    }

    private static final class RemovingNpcController extends NpcController {
        private final Collection<Npc> liveNpcs;
        private final Npc npc;

        private RemovingNpcController(Collection<Npc> liveNpcs, Npc npc) {
            this.liveNpcs = liveNpcs;
            this.npc = npc;
        }

        @Override
        public void onDelete() {
            liveNpcs.remove(npc);
        }
    }
}
