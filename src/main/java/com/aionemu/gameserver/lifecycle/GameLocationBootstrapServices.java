package com.aionemu.gameserver.lifecycle;

import java.util.function.Supplier;

import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.AbyssLandingService;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 地点/副本引导服务注册表：将 Spring {@link ObjectProvider} 写入静态字段，
 * 并为各地点服务调用 {@code setInstanceProvider}；销毁时清空。
 * LocationBootstrap services registry that stores Spring {@link ObjectProvider}s
 * in static fields, wires {@code setInstanceProvider} on location services,
 * and clears them on destroy.
 *
 * <p>实现 {@link DisposableBean}，容器关闭时清理静态引用，避免泄漏。
 * Implements {@link DisposableBean} so static references are cleared when the container shuts down.</p>
 */
@Component
public final class GameLocationBootstrapServices implements DisposableBean {

    /**
     * 漩涡服务提供者。
     * Vortex service provider.
     */
    private static volatile ObjectProvider<VortexService> vortexServiceProvider;

    /**
     * 贝里特拉入侵服务提供者。
     * Beritra service provider.
     */
    private static volatile ObjectProvider<BeritraService> beritraServiceProvider;

    /**
     * 代理人服务提供者。
     * Agent service provider.
     */
    private static volatile ObjectProvider<AgentService> agentServiceProvider;

    /**
     * 阿诺哈服务提供者。
     * Anoha service provider.
     */
    private static volatile ObjectProvider<AnohaService> anohaServiceProvider;

    /**
     * SvS 服务提供者。
     * SvS service provider.
     */
    private static volatile ObjectProvider<SvsService> svsServiceProvider;

    /**
     * RvR 服务提供者。
     * RvR service provider.
     */
    private static volatile ObjectProvider<RvrService> rvrServiceProvider;

    /**
     * IU 演唱会服务提供者。
     * IU concert service provider.
     */
    private static volatile ObjectProvider<IuService> iuServiceProvider;

    /**
     * 噩梦马戏团服务提供者。
     * Nightmare Circus service provider.
     */
    private static volatile ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider;

    /**
     * 动态裂隙服务提供者。
     * Dynamic Rift service provider.
     */
    private static volatile ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider;

    /**
     * 副本裂隙服务提供者。
     * Instance Rift service provider.
     */
    private static volatile ObjectProvider<InstanceRiftService> instanceRiftServiceProvider;

    /**
     * 前哨服务提供者。
     * Outpost service provider.
     */
    private static volatile ObjectProvider<OutpostService> outpostServiceProvider;

    /**
     * 佐西夫战舰服务提供者。
     * Zorshiv Dredgion service provider.
     */
    private static volatile ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider;

    /**
     * 熔岩巨兽服务提供者。
     * Moltenus service provider.
     */
    private static volatile ObjectProvider<MoltenusService> moltenusServiceProvider;

    /**
     * 裂隙服务提供者。
     * Rift service provider.
     */
    private static volatile ObjectProvider<RiftService> riftServiceProvider;

    /**
     * 征服服务提供者。
     * Conquest service provider.
     */
    private static volatile ObjectProvider<ConquestService> conquestServiceProvider;

    /**
     * 伊迪安深渊服务提供者。
     * Idian Depths service provider.
     */
    private static volatile ObjectProvider<IdianDepthsService> idianDepthsServiceProvider;

    /**
     * 永恒之塔服务提供者。
     * Tower of Eternity service provider.
     */
    private static volatile ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider;

    /**
     * 欧比斯登陆服务提供者。
     * Abyss Landing service provider.
     */
    private static volatile ObjectProvider<AbyssLandingService> abyssLandingServiceProvider;

    /**
     * 欧比斯特殊登陆服务提供者。
     * Abyss Landing special service provider.
     */
    private static volatile ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider;

