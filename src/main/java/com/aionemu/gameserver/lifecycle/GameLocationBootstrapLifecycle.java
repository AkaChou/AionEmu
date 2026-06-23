package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AbyssLandingService;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.ConquestService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.IdianDepthsService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.TowerOfEternityService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameLocationBootstrapLifecycle {

    private final List<Runnable> bootstrappers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameLocationBootstrapLifecycle() {
        this(List.of(
            () -> Util.printSection(" *** Siege & Battlefield Locations *** "),
            () -> SiegeService.getInstance().initSiegeLocations(),
            () -> BaseService.getInstance().initBaseLocations(),
            () -> BaseService.getInstance().initBaseReset(),
            () -> OutpostService.getInstance().initOutpostLocations(),
            () -> OutpostService.getInstance().initOupostReset(),
            () -> Util.printSection(" *** Vortex & World Boss Locations *** "),
            () -> VortexService.getInstance().initVortex(),
            () -> VortexService.getInstance().initVortexLocations(),
            () -> BeritraService.getInstance().initBeritra(),
            () -> BeritraService.getInstance().initBeritraLocations(),
            () -> AgentService.getInstance().initAgent(),
            () -> AgentService.getInstance().initAgentLocations(),
            () -> AnohaService.getInstance().initAnoha(),
            () -> AnohaService.getInstance().initAnohaLocations(),
            () -> SvsService.getInstance().initSvs(),
            () -> SvsService.getInstance().initSvsLocations(),
            () -> Util.printSection(" *** PvP & RvR Locations *** "),
            () -> RvrService.getInstance().initRvr(),
            () -> RvrService.getInstance().initRvrLocations(),
            () -> IuService.getInstance().initConcert(),
            () -> IuService.getInstance().initConcertLocations(),
            () -> NightmareCircusService.getInstance().initCircus(),
            () -> NightmareCircusService.getInstance().initCircusLocations(),
            () -> DynamicRiftService.getInstance().initDynamicRift(),
            () -> DynamicRiftService.getInstance().initDynamicRiftLocations(),
            () -> Util.printSection(" *** Instance & Dungeon Locations *** "),
            () -> InstanceRiftService.getInstance().initInstance(),
            () -> InstanceRiftService.getInstance().initInstanceLocations(),
            () -> ZorshivDredgionService.getInstance().initZorshivDredgion(),
            () -> ZorshivDredgionService.getInstance().initZorshivDredgionLocations(),
            () -> MoltenusService.getInstance().initMoltenus(),
            () -> MoltenusService.getInstance().initMoltenusLocations(),
            () -> RiftService.getInstance().initRifts(),
            () -> RiftService.getInstance().initRiftLocations(),
            () -> ConquestService.getInstance().initOffering(),
            () -> ConquestService.getInstance().initConquestLocations(),
            () -> IdianDepthsService.getInstance().initIdianDepths(),
            () -> IdianDepthsService.getInstance().initIdianDepthsLocations(),
            () -> TowerOfEternityService.getInstance().initTowerOfEternity(),
            () -> TowerOfEternityService.getInstance().initTowerOfEternityLocation(),
            () -> Util.printSection(" *** Abyss & Landing Locations *** "),
            () -> AbyssLandingService.getInstance().initLandingLocations(),
            () -> LandingUpdateService.getInstance().initResetQuestPoints(),
            () -> LandingUpdateService.getInstance().initResetAbyssLandingPoints(),
            () -> AbyssLandingSpecialService.getInstance().initLandingSpecialLocations()
        ));
    }

    GameLocationBootstrapLifecycle(List<Runnable> bootstrappers) {
        this.bootstrappers = List.copyOf(bootstrappers);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            bootstrappers.forEach(Runnable::run);
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
