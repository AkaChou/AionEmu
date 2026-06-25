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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameLocationBootstrapRuntimeBridge {

    public void printSiegeAndBattlefieldLocationsSection() {
        Util.printSection(" *** Siege & Battlefield Locations *** ");
    }

    public void printVortexAndWorldBossLocationsSection() {
        Util.printSection(" *** Vortex & World Boss Locations *** ");
    }

    public void printPvpAndRvrLocationsSection() {
        Util.printSection(" *** PvP & RvR Locations *** ");
    }

    public void printInstanceAndDungeonLocationsSection() {
        Util.printSection(" *** Instance & Dungeon Locations *** ");
    }

    public void printAbyssAndLandingLocationsSection() {
        Util.printSection(" *** Abyss & Landing Locations *** ");
    }

    public SiegeService siegeService() {
        return SiegeService.getInstance();
    }

    public BaseService baseService() {
        return BaseService.getInstance();
    }

    public OutpostService outpostService() {
        return OutpostService.getInstance();
    }

    public VortexService vortexService() {
        return VortexService.getInstance();
    }

    public BeritraService beritraService() {
        return BeritraService.getInstance();
    }

    public AgentService agentService() {
        return AgentService.getInstance();
    }

    public AnohaService anohaService() {
        return AnohaService.getInstance();
    }

    public SvsService svsService() {
        return SvsService.getInstance();
    }

    public RvrService rvrService() {
        return RvrService.getInstance();
    }

    public IuService iuService() {
        return IuService.getInstance();
    }

    public NightmareCircusService nightmareCircusService() {
        return NightmareCircusService.getInstance();
    }

    public DynamicRiftService dynamicRiftService() {
        return DynamicRiftService.getInstance();
    }

    public InstanceRiftService instanceRiftService() {
        return InstanceRiftService.getInstance();
    }

    public ZorshivDredgionService zorshivDredgionService() {
        return ZorshivDredgionService.getInstance();
    }

    public MoltenusService moltenusService() {
        return MoltenusService.getInstance();
    }

    public RiftService riftService() {
        return RiftService.getInstance();
    }

    public ConquestService conquestService() {
        return ConquestService.getInstance();
    }

    public IdianDepthsService idianDepthsService() {
        return IdianDepthsService.getInstance();
    }

    public TowerOfEternityService towerOfEternityService() {
        return TowerOfEternityService.getInstance();
    }

    public AbyssLandingService abyssLandingService() {
        return AbyssLandingService.getInstance();
    }

    public LandingUpdateService landingUpdateService() {
        return LandingUpdateService.getInstance();
    }

    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return AbyssLandingSpecialService.getInstance();
    }
}