    /**
     * 构造并注册全部地点服务的 {@link ObjectProvider}，同时调用各服务的 {@code setInstanceProvider}。
     * Construct and register {@link ObjectProvider}s for all location services,
     * and call each service's {@code setInstanceProvider}.
     *
     * <p>参数为各地点相关服务的 Spring 提供者，包括：漩涡、贝里特拉、代理人、阿诺哈、SvS、RvR、
     * IU、噩梦马戏团、动态/副本裂隙、攻城、基地、前哨、佐西夫战舰、熔岩巨兽、裂隙、征服、
     * 伊迪安深渊、永恒之塔、欧比斯登陆、登陆更新与特殊登陆。
     * Parameters are Spring providers for location-related services including:
     * Vortex, Beritra, Agent, Anoha, SvS, RvR, IU, Nightmare Circus, Dynamic/Instance Rift,
     * Siege, Base, Outpost, Zorshiv Dredgion, Moltenus, Rift, Conquest, Idian Depths,
     * Tower of Eternity, Abyss Landing, Landing Update, and Abyss Landing Special.</p>
     *
     * @param vortexServiceProvider 漩涡服务提供者 / Vortex service provider
     * @param beritraServiceProvider 贝里特拉服务提供者 / Beritra service provider
     * @param agentServiceProvider 代理人服务提供者 / Agent service provider
     * @param anohaServiceProvider 阿诺哈服务提供者 / Anoha service provider
     * @param svsServiceProvider SvS 服务提供者 / SvS service provider
     * @param rvrServiceProvider RvR 服务提供者 / RvR service provider
     * @param iuServiceProvider IU 服务提供者 / IU service provider
     * @param nightmareCircusServiceProvider 噩梦马戏团服务提供者 / Nightmare Circus service provider
     * @param dynamicRiftServiceProvider 动态裂隙服务提供者 / Dynamic Rift service provider
     * @param instanceRiftServiceProvider 副本裂隙服务提供者 / Instance Rift service provider
     * @param siegeServiceProvider 攻城服务提供者 / Siege service provider
     * @param baseServiceProvider 基地服务提供者 / Base service provider
     * @param outpostServiceProvider 前哨服务提供者 / Outpost service provider
     * @param zorshivDredgionServiceProvider 佐西夫战舰服务提供者 / Zorshiv Dredgion service provider
     * @param moltenusServiceProvider 熔岩巨兽服务提供者 / Moltenus service provider
     * @param riftServiceProvider 裂隙服务提供者 / Rift service provider
     * @param conquestServiceProvider 征服服务提供者 / Conquest service provider
     * @param idianDepthsServiceProvider 伊迪安深渊服务提供者 / Idian Depths service provider
     * @param towerOfEternityServiceProvider 永恒之塔服务提供者 / Tower of Eternity service provider
     * @param abyssLandingServiceProvider 欧比斯登陆服务提供者 / Abyss Landing service provider
     * @param landingUpdateServiceProvider 登陆更新服务提供者 / Landing update service provider
     * @param abyssLandingSpecialServiceProvider 欧比斯特殊登陆服务提供者 / Abyss Landing special service provider
     */
    public GameLocationBootstrapServices(ObjectProvider<VortexService> vortexServiceProvider,
            ObjectProvider<BeritraService> beritraServiceProvider, ObjectProvider<AgentService> agentServiceProvider,
            ObjectProvider<AnohaService> anohaServiceProvider, ObjectProvider<SvsService> svsServiceProvider,
            ObjectProvider<RvrService> rvrServiceProvider, ObjectProvider<IuService> iuServiceProvider,
            ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider,
            ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider,
            ObjectProvider<InstanceRiftService> instanceRiftServiceProvider,
            ObjectProvider<SiegeService> siegeServiceProvider, ObjectProvider<BaseService> baseServiceProvider,
            ObjectProvider<OutpostService> outpostServiceProvider,
            ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider,
            ObjectProvider<MoltenusService> moltenusServiceProvider, ObjectProvider<RiftService> riftServiceProvider,
            ObjectProvider<ConquestService> conquestServiceProvider,
            ObjectProvider<IdianDepthsService> idianDepthsServiceProvider,
            ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider,
            ObjectProvider<AbyssLandingService> abyssLandingServiceProvider,
            ObjectProvider<LandingUpdateService> landingUpdateServiceProvider,
            ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider) {
        GameLocationBootstrapServices.vortexServiceProvider = vortexServiceProvider;
        GameLocationBootstrapServices.beritraServiceProvider = beritraServiceProvider;
        GameLocationBootstrapServices.agentServiceProvider = agentServiceProvider;
        GameLocationBootstrapServices.anohaServiceProvider = anohaServiceProvider;
        GameLocationBootstrapServices.svsServiceProvider = svsServiceProvider;
        GameLocationBootstrapServices.rvrServiceProvider = rvrServiceProvider;
        GameLocationBootstrapServices.iuServiceProvider = iuServiceProvider;
        GameLocationBootstrapServices.nightmareCircusServiceProvider = nightmareCircusServiceProvider;
        GameLocationBootstrapServices.dynamicRiftServiceProvider = dynamicRiftServiceProvider;
        GameLocationBootstrapServices.instanceRiftServiceProvider = instanceRiftServiceProvider;
        GameLocationBootstrapServices.outpostServiceProvider = outpostServiceProvider;
        GameLocationBootstrapServices.zorshivDredgionServiceProvider = zorshivDredgionServiceProvider;
        GameLocationBootstrapServices.moltenusServiceProvider = moltenusServiceProvider;
        GameLocationBootstrapServices.riftServiceProvider = riftServiceProvider;
        GameLocationBootstrapServices.conquestServiceProvider = conquestServiceProvider;
        GameLocationBootstrapServices.idianDepthsServiceProvider = idianDepthsServiceProvider;
        GameLocationBootstrapServices.towerOfEternityServiceProvider = towerOfEternityServiceProvider;
        GameLocationBootstrapServices.abyssLandingSpecialServiceProvider = abyssLandingSpecialServiceProvider;
        VortexService.setInstanceProvider(vortexServiceProvider);
        BeritraService.setInstanceProvider(beritraServiceProvider);
        AgentService.setInstanceProvider(agentServiceProvider);
        AnohaService.setInstanceProvider(anohaServiceProvider);
        SvsService.setInstanceProvider(svsServiceProvider);
        RvrService.setInstanceProvider(rvrServiceProvider);
        IuService.setInstanceProvider(iuServiceProvider);
        NightmareCircusService.setInstanceProvider(nightmareCircusServiceProvider);
        DynamicRiftService.setInstanceProvider(dynamicRiftServiceProvider);
        InstanceRiftService.setInstanceProvider(instanceRiftServiceProvider);
        SiegeService.setInstanceProvider(siegeServiceProvider);
        BaseService.setInstanceProvider(baseServiceProvider);
        OutpostService.setInstanceProvider(outpostServiceProvider);
        ZorshivDredgionService.setInstanceProvider(zorshivDredgionServiceProvider);
        MoltenusService.setInstanceProvider(moltenusServiceProvider);
        RiftService.setInstanceProvider(riftServiceProvider);
        ConquestService.setInstanceProvider(conquestServiceProvider);
        IdianDepthsService.setInstanceProvider(idianDepthsServiceProvider);
        TowerOfEternityService.setInstanceProvider(towerOfEternityServiceProvider);
        GameLocationBootstrapServices.abyssLandingServiceProvider = abyssLandingServiceProvider;
        AbyssLandingService.setInstanceProvider(abyssLandingServiceProvider);
        LandingUpdateService.setInstanceProvider(landingUpdateServiceProvider);
        AbyssLandingSpecialService.setInstanceProvider(abyssLandingSpecialServiceProvider);
    }

