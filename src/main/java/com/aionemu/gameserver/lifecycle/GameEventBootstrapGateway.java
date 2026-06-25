package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameEventBootstrapGateway {

    private final StartupProgressReporter progressReporter;
    private ObjectProvider<LunaShopService> lunaShopServiceProvider;
    private ObjectProvider<MinionService> minionServiceProvider;
    private ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    private ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;
    private ObjectProvider<EventWindowService> eventWindowServiceProvider;
    private ObjectProvider<GameEventBootstrapRuntimeBridge> runtimeBridgeProvider;

    public GameEventBootstrapGateway() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    GameEventBootstrapGateway(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
    }

    @Autowired(required = false)
    void setLunaShopServiceProvider(ObjectProvider<LunaShopService> lunaShopServiceProvider) {
        this.lunaShopServiceProvider = lunaShopServiceProvider;
    }

    @Autowired(required = false)
    void setMinionServiceProvider(ObjectProvider<MinionService> minionServiceProvider) {
        this.minionServiceProvider = minionServiceProvider;
    }

    @Autowired(required = false)
    void setShugoSweepServiceProvider(ObjectProvider<ShugoSweepService> shugoSweepServiceProvider) {
        this.shugoSweepServiceProvider = shugoSweepServiceProvider;
    }

    @Autowired(required = false)
    void setAtreianPassportServiceProvider(ObjectProvider<AtreianPassportService> atreianPassportServiceProvider) {
        this.atreianPassportServiceProvider = atreianPassportServiceProvider;
    }

    @Autowired(required = false)
    void setEventWindowServiceProvider(ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        this.eventWindowServiceProvider = eventWindowServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameEventBootstrapRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void bootstrap() {
        long start = System.currentTimeMillis();
        progressReporter.start("game event systems");
        try {
            loadStep("Luna Shop System", this::initializeLunaShopSystem);
            loadStep("Minion System", this::initializeMinionSystem);
            loadStep("Shugo Sweep System", this::initializeShugoSweepSystem);
            loadStep("Atreian Passport System", this::initializeAtreianPassportSystem);
            loadStep("Event Window System", this::initializeEventWindowSystem);
            progressReporter.finish("game event systems", System.currentTimeMillis() - start);
        } catch (RuntimeException | Error e) {
            progressReporter.failed();
            throw e;
        }
    }

    protected void initializeLunaShopSystem() {
        lunaShopService().init();
    }

    protected void initializeMinionSystem() {
        minionService().init();
    }

    protected void initializeShugoSweepSystem() {
        shugoSweepService().initShugoSweep();
    }

    protected void initializeAtreianPassportSystem() {
        atreianPassportService().onStart();
    }

    protected void initializeEventWindowSystem() {
        eventWindowService().initialize();
    }

    private LunaShopService lunaShopService() {
        if (lunaShopServiceProvider == null) {
            return runtimeBridge().lunaShopService();
        }
        return lunaShopServiceProvider.getIfAvailable(() -> runtimeBridge().lunaShopService());
    }

    private MinionService minionService() {
        if (minionServiceProvider == null) {
            return runtimeBridge().minionService();
        }
        return minionServiceProvider.getIfAvailable(() -> runtimeBridge().minionService());
    }

    private ShugoSweepService shugoSweepService() {
        if (shugoSweepServiceProvider == null) {
            return runtimeBridge().shugoSweepService();
        }
        return shugoSweepServiceProvider.getIfAvailable(() -> runtimeBridge().shugoSweepService());
    }

    private AtreianPassportService atreianPassportService() {
        if (atreianPassportServiceProvider == null) {
            return runtimeBridge().atreianPassportService();
        }
        return atreianPassportServiceProvider.getIfAvailable(() -> runtimeBridge().atreianPassportService());
    }

    private EventWindowService eventWindowService() {
        if (eventWindowServiceProvider == null) {
            return runtimeBridge().eventWindowService();
        }
        return eventWindowServiceProvider.getIfAvailable(() -> runtimeBridge().eventWindowService());
    }

    private GameEventBootstrapRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameEventBootstrapRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameEventBootstrapRuntimeBridge::new);
    }

    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }
}
