package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.stereotype.Component;

@Component
public class GameEventBootstrapGateway {

    private final StartupProgressReporter progressReporter;

    public GameEventBootstrapGateway() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    GameEventBootstrapGateway(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
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
        LunaShopService.getInstance().init();
    }

    protected void initializeMinionSystem() {
        MinionService.getInstance().init();
    }

    protected void initializeShugoSweepSystem() {
        ShugoSweepService.getInstance().initShugoSweep();
    }

    protected void initializeAtreianPassportSystem() {
        AtreianPassportService.getInstance().onStart();
    }

    protected void initializeEventWindowSystem() {
        EventWindowService.getInstance().initialize();
    }

    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }
}