    /**
     * 获取漩涡服务。
     * Obtain the vortex service.
     *
     * Vortex service
     */
    public static VortexService vortexService() {
        return getIfAvailable(vortexServiceProvider, VortexService::getInstance);
    }

    /**
     * 获取贝里特拉服务。
     * Obtain the Beritra service.
     *
     * @return 贝里特拉服务 / Beritra service
     */
    public static BeritraService beritraService() {
        return getIfAvailable(beritraServiceProvider, BeritraService::getInstance);
    }

    /**
     * 获取代理人服务。
     * Obtain the agent service.
     *
     * @return 代理人服务 / Agent service
     */
    public static AgentService agentService() {
        return getIfAvailable(agentServiceProvider, AgentService::getInstance);
    }

    /**
     * 获取阿诺哈服务。
     * Obtain the Anoha service.
     *
     * @return 阿诺哈服务 / Anoha service
     */
    public static AnohaService anohaService() {
        return getIfAvailable(anohaServiceProvider, AnohaService::getInstance);
    }

    /**
     * 获取 SvS 服务。
     * Obtain the SvS service.
     *
     * SvS service
     */
    public static SvsService svsService() {
        return getIfAvailable(svsServiceProvider, SvsService::getInstance);
    }

    /**
     * 获取 RvR 服务。
     * Obtain the RvR service.
     *
     * RvR service
     */
    public static RvrService rvrService() {
        return getIfAvailable(rvrServiceProvider, RvrService::getInstance);
    }

