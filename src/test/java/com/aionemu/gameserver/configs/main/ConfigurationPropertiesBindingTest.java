package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.testutil.ConfigSnapshot;

/**
 * 现代外部化配置与 Spring ConfigurationProperties 绑定单元测试。
 * Unit tests verifying that legacy properties bind onto Spring @ConfigurationProperties
 * and synchronize with the static facades.
 */
class ConfigurationPropertiesBindingTest {

	@Test
	void bindsPeriodicSaveConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PeriodicSaveConfig.class,
			"PLAYER_GENERAL", "PLAYER_ITEMS", "LEGION_ITEMS", "PLAYER_PETS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.periodicsave.player.general", "600",
				"gameserver.periodicsave.player.items", "450",
				"gameserver.periodicsave.legion.items", "1800",
				"gameserver.periodicsave.player.pets", "10"
			)));

			PeriodicSaveConfig config = new PeriodicSaveConfig();
			Binder.get(environment).bind("gameserver.periodicsave", Bindable.ofInstance(config));

			assertEquals(600, PeriodicSaveConfig.PLAYER_GENERAL);
			assertEquals(450, PeriodicSaveConfig.PLAYER_ITEMS);
			assertEquals(1800, PeriodicSaveConfig.LEGION_ITEMS);
			assertEquals(10, PeriodicSaveConfig.PLAYER_PETS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsShutdownConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(ShutdownConfig.class,
			"HOOK_MODE", "HOOK_DELAY", "ANNOUNCE_INTERVAL", "SAFE_REBOOT", "DESPAWN_NPCS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.shutdown.mode", "2",
				"gameserver.shutdown.delay", "120",
				"gameserver.shutdown.interval", "5",
				"gameserver.shutdown.safereboot", "false",
				"gameserver.shutdown.despnpcs", "true"
			)));

			ShutdownConfig config = new ShutdownConfig();
			Binder.get(environment).bind("gameserver.shutdown", Bindable.ofInstance(config));

			assertEquals(2, ShutdownConfig.HOOK_MODE);
			assertEquals(120, ShutdownConfig.HOOK_DELAY);
			assertEquals(5, ShutdownConfig.ANNOUNCE_INTERVAL);
			assertFalse(ShutdownConfig.SAFE_REBOOT);
			assertTrue(ShutdownConfig.DESPAWN_NPCS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsCacheConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(CacheConfig.class,
			"SOFT_CACHE_MAP", "CACHE_PLAYERS", "CACHE_COMMONDATA", "CACHE_ACCOUNTS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.cache.softcache", "true",
				"gameserver.cache.players", "true",
				"gameserver.cache.pcd", "true",
				"gameserver.cache.accounts", "true"
			)));

			CacheConfig config = new CacheConfig();
			Binder.get(environment).bind("gameserver.cache", Bindable.ofInstance(config));

			assertTrue(CacheConfig.SOFT_CACHE_MAP);
			assertTrue(CacheConfig.CACHE_PLAYERS);
			assertTrue(CacheConfig.CACHE_COMMONDATA);
			assertTrue(CacheConfig.CACHE_ACCOUNTS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsCleaningConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(CleaningConfig.class,
			"CLEANING_ENABLE", "CLEANING_PERIOD", "CLEANING_THREADS", "CLEANING_LIMIT");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.cleaning.enable", "true",
				"gameserver.cleaning.period", "90",
				"gameserver.cleaning.threads", "4",
				"gameserver.cleaning.limit", "10000"
			)));

			CleaningConfig config = new CleaningConfig();
			Binder.get(environment).bind("gameserver.cleaning", Bindable.ofInstance(config));

			assertTrue(CleaningConfig.CLEANING_ENABLE);
			assertEquals(90, CleaningConfig.CLEANING_PERIOD);
			assertEquals(4, CleaningConfig.CLEANING_THREADS);
			assertEquals(10000, CleaningConfig.CLEANING_LIMIT);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsSecurityConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(SecurityConfig.class,
			"SPEEDHACK", "SPEEDHACK_COUNTER", "CAPTCHA_ENABLE", "CAPTCHA_APPEAR_RATE",
			"PASSKEY_ENABLE", "PASSKEY_WRONG_MAXCOUNT", "SECURITY_ENABLE", "PING_INTERVAL",
			"BROKER_PREBUY_CHECK", "SURVEY_DELAY");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.security.antihack.speedhack.enable", "true",
				"gameserver.security.antihack.speedhack.counter", "3",
				"gameserver.security.captcha.enable", "true",
				"gameserver.security.captcha.appear.rate", "15",
				"gameserver.security.passkey.enable", "true",
				"gameserver.security.passkey.wrong.maxcount", "8",
				"gameserver.security.pingcheck.enable", "false",
				"gameserver.security.pingcheck.interval", "120",
				"gameserver.security.broker.prebuy", "false",
				"gameserver.security.survey.delay.minute", "45"
			)));

			SecurityConfig config = new SecurityConfig();
			Binder.get(environment).bind("gameserver.security", Bindable.ofInstance(config));

			assertTrue(SecurityConfig.SPEEDHACK);
			assertEquals(3, SecurityConfig.SPEEDHACK_COUNTER);
			assertTrue(SecurityConfig.CAPTCHA_ENABLE);
			assertEquals(15, SecurityConfig.CAPTCHA_APPEAR_RATE);
			assertTrue(SecurityConfig.PASSKEY_ENABLE);
			assertEquals(8, SecurityConfig.PASSKEY_WRONG_MAXCOUNT);
			assertFalse(SecurityConfig.SECURITY_ENABLE);
			assertEquals(120, SecurityConfig.PING_INTERVAL);
			assertFalse(SecurityConfig.BROKER_PREBUY_CHECK);
			assertEquals(45, SecurityConfig.SURVEY_DELAY);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsNetworkConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(NetworkConfig.class,
			"PUBLIC_ADDRESS", "GAME_PORT", "GAME_BIND_ADDRESS", "MAX_ONLINE_PLAYERS",
			"LOGIN_ADDRESS", "GAMESERVER_ID", "LOGIN_PASSWORD", "NIO_READ_WRITE_THREADS",
			"PACKET_PROCESSOR_MIN_THREADS", "PACKET_PROCESSOR_MAX_THREADS",
			"ENABLE_FLOOD_CONNECTIONS", "Flood_Tick", "Flood_SWARN");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.network.address", "192.168.1.100"),
				Map.entry("gameserver.network.client.port", "8888"),
				Map.entry("gameserver.network.client.host", "127.0.0.1"),
				Map.entry("gameserver.network.client.maxplayers", "500"),
				Map.entry("gameserver.network.login.address", "10.0.0.1:9014"),
				Map.entry("gameserver.network.login.gsid", "2"),
				Map.entry("gameserver.network.login.password", "secret123"),
				Map.entry("gameserver.network.nio.threads", "4"),
				Map.entry("gameserver.network.packet.processor.threads.min", "8"),
				Map.entry("gameserver.network.packet.processor.threads.max", "16"),
				Map.entry("gameserver.network.flood.connections", "true"),
				Map.entry("gameserver.network.flood.tick", "2000"),
				Map.entry("gameserver.network.flood.short.warn", "25")
			)));

			NetworkConfig config = new NetworkConfig();
			Binder.get(environment).bind("gameserver.network", Bindable.ofInstance(config));

			assertEquals("192.168.1.100", NetworkConfig.PUBLIC_ADDRESS);
			assertEquals(8888, NetworkConfig.GAME_PORT);
			assertEquals("127.0.0.1", NetworkConfig.GAME_BIND_ADDRESS);
			assertEquals(500, NetworkConfig.MAX_ONLINE_PLAYERS);
			assertEquals(9014, NetworkConfig.LOGIN_ADDRESS.getPort());
			assertEquals("10.0.0.1", NetworkConfig.LOGIN_ADDRESS.getHostString());
			assertEquals(2, NetworkConfig.GAMESERVER_ID);
			assertEquals("secret123", NetworkConfig.LOGIN_PASSWORD);
			assertEquals(4, NetworkConfig.NIO_READ_WRITE_THREADS);
			assertEquals(8, NetworkConfig.PACKET_PROCESSOR_MIN_THREADS);
			assertEquals(16, NetworkConfig.PACKET_PROCESSOR_MAX_THREADS);
			assertTrue(NetworkConfig.ENABLE_FLOOD_CONNECTIONS);
			assertEquals(2000, NetworkConfig.Flood_Tick);
			assertEquals(25, NetworkConfig.Flood_SWARN);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsGroupConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(GroupConfig.class,
			"GROUP_REMOVE_TIME", "GROUP_MAX_DISTANCE", "GROUP_INVITEOTHERFACTION",
			"ALLIANCE_REMOVE_TIME", "ALLIANCE_INVITEOTHERFACTION");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.playergroup.removetime", "300",
				"gameserver.playergroup.maxdistance", "150",
				"gameserver.group.inviteotherfaction", "true",
				"gameserver.playeralliance.removetime", "450",
				"gameserver.playeralliance.inviteotherfaction", "true"
			)));

			GroupConfig config = new GroupConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(300, GroupConfig.GROUP_REMOVE_TIME);
			assertEquals(150, GroupConfig.GROUP_MAX_DISTANCE);
			assertTrue(GroupConfig.GROUP_INVITEOTHERFACTION);
			assertEquals(450, GroupConfig.ALLIANCE_REMOVE_TIME);
			assertTrue(GroupConfig.ALLIANCE_INVITEOTHERFACTION);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsAdminConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(AdminConfig.class,
			"GM_SPECIAL_SKILLS", "GM_LEVEL", "GM_PANEL", "GM_FLIGHT_FREE",
			"DOORS_OPEN", "ADMIN_AUTO_RES", "INVISIBLE_GM_CONNECTION",
			"ADMIN_TAG_ENABLE", "ADMIN_TAG_1", "ANNOUNCE_LEVEL_LIST");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.administration.gmskills", "4",
				"gameserver.administration.gmlevel", "2",
				"gameserver.administration.gmpanel", "1",
				"gameserver.administration.flight.freefly", "5",
				"gameserver.administration.doors.opening", "2",
				"gameserver.administration.auto.res", "1",
				"gameserver.administration.invis.gm.connection", "true",
				"gameserver.admin.tag.enable", "false",
				"gameserver.admin.tag.1", "<Helper> %s",
				"gameserver.admin.announce.levels", "3,4,5"
			)));

			AdminConfig config = new AdminConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(4, AdminConfig.GM_SPECIAL_SKILLS);
			assertEquals(2, AdminConfig.GM_LEVEL);
			assertEquals(1, AdminConfig.GM_PANEL);
			assertEquals(5, AdminConfig.GM_FLIGHT_FREE);
			assertEquals(2, AdminConfig.DOORS_OPEN);
			assertEquals(1, AdminConfig.ADMIN_AUTO_RES);
			assertTrue(AdminConfig.INVISIBLE_GM_CONNECTION);
			assertFalse(AdminConfig.ADMIN_TAG_ENABLE);
			assertEquals("<Helper> %s", AdminConfig.ADMIN_TAG_1);
			assertEquals("3,4,5", AdminConfig.ANNOUNCE_LEVEL_LIST);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsEnchantsConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class,
			"SOCKET_MANASTONE", "ENCHANT_ITEM", "ENCHANT_ITEM_KINAH",
			"MAX_EQUIPMENT_ENCHANT_LEVEL", "ENCHANT_PLUME", "ENCHANT_BRACELET",
			"ENCHANT_ACCESSORY", "ENCHANT_STIGMA", "CLEAN_STONE", "ENCHANT_SPEED",
			"ENABLE_ARCHDAEVA_ITEM_BROKE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.socket.manastone", "75.5"),
				Map.entry("gameserver.enchant.item.value", "65.0"),
				Map.entry("gameserver.enchant.item.kinah", "50000"),
				Map.entry("gameserver.enchant.item.broke", "false"),
				Map.entry("gameserver.enchant.equipment.max.level", "50"),
				Map.entry("gameserver.enchant.plume", "80.0"),
				Map.entry("gameserver.enchant.bracelet", "70.0"),
				Map.entry("gameserver.enchant.accessory", "60.0"),
				Map.entry("gameserver.enchant.stigma", "55.0"),
				Map.entry("gameserver.manastone.clean", "true"),
				Map.entry("gameserver.enchant.cast.speed", "2500")
			)));

			EnchantsConfig config = new EnchantsConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(75.5f, EnchantsConfig.SOCKET_MANASTONE);
			assertEquals(65.0f, EnchantsConfig.ENCHANT_ITEM);
			assertEquals(50000, EnchantsConfig.ENCHANT_ITEM_KINAH);
			assertFalse(EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE);
			assertEquals(50, EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL);
			assertEquals(80.0f, EnchantsConfig.ENCHANT_PLUME);
			assertEquals(70.0f, EnchantsConfig.ENCHANT_BRACELET);
			assertEquals(60.0f, EnchantsConfig.ENCHANT_ACCESSORY);
			assertEquals(55.0f, EnchantsConfig.ENCHANT_STIGMA);
			assertTrue(EnchantsConfig.CLEAN_STONE);
			assertEquals(2500, EnchantsConfig.ENCHANT_SPEED);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsEnchantsConfigLegacyScalarItemProperty() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class, "ENCHANT_ITEM");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.enchant.item", "88.5"
			)));

			EnchantsConfig config = new EnchantsConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(88.5f, EnchantsConfig.ENCHANT_ITEM);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsRateConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(RateConfig.class,
			"XP_RATE", "PREMIUM_XP_RATE", "VIP_XP_RATE", "DROP_RATE",
			"PREMIUM_DROP_RATE", "VIP_DROP_RATE", "GP_PLAYER_LOSS_RATE",
			"PREMIUM_GP_PLAYER_LOSS_RATE", "VIP_GP_PLAYER_LOSS_RATE",
			"DREDGION_REWARD_RATE", "AURA_OF_GROWTH", "NORMAL_MOBS_RATE_HP",
			"NORMAL_MOBS_RATE_PW", "DAMAGE_MULTIPLIER");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.rate.regular.xp", "2.0"),
				Map.entry("gameserver.rate.premium.xp", "4.0"),
				Map.entry("gameserver.rate.vip.xp", "6.0"),
				Map.entry("gameserver.rate.regular.drop", "1.5"),
				Map.entry("gameserver.rate.premium.drop", "3.0"),
				Map.entry("gameserver.rate.vip.drop", "4.5"),
				Map.entry("gameserver.rate.regular.gp.player.loss", "0.8"),
				Map.entry("gameserver.rate.premium.gp.player.loss", "1.6"),
				Map.entry("gameserver.rate.vip.gp.player.loss", "2.4"),
				Map.entry("gameserver.rate.dredgion", "2.5"),
				Map.entry("gameserver.rate.aura.of.growth.chance", "85"),
				Map.entry("gameserver.rate.normal.mobs.rate.hp", "1.2"),
				Map.entry("gameserver.rate.normal.mobs.rate.pw", "1.3"),
				Map.entry("gameserver.damage.multiplier", "1.8")
			)));

			RateConfig config = new RateConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(2.0f, RateConfig.XP_RATE);
			assertEquals(4.0f, RateConfig.PREMIUM_XP_RATE);
			assertEquals(6.0f, RateConfig.VIP_XP_RATE);
			assertEquals(1.5f, RateConfig.DROP_RATE);
			assertEquals(3.0f, RateConfig.PREMIUM_DROP_RATE);
			assertEquals(4.5f, RateConfig.VIP_DROP_RATE);
			assertEquals(0.8f, RateConfig.GP_PLAYER_LOSS_RATE);
			assertEquals(1.6f, RateConfig.PREMIUM_GP_PLAYER_LOSS_RATE);
			assertEquals(2.4f, RateConfig.VIP_GP_PLAYER_LOSS_RATE);
			assertEquals(2.5f, RateConfig.DREDGION_REWARD_RATE);
			assertEquals(85, RateConfig.AURA_OF_GROWTH);
			assertEquals(1.2f, RateConfig.NORMAL_MOBS_RATE_HP);
			assertEquals(1.3f, RateConfig.NORMAL_MOBS_RATE_PW);
			assertEquals(1.8f, RateConfig.DAMAGE_MULTIPLIER);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsCustomConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(CustomConfig.class,
			"LOGIN_SERVER_INFO", "ENABLE_PVE_TOLL_REWARD", "TOLL_PVE_CHANCE",
			"WORLD_CHANNEL_AP_COSTS", "FACTION_FREE_USE", "SPEAKING_BETWEEN_FACTIONS",
			"DYE_PRICE", "BASE_FLYTIME", "FRIENDLIST_SIZE", "ENABLE_INSTANCES",
			"KINAH_CAP_VALUE", "LIMITS_ENABLED", "EXPRESS_MAIL_COOLDOWN_SECONDS",
			"RIFT_DURATION", "GODSTONE_ACTIVATION_RATE", "TAG_PK", "CUBE_SIZE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.loginserver.info", "true"),
				Map.entry("gameserver.pve.toll.rewarding.enable", "true"),
				Map.entry("gameserver.pve.toll.reward.chance", "75"),
				Map.entry("gameserver.worldchannel.costs", "30000"),
				Map.entry("gameserver.faction.free", "false"),
				Map.entry("gameserver.chat.factions.enable", "true"),
				Map.entry("gameserver.admin.dye.price", "500000"),
				Map.entry("gameserver.base.flytime", "120"),
				Map.entry("gameserver.friendlist.size", "150"),
				Map.entry("gameserver.instances.enable", "false"),
				Map.entry("gameserver.kinah.cap.value", "2000000000"),
				Map.entry("gameserver.limits.enable", "false"),
				Map.entry("gameserver.express.mail.cooldown_seconds", "30"),
				Map.entry("gameserver.rift.duration", "3"),
				Map.entry("gameserver.rates.godstone.activation.rate", "2.0"),
				Map.entry("gameserver.pk.tag", "[PK] %s"),
				Map.entry("gameserver.cube.size", "9")
			)));

			CustomConfig config = new CustomConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(CustomConfig.LOGIN_SERVER_INFO);
			assertTrue(CustomConfig.ENABLE_PVE_TOLL_REWARD);
			assertEquals(75, CustomConfig.TOLL_PVE_CHANCE);
			assertEquals(30000, CustomConfig.WORLD_CHANNEL_AP_COSTS);
			assertFalse(CustomConfig.FACTION_FREE_USE);
			assertTrue(CustomConfig.SPEAKING_BETWEEN_FACTIONS);
			assertEquals(500000, CustomConfig.DYE_PRICE);
			assertEquals(120, CustomConfig.BASE_FLYTIME);
			assertEquals(150, CustomConfig.FRIENDLIST_SIZE);
			assertFalse(CustomConfig.ENABLE_INSTANCES);
			assertEquals(2000000000L, CustomConfig.KINAH_CAP_VALUE);
			assertFalse(CustomConfig.LIMITS_ENABLED);
			assertEquals(30, CustomConfig.EXPRESS_MAIL_COOLDOWN_SECONDS);
			assertEquals(3, CustomConfig.RIFT_DURATION);
			assertEquals(2.0f, CustomConfig.GODSTONE_ACTIVATION_RATE);
			assertEquals("[PK] %s", CustomConfig.TAG_PK);
			assertEquals(9, CustomConfig.CUBE_SIZE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsHousingConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(HousingConfig.class,
			"VISIBILITY_DISTANCE", "ENABLE_SHOW_HOUSE_DOORID", "ENTER_HOUSE_ACCESSLEVEL",
			"ENABLE_HOUSE_AUCTIONS", "ENABLE_HOUSE_PAY", "HOUSE_AUCTION_TIME",
			"HOUSE_REGISTER_END", "HOUSE_MAINTENANCE_TIME", "HOUSE_MIN_BID",
			"MANSION_MIN_BID", "ESTATE_MIN_BID", "PALACE_MIN_BID",
			"HOUSE_MIN_BID_LEVEL", "MANSION_MIN_BID_LEVEL", "ESTATE_MIN_BID_LEVEL",
			"PALACE_MIN_BID_LEVEL", "BID_REFUND_PERCENT", "HOUSE_AUCTION_BID_LIMIT",
			"HOUSE_SCRIPT_DEBUG", "FILL_HOUSE_BIDS_AUTO", "FILL_AUTO_HOUSES_COUNT",
			"FILL_AUTO_MANSION_COUNT", "FILL_AUTO_ESTATE_COUNT", "FILL_AUTO_PALACE_COUNT");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.housing.visibility.distance", "250"),
				Map.entry("gameserver.housedoor.showid", "false"),
				Map.entry("gameserver.housedoor.accesslevel", "5"),
				Map.entry("gameserver.housing.auction.enable", "true"),
				Map.entry("gameserver.housing.pay.enable", "true"),
				Map.entry("gameserver.housing.auction.time", "0 0 0 ? * MON"),
				Map.entry("gameserver.housing.auction.registerend", "0 0 12 ? * SUN"),
				Map.entry("gameserver.housing.maintain.time", "0 0 0 ? * TUE"),
				Map.entry("gameserver.housing.auction.default_bid.house", "15000000"),
				Map.entry("gameserver.housing.auction.default_bid.mansion", "120000000"),
				Map.entry("gameserver.housing.auction.default_bid.estate", "350000000"),
				Map.entry("gameserver.housing.auction.default_bid.palace", "1100000000"),
				Map.entry("gameserver.housing.auction.bidding.min_level.house", "25"),
				Map.entry("gameserver.housing.auction.bidding.min_level.mansion", "35"),
				Map.entry("gameserver.housing.auction.bidding.min_level.estate", "45"),
				Map.entry("gameserver.housing.auction.bidding.min_level.palace", "55"),
				Map.entry("gameserver.housing.auction.default_refund", "0.5f"),
				Map.entry("gameserver.housing.auction.steplimit", "80"),
				Map.entry("gameserver.housing.scripts.debug", "true"),
				Map.entry("gameserver.housing.auction.fill.auto.value", "true"),
				Map.entry("gameserver.housing.auction.fill.auto.houses", "100"),
				Map.entry("gameserver.housing.auction.fill.auto.mansion", "50"),
				Map.entry("gameserver.housing.auction.fill.auto.estate", "20"),
				Map.entry("gameserver.housing.auction.fill.auto.palace", "5")
			)));

			HousingConfig config = new HousingConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(250f, HousingConfig.VISIBILITY_DISTANCE);
			assertFalse(HousingConfig.ENABLE_SHOW_HOUSE_DOORID);
			assertEquals(5, HousingConfig.ENTER_HOUSE_ACCESSLEVEL);
			assertTrue(HousingConfig.ENABLE_HOUSE_AUCTIONS);
			assertTrue(HousingConfig.ENABLE_HOUSE_PAY);
			assertEquals("0 0 0 ? * MON", HousingConfig.HOUSE_AUCTION_TIME);
			assertEquals("0 0 12 ? * SUN", HousingConfig.HOUSE_REGISTER_END);
			assertEquals("0 0 0 ? * TUE", HousingConfig.HOUSE_MAINTENANCE_TIME);
			assertEquals(15000000, HousingConfig.HOUSE_MIN_BID);
			assertEquals(120000000, HousingConfig.MANSION_MIN_BID);
			assertEquals(350000000, HousingConfig.ESTATE_MIN_BID);
			assertEquals(1100000000, HousingConfig.PALACE_MIN_BID);
			assertEquals(25, HousingConfig.HOUSE_MIN_BID_LEVEL);
			assertEquals(35, HousingConfig.MANSION_MIN_BID_LEVEL);
			assertEquals(45, HousingConfig.ESTATE_MIN_BID_LEVEL);
			assertEquals(55, HousingConfig.PALACE_MIN_BID_LEVEL);
			assertEquals(0.5f, HousingConfig.BID_REFUND_PERCENT);
			assertEquals(80f, HousingConfig.HOUSE_AUCTION_BID_LIMIT);
			assertTrue(HousingConfig.HOUSE_SCRIPT_DEBUG);
			assertTrue(HousingConfig.FILL_HOUSE_BIDS_AUTO);
			assertEquals(100, HousingConfig.FILL_AUTO_HOUSES_COUNT);
			assertEquals(50, HousingConfig.FILL_AUTO_MANSION_COUNT);
			assertEquals(20, HousingConfig.FILL_AUTO_ESTATE_COUNT);
			assertEquals(5, HousingConfig.FILL_AUTO_PALACE_COUNT);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsHousingConfigLegacyScalarFillAutoProperty() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(HousingConfig.class, "FILL_HOUSE_BIDS_AUTO");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.housing.auction.fill.auto", "true"
			)));

			HousingConfig config = new HousingConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(HousingConfig.FILL_HOUSE_BIDS_AUTO);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsSiegeConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(SiegeConfig.class,
			"SIEGE_ENABLED", "SIEGE_MEDAL_RATE", "SIEGE_SHIELD_ENABLED",
			"BALAUR_AUTO_ASSAULT", "BALAUR_ASSAULT_RATE", "SIEGE_AUTO_RACE",
			"SIEGE_AUTO_LOCID");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.siege.enable", "false",
				"gameserver.siege.medal.rate", "2",
				"gameserver.siege.shield.enable", "false",
				"gameserver.siege.assault.enable", "true",
				"gameserver.siege.assault.rate", "2.5",
				"gameserver.auto.siege.race", "true",
				"gameserver.auto.siege.id", "1011,1012"
			)));

			SiegeConfig config = new SiegeConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(SiegeConfig.SIEGE_ENABLED);
			assertEquals(2, SiegeConfig.SIEGE_MEDAL_RATE);
			assertFalse(SiegeConfig.SIEGE_SHIELD_ENABLED);
			assertTrue(SiegeConfig.BALAUR_AUTO_ASSAULT);
			assertEquals(2.5f, SiegeConfig.BALAUR_ASSAULT_RATE);
			assertTrue(SiegeConfig.SIEGE_AUTO_RACE);
			assertEquals("1011,1012", SiegeConfig.SIEGE_AUTO_LOCID);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsDropConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(DropConfig.class,
			"DISABLE_DROP_REDUCTION", "ENABLE_UNIQUE_DROP_ANNOUNCE",
			"DISABLE_DROP_REDUCTION_IN_ZONES", "ENABLE_GLOBAL_DROPS",
			"KINAH_RATE", "MAX_GLOBAL_DROPS_PER_NPC");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.drop.reduction.disable", "true",
				"gameserver.unique.drop.announce.enable", "false",
				"gameserver.drop.noreduction", "1001,1002",
				"gameserver.drop.enable.global.drops", "false",
				"gameserver.drop.kinah.rate", "3.0",
				"gameserver.drop.max.global.drops.per.npc", "30"
			)));

			DropConfig config = new DropConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(DropConfig.DISABLE_DROP_REDUCTION);
			assertFalse(DropConfig.ENABLE_UNIQUE_DROP_ANNOUNCE);
			assertEquals("1001,1002", DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES);
			assertFalse(DropConfig.ENABLE_GLOBAL_DROPS);
			assertEquals(3.0f, DropConfig.KINAH_RATE);
			assertEquals(30, DropConfig.MAX_GLOBAL_DROPS_PER_NPC);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsCraftConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(CraftConfig.class,
			"UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP", "MAX_EXPERT_CRAFTING_SKILLS",
			"MAX_MASTER_CRAFTING_SKILLS", "CRAFT_CRIT_RATE", "PREMIUM_CRAFT_CRIT_RATE",
			"VIP_CRAFT_CRIT_RATE", "CRAFT_COMBO_RATE", "PREMIUM_CRAFT_COMBO_RATE",
			"VIP_CRAFT_COMBO_RATE", "CRAFT_CHANCE_PURPLE_CRIT", "CRAFT_CHANCE_BLUE_CRIT",
			"CRAFT_CHANCE_INSTANT", "PROTECTION_GATHER_ENABLE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.craft.skills.unrestricted.levelup.enable", "true"),
				Map.entry("gameserver.craft.max.expert.skills", "4"),
				Map.entry("gameserver.craft.max.master.skills", "3"),
				Map.entry("gameserver.craft.critical.rate.regular", "20"),
				Map.entry("gameserver.craft.critical.rate.premium", "25"),
				Map.entry("gameserver.craft.critical.rate.vip", "30"),
				Map.entry("gameserver.craft.combo.rate.regular", "35"),
				Map.entry("gameserver.craft.combo.rate.premium", "40"),
				Map.entry("gameserver.craft.combo.rate.vip", "45"),
				Map.entry("gameserver.craft.chance.purple.crit", "5"),
				Map.entry("gameserver.craft.chance.blue.crit", "15"),
				Map.entry("gameserver.craft.chance.instant", "80"),
				Map.entry("gameserver.protection.gather.enable", "false")
			)));

			CraftConfig config = new CraftConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(CraftConfig.UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP);
			assertEquals(4, CraftConfig.MAX_EXPERT_CRAFTING_SKILLS);
			assertEquals(3, CraftConfig.MAX_MASTER_CRAFTING_SKILLS);
			assertEquals(20, CraftConfig.CRAFT_CRIT_RATE);
			assertEquals(25, CraftConfig.PREMIUM_CRAFT_CRIT_RATE);
			assertEquals(30, CraftConfig.VIP_CRAFT_CRIT_RATE);
			assertEquals(35, CraftConfig.CRAFT_COMBO_RATE);
			assertEquals(40, CraftConfig.PREMIUM_CRAFT_COMBO_RATE);
			assertEquals(45, CraftConfig.VIP_CRAFT_COMBO_RATE);
			assertEquals(5, CraftConfig.CRAFT_CHANCE_PURPLE_CRIT);
			assertEquals(15, CraftConfig.CRAFT_CHANCE_BLUE_CRIT);
			assertEquals(80, CraftConfig.CRAFT_CHANCE_INSTANT);
			assertFalse(CraftConfig.PROTECTION_GATHER_ENABLE);
		} finally {
			snapshot.restore();
		}
	}
}
