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
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameLocationBootstrapRuntimeBridge {

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
        return getIfAvailable(siegeServiceProvider, SiegeService::getInstance);
    }

    public BaseService baseService() {
        return getIfAvailable(baseServiceProvider, BaseService::getInstance);
    }

    public OutpostService outpostService() {
        return getIfAvailable(outpostServiceProvider, OutpostService::getInstance);
    }

    public VortexService vortexService() {
        return getIfAvailable(vortexServiceProvider, VortexService::getInstance);
    }

    public BeritraService beritraService() {
        return getIfAvailable(beritraServiceProvider, BeritraService::getInstance);
    }

    public AgentService agentService() {
        return getIfAvailable(agentServiceProvider, AgentService::getInstance);
    }

    public AnohaService anohaService() {
        return getIfAvailable(anohaServiceProvider, AnohaService::getInstance);
    }

    public SvsService svsService() {
        return getIfAvailable(svsServiceProvider, SvsService::getInstance);
    }

    public RvrService rvrService() {
        return getIfAvailable(rvrServiceProvider, RvrService::getInstance);
    }

    public IuService iuService() {
        return getIfAvailable(iuServiceProvider, IuService::getInstance);
    }

    public NightmareCircusService nightmareCircusService() {
        return getIfAvailable(nightmareCircusServiceProvider, NightmareCircusService::getInstance);
    }

    public DynamicRiftService dynamicRiftService() {
        return getIfAvailable(dynamicRiftServiceProvider, DynamicRiftService::getInstance);
    }

    public InstanceRiftService instanceRiftService() {
        return getIfAvailable(instanceRiftServiceProvider, InstanceRiftService::getInstance);
    }

    public ZorshivDredgionService zorshivDredgionService() {
        return getIfAvailable(zorshivDredgionServiceProvider, ZorshivDredgionService::getInstance);
    }

    public MoltenusService moltenusService() {
        return getIfAvailable(moltenusServiceProvider, MoltenusService::getInstance);
    }

    public RiftService riftService() {
        return getIfAvailable(riftServiceProvider, RiftService::getInstance);
    }

    public ConquestService conquestService() {
        return getIfAvailable(conquestServiceProvider, ConquestService::getInstance);
    }

    public IdianDepthsService idianDepthsService() {
        return getIfAvailable(idianDepthsServiceProvider, IdianDepthsService::getInstance);
    }

    public TowerOfEternityService towerOfEternityService() {
        return getIfAvailable(towerOfEternityServiceProvider, TowerOfEternityService::getInstance);
    }

    public AbyssLandingService abyssLandingService() {
        return getIfAvailable(abyssLandingServiceProvider, AbyssLandingService::getInstance);
    }

    public LandingUpdateService landingUpdateService() {
        return getIfAvailable(landingUpdateServiceProvider, LandingUpdateService::getInstance);
    }

    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return getIfAvailable(abyssLandingSpecialServiceProvider, AbyssLandingSpecialService::getInstance);
    }

    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }
}