    /**
     * 获取 IU 演唱会服务。
     * Obtain the IU concert service.
     *
     * IU service
     */
    public static IuService iuService() {
        return getIfAvailable(iuServiceProvider, IuService::getInstance);
    }

    /**
     * 获取噩梦马戏团服务。
     * Obtain the Nightmare Circus service.
     *
     * @return 噩梦马戏团服务 / Nightmare Circus service
     */
    public static NightmareCircusService nightmareCircusService() {
        return getIfAvailable(nightmareCircusServiceProvider, NightmareCircusService::getInstance);
    }

    /**
     * 获取动态裂隙服务。
     * Obtain the Dynamic Rift service.
     *
     * @return 动态裂隙服务 / Dynamic Rift service
     */
    public static DynamicRiftService dynamicRiftService() {
        return getIfAvailable(dynamicRiftServiceProvider, DynamicRiftService::getInstance);
    }

    /**
     * 获取副本裂隙服务。
     * Obtain the Instance Rift service.
     *
     * @return 副本裂隙服务 / Instance Rift service
     */
    public static InstanceRiftService instanceRiftService() {
        return getIfAvailable(instanceRiftServiceProvider, InstanceRiftService::getInstance);
    }

    /**
     * 获取前哨服务。
     * Obtain the outpost service.
     *
     * Outpost service
     */
    public static OutpostService outpostService() {
        return getIfAvailable(outpostServiceProvider, OutpostService::getInstance);
    }

    /**
     * 获取佐西夫战舰服务。
     * Obtain the Zorshiv Dredgion service.
     *
     * @return 佐西夫战舰服务 / Zorshiv Dredgion service
     */
    public static ZorshivDredgionService zorshivDredgionService() {
        return getIfAvailable(zorshivDredgionServiceProvider, ZorshivDredgionService::getInstance);
    }

    /**
     * 获取熔岩巨兽服务。
     * Obtain the Moltenus service.
     *
     * @return 熔岩巨兽服务 / Moltenus service
     */
    public static MoltenusService moltenusService() {
        return getIfAvailable(moltenusServiceProvider, MoltenusService::getInstance);
    }

    /**
     * 获取裂隙服务。
     * Obtain the Rift service.
     *
     * Rift service
     */
    public static RiftService riftService() {
        return getIfAvailable(riftServiceProvider, RiftService::getInstance);
    }

    /**
     * 获取征服服务。
     * Obtain the Conquest service.
     *
     * Conquest service
     */
    public static ConquestService conquestService() {
        return getIfAvailable(conquestServiceProvider, ConquestService::getInstance);
    }

    /**
     * 获取伊迪安深渊服务。
     * Obtain the Idian Depths service.
     *
     * @return 伊迪安深渊服务 / Idian Depths service
     */
    public static IdianDepthsService idianDepthsService() {
        return getIfAvailable(idianDepthsServiceProvider, IdianDepthsService::getInstance);
    }

