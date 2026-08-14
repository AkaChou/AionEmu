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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 地点/副本引导启动网关：按固定顺序引导世界地点系统（攻城/战场、漩涡/世界 Boss、PvP/RvR、副本/地牢、欧比斯/登陆）。
 * LocationBootstrap gateway that bootstraps world location systems in a fixed order:
 * siege/battlefield, vortex/world bosses, PvP/RvR, instance/dungeon, and abyss/landing.
 *
 * <p>各服务优先通过注入的 {@link ObjectProvider} 解析；不可用时回退到
 * {@link GameLocationBootstrapRuntimeBridge}。
 * Services are resolved via injected {@link ObjectProvider}s first, falling back to
 * {@link GameLocationBootstrapRuntimeBridge} when unavailable.</p>
 */
@Component
public class GameLocationBootstrapGateway {

    /**
     * 攻城服务提供者。
     * Siege service provider.
     */
    private ObjectProvider<SiegeService> siegeServiceProvider;

    /**
     * 基地服务提供者。
     * Base service provider.
     */
    private ObjectProvider<BaseService> baseServiceProvider;

    /**
     * 前哨服务提供者。
     * Outpost service provider.
     */
    private ObjectProvider<OutpostService> outpostServiceProvider;

    /**
     * 漩涡服务提供者。
     * Vortex service provider.
     */
    private ObjectProvider<VortexService> vortexServiceProvider;

    /**
     * 贝里特拉入侵服务提供者。
     * Beritra service provider.
     */
    private ObjectProvider<BeritraService> beritraServiceProvider;

    /**
     * 代理人服务提供者。
     * Agent service provider.
     */
    private ObjectProvider<AgentService> agentServiceProvider;

    /**
     * 阿诺哈服务提供者。
     * Anoha service provider.
     */
    private ObjectProvider<AnohaService> anohaServiceProvider;

    /**
     * SvS 服务提供者。
     * SvS service provider.
     */
    private ObjectProvider<SvsService> svsServiceProvider;

    /**
     * RvR 服务提供者。
     * RvR service provider.
     */
    private ObjectProvider<RvrService> rvrServiceProvider;

    /**
     * IU 演唱会服务提供者。
     * IU concert service provider.
     */
    private ObjectProvider<IuService> iuServiceProvider;

    /**
     * 噩梦马戏团服务提供者。
     * Nightmare Circus service provider.
     */
    private ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider;

    /**
     * 动态裂隙服务提供者。
     * Dynamic Rift service provider.
     */
    private ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider;

    /**
     * 副本裂隙服务提供者。
     * Instance Rift service provider.
     */
    private ObjectProvider<InstanceRiftService> instanceRiftServiceProvider;

    /**
     * 佐西夫战舰服务提供者。
     * Zorshiv Dredgion service provider.
     */
    private ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider;

    /**
     * 熔岩巨兽服务提供者。
     * Moltenus service provider.
     */
    private ObjectProvider<MoltenusService> moltenusServiceProvider;

    /**
     * 裂隙服务提供者。
     * Rift service provider.
     */
    private ObjectProvider<RiftService> riftServiceProvider;

    /**
     * 征服服务提供者。
     * Conquest service provider.
     */
    private ObjectProvider<ConquestService> conquestServiceProvider;

    /**
     * 伊迪安深渊服务提供者。
     * Idian Depths service provider.
     */
    private ObjectProvider<IdianDepthsService> idianDepthsServiceProvider;

    /**
     * 永恒之塔服务提供者。
     * Tower of Eternity service provider.
     */
    private ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider;

    /**
     * 欧比斯登陆服务提供者。
     * Abyss Landing service provider.
     */
    private ObjectProvider<AbyssLandingService> abyssLandingServiceProvider;

    /**
     * 登陆更新服务提供者。
     * Landing update service provider.
     */
    private ObjectProvider<LandingUpdateService> landingUpdateServiceProvider;

    /**
     * 欧比斯特殊登陆服务提供者。
     * Abyss Landing special service provider.
     */
    private ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider;

