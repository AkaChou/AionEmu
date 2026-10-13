package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 核心玩法服务静态绑定：把 Spring ObjectProvider 喂入 8 个域服务自持的 fail-fast 单例通道。
 * Core gameplay static bindings: feeds the Spring ObjectProviders into the fail-fast singleton
 * accessors held by the eight domain services.
 * <p>本组件取代已退役的核心玩法服务静态定位器（R1 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道（静态宿主）或构造注入（Spring 域服务）。
 * Replaces the retired core-gameplay static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel or constructor injection.</p>
 */
@Component
public final class CoreGameplayServiceBindings implements DisposableBean {

	/**
	 * 将各核心玩法服务的 Spring 提供者注册到域服务单例通道。
	 * Registers each core gameplay service's Spring provider with its domain-service singleton channel.
	 *
	 * @param dropServiceProvider 掉落服务提供者 / Drop service provider
	 * @param mailServiceProvider 邮件服务提供者 / Mail service provider
	 * @param pvpServiceProvider PvP 服务提供者 / PvP service provider
	 * @param autoGroupServiceProvider 自动组队服务提供者 / Auto-group service provider
	 * @param abyssRankingCacheProvider 欧比斯排名缓存提供者 / Abyss ranking cache provider
	 * @param legionServiceProvider 军团服务提供者 / Legion service provider
	 * @param balaurAssaultServiceProvider 龙族袭击服务提供者 / Balaur assault service provider
	 * @param battlefieldUnionServiceProvider 战场联盟服务提供者 / Battlefield union service provider
	 */
	public CoreGameplayServiceBindings(ObjectProvider<DropService> dropServiceProvider,
			ObjectProvider<MailService> mailServiceProvider,
			ObjectProvider<PvpService> pvpServiceProvider,
			ObjectProvider<AutoGroupService> autoGroupServiceProvider,
			ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider,
			ObjectProvider<LegionService> legionServiceProvider,
			ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider,
			ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider) {
		DropService.setInstanceProvider(dropServiceProvider);
		MailService.setInstanceProvider(mailServiceProvider);
		PvpService.setInstanceProvider(pvpServiceProvider);
		AutoGroupService.setInstanceProvider(autoGroupServiceProvider);
		AbyssRankingCache.setInstanceProvider(abyssRankingCacheProvider);
		LegionService.setInstanceProvider(legionServiceProvider);
		BalaurAssaultService.setInstanceProvider(balaurAssaultServiceProvider);
		BattlefieldUnionService.setInstanceProvider(battlefieldUnionServiceProvider);
	}

	/**
	 * 销毁时清空各域服务的静态提供者通道。
	 * Clears each domain service's static provider channel on destroy.
	 */
	@Override
	public void destroy() {
		DropService.setInstanceProvider(null);
		MailService.setInstanceProvider(null);
		PvpService.setInstanceProvider(null);
		AutoGroupService.setInstanceProvider(null);
		AbyssRankingCache.setInstanceProvider(null);
		LegionService.setInstanceProvider(null);
		BalaurAssaultService.setInstanceProvider(null);
		BattlefieldUnionService.setInstanceProvider(null);
	}
}