    /**
     * 获取永恒之塔服务。
     * Obtain the Tower of Eternity service.
     *
     * @return 永恒之塔服务 / Tower of Eternity service
     */
    public static TowerOfEternityService towerOfEternityService() {
        return getIfAvailable(towerOfEternityServiceProvider, TowerOfEternityService::getInstance);
    }

    /**
     * 获取欧比斯登陆服务。
     * Obtain the Abyss Landing service.
     *
     * @return 欧比斯登陆服务 / Abyss Landing service
     */
    public static AbyssLandingService abyssLandingService() {
        return getIfAvailable(abyssLandingServiceProvider, AbyssLandingService::getInstance);
    }

    /**
     * 获取欧比斯特殊登陆服务。
     * Obtain the Abyss Landing special service.
     *
     * @return 欧比斯特殊登陆服务 / Abyss Landing special service
     */
    public static AbyssLandingSpecialService abyssLandingSpecialService() {
        return getIfAvailable(abyssLandingSpecialServiceProvider, AbyssLandingSpecialService::getInstance);
    }

    /**
     * 在 {@link ObjectProvider} 可用时取 Bean，否则走单例回退。
     * Return the bean from {@link ObjectProvider} when available, otherwise use the singleton fallback.
     *
     * @param provider 可选提供者 / Optional provider
     * @param fallback 单例回退供应器 / Singleton fallback supplier
     * @param <T> 服务类型 / Service type
     * Service instance
     */
    private static <T> T getIfAvailable(ObjectProvider<T> provider, Supplier<T> fallback) {
        if (provider == null) {
            return fallback.get();
        }
        return provider.getIfAvailable(fallback);
    }

    /**
     * 容器销毁时清空各地点服务的 {@code setInstanceProvider} 与静态 {@link ObjectProvider} 引用。
     * Clear each location service's {@code setInstanceProvider} and static {@link ObjectProvider}
     * references when the container is destroyed.
     */
    @Override
    public void destroy() {
        VortexService.setInstanceProvider(null);
        BeritraService.setInstanceProvider(null);
        AgentService.setInstanceProvider(null);
        AnohaService.setInstanceProvider(null);
        SvsService.setInstanceProvider(null);
        RvrService.setInstanceProvider(null);
        IuService.setInstanceProvider(null);
        NightmareCircusService.setInstanceProvider(null);
        DynamicRiftService.setInstanceProvider(null);
        InstanceRiftService.setInstanceProvider(null);
        SiegeService.setInstanceProvider(null);
        BaseService.setInstanceProvider(null);
        OutpostService.setInstanceProvider(null);
        ZorshivDredgionService.setInstanceProvider(null);
        MoltenusService.setInstanceProvider(null);
        RiftService.setInstanceProvider(null);
        ConquestService.setInstanceProvider(null);
        IdianDepthsService.setInstanceProvider(null);
        TowerOfEternityService.setInstanceProvider(null);
        vortexServiceProvider = null;
        beritraServiceProvider = null;
        agentServiceProvider = null;
        anohaServiceProvider = null;
        svsServiceProvider = null;
        rvrServiceProvider = null;
        iuServiceProvider = null;
        nightmareCircusServiceProvider = null;
        dynamicRiftServiceProvider = null;
        instanceRiftServiceProvider = null;
        outpostServiceProvider = null;
        zorshivDredgionServiceProvider = null;
        moltenusServiceProvider = null;
        riftServiceProvider = null;
        conquestServiceProvider = null;
        idianDepthsServiceProvider = null;
        towerOfEternityServiceProvider = null;
        abyssLandingServiceProvider = null;
        abyssLandingSpecialServiceProvider = null;
        AbyssLandingService.setInstanceProvider(null);
        LandingUpdateService.setInstanceProvider(null);
        AbyssLandingSpecialService.setInstanceProvider(null);
    }
}
