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
}
