package com.aionemu.gameserver.configs;


import com.aionemu.boot.i18n.I18n;
import java.io.File;
import java.util.Objects;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.configs.CommonsConfig;
import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.utils.PropertiesUtils;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.AStationConfig;
import com.aionemu.gameserver.configs.main.AbyssLandingConfig;
import com.aionemu.gameserver.configs.main.AdvCustomConfig;
import com.aionemu.gameserver.configs.main.ArchDaevaConfig;
import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.BrokerConfig;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.CleaningConfig;
import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.DropConfig;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.FallDamageConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.HTMLConfig;
import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.configs.main.InGameShopConfig;
import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.configs.main.NameConfig;
import com.aionemu.gameserver.configs.main.PeriodicSaveConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import com.aionemu.gameserver.configs.main.PricesConfig;
import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.configs.main.PvPConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.main.TransportConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.configs.network.IPConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.services.instance.InstanceScaler;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameMaintenanceServices;
import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;

/**
 * 游戏服配置加载与热更新入口，聚合 administration/main/network 等配置类。
 * Gameserver config load/reload entry aggregating administration, main and network config classes.
 */
@Slf4j
public class Config {
	/**
	 * 启动期属性覆盖。
	 * Boot-time property overrides.
	 */
	private static volatile Properties bootOverrides = new Properties();
	/**
	 * 配置目录路径。
	 * Config directory path.
	 *
	 * config directory
	 */
	private static String configDir() {
		return Objects.requireNonNull(System.getProperty("aion.config.dir"), "aion.config.dir is not configured");
	}

	public static void setBootOverrides(Properties properties) {
		Properties copy = new Properties();
		if (properties != null) {
			copy.putAll(properties);
		}
		bootOverrides = copy;
	}

	public static String bootOverride(String key) {
		return bootOverrides.getProperty(key);
	}

	public static File configFile(String relativePath) {
		return new File(configDir(), relativePath);
	}

	public static File dataFile(String relativePath) {
		String dataDir = System.getProperty("aion.game.data.dir");
		if (dataDir == null) {
			return new File(relativePath);
		}
		return new File(dataDir, stripDataPrefix(relativePath));
	}

	public static File definitionFile(String relativePath) {
		String definitionsDir = System.getProperty("aion.game.definitions.dir");
		if (definitionsDir == null) {
			String dataDir = System.getProperty("aion.game.data.dir");
			File parent = dataDir == null ? null : new File(dataDir).getParentFile();
			definitionsDir = new File(parent == null ? new File(".") : parent, "definitions").getPath();
		}
		String normalized = relativePath.replace('\\', '/');
		if (normalized.startsWith("./definitions/")) {
			normalized = normalized.substring("./definitions/".length());
		} else if (normalized.startsWith("definitions/")) {
			normalized = normalized.substring("definitions/".length());
		}
		return new File(definitionsDir, normalized);
	}

	public static File geoFile(String relativePath) {
		String geoDir = System.getProperty("aion.game.geo.dir");
		if (geoDir == null) {
			return dataFile(relativePath);
		}
		return new File(geoDir, stripGeoPrefix(relativePath));
	}

	public static File cacheFile(String relativePath) {
		String cacheDir = System.getProperty("aion.game.cache.dir");
		if (cacheDir == null) {
			return new File(relativePath);
		}
		return new File(cacheDir, stripCachePrefix(relativePath));
	}

	private static String stripDataPrefix(String path) {
		String normalized = path.replace('\\', '/');
		if (normalized.startsWith("./data/")) {
			return normalized.substring("./data/".length());
		}
		if (normalized.startsWith("data/")) {
			return normalized.substring("data/".length());
		}
		return normalized;
	}

	private static String stripGeoPrefix(String path) {
		String normalized = path.replace('\\', '/');
		if (normalized.startsWith("./geo/")) {
			return normalized.substring("./geo/".length());
		}
		if (normalized.startsWith("geo/")) {
			return normalized.substring("geo/".length());
		}
		return normalized;
	}

	private static String stripCachePrefix(String path) {
		String normalized = path.replace('\\', '/');
		if (normalized.startsWith("./cache/")) {
			return normalized.substring("./cache/".length());
		}
		if (normalized.startsWith("cache/")) {
			return normalized.substring("cache/".length());
		}
		return normalized;
	}

	private static void overrideRuntimeProperties(Properties[] targetProperties, Properties fileOverrides) {
		PropertiesUtils.overrideProperties(targetProperties, fileOverrides);
		PropertiesUtils.overrideProperties(targetProperties, bootOverrides);
	}

