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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameLocationBootstrapGateway {

    private ObjectProvider<SiegeService> siegeServiceProvider;
    private ObjectProvider<BaseService> baseServiceProvider;
    private ObjectProvider<OutpostService> outpostServiceProvider;
    private ObjectProvider<VortexService> vortexServiceProvider;
    private ObjectProvider<BeritraService> beritraServiceProvider;
    private ObjectProvider<AgentService> agentServiceProvider;
    private ObjectProvider<AnohaService> anohaServiceProvider;
    private ObjectProvider<SvsService> svsServiceProvider;
    private ObjectProvider<RvrService> rvrServiceProvider;
    private ObjectProvider<IuService> iuServiceProvider;
    private ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider;
    private ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider;
    private ObjectProvider<InstanceRiftService> instanceRiftServiceProvider;
    private ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider;
    private ObjectProvider<MoltenusService> moltenusServiceProvider;
    private ObjectProvider<RiftService> riftServiceProvider;
    private ObjectProvider<ConquestService> conquestServiceProvider;
    private ObjectProvider<IdianDepthsService> idianDepthsServiceProvider;
    private ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider;
    private ObjectProvider<AbyssLandingService> abyssLandingServiceProvider;
    private ObjectProvider<LandingUpdateService> landingUpdateServiceProvider;
    private ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider;

    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
    }

    @Autowired(required = false)
    void setVortexServiceProvider(ObjectProvider<VortexService> vortexServiceProvider) {
        this.vortexServiceProvider = vortexServiceProvider;
    }

    @Autowired(required = false)
    void setBeritraServiceProvider(ObjectProvider<BeritraService> beritraServiceProvider) {
        this.beritraServiceProvider = beritraServiceProvider;
    }

    @Autowired(required = false)
    void setAgentServiceProvider(ObjectProvider<AgentService> agentServiceProvider) {
        this.agentServiceProvider = agentServiceProvider;
    }

    @Autowired(required = false)
    void setAnohaServiceProvider(ObjectProvider<AnohaService> anohaServiceProvider) {
        this.anohaServiceProvider = anohaServiceProvider;
    }

    @Autowired(required = false)
    void setSvsServiceProvider(ObjectProvider<SvsService> svsServiceProvider) {
        this.svsServiceProvider = svsServiceProvider;
    }

    @Autowired(required = false)
    void setRvrServiceProvider(ObjectProvider<RvrService> rvrServiceProvider) {
        this.rvrServiceProvider = rvrServiceProvider;
    }

    @Autowired(required = false)
    void setIuServiceProvider(ObjectProvider<IuService> iuServiceProvider) {
        this.iuServiceProvider = iuServiceProvider;
    }

    @Autowired(required = false)
    void setNightmareCircusServiceProvider(ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider) {
        this.nightmareCircusServiceProvider = nightmareCircusServiceProvider;
    }

    @Autowired(required = false)
    void setDynamicRiftServiceProvider(ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider) {
        this.dynamicRiftServiceProvider = dynamicRiftServiceProvider;
    }

    @Autowired(required = false)
    void setInstanceRiftServiceProvider(ObjectProvider<InstanceRiftService> instanceRiftServiceProvider) {
        this.instanceRiftServiceProvider = instanceRiftServiceProvider;
    }

    @Autowired(required = false)
    void setZorshivDredgionServiceProvider(ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider) {
        this.zorshivDredgionServiceProvider = zorshivDredgionServiceProvider;
    }

    @Autowired(required = false)
    void setMoltenusServiceProvider(ObjectProvider<MoltenusService> moltenusServiceProvider) {
        this.moltenusServiceProvider = moltenusServiceProvider;
    }

    @Autowired(required = false)
    void setRiftServiceProvider(ObjectProvider<RiftService> riftServiceProvider) {
        this.riftServiceProvider = riftServiceProvider;
    }

    @Autowired(required = false)
    void setConquestServiceProvider(ObjectProvider<ConquestService> conquestServiceProvider) {
        this.conquestServiceProvider = conquestServiceProvider;
    }

    @Autowired(required = false)
    void setIdianDepthsServiceProvider(ObjectProvider<IdianDepthsService> idianDepthsServiceProvider) {
        this.idianDepthsServiceProvider = idianDepthsServiceProvider;
    }

    @Autowired(required = false)
    void setTowerOfEternityServiceProvider(ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider) {
        this.towerOfEternityServiceProvider = towerOfEternityServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssLandingServiceProvider(ObjectProvider<AbyssLandingService> abyssLandingServiceProvider) {
        this.abyssLandingServiceProvider = abyssLandingServiceProvider;
    }

    @Autowired(required = false)
    void setLandingUpdateServiceProvider(ObjectProvider<LandingUpdateService> landingUpdateServiceProvider) {
        this.landingUpdateServiceProvider = landingUpdateServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssLandingSpecialServiceProvider(ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider) {
        this.abyssLandingSpecialServiceProvider = abyssLandingSpecialServiceProvider;
    }

    public void bootstrap() {
        Util.printSection(" *** Siege & Battlefield Locations *** ");
        siegeService().initSiegeLocations();
        BaseService baseService = baseService();
        baseService.initBaseLocations();
        baseService.initBaseReset();
        OutpostService outpostService = outpostService();
        outpostService.initOutpostLocations();
        outpostService.initOupostReset();
        Util.printSection(" *** Vortex & World Boss Locations *** ");
        VortexService vortexService = vortexService();
        vortexService.initVortex();
        vortexService.initVortexLocations();
        BeritraService beritraService = beritraService();
        beritraService.initBeritra();
        beritraService.initBeritraLocations();
        AgentService agentService = agentService();
        agentService.initAgent();
        agentService.initAgentLocations();
        AnohaService anohaService = anohaService();
        anohaService.initAnoha();
        anohaService.initAnohaLocations();
        SvsService svsService = svsService();
        svsService.initSvs();
        svsService.initSvsLocations();
        Util.printSection(" *** PvP & RvR Locations *** ");
        RvrService rvrService = rvrService();
        rvrService.initRvr();
        rvrService.initRvrLocations();
        IuService iuService = iuService();
        iuService.initConcert();
        iuService.initConcertLocations();
        NightmareCircusService nightmareCircusService = nightmareCircusService();
        nightmareCircusService.initCircus();
        nightmareCircusService.initCircusLocations();
        DynamicRiftService dynamicRiftService = dynamicRiftService();
        dynamicRiftService.initDynamicRift();
        dynamicRiftService.initDynamicRiftLocations();
        Util.printSection(" *** Instance & Dungeon Locations *** ");
        InstanceRiftService instanceRiftService = instanceRiftService();
        instanceRiftService.initInstance();
        instanceRiftService.initInstanceLocations();
        ZorshivDredgionService zorshivDredgionService = zorshivDredgionService();
        zorshivDredgionService.initZorshivDredgion();
        zorshivDredgionService.initZorshivDredgionLocations();
        MoltenusService moltenusService = moltenusService();
        moltenusService.initMoltenus();
        moltenusService.initMoltenusLocations();
        RiftService riftService = riftService();
        riftService.initRifts();
        riftService.initRiftLocations();
        ConquestService conquestService = conquestService();
        conquestService.initOffering();
        conquestService.initConquestLocations();
        IdianDepthsService idianDepthsService = idianDepthsService();
        idianDepthsService.initIdianDepths();
        idianDepthsService.initIdianDepthsLocations();
        TowerOfEternityService towerOfEternityService = towerOfEternityService();
        towerOfEternityService.initTowerOfEternity();
        towerOfEternityService.initTowerOfEternityLocation();
        Util.printSection(" *** Abyss & Landing Locations *** ");
        abyssLandingService().initLandingLocations();
        LandingUpdateService landingUpdateService = landingUpdateService();
        landingUpdateService.initResetQuestPoints();
        landingUpdateService.initResetAbyssLandingPoints();
        abyssLandingSpecialService().initLandingSpecialLocations();
    }

    private SiegeService siegeService() {
        if (siegeServiceProvider == null) {
            return SiegeService.getInstance();
        }
        return siegeServiceProvider.getIfAvailable(SiegeService::getInstance);
    }

    private BaseService baseService() {
        if (baseServiceProvider == null) {
            return BaseService.getInstance();
        }
        return baseServiceProvider.getIfAvailable(BaseService::getInstance);
    }

    private OutpostService outpostService() {
        if (outpostServiceProvider == null) {
            return OutpostService.getInstance();
        }
        return outpostServiceProvider.getIfAvailable(OutpostService::getInstance);
    }

    private VortexService vortexService() {
        if (vortexServiceProvider == null) {
            return VortexService.getInstance();
        }
        return vortexServiceProvider.getIfAvailable(VortexService::getInstance);
    }

    private BeritraService beritraService() {
        if (beritraServiceProvider == null) {
            return BeritraService.getInstance();
        }
        return beritraServiceProvider.getIfAvailable(BeritraService::getInstance);
    }

    private AgentService agentService() {
        if (agentServiceProvider == null) {
            return AgentService.getInstance();
        }
        return agentServiceProvider.getIfAvailable(AgentService::getInstance);
    }

    private AnohaService anohaService() {
        if (anohaServiceProvider == null) {
            return AnohaService.getInstance();
        }
        return anohaServiceProvider.getIfAvailable(AnohaService::getInstance);
    }

    private SvsService svsService() {
        if (svsServiceProvider == null) {
            return SvsService.getInstance();
        }
        return svsServiceProvider.getIfAvailable(SvsService::getInstance);
    }

    private RvrService rvrService() {
        if (rvrServiceProvider == null) {
            return RvrService.getInstance();
        }
        return rvrServiceProvider.getIfAvailable(RvrService::getInstance);
    }

    private IuService iuService() {
        if (iuServiceProvider == null) {
            return IuService.getInstance();
        }
        return iuServiceProvider.getIfAvailable(IuService::getInstance);
    }

    private NightmareCircusService nightmareCircusService() {
        if (nightmareCircusServiceProvider == null) {
            return NightmareCircusService.getInstance();
        }
        return nightmareCircusServiceProvider.getIfAvailable(NightmareCircusService::getInstance);
    }

    private DynamicRiftService dynamicRiftService() {
        if (dynamicRiftServiceProvider == null) {
            return DynamicRiftService.getInstance();
        }
        return dynamicRiftServiceProvider.getIfAvailable(DynamicRiftService::getInstance);
    }

    private InstanceRiftService instanceRiftService() {
        if (instanceRiftServiceProvider == null) {
            return InstanceRiftService.getInstance();
        }
        return instanceRiftServiceProvider.getIfAvailable(InstanceRiftService::getInstance);
    }

    private ZorshivDredgionService zorshivDredgionService() {
        if (zorshivDredgionServiceProvider == null) {
            return ZorshivDredgionService.getInstance();
        }
        return zorshivDredgionServiceProvider.getIfAvailable(ZorshivDredgionService::getInstance);
    }

    private MoltenusService moltenusService() {
        if (moltenusServiceProvider == null) {
            return MoltenusService.getInstance();
        }
        return moltenusServiceProvider.getIfAvailable(MoltenusService::getInstance);
    }

    private RiftService riftService() {
        if (riftServiceProvider == null) {
            return RiftService.getInstance();
        }
        return riftServiceProvider.getIfAvailable(RiftService::getInstance);
    }

    private ConquestService conquestService() {
        if (conquestServiceProvider == null) {
            return ConquestService.getInstance();
        }
        return conquestServiceProvider.getIfAvailable(ConquestService::getInstance);
    }

    private IdianDepthsService idianDepthsService() {
        if (idianDepthsServiceProvider == null) {
            return IdianDepthsService.getInstance();
        }
        return idianDepthsServiceProvider.getIfAvailable(IdianDepthsService::getInstance);
    }

    private TowerOfEternityService towerOfEternityService() {
        if (towerOfEternityServiceProvider == null) {
            return TowerOfEternityService.getInstance();
        }
        return towerOfEternityServiceProvider.getIfAvailable(TowerOfEternityService::getInstance);
    }

    private AbyssLandingService abyssLandingService() {
        if (abyssLandingServiceProvider == null) {
            return AbyssLandingService.getInstance();
        }
        return abyssLandingServiceProvider.getIfAvailable(AbyssLandingService::getInstance);
    }

    private LandingUpdateService landingUpdateService() {
        if (landingUpdateServiceProvider == null) {
            return LandingUpdateService.getInstance();
        }
        return landingUpdateServiceProvider.getIfAvailable(LandingUpdateService::getInstance);
    }

    private AbyssLandingSpecialService abyssLandingSpecialService() {
        if (abyssLandingSpecialServiceProvider == null) {
            return AbyssLandingSpecialService.getInstance();
        }
        return abyssLandingSpecialServiceProvider.getIfAvailable(AbyssLandingSpecialService::getInstance);
    }
}
