package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
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
import org.springframework.stereotype.Component;

/**
 * 地点/副本引导运行时桥接：通过 {@link ObjectProvider} 或单例 {@code getInstance} 解析地点服务，
 * 并输出控制台分区标题。
 * LocationBootstrap runtime bridge that resolves location services via
 * {@link ObjectProvider} or singleton {@code getInstance}, and prints console section headers.
 */
@Component
public class GameLocationBootstrapRuntimeBridge {

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
     * 输出攻城与战场地点控制台分区标题。
     * Print the console section header for siege and battlefield locations.
     */
    public void printSiegeAndBattlefieldLocationsSection() {
        Util.printSection(I18n.get("console.section.locations.siege_battlefield"));
    }

    /**
     * 输出漩涡与世界 Boss 地点控制台分区标题。
     * Print the console section header for vortex and world-boss locations.
     */
    public void printVortexAndWorldBossLocationsSection() {
        Util.printSection(I18n.get("console.section.locations.vortex_world_boss"));
    }

    /**
     * 输出 PvP 与 RvR 地点控制台分区标题。
     * Print the console section header for PvP and RvR locations.
     */
    public void printPvpAndRvrLocationsSection() {
        Util.printSection(I18n.get("console.section.locations.pvp_rvr"));
    }

    /**
     * 输出副本与地牢地点控制台分区标题。
     * Print the console section header for instance and dungeon locations.
     */
    public void printInstanceAndDungeonLocationsSection() {
        Util.printSection(I18n.get("console.section.locations.instance_dungeon"));
    }

    /**
     * 输出欧比斯与登陆地点控制台分区标题。
     * Print the console section header for abyss and landing locations.
     */
    public void printAbyssAndLandingLocationsSection() {
        Util.printSection(I18n.get("console.section.locations.abyss_landing"));
    }

    /**
     * 解析攻城服务。
     * Resolve the siege service.
     *
     * @return 攻城服务 / Siege service
     */
    public SiegeService siegeService() {
        return getIfAvailable(siegeServiceProvider, SiegeService::getInstance);
    }

    /**
     * 解析基地服务。
     * Resolve the base service.
     *
     * @return 基地服务 / Base service
     */
    public BaseService baseService() {
        return getIfAvailable(baseServiceProvider, BaseService::getInstance);
    }

    /**
     * 解析前哨服务。
     * Resolve the outpost service.
     *
     * @return 前哨服务 / Outpost service
     */
    public OutpostService outpostService() {
        return getIfAvailable(outpostServiceProvider, OutpostService::getInstance);
    }

    /**
     * 解析漩涡服务。
     * Resolve the vortex service.
     *
     * @return 漩涡服务 / Vortex service
     */
    public VortexService vortexService() {
        return getIfAvailable(vortexServiceProvider, VortexService::getInstance);
    }

    /**
     * 解析贝里特拉服务。
     * Resolve the Beritra service.
     *
     * @return 贝里特拉服务 / Beritra service
     */
    public BeritraService beritraService() {
        return getIfAvailable(beritraServiceProvider, BeritraService::getInstance);
    }

    /**
     * 解析代理人服务。
     * Resolve the agent service.
     *
     * @return 代理人服务 / Agent service
     */
    public AgentService agentService() {
        return getIfAvailable(agentServiceProvider, AgentService::getInstance);
    }

    /**
     * 解析阿诺哈服务。
     * Resolve the Anoha service.
     *
     * @return 阿诺哈服务 / Anoha service
     */
    public AnohaService anohaService() {
        return getIfAvailable(anohaServiceProvider, AnohaService::getInstance);
    }

    /**
     * 解析 SvS 服务。
     * Resolve the SvS service.
     *
     * @return SvS 服务 / SvS service
     */
    public SvsService svsService() {
        return getIfAvailable(svsServiceProvider, SvsService::getInstance);
    }

    /**
     * 解析 RvR 服务。
     * Resolve the RvR service.
     *
     * @return RvR 服务 / RvR service
     */
    public RvrService rvrService() {
        return getIfAvailable(rvrServiceProvider, RvrService::getInstance);
    }

    /**
     * 解析 IU 演唱会服务。
     * Resolve the IU concert service.
     *
     * @return IU 服务 / IU service
     */
    public IuService iuService() {
        return getIfAvailable(iuServiceProvider, IuService::getInstance);
    }

