package com.aionemu.gameserver.model.templates.spawns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * {@link TemporarySpawn} 时间窗解析与缓存契约的回归用例。
 * Regression coverage for {@link TemporarySpawn} time-window parsing and caching.
 *
 * <p>背景：时间窗以前每次调用都用 {@code String.split("\\.")} 重新解析（{@code .} 不是单字符快路径，会走正则编译），
 * 游戏内 300s JFR 显示该站点占 17.2% 采样分配。现在改为首次使用时解析并缓存，因此这里同时锁定“解析结果正确”与
 * “加载后不再重解析”两条契约。 Background: the window used to be re-parsed with {@code String.split("\\.")} on every call;
 * the 300s gameplay JFR attributed 17.2% of sampled allocation to that site. It is now parsed once and cached, so this
 * test pins both the parsed values and the parse-once behaviour.</p>
 */
class TemporarySpawnTimeWindowTest {

	private static final String SPAWN_TIME = "spawnTime";
	private static final String DESPAWN_TIME = "despawnTime";

	/** 通配段解析为 null（{@code 18.*.*}）。 / Wildcard segments parse to null. */
	@Test
	void wildcardSegmentsBecomeNull() throws ReflectiveOperationException {
		TemporarySpawn spawn = temporarySpawn("18.*.*", "6.*.*");

		assertEquals(18, spawn.geSpawnHour());
		assertNull(spawn.geSpawnDay());
		assertNull(spawn.getSpawnMonth());
		assertEquals(6, spawn.geDespawnHour());
		assertNull(spawn.geDespawnDay());
		assertNull(spawn.getDespawnMonth());
	}

	/** 完整的时间窗按 时/日/月 解析。 / A full window parses into hour/day/month. */
	@Test
	void concreteSegmentsAreParsedInOrder() throws ReflectiveOperationException {
		TemporarySpawn spawn = temporarySpawn("6.5.3", "18.*.*");

		assertEquals(6, spawn.geSpawnHour());
		assertEquals(5, spawn.geSpawnDay());
		assertEquals(3, spawn.getSpawnMonth());
		assertEquals(18, spawn.geDespawnHour());
	}

	/** 未配置时间窗时六个取值全为 null（与旧实现一致）。 / An absent window yields nulls, as before. */
	@Test
	void absentWindowYieldsNulls() throws ReflectiveOperationException {
		TemporarySpawn spawn = temporarySpawn(null, null);

		assertNull(spawn.geSpawnHour());
		assertNull(spawn.geSpawnDay());
		assertNull(spawn.getSpawnMonth());
		assertNull(spawn.geDespawnHour());
		assertNull(spawn.geDespawnDay());
		assertNull(spawn.getDespawnMonth());
	}

	/**
	 * 时间窗在首次读取时解析并缓存：模板加载完成后字段不再变化，改写字段也不会影响已缓存的结果。
	 * The window is parsed and cached on first read: templates are immutable after loading, so later field writes do not
	 * change the cached window.
	 */
	@Test
	void windowIsParsedOnceAndCached() throws ReflectiveOperationException {
		TemporarySpawn spawn = temporarySpawn("7.*.*", "18.*.*");
		assertEquals(7, spawn.geSpawnHour());

		// 缓存建立后改写原字符串不应影响结果。 / After the cache is built, rewriting the raw string must not affect the result.
		setField(spawn, SPAWN_TIME, "23.*.*");

		assertEquals(7, spawn.geSpawnHour());
		assertEquals(18, spawn.geDespawnHour());
	}

	/**
	 * 构造一个仅注入时间窗字段的模板。
	 * Builds a template with only the time-window fields injected.
	 *
	 * @param spawnTime 刷新时间 / spawn time
	 * @param despawnTime 消失时间 / despawn time
	 * @return 模板实例 / the template
	 * @throws ReflectiveOperationException 字段不存在时 / when the field is missing
	 */
	private static TemporarySpawn temporarySpawn(String spawnTime, String despawnTime) throws ReflectiveOperationException {
		TemporarySpawn spawn = new TemporarySpawn();
		setField(spawn, SPAWN_TIME, spawnTime);
		setField(spawn, DESPAWN_TIME, despawnTime);
		return spawn;
	}

	/**
	 * 通过反射写入私有字段（XML 在加载期直接注入字段）。
	 * Writes a private field via reflection, mirroring how the XML binder injects it at load time.
	 *
	 * @param spawn 模板 / the template
	 * @param name 字段名 / field name
	 * @param value 字段值 / field value
	 * @throws ReflectiveOperationException 字段不存在时 / when the field is missing
	 */
	private static void setField(TemporarySpawn spawn, String name, String value) throws ReflectiveOperationException {
		Field field = TemporarySpawn.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(spawn, value);
	}
}
