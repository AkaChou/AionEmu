package com.aionemu.boot.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.transport.AionTransportBoundary;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.lifecycle.GameShutdownRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 内嵌服务编排器：按相位启动、有序关闭，并处理嵌入式失败与关闭请求。
 * Orchestrates embedded services: phased start, ordered stop, and embedded failure/shutdown handling.
 */
@Slf4j
@Component
public class AionServiceLauncher implements ApplicationRunner, DisposableBean, ApplicationContextAware,
    ApplicationListener<ContextClosedEvent> {

    private final AionServicesProperties services;
    private final AionTransportBoundary transportBoundary;
    private final List<AionServiceLifecycle> serviceLifecycles;
    private final IntConsumer haltAction;
    private final List<AionServiceLifecycle> startedServices = new ArrayList<>();
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final AtomicBoolean gracefulShutdownRequested = new AtomicBoolean(false);
    private final Consumer<RuntimeException> embeddedFailureHandler = this::handleEmbeddedFailure;
    private final Consumer<AionEmbeddedShutdownMode> embeddedShutdownHandler = this::handleEmbeddedShutdown;
    private ConfigurableApplicationContext applicationContext;

    /**
     * Spring 注入入口：使用进程运行时桥接的 halt。
     * Spring injection entry: uses process runtime bridge halt.
     *
     * @param services 服务开关配置 / service enablement properties
     * @param transportBoundary 传输边界 / transport boundary
     * @param serviceLifecycles 各服务生命周期实现 / service lifecycle beans
     * @param runtimeBridge 进程运行时桥接 / process runtime bridge
     */
    @Autowired
    public AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles,
        AionProcessRuntimeBridge runtimeBridge
    ) {
        this(services, transportBoundary, serviceLifecycles, runtimeBridge::halt);
    }

    /**
     * 测试用构造：使用默认进程桥接 halt。
     * Test constructor: uses a default process bridge halt.
     *
     * @param services 服务开关配置 / service enablement properties
     * @param transportBoundary 传输边界 / transport boundary
     * @param serviceLifecycles 各服务生命周期实现 / service lifecycle beans
     */
    AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles
    ) {
        this(services, transportBoundary, serviceLifecycles, new AionProcessRuntimeBridge()::halt);
    }

    /**
     * 核心构造：按相位排序生命周期并绑定 halt 动作。
     * Core constructor: sorts lifecycles by phase and binds the halt action.
     *
     * @param services 服务开关配置 / service enablement properties
     * @param transportBoundary 传输边界 / transport boundary
     * @param serviceLifecycles 各服务生命周期实现 / service lifecycle beans
     * @param haltAction 进程中止动作 / process halt action
     */
    AionServiceLauncher(
        AionServicesProperties services,
        AionTransportBoundary transportBoundary,
        List<AionServiceLifecycle> serviceLifecycles,
        IntConsumer haltAction
    ) {
        this.services = services;
        this.transportBoundary = transportBoundary;
        this.haltAction = haltAction;
        this.serviceLifecycles = serviceLifecycles.stream()
            .sorted(Comparator.comparingInt(AionServiceLifecycle::getPhase))
            .toList();
    }

    /**
     * 应用启动后按配置启动各内嵌服务。
     * Starts each embedded service after application bootstrap according to config.
     *
     * @param args 应用启动参数 / application arguments
     * @throws Exception 任一服务启动失败时抛出 / if any service fails to start
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        AionRuntimeMode.enableBootEmbeddedMode();
        AionEmbeddedFailureHandler.register(embeddedFailureHandler);
        AionEmbeddedShutdownHandler.register(embeddedShutdownHandler);
        String[] sourceArgs = args.getSourceArgs();
        boolean loginEnabled = services.getLogin().isEnabled();
        boolean chatEnabled = services.getChat().isEnabled();
        boolean gameEnabled = services.getGame().isEnabled();

        log.info(I18n.get("boot.startup.summary", loginEnabled, chatEnabled, gameEnabled));
        try {
            transportBoundary.prepare();
            if (gameEnabled && !loginEnabled) {
                log.warn(I18n.get("boot.startup.game_without_login"));
            }
            if (!chatEnabled) {
                log.info(I18n.get("boot.startup.chat_disabled"));
            }

            for (AionServiceLifecycle serviceLifecycle : serviceLifecycles) {
                if (serviceLifecycle.isEnabled()) {
                    startService(serviceLifecycle, args);
                } else {
                    log.info(I18n.get("boot.startup.service_disabled", serviceLifecycle.getName()));
                }
            }
        } catch (Exception | Error e) {
            log.error(I18n.get("boot.startup.failed"), e);
            destroy();
            throw e;
        }
    }

    /**
     * 保存可配置的应用上下文，供嵌入式关闭时关闭容器。
     * Stores the configurable application context for container close on embedded shutdown.
     *
     * @param applicationContext Spring 应用上下文 / Spring application context
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext) {
            this.applicationContext = configurableApplicationContext;
        }
    }

    /**
     * 在服务上下文作用域内启动单个服务并记录日志。
     * Starts one service under its ServiceContext scope and logs progress.
     *
     * @param serviceLifecycle 目标服务生命周期 / target service lifecycle
     * @param args 应用启动参数 / application arguments
     * @throws Exception 启动失败时抛出 / if start fails
     */
    private void startService(AionServiceLifecycle serviceLifecycle, ApplicationArguments args) throws Exception {
        String name = serviceLifecycle.getName();
        log.info(I18n.get("boot.startup.starting", name));
        startedServices.add(serviceLifecycle);
        try (ServiceContext.Scope ignored = ServiceContext.use(name)) {
            serviceLifecycle.start(args);
        }
        log.info(I18n.get("boot.startup.returned", name));
    }

    /**
     * 仅执行一次的优雅关闭请求。
     * Runs a graceful shutdown request at most once.
     *
     * @param shutdown 关闭动作 / shutdown action
     */
    void requestGracefulShutdown(Runnable shutdown) {
        if (gracefulShutdownRequested.compareAndSet(false, true)) {
            shutdown.run();
        }
    }

    /**
     * 上下文关闭时：若 game 已启动且未在停止中，则等待玩家离线并完成 game 关闭。
     * On context close: if game started and not already stopping, wait for players and finish game shutdown.
     *
     * @param event 上下文关闭事件 / context closed event
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        boolean gameStarted = startedServices.stream().anyMatch(service -> "game".equals(service.getName()));
        if (gameStarted && !stopping.get()) {
            // 须在游戏上下文运行：DAOManager/DatabaseFactory 按 ServiceContext 键控。 / Must run under game context: DAOManager/DatabaseFactory are keyed by ServiceContext.
            requestGracefulShutdown(() -> {
                try (ServiceContext.Scope ignored = ServiceContext.use("game")) {
                    GameShutdownRequest.waitForPlayersToLeave(ShutdownConfig.HOOK_DELAY, ShutdownConfig.ANNOUNCE_INTERVAL);
                    GameShutdownRequest.completeShutdown(ShutdownMode.SHUTDOWN, false);
                }
            });
        }
    }

    /**
     * 逆序停止已启动服务并销毁传输边界；幂等。
     * Stops started services in reverse order and destroys the transport boundary; idempotent.
     */
    @Override
    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        AionEmbeddedFailureHandler.clear(embeddedFailureHandler);
        AionEmbeddedShutdownHandler.clear(embeddedShutdownHandler);
        ListIterator<AionServiceLifecycle> iterator = startedServices.listIterator(startedServices.size());
        while (iterator.hasPrevious()) {
            AionServiceLifecycle serviceLifecycle = iterator.previous();
            stopService(serviceLifecycle);
            iterator.remove();
        }
        stopTransportBoundary();
    }

    /**
     * 在服务上下文作用域内停止单个服务。
     * Stops one service under its ServiceContext scope.
     *
     * @param serviceLifecycle 目标服务生命周期 / target service lifecycle
     */
    private void stopService(AionServiceLifecycle serviceLifecycle) {
        String name = serviceLifecycle.getName();
        log.info(I18n.get("boot.shutdown.stopping", name));
        try (ServiceContext.Scope ignored = ServiceContext.use(name)) {
            serviceLifecycle.stop();
        } catch (Exception e) {
            log.warn(I18n.get("boot.shutdown.stop_failed", name), e);
        }
    }

    /**
     * 销毁传输边界并吞掉异常。
     * Destroys the transport boundary and swallows exceptions.
     */
    private void stopTransportBoundary() {
        try {
            transportBoundary.destroy();
        } catch (Exception e) {
            log.warn(I18n.get("boot.shutdown.transport_stop_failed"), e);
        }
    }

    /**
     * 处理嵌入式运行时失败：停止服务并关闭应用上下文。
     * Handles embedded runtime failure: stop services and close the application context.
     *
     * @param failure 运行时失败 / runtime failure
     */
    private void handleEmbeddedFailure(RuntimeException failure) {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }
        log.error(I18n.get("boot.embedded.failure"), failure);
        stopServicesAndCloseContext();
    }

    /**
     * 处理嵌入式关闭请求；若为 RESTART 则在关闭后 halt 进程。
     * Handles embedded shutdown requests; on RESTART, halt the process after close.
     *
     * @param mode 关闭模式 / shutdown mode
     */
    private void handleEmbeddedShutdown(AionEmbeddedShutdownMode mode) {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }
        log.info(I18n.get("boot.embedded.requested", mode.name().toLowerCase()));
        stopServicesAndCloseContext();
        if (mode == AionEmbeddedShutdownMode.RESTART) {
            haltAction.accept(mode.exitCode());
        }
    }

    /**
     * 销毁服务并关闭仍活跃的应用上下文。
     * Destroys services and closes an active application context.
     */
    private void stopServicesAndCloseContext() {
        destroy();
        if (applicationContext != null && applicationContext.isActive()) {
            applicationContext.close();
        }
    }
}