    /**
     * 解析噩梦马戏团服务。
     * Resolve the Nightmare Circus service.
     *
     * @return 噩梦马戏团服务 / Nightmare Circus service
     */
    public NightmareCircusService nightmareCircusService() {
        return getIfAvailable(nightmareCircusServiceProvider, NightmareCircusService::getInstance);
    }

    /**
     * 解析动态裂隙服务。
     * Resolve the Dynamic Rift service.
     *
     * @return 动态裂隙服务 / Dynamic Rift service
     */
    public DynamicRiftService dynamicRiftService() {
        return getIfAvailable(dynamicRiftServiceProvider, DynamicRiftService::getInstance);
    }

    /**
     * 解析副本裂隙服务。
     * Resolve the Instance Rift service.
     *
     * @return 副本裂隙服务 / Instance Rift service
     */
    public InstanceRiftService instanceRiftService() {
        return getIfAvailable(instanceRiftServiceProvider, InstanceRiftService::getInstance);
    }

    /**
     * 解析佐西夫战舰服务。
     * Resolve the Zorshiv Dredgion service.
     *
     * @return 佐西夫战舰服务 / Zorshiv Dredgion service
     */
    public ZorshivDredgionService zorshivDredgionService() {
        return getIfAvailable(zorshivDredgionServiceProvider, ZorshivDredgionService::getInstance);
    }

    /**
     * 解析熔岩巨兽服务。
     * Resolve the Moltenus service.
     *
     * @return 熔岩巨兽服务 / Moltenus service
     */
    public MoltenusService moltenusService() {
        return getIfAvailable(moltenusServiceProvider, MoltenusService::getInstance);
    }

    /**
     * 解析裂隙服务。
     * Resolve the Rift service.
     *
     * @return 裂隙服务 / Rift service
     */
    public RiftService riftService() {
        return getIfAvailable(riftServiceProvider, RiftService::getInstance);
    }

    /**
     * 解析征服服务。
     * Resolve the Conquest service.
     *
     * @return 征服服务 / Conquest service
     */
    public ConquestService conquestService() {
        return getIfAvailable(conquestServiceProvider, ConquestService::getInstance);
    }

    /**
     * 解析伊迪安深渊服务。
     * Resolve the Idian Depths service.
     *
     * @return 伊迪安深渊服务 / Idian Depths service
     */
    public IdianDepthsService idianDepthsService() {
        return getIfAvailable(idianDepthsServiceProvider, IdianDepthsService::getInstance);
    }

    /**
     * 解析永恒之塔服务。
     * Resolve the Tower of Eternity service.
     *
     * @return 永恒之塔服务 / Tower of Eternity service
     */
    public TowerOfEternityService towerOfEternityService() {
        return getIfAvailable(towerOfEternityServiceProvider, TowerOfEternityService::getInstance);
    }

    /**
     * 解析欧比斯登陆服务。
     * Resolve the Abyss Landing service.
     *
     * @return 欧比斯登陆服务 / Abyss Landing service
     */
    public AbyssLandingService abyssLandingService() {
        return getIfAvailable(abyssLandingServiceProvider, AbyssLandingService::getInstance);
    }

    /**
     * 解析登陆更新服务。
     * Resolve the landing update service.
     *
     * @return 登陆更新服务 / Landing update service
     */
    public LandingUpdateService landingUpdateService() {
        return getIfAvailable(landingUpdateServiceProvider, LandingUpdateService::getInstance);
    }

    /**
     * 解析欧比斯特殊登陆服务。
     * Resolve the Abyss Landing special service.
     *
     * @return 欧比斯特殊登陆服务 / Abyss Landing special service
     */
    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return getIfAvailable(abyssLandingSpecialServiceProvider, AbyssLandingSpecialService::getInstance);
    }

    /**
     * 在 {@link ObjectProvider} 可用时取 Bean，否则走单例回退。
     * Return the bean from {@link ObjectProvider} when available, otherwise use the singleton fallback.
     *
     * @param provider 可选提供者 / Optional provider
     * @param fallback 单例回退供应器 / Singleton fallback supplier
     * @param <T> 服务类型 / Service type
     * @return 服务实例 / Service instance
     */
    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }
}
