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
import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.administration.PanelConfig;
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
			"ADMIN_TAG_ENABLE", "ADMIN_TAG_1", "ANNOUNCE_LEVEL_LIST", "SHOW_NPC_ID");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.administration.gmskills", "4"),
				Map.entry("gameserver.administration.gmlevel", "2"),
				Map.entry("gameserver.administration.gmpanel", "1"),
				Map.entry("gameserver.administration.flight.freefly", "5"),
				Map.entry("gameserver.administration.doors.opening", "2"),
				Map.entry("gameserver.administration.auto.res", "1"),
				Map.entry("gameserver.administration.invis.gm.connection", "true"),
				Map.entry("gameserver.admin.tag.enable", "false"),
				Map.entry("gameserver.admin.tag.1", "<Helper> %s"),
				Map.entry("gameserver.admin.announce.levels", "3,4,5"),
				Map.entry("gameserver.administration.view.npcid", "false")
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
			assertFalse(AdminConfig.SHOW_NPC_ID);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsEnchantsConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class,
			"SOCKET_MANASTONE", "ENCHANT_ITEM", "ENCHANT_ITEM_KINAH",
			"MAX_EQUIPMENT_ENCHANT_LEVEL", "MAX_ESTIMA_ENCHANT_LEVEL", "ENCHANT_ESTIMA", "ESTIMA_ENCHANT_SPEED",
			"MAX_AUTHORIZE_ACCESSORY_LEVEL",
			"MAX_AUTHORIZE_PLUME_LEVEL", "MAX_AUTHORIZE_BRACELET_LEVEL", "ENCHANT_PLUME", "ENCHANT_BRACELET",
			"ENCHANT_ACCESSORY", "ENCHANT_STIGMA", "CLEAN_STONE", "ENCHANT_SPEED", "AUTHORIZE_SPEED",
			"ENABLE_ARCHDAEVA_ITEM_BROKE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.socket.manastone", "75.5"),
				Map.entry("gameserver.enchant.item.value", "65.0"),
				Map.entry("gameserver.enchant.item.kinah", "50000"),
				Map.entry("gameserver.enchant.item.broke", "false"),
				Map.entry("gameserver.enchant.equipment.max.level", "50"),
				Map.entry("gameserver.enchant.estima.max.level", "100"),
				Map.entry("gameserver.enchant.estima.value", "85.0"),
				Map.entry("gameserver.enchant.estima.speed", "1500"),
				Map.entry("gameserver.enchant.authorize.accessory.max.level", "42"),
				Map.entry("gameserver.enchant.authorize.plume.max.level", "48"),
				Map.entry("gameserver.enchant.authorize.bracelet.max.level", "52"),
				Map.entry("gameserver.enchant.plume", "80.0"),
				Map.entry("gameserver.enchant.bracelet", "70.0"),
				Map.entry("gameserver.enchant.accessory", "60.0"),
				Map.entry("gameserver.enchant.stigma", "55.0"),
				Map.entry("gameserver.manastone.clean", "true"),
				Map.entry("gameserver.enchant.cast.speed", "2500"),
				Map.entry("gameserver.enchant.authorize.speed", "1200")
			)));

			EnchantsConfig config = new EnchantsConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(75.5f, EnchantsConfig.SOCKET_MANASTONE);
			assertEquals(65.0f, EnchantsConfig.ENCHANT_ITEM);
			assertEquals(50000, EnchantsConfig.ENCHANT_ITEM_KINAH);
			assertFalse(EnchantsConfig.ENABLE_ARCHDAEVA_ITEM_BROKE);
			assertEquals(50, EnchantsConfig.MAX_EQUIPMENT_ENCHANT_LEVEL);
			assertEquals(100, EnchantsConfig.MAX_ESTIMA_ENCHANT_LEVEL);
			assertEquals(85.0f, EnchantsConfig.ENCHANT_ESTIMA);
			assertEquals(1500, EnchantsConfig.ESTIMA_ENCHANT_SPEED);
			assertEquals(42, EnchantsConfig.MAX_AUTHORIZE_ACCESSORY_LEVEL);
			assertEquals(48, EnchantsConfig.MAX_AUTHORIZE_PLUME_LEVEL);
			assertEquals(52, EnchantsConfig.MAX_AUTHORIZE_BRACELET_LEVEL);
			assertEquals(80.0f, EnchantsConfig.ENCHANT_PLUME);
			assertEquals(70.0f, EnchantsConfig.ENCHANT_BRACELET);
			assertEquals(60.0f, EnchantsConfig.ENCHANT_ACCESSORY);
			assertEquals(55.0f, EnchantsConfig.ENCHANT_STIGMA);
			assertTrue(EnchantsConfig.CLEAN_STONE);
			assertEquals(2500, EnchantsConfig.ENCHANT_SPEED);
			assertEquals(1200, EnchantsConfig.AUTHORIZE_SPEED);
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
	void bindsEnchantsConfigLegacyScalarEstimaProperty() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class, "ENCHANT_ESTIMA");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.enchant.estima", "77.5"
			)));

			EnchantsConfig config = new EnchantsConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(77.5f, EnchantsConfig.ENCHANT_ESTIMA);
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

	@Test
	void bindsRankingConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(RankingConfig.class,
			"TOP_RANKING_UPDATE_SETTING", "TOP_RANKING_UPDATE_RULE", "TOP_RANKING_UPDATE_RULE2",
			"TOP_RANKING_MAX_OFFLINE_DAYS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.top.ranking.update.setting", "false",
				"gameserver.top.ranking.update.hour", "0 0 */3 ? * *",
				"gameserver.top.ranking.update.minute", "20",
				"gameserver.top.ranking.max.offline.days", "7"
			)));

			RankingConfig config = new RankingConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(RankingConfig.TOP_RANKING_UPDATE_SETTING);
			assertEquals("0 0 */3 ? * *", RankingConfig.TOP_RANKING_UPDATE_RULE);
			assertEquals(20, RankingConfig.TOP_RANKING_UPDATE_RULE2);
			assertEquals(7, RankingConfig.TOP_RANKING_MAX_OFFLINE_DAYS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPunishmentConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PunishmentConfig.class,
			"PUNISHMENT_ENABLE", "PUNISHMENT_TYPE", "PUNISHMENT_TIME", "PUNISHMENT_REDUCEAP");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.punishment.enable", "true",
				"gameserver.punishment.type", "2",
				"gameserver.punishment.time", "60",
				"gameserver.punishment.reduceap", "100"
			)));

			PunishmentConfig config = new PunishmentConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(PunishmentConfig.PUNISHMENT_ENABLE);
			assertEquals(2, PunishmentConfig.PUNISHMENT_TYPE);
			assertEquals(60, PunishmentConfig.PUNISHMENT_TIME);
			assertEquals(100, PunishmentConfig.PUNISHMENT_REDUCEAP);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsHtmlConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(HTMLConfig.class,
			"ENABLE_HTML_WELCOME", "ENABLE_GUIDES", "HTML_ROOT", "HTML_ENCODING");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.html.welcome.enable", "true",
				"gameserver.html.guides.enable", "false",
				"gameserver.html.root", "./data/static_data/HTML2/",
				"gameserver.html.encoding", "GBK"
			)));

			HTMLConfig config = new HTMLConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(HTMLConfig.ENABLE_HTML_WELCOME);
			assertFalse(HTMLConfig.ENABLE_GUIDES);
			assertEquals("./data/static_data/HTML2/", HTMLConfig.HTML_ROOT);
			assertEquals("GBK", HTMLConfig.HTML_ENCODING);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsFallDamageConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(FallDamageConfig.class,
			"FALL_DAMAGE_PERCENTAGE", "MINIMUM_DISTANCE_DAMAGE", "MAXIMUM_DISTANCE_DAMAGE",
			"MAXIMUM_DISTANCE_MIDAIR");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.falldamage.percentage", "2.5",
				"gameserver.falldamage.distance.minimum", "15",
				"gameserver.falldamage.distance.maximum", "80",
				"gameserver.falldamage.distance.midair", "300"
			)));

			FallDamageConfig config = new FallDamageConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(2.5f, FallDamageConfig.FALL_DAMAGE_PERCENTAGE);
			assertEquals(15, FallDamageConfig.MINIMUM_DISTANCE_DAMAGE);
			assertEquals(80, FallDamageConfig.MAXIMUM_DISTANCE_DAMAGE);
			assertEquals(300, FallDamageConfig.MAXIMUM_DISTANCE_MIDAIR);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsVeteranRewardConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(VeteranRewardConfig.class,
			"VETERANREWARDS_ENABLED", "VETERANREWARDS_ENABLED_ERROR_LOG", "VETERANREWARDS_ENABLED_INFO_LOG");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.veteranreward.enabled", "true",
				"gameserver.veteranreward.log_error", "false",
				"gameserver.veteranreward.log_info", "false"
			)));

			VeteranRewardConfig config = new VeteranRewardConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(VeteranRewardConfig.VETERANREWARDS_ENABLED);
			assertFalse(VeteranRewardConfig.VETERANREWARDS_ENABLED_ERROR_LOG);
			assertFalse(VeteranRewardConfig.VETERANREWARDS_ENABLED_INFO_LOG);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsSkillConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(SkillConfig.class,
			"COOLDOWN_MULTIPLIER", "CONSUME_DP", "MAGICBOOST_CAP");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.skill.cooldown.multiplier", "0.5",
				"gameserver.skill.dp.consume", "false",
				"gameserver.magicboost.cap", "7000"
			)));

			SkillConfig config = new SkillConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(0.5d, SkillConfig.COOLDOWN_MULTIPLIER);
			assertFalse(SkillConfig.CONSUME_DP);
			assertEquals(7000, SkillConfig.MAGICBOOST_CAP);
			assertEquals(50, SkillConfig.scaleCooldown(100));
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsBrokerConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(BrokerConfig.class,
			"CHECK_EXPIRED_ITEMS_INTERVAL", "ANTI_HACK_PUNISHMENT", "ITEMS_EXPIRE_TIME");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.broker.time.check.expired.items.interval", "120",
				"gameserver.broker.anti.hack.punishment", "1",
				"gameserver.broker.items.expire.time", "24"
			)));

			BrokerConfig config = new BrokerConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(120, BrokerConfig.CHECK_EXPIRED_ITEMS_INTERVAL);
			assertEquals(1, BrokerConfig.ANTI_HACK_PUNISHMENT);
			assertEquals(24, BrokerConfig.ITEMS_EXPIRE_TIME);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsAStationConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(AStationConfig.class,
			"A_STATION_SERVER_ID", "A_STATION_MAX_LEVEL", "A_STATION_ENABLE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"a.station.server.id", "5",
				"a.station.max.level", "85",
				"a.station.enable", "false"
			)));

			AStationConfig config = new AStationConfig();
			Binder.get(environment).bind("a", Bindable.ofInstance(config));

			assertEquals(5, AStationConfig.A_STATION_SERVER_ID);
			assertEquals(85, AStationConfig.A_STATION_MAX_LEVEL);
			assertFalse(AStationConfig.A_STATION_ENABLE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsTransportConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(TransportConfig.class,
			"HOTSPOT_CAST_TIME_SECONDS", "HOTSPOT_COOLDOWN_SECONDS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.transport.hotspot.cast_time_seconds", "3",
				"gameserver.transport.hotspot.cooldown_seconds", "10"
			)));

			TransportConfig config = new TransportConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(3, TransportConfig.HOTSPOT_CAST_TIME_SECONDS);
			assertEquals(10, TransportConfig.HOTSPOT_COOLDOWN_SECONDS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsArchDaevaConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(ArchDaevaConfig.class,
			"CP_LIMIT_MAX", "ITEM_NOT_FOR_HIGHDAEVA_ENABLE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.max.cp.limit", "2000",
				"gameserver.item.not.for.highdaeva.enable", "true"
			)));

			ArchDaevaConfig config = new ArchDaevaConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(2000, ArchDaevaConfig.CP_LIMIT_MAX);
			assertTrue(ArchDaevaConfig.ITEM_NOT_FOR_HIGHDAEVA_ENABLE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPvPModConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PvPModConfig.class, "BG_ENABLED");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.pvp.mod.bg.enabled", "false"
			)));

			PvPModConfig config = new PvPModConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(PvPModConfig.BG_ENABLED);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsAutoGroupConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(AutoGroupConfig.class,
			"AUTO_GROUP_ENABLED", "DREDGION_TIMER", "DREDGION_ENABLED",
			"DREDGION_SCHEDULE_MIDDAY", "DREDGION_SCHEDULE_EVENING", "DREDGION_SCHEDULE_MIDNIGHT",
			"KAMAR_TIMER", "KAMAR_ENABLED", "KAMAR_SCHEDULE_MIDNIGHT",
			"OPHIDAN_TIMER", "OPHIDAN_ENABLED", "OPHIDAN_SCHEDULE_MIDDAY", "OPHIDAN_SCHEDULE_MIDNIGHT",
			"OPHIDAN_WARPATH_TIMER", "OPHIDAN_WARPATH_ENABLED", "OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT",
			"BASTION_TIMER", "BASTION_ENABLED", "BASTION_SCHEDULE_MIDNIGHT",
			"IDGEL_TIMER", "IDGEL_ENABLED", "IDGEL_SCHEDULE_MIDDAY", "IDGEL_SCHEDULE_MIDNIGHT",
			"IDGEL_DOME_LANDMARK_TIMER", "IDGEL_DOME_LANDMARK_ENABLED", "IDGEL_DOME_LANDMARK_SCHEDULE_MIDNIGHT",
			"ASHUNATAL_TIMER", "ASHUNATAL_ENABLED",
			"ASHUNATAL_SCHEDULE_MIDDAY", "ASHUNATAL_SCHEDULE_EVENING", "ASHUNATAL_SCHEDULE_MIDNIGHT",
			"HALL_OF_TENACITY_TIMER", "HALL_OF_TENACITY_ENABLED",
			"HALL_OF_TENACITY_SCHEDULE_MORNING", "HALL_OF_TENACITY_SCHEDULE_EVENING",
			"GRAND_ARENA_TRAINING_CAMP_TIMER", "GRAND_ARENA_TRAINING_CAMP_ENABLED",
			"GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING",
			"IDTM_LOBBY_P02_TIMER", "IDTM_LOBBY_P02_ENABLED", "IDTM_LOBBY_P02_SCHEDULE_EVENING",
			"IDTM_LOBBY_E01_TIMER", "IDTM_LOBBY_E01_ENABLED", "IDTM_LOBBY_E01_SCHEDULE_EVENING",
			"IDRUN_TIMER", "IDRUN_ENABLED", "IDRUN_SCHEDULE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.autogroup.enable", "false"),
				Map.entry("gameserver.dredgion.timer", "45"),
				Map.entry("gameserver.dredgion.enable", "false"),
				Map.entry("gameserver.dredgion.schedule.midday", "0 30 12 ? * MON *"),
				Map.entry("gameserver.dredgion.schedule.evening", "0 30 20 ? * MON *"),
				Map.entry("gameserver.dredgion.schedule.midnight", "0 30 23 ? * MON *"),
				Map.entry("gameserver.kamar.timer", "90"),
				Map.entry("gameserver.kamar.enable", "false"),
				Map.entry("gameserver.kamar.schedule.midnight", "0 30 23 ? * SAT *"),
				Map.entry("gameserver.ophidan.timer", "70"),
				Map.entry("gameserver.ophidan.enable", "false"),
				Map.entry("gameserver.ophidan.schedule.midday", "0 30 12 ? * TUE *"),
				Map.entry("gameserver.ophidan.schedule.midnight", "0 30 23 ? * TUE *"),
				Map.entry("gameserver.ophidan.warpath.timer", "80"),
				Map.entry("gameserver.ophidan.warpath.enable", "false"),
				Map.entry("gameserver.ophidan.warpath.schedule.midnight", "0 30 23 ? * THU *"),
				Map.entry("gameserver.bastion.timer", "75"),
				Map.entry("gameserver.bastion.enable", "false"),
				Map.entry("gameserver.bastion.schedule.midnight", "0 30 23 ? * FRI *"),
				Map.entry("gameserver.idgel.dome.timer", "65"),
				Map.entry("gameserver.idgel.dome.enable", "false"),
				Map.entry("gameserver.idgel.dome.schedule.midday", "0 30 12 ? * WED *"),
				Map.entry("gameserver.idgel.dome.schedule.midnight", "0 30 23 ? * WED *"),
				Map.entry("gameserver.idgel.dome.landmark.timer", "85"),
				Map.entry("gameserver.idgel.dome.landmark.enable", "false"),
				Map.entry("gameserver.idgel.dome.landmark.schedule.midnight", "0 30 23 ? * MON *"),
				Map.entry("gameserver.ashunatal.dredgion.timer", "95"),
				Map.entry("gameserver.ashunatal.dredgion.enable", "false"),
				Map.entry("gameserver.ashunatal.schedule.midday", "0 30 12 ? * FRI *"),
				Map.entry("gameserver.ashunatal.schedule.evening", "0 30 20 ? * FRI *"),
				Map.entry("gameserver.ashunatal.schedule.midnight", "0 30 23 ? * SUN *"),
				Map.entry("gameserver.hall.of.tenacity.timer", "300"),
				Map.entry("gameserver.hall.of.tenacity.enable", "false"),
				Map.entry("gameserver.hall.of.tenacity.schedule.morning", "0 30 9 ? * SAT *"),
				Map.entry("gameserver.hall.of.tenacity.schedule.evening", "0 30 18 ? * MON *"),
				Map.entry("gameserver.grand.arena.training.camp.timer", "320"),
				Map.entry("gameserver.grand.arena.training.camp.enable", "false"),
				Map.entry("gameserver.grand.arena.training.camp.schedule.evening", "0 30 18 ? * SAT *"),
				Map.entry("gameserver.IDTM_Lobby_P02.timer", "340"),
				Map.entry("gameserver.IDTM_Lobby_P02.enable", "false"),
				Map.entry("gameserver.IDTM_Lobby_P02.schedule.evening", "0 30 18 ? * SUN *"),
				Map.entry("gameserver.IDTM_Lobby_E01.timer", "350"),
				Map.entry("gameserver.IDTM_Lobby_E01.enable", "false"),
				Map.entry("gameserver.IDTM_Lobby_E01.schedule.evening", "0 45 18 ? * SAT *"),
				Map.entry("gameserver.IDRun.timer", "55"),
				Map.entry("gameserver.IDRun.enable", "false"),
				Map.entry("gameserver.IDRun.schedule", "0 30 0/2 1/1 * ? *")
			)));

			AutoGroupConfig config = new AutoGroupConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(AutoGroupConfig.AUTO_GROUP_ENABLED);
			assertEquals(45L, AutoGroupConfig.DREDGION_TIMER);
			assertFalse(AutoGroupConfig.DREDGION_ENABLED);
			assertEquals("0 30 12 ? * MON *", AutoGroupConfig.DREDGION_SCHEDULE_MIDDAY);
			assertEquals("0 30 20 ? * MON *", AutoGroupConfig.DREDGION_SCHEDULE_EVENING);
			assertEquals("0 30 23 ? * MON *", AutoGroupConfig.DREDGION_SCHEDULE_MIDNIGHT);
			assertEquals(90L, AutoGroupConfig.KAMAR_TIMER);
			assertFalse(AutoGroupConfig.KAMAR_ENABLED);
			assertEquals("0 30 23 ? * SAT *", AutoGroupConfig.KAMAR_SCHEDULE_MIDNIGHT);
			assertEquals(70L, AutoGroupConfig.OPHIDAN_TIMER);
			assertFalse(AutoGroupConfig.OPHIDAN_ENABLED);
			assertEquals("0 30 12 ? * TUE *", AutoGroupConfig.OPHIDAN_SCHEDULE_MIDDAY);
			assertEquals("0 30 23 ? * TUE *", AutoGroupConfig.OPHIDAN_SCHEDULE_MIDNIGHT);
			assertEquals(80L, AutoGroupConfig.OPHIDAN_WARPATH_TIMER);
			assertFalse(AutoGroupConfig.OPHIDAN_WARPATH_ENABLED);
			assertEquals("0 30 23 ? * THU *", AutoGroupConfig.OPHIDAN_WARPATH_SCHEDULE_MIDNIGHT);
			assertEquals(75L, AutoGroupConfig.BASTION_TIMER);
			assertFalse(AutoGroupConfig.BASTION_ENABLED);
			assertEquals("0 30 23 ? * FRI *", AutoGroupConfig.BASTION_SCHEDULE_MIDNIGHT);
			assertEquals(65L, AutoGroupConfig.IDGEL_TIMER);
			assertFalse(AutoGroupConfig.IDGEL_ENABLED);
			assertEquals("0 30 12 ? * WED *", AutoGroupConfig.IDGEL_SCHEDULE_MIDDAY);
			assertEquals("0 30 23 ? * WED *", AutoGroupConfig.IDGEL_SCHEDULE_MIDNIGHT);
			assertEquals(85L, AutoGroupConfig.IDGEL_DOME_LANDMARK_TIMER);
			assertFalse(AutoGroupConfig.IDGEL_DOME_LANDMARK_ENABLED);
			assertEquals("0 30 23 ? * MON *", AutoGroupConfig.IDGEL_DOME_LANDMARK_SCHEDULE_MIDNIGHT);
			assertEquals(95L, AutoGroupConfig.ASHUNATAL_TIMER);
			assertFalse(AutoGroupConfig.ASHUNATAL_ENABLED);
			assertEquals("0 30 12 ? * FRI *", AutoGroupConfig.ASHUNATAL_SCHEDULE_MIDDAY);
			assertEquals("0 30 20 ? * FRI *", AutoGroupConfig.ASHUNATAL_SCHEDULE_EVENING);
			assertEquals("0 30 23 ? * SUN *", AutoGroupConfig.ASHUNATAL_SCHEDULE_MIDNIGHT);
			assertEquals(300L, AutoGroupConfig.HALL_OF_TENACITY_TIMER);
			assertFalse(AutoGroupConfig.HALL_OF_TENACITY_ENABLED);
			assertEquals("0 30 9 ? * SAT *", AutoGroupConfig.HALL_OF_TENACITY_SCHEDULE_MORNING);
			assertEquals("0 30 18 ? * MON *", AutoGroupConfig.HALL_OF_TENACITY_SCHEDULE_EVENING);
			assertEquals(320L, AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_TIMER);
			assertFalse(AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_ENABLED);
			assertEquals("0 30 18 ? * SAT *", AutoGroupConfig.GRAND_ARENA_TRAINING_CAMP_SCHEDULE_EVENING);
			assertEquals(340L, AutoGroupConfig.IDTM_LOBBY_P02_TIMER);
			assertFalse(AutoGroupConfig.IDTM_LOBBY_P02_ENABLED);
			assertEquals("0 30 18 ? * SUN *", AutoGroupConfig.IDTM_LOBBY_P02_SCHEDULE_EVENING);
			assertEquals(350L, AutoGroupConfig.IDTM_LOBBY_E01_TIMER);
			assertFalse(AutoGroupConfig.IDTM_LOBBY_E01_ENABLED);
			assertEquals("0 45 18 ? * SAT *", AutoGroupConfig.IDTM_LOBBY_E01_SCHEDULE_EVENING);
			assertEquals(55L, AutoGroupConfig.IDRUN_TIMER);
			assertFalse(AutoGroupConfig.IDRUN_ENABLED);
			assertEquals("0 30 0/2 1/1 * ? *", AutoGroupConfig.IDRUN_SCHEDULE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsLegionConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(LegionConfig.class,
			"LEGION_NAME_PATTERN", "SELF_INTRO_PATTERN", "NICKNAME_PATTERN", "ANNOUNCEMENT_PATTERN",
			"LEGION_DISBAND_TIME", "LEGION_CREATE_REQUIRED_KINAH", "LEGION_EMBLEM_REQUIRED_KINAH",
			"ENABLE_GUILD_TASK_REQ", "LEGION_INVITEOTHERFACTION", "LEGION_WAREHOUSE",
			"LEGION_LEVEL1_MAX_MEMBERS", "LEGION_LEVEL2_MAX_MEMBERS", "LEGION_LEVEL8_MAX_MEMBERS",
			"LEGION_LEVEL2_REQUIRED_KINAH", "LEGION_LEVEL2_REQUIRED_MEMBERS", "LEGION_LEVEL2_REQUIRED_CONTRIBUTION",
			"LEGION_LEVEL8_REQUIRED_KINAH", "LEGION_LEVEL8_REQUIRED_MEMBERS", "LEGION_LEVEL8_REQUIRED_CONTRIBUTION",
			"LWH_LEVEL1_SLOTS", "LWH_LEVEL8_SLOTS");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.legion.pattern", "[a-zA-Z]{2,16}"),
				Map.entry("gameserver.legion.self.intro.pattern", ".{1,64}"),
				Map.entry("gameserver.legion.nick.name.pattern", ".{1,20}"),
				Map.entry("gameserver.legion.announcement.pattern", ".{1,512}"),
				Map.entry("gameserver.legion.disband.time", "3600"),
				Map.entry("gameserver.legion.creation.required.kinah", "20000"),
				Map.entry("gameserver.legion.emblem.required.kinah", "30000"),
				Map.entry("gameserver.legion.task.requirement.enable", "false"),
				Map.entry("gameserver.legion.invite.other.faction", "true"),
				Map.entry("gameserver.legion.warehouse.enable", "false"),
				Map.entry("gameserver.legion.level1.max.members", "31"),
				Map.entry("gameserver.legion.level2.required.kinah", "110000"),
				Map.entry("gameserver.legion.level2.required.members", "11"),
				Map.entry("gameserver.legion.level2.required.contribution", "1000"),
				Map.entry("gameserver.legion.level2.max.members", "61"),
				Map.entry("gameserver.legion.level8.required.kinah", "110000000"),
				Map.entry("gameserver.legion.level8.required.members", "71"),
				Map.entry("gameserver.legion.level8.required.contribution", "63500000"),
				Map.entry("gameserver.legion.level8.max.members", "241"),
				Map.entry("gameserver.legion.warehouse.level1.slots", "25"),
				Map.entry("gameserver.legion.warehouse.level8.slots", "81")
			)));

			LegionConfig config = new LegionConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals("[a-zA-Z]{2,16}", LegionConfig.LEGION_NAME_PATTERN.pattern());
			assertEquals(".{1,64}", LegionConfig.SELF_INTRO_PATTERN.pattern());
			assertEquals(".{1,20}", LegionConfig.NICKNAME_PATTERN.pattern());
			assertEquals(".{1,512}", LegionConfig.ANNOUNCEMENT_PATTERN.pattern());
			assertEquals(3600, LegionConfig.LEGION_DISBAND_TIME);
			assertEquals(20000, LegionConfig.LEGION_CREATE_REQUIRED_KINAH);
			assertEquals(30000, LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH);
			assertFalse(LegionConfig.ENABLE_GUILD_TASK_REQ);
			assertTrue(LegionConfig.LEGION_INVITEOTHERFACTION);
			assertFalse(LegionConfig.LEGION_WAREHOUSE);
			assertEquals(31, LegionConfig.LEGION_LEVEL1_MAX_MEMBERS);
			assertEquals(110000, LegionConfig.LEGION_LEVEL2_REQUIRED_KINAH);
			assertEquals(11, LegionConfig.LEGION_LEVEL2_REQUIRED_MEMBERS);
			assertEquals(1000, LegionConfig.LEGION_LEVEL2_REQUIRED_CONTRIBUTION);
			assertEquals(61, LegionConfig.LEGION_LEVEL2_MAX_MEMBERS);
			assertEquals(110000000, LegionConfig.LEGION_LEVEL8_REQUIRED_KINAH);
			assertEquals(71, LegionConfig.LEGION_LEVEL8_REQUIRED_MEMBERS);
			assertEquals(63500000, LegionConfig.LEGION_LEVEL8_REQUIRED_CONTRIBUTION);
			assertEquals(241, LegionConfig.LEGION_LEVEL8_MAX_MEMBERS);
			assertEquals(25, LegionConfig.LWH_LEVEL1_SLOTS);
			assertEquals(81, LegionConfig.LWH_LEVEL8_SLOTS);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsFfaConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(FFAConfig.class,
			"FFA_ENABLED", "FFA_SPREE_REWARD_TOLL_QUANTITY", "FFA_SPREE_REWARD_ITEM",
			"FFA_SPREE_1", "FFA_SPREE_10");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.pvp.mod.ffa.enabled", "false",
				"gameserver.pvp.mod.ffa.spree.toll.quantity", "20",
				"gameserver.pvp.mod.ffa.spree.reward.item", "166030006",
				"gameserver.pvp.mod.ffa.spree1", "test spree 1",
				"gameserver.pvp.mod.ffa.spree10", "test spree 10"
			)));

			FFAConfig config = new FFAConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(FFAConfig.FFA_ENABLED);
			assertEquals(20, FFAConfig.FFA_SPREE_REWARD_TOLL_QUANTITY);
			assertEquals(166030006, FFAConfig.FFA_SPREE_REWARD_ITEM);
			assertEquals("test spree 1", FFAConfig.FFA_SPREE_1);
			assertEquals("test spree 10", FFAConfig.FFA_SPREE_10);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsLoggingConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(LoggingConfig.class,
			"ENABLE_ADVANCED_LOGGING", "LOG_AUDIT", "LOG_CHAT", "LOG_FACTION",
			"LOG_GMAUDIT", "LOG_ITEM", "LOG_KILL", "LOG_PL", "LOG_SIEGE",
			"LOG_HOUSE_AUCTION", "LOG_QUEST_TRACE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.enable.advanced.logging", "true"),
				Map.entry("gameserver.log.audit", "false"),
				Map.entry("gameserver.log.chat", "false"),
				Map.entry("gameserver.log.faction", "true"),
				Map.entry("gameserver.log.gmaudit", "false"),
				Map.entry("gameserver.log.item", "false"),
				Map.entry("gameserver.log.kill", "true"),
				Map.entry("gameserver.log.pl", "true"),
				Map.entry("gameserver.log.siege", "true"),
				Map.entry("gameserver.log.auction", "false"),
				Map.entry("gameserver.log.questtrace", "true")
			)));

			LoggingConfig config = new LoggingConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(LoggingConfig.ENABLE_ADVANCED_LOGGING);
			assertFalse(LoggingConfig.LOG_AUDIT);
			assertFalse(LoggingConfig.LOG_CHAT);
			assertTrue(LoggingConfig.LOG_FACTION);
			assertFalse(LoggingConfig.LOG_GMAUDIT);
			assertFalse(LoggingConfig.LOG_ITEM);
			assertTrue(LoggingConfig.LOG_KILL);
			assertTrue(LoggingConfig.LOG_PL);
			assertTrue(LoggingConfig.LOG_SIEGE);
			assertFalse(LoggingConfig.LOG_HOUSE_AUCTION);
			assertTrue(LoggingConfig.LOG_QUEST_TRACE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsAiConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(AIConfig.class,
			"MOVE_DEBUG", "EVENT_DEBUG", "ONCREATE_DEBUG", "AGGRO_LEVEL_IMMUNE",
			"ENABLE_FEARFUL_BEAST_AI", "ACTIVE_NPC_MOVEMENT", "ENHANCED_HOME_RETURN",
			"MINIMIMUM_DELAY", "MAXIMUM_DELAY", "SHOUTS_ENABLE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.ai.move.debug", "false",
				"gameserver.ai.event.debug", "true",
				"gameserver.ai.oncreate.debug", "true",
				"gameserver.ai.aggro.level.immune", "15",
				"gameserver.npc.fearful_beast.enable", "false",
				"gameserver.npcmovement.enable", "false",
				"gameserver.ai.home.return.enhanced.enable", "false",
				"gameserver.npcmovement.delay.minimum", "5",
				"gameserver.npcmovement.delay.maximum", "25",
				"gameserver.npcshouts.enable", "true"
			)));

			AIConfig config = new AIConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(AIConfig.MOVE_DEBUG);
			assertTrue(AIConfig.EVENT_DEBUG);
			assertTrue(AIConfig.ONCREATE_DEBUG);
			assertEquals(15, AIConfig.AGGRO_LEVEL_IMMUNE);
			assertFalse(AIConfig.ENABLE_FEARFUL_BEAST_AI);
			assertFalse(AIConfig.ACTIVE_NPC_MOVEMENT);
			assertFalse(AIConfig.ENHANCED_HOME_RETURN);
			assertEquals(5, AIConfig.MINIMIMUM_DELAY);
			assertEquals(25, AIConfig.MAXIMUM_DELAY);
			assertTrue(AIConfig.SHOUTS_ENABLE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsInstanceConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(InstanceConfig.class,
			"COOLDOWN_RATE", "DESTROY_DELAY_SECONDS", "SOLO_DESTROY_DELAY_SECONDS",
			"SCALING_ENABLE", "SCALING_HP_FLOOR", "SCALING_DMG_FLOOR");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.instances.cooldown.rate", "2",
				"gameserver.instances.cooldown.filter", "300010000,300020000",
				"gameserver.instance.destroy_delay_seconds", "1200",
				"gameserver.instance.solo.destroy_delay_seconds", "300",
				"gameserver.instance.scaling.enable", "true",
				"gameserver.instance.scaling.hp_floor", "0.8",
				"gameserver.instance.scaling.dmg_floor", "0.7",
				"gameserver.instance.scaling.excluded_maps", "300030000"
			)));

			InstanceConfig config = new InstanceConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(2, InstanceConfig.COOLDOWN_RATE);
			assertEquals(1200, InstanceConfig.DESTROY_DELAY_SECONDS);
			assertEquals(300, InstanceConfig.SOLO_DESTROY_DELAY_SECONDS);
			assertTrue(InstanceConfig.SCALING_ENABLE);
			assertEquals(0.8f, InstanceConfig.SCALING_HP_FLOOR);
			assertEquals(0.7f, InstanceConfig.SCALING_DMG_FLOOR);
			assertTrue(InstanceConfig.isCooldownExcluded(300010000));
			assertTrue(InstanceConfig.isScalingExcluded(300030000));
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsInGameShopConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(InGameShopConfig.class,
			"ENABLE_IN_GAME_SHOP", "ENABLE_GIFT_OTHER_RACE", "ALLOW_GIFTS",
			"GAMESHOP_LIMIT", "GAMESHOP_CATEGORY", "GAMESHOP_LIMIT_TIME");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.ingameshop.enable", "true",
				"gameserver.ingameshop.gift", "true",
				"gameserver.ingameshop.allow.gift", "false",
				"gameserver.gameshop.limit.value", "true",
				"gameserver.gameshop.category", "1",
				"gameserver.gameshop.limit.time", "120"
			)));

			InGameShopConfig config = new InGameShopConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(InGameShopConfig.ENABLE_IN_GAME_SHOP);
			assertTrue(InGameShopConfig.ENABLE_GIFT_OTHER_RACE);
			assertFalse(InGameShopConfig.ALLOW_GIFTS);
			assertTrue(InGameShopConfig.GAMESHOP_LIMIT);
			assertEquals((byte) 1, InGameShopConfig.GAMESHOP_CATEGORY);
			assertEquals(120L, InGameShopConfig.GAMESHOP_LIMIT_TIME);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsWorldConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(WorldConfig.class,
			"WORLD_REGION_SIZE", "WORLD_ACTIVE_TRACE", "WORLD_EMULATE_A_STATION",
			"WORLD_MAX_TWINS_USUAL", "WORLD_MAX_TWINS_BEGINNER");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.world.region.size", "256",
				"gameserver.world.region.active.trace", "false",
				"gameserver.world.emulate.a.station", "true",
				"gameserver.world.max.twincount.usual", "2",
				"gameserver.world.max.twincount.beginner", "5"
			)));

			WorldConfig config = new WorldConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(256, WorldConfig.WORLD_REGION_SIZE);
			assertFalse(WorldConfig.WORLD_ACTIVE_TRACE);
			assertTrue(WorldConfig.WORLD_EMULATE_A_STATION);
			assertEquals(2, WorldConfig.WORLD_MAX_TWINS_USUAL);
			assertEquals(5, WorldConfig.WORLD_MAX_TWINS_BEGINNER);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPricesConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PricesConfig.class,
			"DEFAULT_PRICES", "DEFAULT_MODIFIER", "DEFAULT_TAXES",
			"VENDOR_BUY_MODIFIER", "VENDOR_SELL_MODIFIER");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.prices.default.prices", "120",
				"gameserver.prices.default.modifier", "110",
				"gameserver.prices.default.taxes", "105",
				"gameserver.prices.vendor.buymod", "90",
				"gameserver.prices.vendor.sellmod", "25"
			)));

			PricesConfig config = new PricesConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(120, PricesConfig.DEFAULT_PRICES);
			assertEquals(110, PricesConfig.DEFAULT_MODIFIER);
			assertEquals(105, PricesConfig.DEFAULT_TAXES);
			assertEquals(90, PricesConfig.VENDOR_BUY_MODIFIER);
			assertEquals(25, PricesConfig.VENDOR_SELL_MODIFIER);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsNameConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(NameConfig.class,
			"ALLOW_CUSTOM_NAMES", "CHAR_NAME_PATTERN", "NAME_SEQUENCE_FORBIDDEN",
			"NAME_FORBIDDEN_ENABLE", "NAME_FORBIDDEN_CLIENT");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.name.allow.custom", "true",
				"gameserver.name.characterpattern", "[a-zA-Z0-9]{3,20}",
				"gameserver.name.forbidden.sequences", "admin,gm",
				"gameserver.name.forbidden.enable.client", "false",
				"gameserver.name.forbidden.client", "badword"
			)));

			NameConfig config = new NameConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(NameConfig.ALLOW_CUSTOM_NAMES);
			assertEquals("[a-zA-Z0-9]{3,20}", NameConfig.CHAR_NAME_PATTERN.pattern());
			assertEquals("admin,gm", NameConfig.NAME_SEQUENCE_FORBIDDEN);
			assertFalse(NameConfig.NAME_FORBIDDEN_ENABLE);
			assertEquals("badword", NameConfig.NAME_FORBIDDEN_CLIENT);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsAbyssLandingConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(AbyssLandingConfig.class,
			"ABYSS_LANDING_QUEST_RESET_ENABLED", "ABYSS_LANDING_POINTS_RESET_ENABLED",
			"ABYSS_LANDING_QUEST_RESET_TIME", "ABYSS_LANDING_POINTS_RESET_TIME",
			"ABYSS_LANDING_QUEST_RATE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.landing.quest.reset.enable", "false",
				"gameserver.landing.points.reset.enable", "false",
				"gameserver.landing.quest.reset.time", "0 0 18 ? * FRI *",
				"gameserver.landing.points.reset.time", "0 0 1 ? * TUE *",
				"gameserver.landing.quest.rate", "3"
			)));

			AbyssLandingConfig config = new AbyssLandingConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_ENABLED);
			assertFalse(AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_ENABLED);
			assertEquals("0 0 18 ? * FRI *", AbyssLandingConfig.ABYSS_LANDING_QUEST_RESET_TIME);
			assertEquals("0 0 1 ? * TUE *", AbyssLandingConfig.ABYSS_LANDING_POINTS_RESET_TIME);
			assertEquals(3, AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPanelConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PanelConfig.class,
			"SKILL_PANEL_LEVEL", "DELQUEST_PANEL_LEVEL", "ENDQUEST_PANEL_LEVEL",
			"GIVETITLE_PANEL_LEVEL", "STARTQUEST_PANEL_LEVEL");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.administration.skilpanel", "5",
				"gameserver.administration.delquestpanel", "4",
				"gameserver.administration.endquestpanel", "4",
				"gameserver.administration.givetitlepanel", "5",
				"gameserver.administration.startquestpanel", "5"
			)));

			PanelConfig config = new PanelConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(5, PanelConfig.SKILL_PANEL_LEVEL);
			assertEquals(4, PanelConfig.DELQUEST_PANEL_LEVEL);
			assertEquals(4, PanelConfig.ENDQUEST_PANEL_LEVEL);
			assertEquals(5, PanelConfig.GIVETITLE_PANEL_LEVEL);
			assertEquals(5, PanelConfig.STARTQUEST_PANEL_LEVEL);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsDeveloperConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(DeveloperConfig.class,
			"SPAWN_ENABLE", "SPAWN_CHECK", "ITEM_STAT_ID", "SHOW_PACKETS",
			"SHOW_PACKET_NAMES_INCHAT", "SHOW_PACKET_BYTES_INCHAT", "TOTAL_PACKET_BYTES_INCHAT",
			"FILTERED_PACKETS_INCHAT", "SHOW_PACKETS_INCHAT_ACCESSLEVEL");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.developer.spawn.enable", "false",
				"gameserver.developer.spawn.check", "true",
				"gameserver.developer.itemstat.id", "123",
				"gameserver.developer.showpackets.enable", "true",
				"gameserver.developer.show.packetnames.inchat.enable", "true",
				"gameserver.developer.show.packetbytes.inchat.enable", "true",
				"gameserver.developer.show.packetbytes.inchat.total", "500",
				"gameserver.developer.filter.packets.inchat", "SM_MOVE",
				"gameserver.developer.show.packets.inchat.accesslevel", "3"
			)));

			DeveloperConfig config = new DeveloperConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertFalse(DeveloperConfig.SPAWN_ENABLE);
			assertTrue(DeveloperConfig.SPAWN_CHECK);
			assertEquals(123, DeveloperConfig.ITEM_STAT_ID);
			assertTrue(DeveloperConfig.SHOW_PACKETS);
			assertTrue(DeveloperConfig.SHOW_PACKET_NAMES_INCHAT);
			assertTrue(DeveloperConfig.SHOW_PACKET_BYTES_INCHAT);
			assertEquals(500, DeveloperConfig.TOTAL_PACKET_BYTES_INCHAT);
			assertEquals("SM_MOVE", DeveloperConfig.FILTERED_PACKETS_INCHAT);
			assertEquals(3, DeveloperConfig.SHOW_PACKETS_INCHAT_ACCESSLEVEL);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPvPConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PvPConfig.class,
			"MAX_DAILY_PVP_KILLS", "ENABLE_TOLL_REWARD", "TOLL_CHANCE", "TOLL_QUANTITY",
			"ENABLE_KILLING_SPREE_SYSTEM", "SPREE_KILL_COUNT", "GENOCIDE_KILL_COUNT",
			"SPREE_REWARD_COUNT1", "SPREE_REWARD_ITEM1",
			"ENABLE_GP_REWARD", "ENABLE_GP_LOSE", "ENABLE_GP_FIXED_LOSE", "GP_LOSE",
			"CHAIN_KILL_TIME_RESTRICTION", "CHAIN_KILL_NUMBER_RESTRICTION", "MAX_AUTHORIZED_LEVEL_DIFF",
			"ENABLE_MEDAL_REWARDING", "MEDAL_REWARD_CHANCE", "GENOCIDE_SPECIAL_REWARDING", "SPECIAL_REWARD_CHANCE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.pvp.maxkills", "7"),
				Map.entry("gameserver.pvp.toll.rewarding.enable", "true"),
				Map.entry("gameserver.pvp.toll.reward.chance", "55"),
				Map.entry("gameserver.pvp.toll.reward.quantity", "6"),
				Map.entry("gameserver.pvp.killingspree.enable", "true"),
				Map.entry("gameserver.pvp.raw.killcount.spree", "6"),
				Map.entry("gameserver.pvp.raw.killcount.genocide", "16"),
				Map.entry("gameserver.pvp.spree_reward1.count", "11"),
				Map.entry("gameserver.pvp.spree_reward1.item", "166020001"),
				Map.entry("gameserver.enable.gp.reward", "true"),
				Map.entry("gameserver.enable.gp.lose.value", "true"),
				Map.entry("gameserver.enable.gp.lose.fixed", "true"),
				Map.entry("gameserver.gp.lose", "150"),
				Map.entry("gameserver.pvp.chainkill.time.restriction", "5"),
				Map.entry("gameserver.pvp.chainkill.number.restriction", "35"),
				Map.entry("gameserver.pvp.max.leveldiff.restriction", "12"),
				Map.entry("gameserver.pvp.medal.rewarding.enable", "true"),
				Map.entry("gameserver.pvp.medal.reward.chance", "15"),
				Map.entry("gameserver.pvp.special_reward.type", "3"),
				Map.entry("gameserver.pvp.special_reward.chance", "4")
			)));

			PvPConfig config = new PvPConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(7, PvPConfig.MAX_DAILY_PVP_KILLS);
			assertTrue(PvPConfig.ENABLE_TOLL_REWARD);
			assertEquals(55, PvPConfig.TOLL_CHANCE);
			assertEquals(6, PvPConfig.TOLL_QUANTITY);
			assertTrue(PvPConfig.ENABLE_KILLING_SPREE_SYSTEM);
			assertEquals(6, PvPConfig.SPREE_KILL_COUNT);
			assertEquals(16, PvPConfig.GENOCIDE_KILL_COUNT);
			assertEquals(11, PvPConfig.SPREE_REWARD_COUNT1);
			assertEquals(166020001, PvPConfig.SPREE_REWARD_ITEM1);
			assertTrue(PvPConfig.ENABLE_GP_REWARD);
			assertTrue(PvPConfig.ENABLE_GP_LOSE);
			assertTrue(PvPConfig.ENABLE_GP_FIXED_LOSE);
			assertEquals(150, PvPConfig.GP_LOSE);
			assertEquals(5, PvPConfig.CHAIN_KILL_TIME_RESTRICTION);
			assertEquals(35, PvPConfig.CHAIN_KILL_NUMBER_RESTRICTION);
			assertEquals(12, PvPConfig.MAX_AUTHORIZED_LEVEL_DIFF);
			assertTrue(PvPConfig.ENABLE_MEDAL_REWARDING);
			assertEquals(15f, PvPConfig.MEDAL_REWARD_CHANCE);
			assertEquals(3, PvPConfig.GENOCIDE_SPECIAL_REWARDING);
			assertEquals(4f, PvPConfig.SPECIAL_REWARD_CHANCE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPvPConfigLegacyGpLoseScalar() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PvPConfig.class, "ENABLE_GP_LOSE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
				"gameserver.enable.gp.lose", "true"
			)));

			PvPConfig config = new PvPConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(PvPConfig.ENABLE_GP_LOSE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsEventsConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EventsConfig.class,
			"EVENT_ENABLED", "ENABLE_DECOR", "EVENT_GIVE_JUICE", "ENABLE_EVENT_SERVICE",
			"ENABLE_VIP_TICKETS", "VIP_TICKETS_PERIOD", "ENABLE_AWAKE_EVENT", "SEED_TRANSFORMATION_PERIOD",
			"IMPERIAL_TOMB_ENABLE", "IMPERIAL_TOMB_TIMER", "IMPERIAL_TOMB_TIMES",
			"ENABLE_CRAZY", "CRAZY_LOWEST_RND", "CRAZY_TIMES", "CRAZY_ENDTIME",
			"ENABLE_EVENT_ARCADE", "EVENT_ARCADE_CHANCE",
			"ENABLE_ABYSS_EVENT", "ABYSS_EVENT_SCHEDULE", "ABYSS_EVENT_REWARDS",
			"ENABLE_PIG_POPPY_EVENT", "PIG_POPPY_EVENT_SCHEDULE", "PIG_POPPY_EVENT_COUNT_REWARD", "PIG_POPPY_REWARDS",
			"EVENT_SHUGOSWEEP_BOARD", "EVENT_SHUGOSWEEP_FREEDICE", "EVENT_YOUTUBE_VIDEO");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.event.enable", "true"),
				Map.entry("gameserver.enable.decor", "3"),
				Map.entry("gameserver.events.give.juice", "160009018"),
				Map.entry("gameserver.event.service.enable", "true"),
				Map.entry("gameserver.vip.tickets.enable", "true"),
				Map.entry("gameserver.vip.tickets.time", "90"),
				Map.entry("gameserver.event.awake.enable", "true"),
				Map.entry("gameserver.event.seed.transformation.time", "75"),
				Map.entry("gameserver.shugo.imperial.tomb.enable", "false"),
				Map.entry("gameserver.shugo.imperial.tomb.timer.from.start.to.end", "15"),
				Map.entry("gameserver.shugo.imperial.tomb.time.to.start", "0 0 1,13,21,0 ? * *"),
				Map.entry("gameserver.crazy.daeva.enable", "true"),
				Map.entry("gameserver.crazy.daeva.lowest.rnd", "12"),
				Map.entry("gameserver.crazy.daeva.time.to.start", "0 0 1,13,21,0 ? * *"),
				Map.entry("gameserver.crazy.daeva.endtime", "7"),
				Map.entry("gameserver.event.arcade.enable", "true"),
				Map.entry("gameserver.upgrade.arcade.chance", "60"),
				Map.entry("gameserver.event.abyss.treasure.enable", "true"),
				Map.entry("gameserver.event.abyss.treasure.time", "0 0 16 ? * SUN"),
				Map.entry("gameserver.event.abyss.rewards", "125040073"),
				Map.entry("gameserver.event.pig.poppy.enable", "false"),
				Map.entry("gameserver.event.pig.poppy.value", "0 0 23 ? * SAT"),
				Map.entry("gameserver.event.pig.poppy.reward.count", "8"),
				Map.entry("gameserver.pig.poppy.rewards", "125040074"),
				Map.entry("gameserver.event.shugoSweep.board", "4"),
				Map.entry("gameserver.event.shugoSweep.freeDice", "9"),
				Map.entry("gameserver.event.youtube_video", "https://example.com/embed/x")
			)));

			EventsConfig config = new EventsConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(EventsConfig.EVENT_ENABLED);
			assertEquals(3, EventsConfig.ENABLE_DECOR);
			assertEquals(160009018, EventsConfig.EVENT_GIVE_JUICE);
			assertTrue(EventsConfig.ENABLE_EVENT_SERVICE);
			assertTrue(EventsConfig.ENABLE_VIP_TICKETS);
			assertEquals(90, EventsConfig.VIP_TICKETS_PERIOD);
			assertTrue(EventsConfig.ENABLE_AWAKE_EVENT);
			assertEquals(75, EventsConfig.SEED_TRANSFORMATION_PERIOD);
			assertFalse(EventsConfig.IMPERIAL_TOMB_ENABLE);
			assertEquals(15L, EventsConfig.IMPERIAL_TOMB_TIMER);
			assertEquals("0 0 1,13,21,0 ? * *", EventsConfig.IMPERIAL_TOMB_TIMES);
			assertTrue(EventsConfig.ENABLE_CRAZY);
			assertEquals(12, EventsConfig.CRAZY_LOWEST_RND);
			assertEquals("0 0 1,13,21,0 ? * *", EventsConfig.CRAZY_TIMES);
			assertEquals(7, EventsConfig.CRAZY_ENDTIME);
			assertTrue(EventsConfig.ENABLE_EVENT_ARCADE);
			assertEquals(60, EventsConfig.EVENT_ARCADE_CHANCE);
			assertTrue(EventsConfig.ENABLE_ABYSS_EVENT);
			assertEquals("0 0 16 ? * SUN", EventsConfig.ABYSS_EVENT_SCHEDULE);
			assertEquals("125040073", EventsConfig.ABYSS_EVENT_REWARDS);
			assertFalse(EventsConfig.ENABLE_PIG_POPPY_EVENT);
			assertEquals("0 0 23 ? * SAT", EventsConfig.PIG_POPPY_EVENT_SCHEDULE);
			assertEquals(8, EventsConfig.PIG_POPPY_EVENT_COUNT_REWARD);
			assertEquals("125040074", EventsConfig.PIG_POPPY_REWARDS);
			assertEquals(4, EventsConfig.EVENT_SHUGOSWEEP_BOARD);
			assertEquals(9, EventsConfig.EVENT_SHUGOSWEEP_FREEDICE);
			assertEquals("https://example.com/embed/x", EventsConfig.EVENT_YOUTUBE_VIDEO);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsMembershipConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(MembershipConfig.class,
			"INSTANCES_TITLE_REQ", "INSTANCES_RACE_REQ", "INSTANCES_COOLDOWN", "INSTANCES_ITEM_REQ",
			"STORE_WH_ALL", "STORE_AWH_ALL", "STORE_LWH_ALL", "TRADE_ALL", "DISABLE_SOULBIND",
			"REMODEL_ALL", "EMOTIONS_ALL", "TITLES_ADDITIONAL_ENABLE", "STIGMA_SLOT_QUEST",
			"DISABLE_SOULSICKNESS", "STIGMA_AUTOLEARN", "CHARACTER_ADDITIONAL_ENABLE",
			"ADVANCED_FRIENDLIST_ENABLE", "CHARACTER_ADDITIONAL_COUNT", "ADVANCED_FRIENDLIST_SIZE",
			"PLAYER_TAG_30", "PLAYER_TAG_34", "PREMIUM_TAG_DISPLAY_ENABLE", "TAG_VIP", "TAG_PREMIUM");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.instances.title.requirement", "11"),
				Map.entry("gameserver.instances.race.requirement", "12"),
				Map.entry("gameserver.instances.cooldown", "13"),
				Map.entry("gameserver.instances.item.requirement", "14"),
				Map.entry("gameserver.store.wh.all", "21"),
				Map.entry("gameserver.store.accountwh.all", "22"),
				Map.entry("gameserver.store.legionwh.all", "23"),
				Map.entry("gameserver.trade.all", "24"),
				Map.entry("gameserver.disable.soulbind", "25"),
				Map.entry("gameserver.remodel.all", "26"),
				Map.entry("gameserver.emotions.all", "27"),
				Map.entry("gameserver.titles.additional.enable", "28"),
				Map.entry("gameserver.quest.stigma.slot", "29"),
				Map.entry("gameserver.soulsickness.disable", "30"),
				Map.entry("gameserver.autolearn.stigma", "31"),
				Map.entry("gameserver.character.additional.enable", "32"),
				Map.entry("gameserver.advanced.friendlist.enable", "33"),
				Map.entry("gameserver.character.additional.count", "9"),
				Map.entry("gameserver.advanced.friendlist.size", "95"),
				Map.entry("gameserver.player.tag.30", "TAG30"),
				Map.entry("gameserver.player.tag.34", "TAG34"),
				Map.entry("gameserver.membership.tag.display.enable", "false"),
				Map.entry("gameserver.membership.tag.vip", "V %s"),
				Map.entry("gameserver.membership.tag.premium", "P %s")
			)));

			MembershipConfig config = new MembershipConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(11, MembershipConfig.INSTANCES_TITLE_REQ);
			assertEquals(12, MembershipConfig.INSTANCES_RACE_REQ);
			assertEquals(13, MembershipConfig.INSTANCES_COOLDOWN);
			assertEquals(14, MembershipConfig.INSTANCES_ITEM_REQ);
			assertEquals(21, MembershipConfig.STORE_WH_ALL);
			assertEquals(22, MembershipConfig.STORE_AWH_ALL);
			assertEquals(23, MembershipConfig.STORE_LWH_ALL);
			assertEquals(24, MembershipConfig.TRADE_ALL);
			assertEquals(25, MembershipConfig.DISABLE_SOULBIND);
			assertEquals(26, MembershipConfig.REMODEL_ALL);
			assertEquals(27, MembershipConfig.EMOTIONS_ALL);
			assertEquals(28, MembershipConfig.TITLES_ADDITIONAL_ENABLE);
			assertEquals(29, MembershipConfig.STIGMA_SLOT_QUEST);
			assertEquals(30, MembershipConfig.DISABLE_SOULSICKNESS);
			assertEquals(31, MembershipConfig.STIGMA_AUTOLEARN);
			assertEquals(32, MembershipConfig.CHARACTER_ADDITIONAL_ENABLE);
			assertEquals(33, MembershipConfig.ADVANCED_FRIENDLIST_ENABLE);
			assertEquals(9, MembershipConfig.CHARACTER_ADDITIONAL_COUNT);
			assertEquals(95, MembershipConfig.ADVANCED_FRIENDLIST_SIZE);
			assertEquals("TAG30", MembershipConfig.PLAYER_TAG_30);
			assertEquals("TAG34", MembershipConfig.PLAYER_TAG_34);
			assertFalse(MembershipConfig.PREMIUM_TAG_DISPLAY_ENABLE);
			assertEquals("V %s", MembershipConfig.TAG_VIP);
			assertEquals("P %s", MembershipConfig.TAG_PREMIUM);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsGeoDataConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(GeoDataConfig.class,
			"GEO_ENABLE", "CANSEE_ENABLE", "FEAR_ENABLE", "GEO_NPC_MOVE", "GEO_MATERIALS_ENABLE",
			"GEO_SHIELDS_ENABLE", "GEO_PATH_ENABLE", "GEO_PATH_DISTANCE_TIERS_ENABLE",
			"GEO_PATH_RECOVERY_ENABLE", "GEO_PATH_HIERARCHICAL_ENABLE", "GEO_PATH_WAYPOINT_LOOKAHEAD",
			"GEO_PATH_CACHE_SIZE", "GEO_PATH_MAX_NODES", "GEO_PATH_TIMEOUT_MS", "GEO_PATH_SPATIAL_STEP",
			"GEO_PATH_WORKERS", "GEO_PATH_QUEUE_CAPACITY", "GEO_NPC_WALK_PATH_MODE",
			"GEO_NPC_WALK_COLLISION_ENABLE", "GEO_NPC_WALK_ROUTE_VALIDATE",
			"GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.geodata.enable", "true"),
				Map.entry("gameserver.geodata.cansee.enable", "false"),
				Map.entry("gameserver.geodata.fear.enable", "false"),
				Map.entry("gameserver.geo.npc.move", "true"),
				Map.entry("gameserver.geo.materials.enable", "true"),
				Map.entry("gameserver.geo.shields.enable", "true"),
				Map.entry("gameserver.geo.path.enable", "true"),
				Map.entry("gameserver.geo.path.distance.tiers.enable", "true"),
				Map.entry("gameserver.geo.path.recovery.enable", "false"),
				Map.entry("gameserver.geo.path.hierarchical.enable", "true"),
				Map.entry("gameserver.geo.path.waypoint.lookahead", "5"),
				Map.entry("gameserver.geo.path.cache.size", "48"),
				Map.entry("gameserver.geo.path.max.nodes", "60000"),
				Map.entry("gameserver.geo.path.timeout.ms", "300"),
				Map.entry("gameserver.geo.path.spatial.step", "3"),
				Map.entry("gameserver.geo.path.workers", "6"),
				Map.entry("gameserver.geo.path.queue.capacity", "512"),
				Map.entry("gameserver.geo.npc.walk.path.mode", "blocked"),
				Map.entry("gameserver.geo.npc.walk.collision.enable", "true"),
				Map.entry("gameserver.geo.npc.walk.route.validate.value", "log"),
				Map.entry("gameserver.geo.npc.walk.route.validate.summary.enable", "true")
			)));

			GeoDataConfig config = new GeoDataConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertTrue(GeoDataConfig.GEO_ENABLE);
			assertFalse(GeoDataConfig.CANSEE_ENABLE);
			assertFalse(GeoDataConfig.FEAR_ENABLE);
			assertTrue(GeoDataConfig.GEO_NPC_MOVE);
			assertTrue(GeoDataConfig.GEO_MATERIALS_ENABLE);
			assertTrue(GeoDataConfig.GEO_SHIELDS_ENABLE);
			assertTrue(GeoDataConfig.GEO_PATH_ENABLE);
			assertTrue(GeoDataConfig.GEO_PATH_DISTANCE_TIERS_ENABLE);
			assertFalse(GeoDataConfig.GEO_PATH_RECOVERY_ENABLE);
			assertTrue(GeoDataConfig.GEO_PATH_HIERARCHICAL_ENABLE);
			assertEquals(5, GeoDataConfig.GEO_PATH_WAYPOINT_LOOKAHEAD);
			assertEquals(48, GeoDataConfig.GEO_PATH_CACHE_SIZE);
			assertEquals(60000, GeoDataConfig.GEO_PATH_MAX_NODES);
			assertEquals(300, GeoDataConfig.GEO_PATH_TIMEOUT_MS);
			assertEquals(3f, GeoDataConfig.GEO_PATH_SPATIAL_STEP);
			assertEquals(6, GeoDataConfig.GEO_PATH_WORKERS);
			assertEquals(512, GeoDataConfig.GEO_PATH_QUEUE_CAPACITY);
			assertEquals("blocked", GeoDataConfig.GEO_NPC_WALK_PATH_MODE);
			assertTrue(GeoDataConfig.GEO_NPC_WALK_COLLISION_ENABLE);
			assertEquals("log", GeoDataConfig.GEO_NPC_WALK_ROUTE_VALIDATE);
			assertTrue(GeoDataConfig.GEO_NPC_WALK_ROUTE_VALIDATE_SUMMARY_ENABLE);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsPlayerTransferConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(PlayerTransferConfig.class,
			"MAX_KINAH", "BIND_ELYOS", "BIND_ASMO", "ALLOW_EMOTIONS", "ALLOW_MACRO", "ALLOW_INV",
			"ALLOW_WAREHOUSE", "ALLOW_STIGMA", "BLOCK_SAMENAME", "NAME_PREFIX", "REUSE_HOURS",
			"REMOVE_SKILL_LIST");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("ptransfer.max.kinah", "500000"),
				Map.entry("ptransfer.bindpoint.elyos", "210010000 1 2 3 4"),
				Map.entry("ptransfer.bindpoint.asmo", "220010000 5 6 7 8"),
				Map.entry("ptransfer.allow.emotions", "false"),
				Map.entry("ptransfer.allow.macro", "false"),
				Map.entry("ptransfer.allow.inventory", "false"),
				Map.entry("ptransfer.allow.warehouse", "false"),
				Map.entry("ptransfer.allow.stigma", "false"),
				Map.entry("ptransfer.block.samename", "true"),
				Map.entry("ptransfer.server.name.prefix", "_TST"),
				Map.entry("ptransfer.retransfer.hours", "24"),
				Map.entry("ptransfer.remove.skills.list", "1,2,3")
			)));

			PlayerTransferConfig config = new PlayerTransferConfig();
			Binder.get(environment).bind("ptransfer", Bindable.ofInstance(config));

			assertEquals(500000L, PlayerTransferConfig.MAX_KINAH);
			assertEquals("210010000 1 2 3 4", PlayerTransferConfig.BIND_ELYOS);
			assertEquals("220010000 5 6 7 8", PlayerTransferConfig.BIND_ASMO);
			assertFalse(PlayerTransferConfig.ALLOW_EMOTIONS);
			assertFalse(PlayerTransferConfig.ALLOW_MACRO);
			assertFalse(PlayerTransferConfig.ALLOW_INV);
			assertFalse(PlayerTransferConfig.ALLOW_WAREHOUSE);
			assertFalse(PlayerTransferConfig.ALLOW_STIGMA);
			assertTrue(PlayerTransferConfig.BLOCK_SAMENAME);
			assertEquals("_TST", PlayerTransferConfig.NAME_PREFIX);
			assertEquals(24, PlayerTransferConfig.REUSE_HOURS);
			assertEquals("1,2,3", PlayerTransferConfig.REMOVE_SKILL_LIST);
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void bindsGsConfigProperties() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(GSConfig.class,
			"SERVER_COUNTRY_CODE", "PLAYER_MAX_LEVEL", "TIME_ZONE_ID", "ENABLE_CHAT_SERVER",
			"STARTUP_PROGRESS_ENABLE", "STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE",
			"CHARACTER_CREATION_MODE", "CHARACTER_LIMIT_COUNT", "CHARACTER_DELETE_DELAY_MINUTES",
			"CHARACTER_FACTION_LIMITATION_MODE", "ENABLE_RATIO_LIMITATION", "RATIO_MIN_REQUIRED_LEVEL",
			"RATIO_MIN_CHARACTERS_COUNT", "ABYSSRANKING_SMALL_CACHE", "CHARACTER_REENTRY_TIME",
			"SERVER_YAADMINPANEL_SWITCH_ON");
		try {
			StandardEnvironment environment = new StandardEnvironment();
			environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
				Map.entry("gameserver.country.code", "5"),
				Map.entry("gameserver.players.max.level", "85"),
				Map.entry("gameserver.timezone", "Asia/Shanghai"),
				Map.entry("gameserver.chatserver.enable", "true"),
				Map.entry("gameserver.startup.progress.enable", "false"),
				Map.entry("gameserver.static_data.progress.entry_counts.enable", "true"),
				Map.entry("gameserver.character.creation.mode", "1"),
				Map.entry("gameserver.character.limit.count", "10"),
				Map.entry("gameserver.character.delete.delay.minutes", "15"),
				Map.entry("gameserver.character.faction.limitation.mode", "2"),
				Map.entry("gameserver.ratio.limitation.enable", "true"),
				Map.entry("gameserver.ratio.min.required.level", "12"),
				Map.entry("gameserver.ratio.min.characters_count", "60"),
				Map.entry("gameserver.abyssranking.small.cache", "true"),
				Map.entry("gameserver.character.reentry.time", "30"),
				Map.entry("gameserver.yaadminpanel.server.enable", "true")
			)));

			GSConfig config = new GSConfig();
			Binder.get(environment).bind("gameserver", Bindable.ofInstance(config));

			assertEquals(5, GSConfig.SERVER_COUNTRY_CODE);
			assertEquals(85, GSConfig.PLAYER_MAX_LEVEL);
			assertEquals("Asia/Shanghai", GSConfig.TIME_ZONE_ID);
			assertTrue(GSConfig.ENABLE_CHAT_SERVER);
			assertFalse(GSConfig.STARTUP_PROGRESS_ENABLE);
			assertTrue(GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE);
			assertEquals(1, GSConfig.CHARACTER_CREATION_MODE);
			assertEquals(10, GSConfig.CHARACTER_LIMIT_COUNT);
			assertEquals(15, GSConfig.CHARACTER_DELETE_DELAY_MINUTES);
			assertEquals(2, GSConfig.CHARACTER_FACTION_LIMITATION_MODE);
			assertTrue(GSConfig.ENABLE_RATIO_LIMITATION);
			assertEquals(12, GSConfig.RATIO_MIN_REQUIRED_LEVEL);
			assertEquals(60, GSConfig.RATIO_MIN_CHARACTERS_COUNT);
			assertTrue(GSConfig.ABYSSRANKING_SMALL_CACHE);
			assertEquals(30, GSConfig.CHARACTER_REENTRY_TIME);
			assertTrue(GSConfig.SERVER_YAADMINPANEL_SWITCH_ON);
		} finally {
			snapshot.restore();
		}
	}
}
