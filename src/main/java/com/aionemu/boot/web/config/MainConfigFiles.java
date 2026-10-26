package com.aionemu.boot.web.config;

import java.util.List;
import java.util.Set;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.AStationConfig;
import com.aionemu.gameserver.configs.main.AbyssLandingConfig;
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
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.main.TransportConfig;
import com.aionemu.gameserver.configs.main.VeteranRewardConfig;
import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.loginserver.configs.VipConfig;

/**
 * 主服配置目录的可编辑文件注册表：把 {@code main/} 下的每个 {@code .properties} 映射到它对应的配置类。
 * The registry of the editable files of the main configuration directory: every
 * {@code .properties} under {@code main/} mapped to the configuration class it feeds.
 * <p>
 * 映射关系由「文件里的键」与「配置类 {@code @Property} 声明的键」求交集得到，
 * 因此新增配置文件时必须同时补上它的映射，否则该文件不会出现在控制台里。
 * <p>
 * The mapping comes from the intersection of the keys in the file and the keys the configuration
 * class declares through {@code @Property}, so a new configuration file must be mapped as well or
 * it will not show up in the console.
 *
 * @author AionEmu
 */
public final class MainConfigFiles {

	/**
	 * 工具类不允许实例化。
	 * The utility class must not be instantiated.
	 */
	private MainConfigFiles() {
	}

	/**
	 * 注册主服配置目录下的全部可编辑文件。
	 * Registers every editable file of the main configuration directory.
	 */
	static void registerAll() {
		EditableFile.create("a_station", ConfigGroup.MAIN,
			"main/a_station.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			AStationConfig.class);
		EditableFile.create("abysslanding", ConfigGroup.MAIN,
			"main/abysslanding.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			AbyssLandingConfig.class);
		EditableFile.create("advcustom", List.of(ConfigGroup.MAIN, ConfigGroup.SHOP),
			"main/advcustom.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			InGameShopConfig.class);
		EditableFile.create("ai", ConfigGroup.MAIN,
			"main/ai.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			AIConfig.class);
		EditableFile.create("archdaeva", ConfigGroup.MAIN,
			"main/archdaeva.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			ArchDaevaConfig.class);
		EditableFile.create("autogroup", ConfigGroup.MAIN,
			"main/autogroup.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			AutoGroupConfig.class);
		EditableFile.create("broker", ConfigGroup.MAIN,
			"main/broker.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			BrokerConfig.class);
		EditableFile.create("cache", ConfigGroup.MAIN,
			"main/cache.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			CacheConfig.class);
		EditableFile.create("cleaning", ConfigGroup.MAIN,
			"main/cleaning.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			CleaningConfig.class);
		EditableFile.create("craft", ConfigGroup.MAIN,
			"main/craft.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			CraftConfig.class);
		EditableFile.create("custom", ConfigGroup.MAIN,
			"main/custom.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			CustomConfig.class);
		EditableFile.create("drop", ConfigGroup.MAIN,
			"main/drop.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			DropConfig.class);
		EditableFile.create("enchants", ConfigGroup.MAIN,
			"main/enchants.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			EnchantsConfig.class);
		EditableFile.create("events", ConfigGroup.MAIN,
			"main/events.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			EventsConfig.class);
		EditableFile.create("falldamage", ConfigGroup.MAIN,
			"main/falldamage.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			FallDamageConfig.class);
		EditableFile.create("ffa", ConfigGroup.MAIN,
			"main/ffa.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			FFAConfig.class);
		EditableFile.create("gameserver", ConfigGroup.MAIN,
			"main/gameserver.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			GSConfig.class);
		EditableFile.create("geodata", ConfigGroup.MAIN,
			"main/geodata.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			GeoDataConfig.class);
		EditableFile.create("group", ConfigGroup.MAIN,
			"main/group.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			GroupConfig.class);
		EditableFile.create("housing", ConfigGroup.MAIN,
			"main/housing.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			HousingConfig.class);
		EditableFile.create("html", ConfigGroup.MAIN,
			"main/html.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			HTMLConfig.class);
		EditableFile.create("ingameshop", List.of(ConfigGroup.MAIN, ConfigGroup.SHOP),
			"main/ingameshop.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			InGameShopConfig.class);
		EditableFile.create("instance", ConfigGroup.MAIN,
			"main/instance.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			InstanceConfig.class);
		EditableFile.create("legions", ConfigGroup.MAIN,
			"main/legions.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			LegionConfig.class);
		EditableFile.create("logging", ConfigGroup.MAIN,
			"main/logging.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			LoggingConfig.class);
		EditableFile.create("membership", ConfigGroup.MAIN,
			"main/membership.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			MembershipConfig.class);
		EditableFile.create("name", ConfigGroup.MAIN,
			"main/name.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			NameConfig.class);
		EditableFile.create("periodicsave", ConfigGroup.MAIN,
			"main/periodicsave.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PeriodicSaveConfig.class);
		EditableFile.create("playertransfer", ConfigGroup.MAIN,
			"main/playertransfer.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PlayerTransferConfig.class);
		EditableFile.create("prices", ConfigGroup.MAIN,
			"main/prices.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PricesConfig.class);
		EditableFile.create("punishment", ConfigGroup.MAIN,
			"main/punishment.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PunishmentConfig.class);
		EditableFile.create("pvp", ConfigGroup.MAIN,
			"main/pvp.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PvPConfig.class);
		EditableFile.create("pvpmod", ConfigGroup.MAIN,
			"main/pvpmod.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			PvPModConfig.class);
		EditableFile.create("ranking", ConfigGroup.MAIN,
			"main/ranking.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			RankingConfig.class);
		EditableFile.create("rates", ConfigGroup.MAIN,
			"main/rates.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			RateConfig.class);
		EditableFile.create("security", ConfigGroup.MAIN,
			"main/security.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			SecurityConfig.class);
		EditableFile.create("shutdown", ConfigGroup.MAIN,
			"main/shutdown.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			ShutdownConfig.class);
		EditableFile.create("siege", ConfigGroup.MAIN,
			"main/siege.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			SiegeConfig.class);
		EditableFile.create("skill", ConfigGroup.MAIN,
			"main/skill.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			SkillConfig.class);
		EditableFile.create("thread", ConfigGroup.MAIN,
			"main/thread.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			ThreadConfig.class);
		EditableFile.create("transport", ConfigGroup.MAIN,
			"main/transport.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			TransportConfig.class);
		EditableFile.create("veteran", ConfigGroup.MAIN,
			"main/veteran.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			VeteranRewardConfig.class);
		EditableFile.create("vip", ConfigGroup.MAIN,
			"main/vip.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.LOGIN_VIP_CONFIG,
			VipConfig.class);
		EditableFile.create("world", ConfigGroup.MAIN,
			"main/world.properties", EditableFile.Format.PROPERTIES, EditableFile.ApplyTarget.MAIN_CONFIG,
			WorldConfig.class);
	}

	/**
	 * 需要额外调用 {@code refresh()} 的配置类。
	 * The configuration classes that need an extra {@code refresh()} call.
	 */
	public static final Set<Class<?>> REFRESH_REQUIRED = Set.of(
		DropConfig.class,
		InstanceConfig.class,
		SkillConfig.class,
		TransportConfig.class
	);
}