	/**
	 * 加载全部游戏服配置。
	 * Loads all gameserver configuration.
	 */
	public static void load() {
		try {
			Properties myProps = null;
			try {
				log.info(I18n.get("log.5685ceb915cb"));
				myProps = PropertiesUtils.load(configDir() + "/mygs.properties");
			} catch (Exception e) {
				log.info(I18n.get("log.c453f21e95f6"));
			}
			String administration = configDir() + "/administration";
			Properties[] adminProps = PropertiesUtils.loadAllFromDirectory(administration);
			overrideRuntimeProperties(adminProps, myProps);
			ConfigurableProcessor.process(AdminConfig.class, adminProps);
			ConfigurableProcessor.process(DeveloperConfig.class, adminProps);
			ConfigurableProcessor.process(PanelConfig.class, adminProps);
			String main = configDir() + "/main";
			Properties[] mainProps = PropertiesUtils.loadAllFromDirectory(main);
			overrideRuntimeProperties(mainProps, myProps);
			ConfigurableProcessor.process(AIConfig.class, mainProps);
			ConfigurableProcessor.process(BrokerConfig.class, mainProps);
			ConfigurableProcessor.process(CommonsConfig.class, mainProps);
			ConfigurableProcessor.process(CacheConfig.class, mainProps);
			ConfigurableProcessor.process(CleaningConfig.class, mainProps);
			ConfigurableProcessor.process(CraftConfig.class, mainProps);
			ConfigurableProcessor.process(CustomConfig.class, mainProps);
			ConfigurableProcessor.process(DropConfig.class, mainProps);
			ConfigurableProcessor.process(EnchantsConfig.class, mainProps);
			ConfigurableProcessor.process(EventsConfig.class, mainProps);
			ConfigurableProcessor.process(FallDamageConfig.class, mainProps);
			ConfigurableProcessor.process(AStationConfig.class, mainProps);
			ConfigurableProcessor.process(GSConfig.class, mainProps);
			ConfigurableProcessor.process(GeoDataConfig.class, mainProps);
			ConfigurableProcessor.process(GroupConfig.class, mainProps);
			ConfigurableProcessor.process(HousingConfig.class, mainProps);
			ConfigurableProcessor.process(TransportConfig.class, mainProps);
			TransportConfig.refresh();
			ConfigurableProcessor.process(HTMLConfig.class, mainProps);
			ConfigurableProcessor.process(InGameShopConfig.class, mainProps);
			ConfigurableProcessor.process(InstanceConfig.class, mainProps);
			InstanceConfig.refresh();
			ConfigurableProcessor.process(AbyssLandingConfig.class, mainProps);
			ConfigurableProcessor.process(LegionConfig.class, mainProps);
			ConfigurableProcessor.process(LoggingConfig.class, mainProps);
			ConfigurableProcessor.process(MembershipConfig.class, mainProps);
			ConfigurableProcessor.process(NameConfig.class, mainProps);
			ConfigurableProcessor.process(PeriodicSaveConfig.class, mainProps);
			ConfigurableProcessor.process(PlayerTransferConfig.class, mainProps);
			ConfigurableProcessor.process(PricesConfig.class, mainProps);
			ConfigurableProcessor.process(PunishmentConfig.class, mainProps);
			ConfigurableProcessor.process(PvPConfig.class, mainProps);
			ConfigurableProcessor.process(RankingConfig.class, mainProps);
			ConfigurableProcessor.process(RateConfig.class, mainProps);
			ConfigurableProcessor.process(SecurityConfig.class, mainProps);
			ConfigurableProcessor.process(ShutdownConfig.class, mainProps);
			ConfigurableProcessor.process(SiegeConfig.class, mainProps);
			ConfigurableProcessor.process(ThreadConfig.class, mainProps);
			ConfigurableProcessor.process(WorldConfig.class, mainProps);
			ConfigurableProcessor.process(AdvCustomConfig.class, mainProps);
			ConfigurableProcessor.process(AutoGroupConfig.class, mainProps);
			ConfigurableProcessor.process(PvPModConfig.class, mainProps);
			ConfigurableProcessor.process(FFAConfig.class, mainProps);
			ConfigurableProcessor.process(ArchDaevaConfig.class, mainProps);
			ConfigurableProcessor.process(VeteranRewardConfig.class, mainProps);
			String network = configDir() + "/network";
			Properties[] networkProps = PropertiesUtils.loadAllFromDirectory(network);
			overrideRuntimeProperties(networkProps, myProps);
			ConfigurableProcessor.process(DatabaseConfig.class, networkProps);
			ConfigurableProcessor.process(NetworkConfig.class, networkProps);
		} catch (Exception e) {
			log.error(I18n.get("log.07e02900f548", e));
			throw new Error("Can't load gameserver configuration: ", e);
		}
		IPConfig.load();
	}

