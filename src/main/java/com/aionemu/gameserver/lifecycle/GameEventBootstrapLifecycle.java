package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 事件引导生命周期：按步骤初始化露娜商店、役从、修勾扫荡与护照，加载状态由基类承载。
 * Event-bootstrap lifecycle: step-wise initialization of Luna shop, minion, Shugo sweep and passport;
 * load state is hosted by the base class.
 */
@Component
public class GameEventBootstrapLifecycle extends GameStartupStage {

    /**
     * 启动进度报告器。
     * Startup progress reporter.
     */
    private final StartupProgressReporter progressReporter;
    /**
     * 露娜商店服务提供者。
     * Luna-shop service provider.
     */
    private ObjectProvider<LunaShopService> lunaShopServiceProvider;
    /**
     * 宠物役从服务提供者。
     * Minion service provider.
     */
    private ObjectProvider<MinionService> minionServiceProvider;
    /**
     * 修勾扫荡服务提供者。
     * Shugo-sweep service provider.
     */
    private ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    /**
     * 阿特里亚护照服务提供者。
     * Atreian-passport service provider.
     */
    private ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;

    /**
     * 使用当前控制台进度报告器构造。
     * Construct with the current console progress reporter.
     */
    public GameEventBootstrapLifecycle() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    /**
     * 使用指定进度报告器构造，供包内测试覆写使用。
     * Construct with the given progress reporter; package-scope injection point for tests.
     * @param progressReporter 进度报告器 / Progress reporter
     */
    GameEventBootstrapLifecycle(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
    }

    /**
     * 可选注入露娜商店服务提供者。
     * Optionally inject the Luna-shop service provider.
     * @param lunaShopServiceProvider 露娜商店服务提供者 / Luna-shop service provider
     */
    @Autowired(required = false)
    void setLunaShopServiceProvider(ObjectProvider<LunaShopService> lunaShopServiceProvider) {
        this.lunaShopServiceProvider = lunaShopServiceProvider;
    }

    /**
     * 可选注入宠物役从服务提供者。
     * Optionally inject the minion service provider.
     * @param minionServiceProvider 宠物役从服务提供者 / Minion service provider
     */
    @Autowired(required = false)
    void setMinionServiceProvider(ObjectProvider<MinionService> minionServiceProvider) {
        this.minionServiceProvider = minionServiceProvider;
    }

    /**
     * 可选注入修勾扫荡服务提供者。
     * Optionally inject the Shugo-sweep service provider.
     * @param shugoSweepServiceProvider 修勾扫荡服务提供者 / Shugo-sweep service provider
     */
    @Autowired(required = false)
    void setShugoSweepServiceProvider(ObjectProvider<ShugoSweepService> shugoSweepServiceProvider) {
        this.shugoSweepServiceProvider = shugoSweepServiceProvider;
    }

    /**
     * 可选注入阿特里亚护照服务提供者。
     * Optionally inject the Atreian-passport service provider.
     * @param atreianPassportServiceProvider 阿特里亚护照服务提供者 / Atreian-passport service provider
     */
    @Autowired(required = false)
    void setAtreianPassportServiceProvider(ObjectProvider<AtreianPassportService> atreianPassportServiceProvider) {
        this.atreianPassportServiceProvider = atreianPassportServiceProvider;
    }


    /**
     * 启动本阶段：引导全部游戏事件子系统并报告进度。
     * Start this stage: bootstrap all game-event subsystems and report progress.
     */
    @Override
    protected void runStage() {
        long start = System.currentTimeMillis();
        progressReporter.start("game event systems");
        try {
            loadStep("Luna Shop System", this::initializeLunaShopSystem);
            loadStep("Minion System", this::initializeMinionSystem);
            loadStep("Shugo Sweep System", this::initializeShugoSweepSystem);
            loadStep("Atreian Passport System", this::initializeAtreianPassportSystem);
            progressReporter.finish("game event systems", System.currentTimeMillis() - start);
        } catch (RuntimeException | Error e) {
            progressReporter.failed();
            throw e;
        }
    }

    /**
     * 初始化露娜商店系统。
     * Initialize the Luna-shop system.
     */
    protected void initializeLunaShopSystem() {
        lunaShopService().init();
    }

    /**
     * 初始化宠物役从系统。
     * Initialize the minion system.
     */
    protected void initializeMinionSystem() {
        minionService().init();
    }

    /**
     * 初始化修勾扫荡系统。
     * Initialize the Shugo-sweep system.
     */
    protected void initializeShugoSweepSystem() {
        shugoSweepService().initShugoSweep();
    }

    /**
     * 初始化阿特里亚护照系统。
     * Initialize the Atreian-passport system.
     */
    protected void initializeAtreianPassportSystem() {
        atreianPassportService().onStart();
    }

    /**
     * 解析露娜商店服务。
     * Resolve the Luna-shop service.
     * @return 露娜商店服务 / Luna-shop service
     */
    private LunaShopService lunaShopService() {
        return lunaShopServiceProvider.getObject();
    }

    /**
     * 解析宠物役从服务。
     * Resolve the minion service.
     * @return 宠物役从服务 / Minion service
     */
    private MinionService minionService() {
        return minionServiceProvider.getObject();
    }

    /**
     * 解析修勾扫荡服务。
     * Resolve the Shugo-sweep service.
     * @return 修勾扫荡服务 / Shugo-sweep service
     */
    private ShugoSweepService shugoSweepService() {
        return shugoSweepServiceProvider.getObject();
    }

    /**
     * 解析阿特里亚护照服务。
     * Resolve the Atreian-passport service.
     * @return 阿特里亚护照服务 / Atreian-passport service
     */
    private AtreianPassportService atreianPassportService() {
        return atreianPassportServiceProvider.getObject();
    }


    /**
     * 执行并报告单个加载步骤。
     * Execute and report a single load step.
     * @param stepName 步骤名 / Step name
     * @param loader 加载逻辑 / Loader logic
     */
    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }
}
