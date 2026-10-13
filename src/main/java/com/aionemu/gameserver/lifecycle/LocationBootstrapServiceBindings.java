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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 位置服务静态绑定：把 Spring ObjectProvider 喂入 22 个域服务自持的 fail-fast 单例通道。
 * Location service static bindings: feeds the Spring ObjectProviders into the fail-fast singleton
 * accessors held by the twenty-two domain services.
 * <p>本组件取代已退役的位置服务静态定位器（R2 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道（静态宿主）或构造注入（Spring 域服务）。
 * Replaces the retired location-service static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel or constructor injection.</p>
 */
@Component
public final class LocationBootstrapServiceBindings implements DisposableBean {

	/**
	 * 将各位置服务的 Spring 提供者注册到域服务单例通道。
	 * Registers each location service's Spring provider with its domain-service singleton channel.
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
	public LocationBootstrapServiceBindings(ObjectProvider<VortexService> vortexServiceProvider,
			ObjectProvider<BeritraService> beritraServiceProvider,
			ObjectProvider<AgentService> agentServiceProvider,
			ObjectProvider<AnohaService> anohaServiceProvider,
			ObjectProvider<SvsService> svsServiceProvider,
			ObjectProvider<RvrService> rvrServiceProvider,
			ObjectProvider<IuService> iuServiceProvider,
			ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider,
			ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider,
			ObjectProvider<InstanceRiftService> instanceRiftServiceProvider,
			ObjectProvider<SiegeService> siegeServiceProvider,
			ObjectProvider<BaseService> baseServiceProvider,
			ObjectProvider<OutpostService> outpostServiceProvider,
			ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider,
			ObjectProvider<MoltenusService> moltenusServiceProvider,
			ObjectProvider<RiftService> riftServiceProvider,
			ObjectProvider<ConquestService> conquestServiceProvider,
			ObjectProvider<IdianDepthsService> idianDepthsServiceProvider,
			ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider,
			ObjectProvider<AbyssLandingService> abyssLandingServiceProvider,
			ObjectProvider<LandingUpdateService> landingUpdateServiceProvider,
			ObjectProvider<AbyssLandingSpecialService> abyssLandingSpecialServiceProvider) {
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
		AbyssLandingService.setInstanceProvider(abyssLandingServiceProvider);
		LandingUpdateService.setInstanceProvider(landingUpdateServiceProvider);
		AbyssLandingSpecialService.setInstanceProvider(abyssLandingSpecialServiceProvider);
	}

	/**
	 * 销毁时清空各域服务的静态提供者通道。
	 * Clears each domain service's static provider channel on destroy.
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
		AbyssLandingService.setInstanceProvider(null);
		LandingUpdateService.setInstanceProvider(null);
		AbyssLandingSpecialService.setInstanceProvider(null);
	}
}