	/**
	 * 重新加载配置并刷新依赖调度。
	 * Reloads configuration and refreshes dependent schedules.
	 */
	public static void reload() {
		int worldRegionSize = WorldConfig.WORLD_REGION_SIZE;
		int baseThreadPoolSize = ThreadConfig.BASE_THREAD_POOL_SIZE;
		int extraThreadsPerCore = ThreadConfig.EXTRA_THREAD_PER_CORE;
		int threadPoolSize = ThreadConfig.THREAD_POOL_SIZE;
		boolean useThreadPriorities = ThreadConfig.USE_PRIORITIES;
		boolean geoEnabled = GeoDataConfig.GEO_ENABLE;
		boolean pathEnabled = GeoDataConfig.GEO_PATH_ENABLE;
		String houseAuctionTime = HousingConfig.HOUSE_AUCTION_TIME;
		String houseRegisterEnd = HousingConfig.HOUSE_REGISTER_END;
		String houseMaintenanceTime = HousingConfig.HOUSE_MAINTENANCE_TIME;
		try {
			Properties myProps = null;
			try {
				log.info(I18n.get("log.5685ceb915cb"));
				myProps = PropertiesUtils.load(configDir() + "/mygs.properties");
			} catch (Exception e) {
				log.info(I18n.get("log.c453f21e95f6"));
			}
			String administration = configDir() + "/administration";
			Properties[] adminProps = PropertiesUtils.loadAllFromDirectory(administration);
			overrideRuntimeProperties(adminProps, myProps);
			ConfigurableProcessor.process(AdminConfig.class, adminProps);
			ConfigurableProcessor.process(DeveloperConfig.class, adminProps);
			ConfigurableProcessor.process(PanelConfig.class, adminProps);
			String main = configDir() + "/main";
			Properties[] mainProps = PropertiesUtils.loadAllFromDirectory(main);
			overrideRuntimeProperties(mainProps, myProps);
			ConfigurableProcessor.process(AIConfig.class, mainProps);
			ConfigurableProcessor.process(BrokerConfig.class, mainProps);
			ConfigurableProcessor.process(CommonsConfig.class, mainProps);
			ConfigurableProcessor.process(CacheConfig.class, mainProps);
			ConfigurableProcessor.process(CleaningConfig.class, mainProps);
			ConfigurableProcessor.process(CraftConfig.class, mainProps);
			ConfigurableProcessor.process(CustomConfig.class, mainProps);
			ConfigurableProcessor.process(DropConfig.class, mainProps);
			ConfigurableProcessor.process(EnchantsConfig.class, mainProps);
			ConfigurableProcessor.process(EventsConfig.class, mainProps);
			ConfigurableProcessor.process(FallDamageConfig.class, mainProps);
			ConfigurableProcessor.process(AStationConfig.class, mainProps);
			ConfigurableProcessor.process(GSConfig.class, mainProps);
			ConfigurableProcessor.process(GeoDataConfig.class, mainProps);
			ConfigurableProcessor.process(GroupConfig.class, mainProps);
			ConfigurableProcessor.process(HousingConfig.class, mainProps);
			ConfigurableProcessor.process(TransportConfig.class, mainProps);
			TransportConfig.refresh();
			ConfigurableProcessor.process(HTMLConfig.class, mainProps);
			ConfigurableProcessor.process(InGameShopConfig.class, mainProps);
			ConfigurableProcessor.process(InstanceConfig.class, mainProps);
			InstanceConfig.refresh();
			InstanceScaler.reload();
			InstanceService.reloadDestroyTasks();
			InstanceService.load();
			ConfigurableProcessor.process(AbyssLandingConfig.class, mainProps);
			ConfigurableProcessor.process(LegionConfig.class, mainProps);
			ConfigurableProcessor.process(LoggingConfig.class, mainProps);
			ConfigurableProcessor.process(MembershipConfig.class, mainProps);
			ConfigurableProcessor.process(NameConfig.class, mainProps);
			ConfigurableProcessor.process(PeriodicSaveConfig.class, mainProps);
			ConfigurableProcessor.process(PlayerTransferConfig.class, mainProps);
			ConfigurableProcessor.process(PricesConfig.class, mainProps);
			ConfigurableProcessor.process(PunishmentConfig.class, mainProps);
			ConfigurableProcessor.process(PvPConfig.class, mainProps);
			ConfigurableProcessor.process(RankingConfig.class, mainProps);
			ConfigurableProcessor.process(RateConfig.class, mainProps);
			ConfigurableProcessor.process(SecurityConfig.class, mainProps);
			ConfigurableProcessor.process(ShutdownConfig.class, mainProps);
			ConfigurableProcessor.process(SiegeConfig.class, mainProps);
			ConfigurableProcessor.process(ThreadConfig.class, mainProps);
			ConfigurableProcessor.process(WorldConfig.class, mainProps);
			ConfigurableProcessor.process(AdvCustomConfig.class, mainProps);
			ConfigurableProcessor.process(AutoGroupConfig.class, mainProps);
			ConfigurableProcessor.process(PvPModConfig.class, mainProps);
			ConfigurableProcessor.process(FFAConfig.class, mainProps);
			ConfigurableProcessor.process(ArchDaevaConfig.class, mainProps);
			ConfigurableProcessor.process(VeteranRewardConfig.class, mainProps);
			WorldConfig.WORLD_REGION_SIZE = worldRegionSize;
			ThreadConfig.BASE_THREAD_POOL_SIZE = baseThreadPoolSize;
			ThreadConfig.EXTRA_THREAD_PER_CORE = extraThreadsPerCore;
			ThreadConfig.THREAD_POOL_SIZE = threadPoolSize;
			ThreadConfig.USE_PRIORITIES = useThreadPriorities;
			GeoDataConfig.GEO_ENABLE = geoEnabled;
			GeoDataConfig.GEO_PATH_ENABLE = pathEnabled;
			HousingConfig.HOUSE_AUCTION_TIME = houseAuctionTime;
			HousingConfig.HOUSE_REGISTER_END = houseRegisterEnd;
			HousingConfig.HOUSE_MAINTENANCE_TIME = houseMaintenanceTime;
			GameServerNetworkServices.packetFloodFilter().reload();
			GameRuntimeServices.periodicSaveService().reload();
			GameRuntimeServices.brokerService().reload();
			GameStaticDataServices.htmlCache().reload(true);
			GameRuntimeServices.surveyService().reload();
			GameRuntimeServices.inGameShopEn().reload();
			GameRuntimeServices.playerTransferService().reload();
			GameWorldServices.dropRegistrationService().reload();
			DateTimeUtil.init();
			GameFeatureServices.protectorConquerorService().initSystem();
			GameEventServices.playerEventService().reload();
			GameEventServices.abyssRankUpdateService().reload();
			GameEventServices.crazyDaevaService().startTimer();
			GameMaintenanceServices.shugoImperialTombSpawnManager().start();
			GameCronServices.cronService().reload();
			GameLocationBootstrapServices.agentService().reloadSchedule();
			GameLocationBootstrapServices.anohaService().reloadSchedule();
			GameLocationBootstrapServices.beritraService().reloadSchedule();
			GameLocationBootstrapServices.conquestService().reloadSchedule();
			GameLocationBootstrapServices.instanceRiftService().reloadSchedule();
			GameLocationBootstrapServices.moltenusService().reloadSchedule();
			GameLocationBootstrapServices.nightmareCircusService().reloadSchedule();
			GameLocationBootstrapServices.riftService().reloadSchedule();
			GameLocationBootstrapServices.rvrService().reloadSchedule();
			GameFeatureServices.siegeService().reloadSchedule();
			GameLocationBootstrapServices.svsService().reloadSchedule();
			GameLocationBootstrapServices.vortexService().reloadSchedule();
			GameLocationBootstrapServices.zorshivDredgionService().reloadSchedule();
			log.warn(I18n.get("log.b5283679a940"));
		} catch (Exception e) {
			WorldConfig.WORLD_REGION_SIZE = worldRegionSize;
			ThreadConfig.BASE_THREAD_POOL_SIZE = baseThreadPoolSize;
			ThreadConfig.EXTRA_THREAD_PER_CORE = extraThreadsPerCore;
			ThreadConfig.THREAD_POOL_SIZE = threadPoolSize;
			ThreadConfig.USE_PRIORITIES = useThreadPriorities;
			GeoDataConfig.GEO_ENABLE = geoEnabled;
			GeoDataConfig.GEO_PATH_ENABLE = pathEnabled;
			HousingConfig.HOUSE_AUCTION_TIME = houseAuctionTime;
			HousingConfig.HOUSE_REGISTER_END = houseRegisterEnd;
			HousingConfig.HOUSE_MAINTENANCE_TIME = houseMaintenanceTime;
			log.error(I18n.get("log.4566dc07cb76", e));
			throw new Error("Can't reload configuration: ", e);
		}
	}
}