    /**
     * 地点引导运行时桥接提供者。
     * LocationBootstrap runtime bridge provider.
     */
    private ObjectProvider<GameLocationBootstrapRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入攻城服务提供者。
     * Inject the siege service provider.
     *
     * @param siegeServiceProvider 攻城服务提供者 / Siege service provider
     */
    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    /**
     * 注入基地服务提供者。
     * Inject the base service provider.
     *
     * @param baseServiceProvider 基地服务提供者 / Base service provider
     */
    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    /**
     * 注入前哨服务提供者。
     * Inject the outpost service provider.
     *
     * @param outpostServiceProvider 前哨服务提供者 / Outpost service provider
     */
    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
    }

    /**
     * 注入漩涡服务提供者。
     * Inject the vortex service provider.
     *
     * @param vortexServiceProvider 漩涡服务提供者 / Vortex service provider
     */
    @Autowired(required = false)
    void setVortexServiceProvider(ObjectProvider<VortexService> vortexServiceProvider) {
        this.vortexServiceProvider = vortexServiceProvider;
    }

    /**
     * 注入贝里特拉服务提供者。
     * Inject the Beritra service provider.
     *
     * @param beritraServiceProvider 贝里特拉服务提供者 / Beritra service provider
     */
    @Autowired(required = false)
    void setBeritraServiceProvider(ObjectProvider<BeritraService> beritraServiceProvider) {
        this.beritraServiceProvider = beritraServiceProvider;
    }

    /**
     * 注入代理人服务提供者。
     * Inject the agent service provider.
     *
     * @param agentServiceProvider 代理人服务提供者 / Agent service provider
     */
    @Autowired(required = false)
    void setAgentServiceProvider(ObjectProvider<AgentService> agentServiceProvider) {
        this.agentServiceProvider = agentServiceProvider;
    }

    /**
     * 注入阿诺哈服务提供者。
     * Inject the Anoha service provider.
     *
     * @param anohaServiceProvider 阿诺哈服务提供者 / Anoha service provider
     */
    @Autowired(required = false)
    void setAnohaServiceProvider(ObjectProvider<AnohaService> anohaServiceProvider) {
        this.anohaServiceProvider = anohaServiceProvider;
    }

    /**
     * 注入 SvS 服务提供者。
     * Inject the SvS service provider.
     *
     * @param svsServiceProvider SvS 服务提供者 / SvS service provider
     */
    @Autowired(required = false)
    void setSvsServiceProvider(ObjectProvider<SvsService> svsServiceProvider) {
        this.svsServiceProvider = svsServiceProvider;
    }

    /**
     * 注入 RvR 服务提供者。
     * Inject the RvR service provider.
     *
     * @param rvrServiceProvider RvR 服务提供者 / RvR service provider
     */
    @Autowired(required = false)
    void setRvrServiceProvider(ObjectProvider<RvrService> rvrServiceProvider) {
        this.rvrServiceProvider = rvrServiceProvider;
    }

    /**
     * 注入 IU 演唱会服务提供者。
     * Inject the IU concert service provider.
     *
     * @param iuServiceProvider IU 服务提供者 / IU service provider
     */
    @Autowired(required = false)
    void setIuServiceProvider(ObjectProvider<IuService> iuServiceProvider) {
        this.iuServiceProvider = iuServiceProvider;
    }

    /**
     * 注入噩梦马戏团服务提供者。
     * Inject the Nightmare Circus service provider.
     *
     * @param nightmareCircusServiceProvider 噩梦马戏团服务提供者 / Nightmare Circus service provider
     */
    @Autowired(required = false)
    void setNightmareCircusServiceProvider(ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider) {
        this.nightmareCircusServiceProvider = nightmareCircusServiceProvider;
    }

    /**
     * 注入动态裂隙服务提供者。
     * Inject the Dynamic Rift service provider.
     *
     * @param dynamicRiftServiceProvider 动态裂隙服务提供者 / Dynamic Rift service provider
     */
    @Autowired(required = false)
    void setDynamicRiftServiceProvider(ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider) {
        this.dynamicRiftServiceProvider = dynamicRiftServiceProvider;
    }

    /**
     * 注入副本裂隙服务提供者。
     * Inject the Instance Rift service provider.
     *
     * @param instanceRiftServiceProvider 副本裂隙服务提供者 / Instance Rift service provider
     */
    @Autowired(required = false)
    void setInstanceRiftServiceProvider(ObjectProvider<InstanceRiftService> instanceRiftServiceProvider) {
        this.instanceRiftServiceProvider = instanceRiftServiceProvider;
    }

    /**
     * 注入佐西夫战舰服务提供者。
     * Inject the Zorshiv Dredgion service provider.
     *
     * @param zorshivDredgionServiceProvider 佐西夫战舰服务提供者 / Zorshiv Dredgion service provider
     */
    @Autowired(required = false)
    void setZorshivDredgionServiceProvider(ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider) {
        this.zorshivDredgionServiceProvider = zorshivDredgionServiceProvider;
    }

    /**
     * 注入熔岩巨兽服务提供者。
     * Inject the Moltenus service provider.
     *
     * @param moltenusServiceProvider 熔岩巨兽服务提供者 / Moltenus service provider
     */
    @Autowired(required = false)
    void setMoltenusServiceProvider(ObjectProvider<MoltenusService> moltenusServiceProvider) {
        this.moltenusServiceProvider = moltenusServiceProvider;
    }

    /**
     * 注入裂隙服务提供者。
     * Inject the Rift service provider.
     *
     * @param riftServiceProvider 裂隙服务提供者 / Rift service provider
     */
    @Autowired(required = false)
    void setRiftServiceProvider(ObjectProvider<RiftService> riftServiceProvider) {
        this.riftServiceProvider = riftServiceProvider;
    }

    /**
     * 注入征服服务提供者。
     * Inject the Conquest service provider.
     *
     * @param conquestServiceProvider 征服服务提供者 / Conquest service provider
     */
    @Autowired(required = false)
    void setConquestServiceProvider(ObjectProvider<ConquestService> conquestServiceProvider) {
        this.conquestServiceProvider = conquestServiceProvider;
    }

    /**
     * 注入伊迪安深渊服务提供者。
     * Inject the Idian Depths service provider.
     *
     * @param idianDepthsServiceProvider 伊迪安深渊服务提供者 / Idian Depths service provider
     */
    @Autowired(required = false)
    void setIdianDepthsServiceProvider(ObjectProvider<IdianDepthsService> idianDepthsServiceProvider) {
        this.idianDepthsServiceProvider = idianDepthsServiceProvider;
    }

    /**
     * 注入永恒之塔服务提供者。
     * Inject the Tower of Eternity service provider.
     *
     * @param towerOfEternityServiceProvider 永恒之塔服务提供者 / Tower of Eternity service provider
     */
    @Autowired(required = false)
    void setTowerOfEternityServiceProvider(ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider) {
        this.towerOfEternityServiceProvider = towerOfEternityServiceProvider;
    }

    /**
     * 注入欧比斯登陆服务提供者。
     * Inject the Abyss Landing service provider.
     *
     * @param abyssLandingServiceProvider 欧比斯登陆服务提供者 / Abyss Landing service provider
     */
    @Autowired(required = false)
    void setAbyssLandingServiceProvider(ObjectProvider<AbyssLandingService> abyssLandingServiceProvider) {
        this.abyssLandingServiceProvider = abyssLandingServiceProvider;
    }

    /**
     * 注入登陆更新服务提供者。
     * Inject the landing update service provider.
     *
     * @param landingUpdateServiceProvider 登陆更新服务提供者 / Landing update service provider
     */
    @Autowired(required = false)
    void setLandingUpdateServiceProvider(ObjectProvider<LandingUpdateService> landingUpdateServiceProvider) {
        this.landingUpdateServiceProvider = landingUpdateServiceProvider;
    }

    /**
     * 注入欧比斯特殊登陆服务提供者。
     * Inject the Abyss Landing special service provider.
     *
     * @param abyssLandingSpecialServiceProvider 欧比斯特殊登陆服务提供者 / Abyss Landing special service provider
     */
    @Autowired(required = false)
    void setAbyssLandingSpecialServiceProvider(ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider) {
        this.abyssLandingSpecialServiceProvider = abyssLandingSpecialServiceProvider;
    }

    /**
     * 注入地点引导运行时桥接提供者。
     * Inject the LocationBootstrap runtime bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameLocationBootstrapRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 按固定顺序引导全部世界地点系统。
     * Bootstrap all world location systems in a fixed order.
     *
     * <p>执行阶段：
     * <ol>
     *   <li>攻城与战场：攻城、基地、前哨地点初始化与重置</li>
     *   <li>漩涡与世界 Boss：漩涡、贝里特拉、代理人、阿诺哈、SvS</li>
     *   <li>PvP 与 RvR：RvR、IU 演唱会、噩梦马戏团、动态裂隙</li>
     *   <li>副本与地牢：副本裂隙、佐西夫战舰、熔岩巨兽、裂隙、征服、伊迪安深渊、永恒之塔</li>
     *   <li>欧比斯与登陆：登陆地点、任务/欧比斯积分重置、特殊登陆地点</li>
     * </ol>
     * Stages:
     * <ol>
     *   <li>Siege and battlefield: siege, base, outpost locations and resets</li>
     *   <li>Vortex and world bosses: vortex, Beritra, agent, Anoha, SvS</li>
     *   <li>PvP and RvR: RvR, IU concert, Nightmare Circus, Dynamic Rift</li>
     *   <li>Instance and dungeon: Instance Rift, Zorshiv Dredgion, Moltenus, Rift, Conquest, Idian Depths, Tower of Eternity</li>
     *   <li>Abyss and landing: landing locations, quest/abyss point resets, special landing locations</li>
     * </ol>
     * </p>
     */
    public void bootstrap() {
        runtimeBridge().printSiegeAndBattlefieldLocationsSection();
        siegeService().initSiegeLocations();
        BaseService baseService = baseService();
        baseService.initBaseLocations();
        baseService.initBaseReset();
        OutpostService outpostService = outpostService();
        outpostService.initOutpostLocations();
        outpostService.initOupostReset();
        runtimeBridge().printVortexAndWorldBossLocationsSection();
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
        runtimeBridge().printPvpAndRvrLocationsSection();
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
        runtimeBridge().printInstanceAndDungeonLocationsSection();
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
        runtimeBridge().printAbyssAndLandingLocationsSection();
        abyssLandingService().initLandingLocations();
        LandingUpdateService landingUpdateService = landingUpdateService();
        landingUpdateService.initResetQuestPoints();
        landingUpdateService.initResetAbyssLandingPoints();
        abyssLandingSpecialService().initLandingSpecialLocations();
    }

    /**
     * 解析攻城服务。
     * Resolve the siege service.
     *
     * @return 攻城服务 / Siege service
     */
    private SiegeService siegeService() {
        if (siegeServiceProvider == null) {
            return runtimeBridge().siegeService();
        }
        return siegeServiceProvider.getIfAvailable(() -> runtimeBridge().siegeService());
    }

    /**
     * 解析基地服务。
     * Resolve the base service.
     *
     * @return 基地服务 / Base service
     */
    private BaseService baseService() {
        if (baseServiceProvider == null) {
            return runtimeBridge().baseService();
        }
        return baseServiceProvider.getIfAvailable(() -> runtimeBridge().baseService());
    }

    /**
     * 解析前哨服务。
     * Resolve the outpost service.
     *
     * @return 前哨服务 / Outpost service
     */
    private OutpostService outpostService() {
        if (outpostServiceProvider == null) {
            return runtimeBridge().outpostService();
        }
        return outpostServiceProvider.getIfAvailable(() -> runtimeBridge().outpostService());
    }

    /**
     * 解析漩涡服务。
     * Resolve the vortex service.
     *
     * @return 漩涡服务 / Vortex service
     */
    private VortexService vortexService() {
        if (vortexServiceProvider == null) {
            return runtimeBridge().vortexService();
        }
        return vortexServiceProvider.getIfAvailable(() -> runtimeBridge().vortexService());
    }

    /**
     * 解析贝里特拉服务。
     * Resolve the Beritra service.
     *
     * @return 贝里特拉服务 / Beritra service
     */
    private BeritraService beritraService() {
        if (beritraServiceProvider == null) {
            return runtimeBridge().beritraService();
        }
        return beritraServiceProvider.getIfAvailable(() -> runtimeBridge().beritraService());
    }

    /**
     * 解析代理人服务。
     * Resolve the agent service.
     *
     * @return 代理人服务 / Agent service
     */
    private AgentService agentService() {
        if (agentServiceProvider == null) {
            return runtimeBridge().agentService();
        }
        return agentServiceProvider.getIfAvailable(() -> runtimeBridge().agentService());
    }

    /**
     * 解析阿诺哈服务。
     * Resolve the Anoha service.
     *
     * @return 阿诺哈服务 / Anoha service
     */
    private AnohaService anohaService() {
        if (anohaServiceProvider == null) {
            return runtimeBridge().anohaService();
        }
        return anohaServiceProvider.getIfAvailable(() -> runtimeBridge().anohaService());
    }

    /**
     * 解析 SvS 服务。
     * Resolve the SvS service.
     *
     * @return SvS 服务 / SvS service
     */
    private SvsService svsService() {
        if (svsServiceProvider == null) {
            return runtimeBridge().svsService();
        }
        return svsServiceProvider.getIfAvailable(() -> runtimeBridge().svsService());
    }

    /**
     * 解析 RvR 服务。
     * Resolve the RvR service.
     *
     * @return RvR 服务 / RvR service
     */
    private RvrService rvrService() {
        if (rvrServiceProvider == null) {
            return runtimeBridge().rvrService();
        }
        return rvrServiceProvider.getIfAvailable(() -> runtimeBridge().rvrService());
    }

    /**
     * 解析 IU 演唱会服务。
     * Resolve the IU concert service.
     *
     * @return IU 服务 / IU service
     */
    private IuService iuService() {
        if (iuServiceProvider == null) {
            return runtimeBridge().iuService();
        }
        return iuServiceProvider.getIfAvailable(() -> runtimeBridge().iuService());
    }

    /**
     * 解析噩梦马戏团服务。
     * Resolve the Nightmare Circus service.
     *
     * @return 噩梦马戏团服务 / Nightmare Circus service
     */
    private NightmareCircusService nightmareCircusService() {
        if (nightmareCircusServiceProvider == null) {
            return runtimeBridge().nightmareCircusService();
        }
        return nightmareCircusServiceProvider.getIfAvailable(() -> runtimeBridge().nightmareCircusService());
    }

    /**
     * 解析动态裂隙服务。
     * Resolve the Dynamic Rift service.
     *
     * @return 动态裂隙服务 / Dynamic Rift service
     */
    private DynamicRiftService dynamicRiftService() {
        if (dynamicRiftServiceProvider == null) {
            return runtimeBridge().dynamicRiftService();
        }
        return dynamicRiftServiceProvider.getIfAvailable(() -> runtimeBridge().dynamicRiftService());
    }

    /**
     * 解析副本裂隙服务。
     * Resolve the Instance Rift service.
     *
     * @return 副本裂隙服务 / Instance Rift service
     */
    private InstanceRiftService instanceRiftService() {
        if (instanceRiftServiceProvider == null) {
            return runtimeBridge().instanceRiftService();
        }
        return instanceRiftServiceProvider.getIfAvailable(() -> runtimeBridge().instanceRiftService());
    }

    /**
     * 解析佐西夫战舰服务。
     * Resolve the Zorshiv Dredgion service.
     *
     * @return 佐西夫战舰服务 / Zorshiv Dredgion service
     */
    private ZorshivDredgionService zorshivDredgionService() {
        if (zorshivDredgionServiceProvider == null) {
            return runtimeBridge().zorshivDredgionService();
        }
        return zorshivDredgionServiceProvider.getIfAvailable(() -> runtimeBridge().zorshivDredgionService());
    }

    /**
     * 解析熔岩巨兽服务。
     * Resolve the Moltenus service.
     *
     * @return 熔岩巨兽服务 / Moltenus service
     */
    private MoltenusService moltenusService() {
        if (moltenusServiceProvider == null) {
            return runtimeBridge().moltenusService();
        }
        return moltenusServiceProvider.getIfAvailable(() -> runtimeBridge().moltenusService());
    }

    /**
     * 解析裂隙服务。
     * Resolve the Rift service.
     *
     * @return 裂隙服务 / Rift service
     */
    private RiftService riftService() {
        if (riftServiceProvider == null) {
            return runtimeBridge().riftService();
        }
        return riftServiceProvider.getIfAvailable(() -> runtimeBridge().riftService());
    }

    /**
     * 解析征服服务。
     * Resolve the Conquest service.
     *
     * @return 征服服务 / Conquest service
     */
    private ConquestService conquestService() {
        if (conquestServiceProvider == null) {
            return runtimeBridge().conquestService();
        }
        return conquestServiceProvider.getIfAvailable(() -> runtimeBridge().conquestService());
    }

    /**
     * 解析伊迪安深渊服务。
     * Resolve the Idian Depths service.
     *
     * @return 伊迪安深渊服务 / Idian Depths service
     */
    private IdianDepthsService idianDepthsService() {
        if (idianDepthsServiceProvider == null) {
            return runtimeBridge().idianDepthsService();
        }
        return idianDepthsServiceProvider.getIfAvailable(() -> runtimeBridge().idianDepthsService());
    }

    /**
     * 解析永恒之塔服务。
     * Resolve the Tower of Eternity service.
     *
     * @return 永恒之塔服务 / Tower of Eternity service
     */
    private TowerOfEternityService towerOfEternityService() {
        if (towerOfEternityServiceProvider == null) {
            return runtimeBridge().towerOfEternityService();
        }
        return towerOfEternityServiceProvider.getIfAvailable(() -> runtimeBridge().towerOfEternityService());
    }

    /**
     * 解析欧比斯登陆服务。
     * Resolve the Abyss Landing service.
     *
     * @return 欧比斯登陆服务 / Abyss Landing service
     */
    private AbyssLandingService abyssLandingService() {
        if (abyssLandingServiceProvider == null) {
            return runtimeBridge().abyssLandingService();
        }
        return abyssLandingServiceProvider.getIfAvailable(() -> runtimeBridge().abyssLandingService());
    }

    /**
     * 解析登陆更新服务。
     * Resolve the landing update service.
     *
     * @return 登陆更新服务 / Landing update service
     */
    private LandingUpdateService landingUpdateService() {
        if (landingUpdateServiceProvider == null) {
            return runtimeBridge().landingUpdateService();
        }
        return landingUpdateServiceProvider.getIfAvailable(() -> runtimeBridge().landingUpdateService());
    }

    /**
     * 解析欧比斯特殊登陆服务。
     * Resolve the Abyss Landing special service.
     *
     * @return 欧比斯特殊登陆服务 / Abyss Landing special service
     */
    private AbyssLandingSpecialService abyssLandingSpecialService() {
        if (abyssLandingSpecialServiceProvider == null) {
            return runtimeBridge().abyssLandingSpecialService();
        }
        return abyssLandingSpecialServiceProvider.getIfAvailable(() -> runtimeBridge().abyssLandingSpecialService());
    }

    /**
     * 解析地点引导运行时桥接。
     * Resolve the LocationBootstrap runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameLocationBootstrapRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameLocationBootstrapRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameLocationBootstrapRuntimeBridge::new);
    }
}
