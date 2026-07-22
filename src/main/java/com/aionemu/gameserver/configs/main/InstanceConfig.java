package com.aionemu.gameserver.configs.main;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.aionemu.commons.configuration.Property;

/**
 * 副本冷却、销毁与缩放相关配置。
 * Instance cooldown, destroy delay and scaling related configuration.
 */
public class InstanceConfig {

	/**
	 * 副本冷却倍率。
	 * Instance cooldown rate multiplier.
	 */
	@Property(key = "gameserver.instances.cooldown.rate", defaultValue = "1")
	public static double COOLDOWN_RATE;

	/**
	 * 排除冷却倍率的地图 ID 列表（逗号分隔）。
	 * Comma-separated map IDs excluded from cooldown rate.
	 */
	@Property(key = "gameserver.instances.cooldown.filter", defaultValue = "")
	private static String cooldownExcludedMaps;

	/**
	 * 队伍/团队副本销毁延迟（秒）。
	 * Destroy delay in seconds for party/alliance instances.
	 */
	@Property(key = "gameserver.instance.destroy_delay_seconds", defaultValue = "600")
	public static int DESTROY_DELAY_SECONDS;

	/**
	 * 单人副本销毁延迟（秒）。
	 * Destroy delay in seconds for solo instances.
	 */
	@Property(key = "gameserver.instance.solo.destroy_delay_seconds", defaultValue = "600")
	public static int SOLO_DESTROY_DELAY_SECONDS;

	/**
	 * 是否启用副本属性缩放。
	 * Whether instance stat scaling is enabled.
	 */
	@Property(key = "gameserver.instance.scaling.enable", defaultValue = "false")
	public static boolean SCALING_ENABLE;

	/**
	 * 副本 HP 缩放下限（0-1）。
	 * Instance HP scaling floor (0-1).
	 */
	@Property(key = "gameserver.instance.scaling.hp_floor", defaultValue = "0.5")
	public static float SCALING_HP_FLOOR;

	/**
	 * 副本伤害缩放下限（0-1）。
	 * Instance damage scaling floor (0-1).
	 */
	@Property(key = "gameserver.instance.scaling.dmg_floor", defaultValue = "0.5")
	public static float SCALING_DMG_FLOOR;

	/**
	 * 排除属性缩放的地图 ID 列表（逗号分隔）。
	 * Comma-separated map IDs excluded from scaling.
	 */
	@Property(key = "gameserver.instance.scaling.excluded_maps", defaultValue = "")
	private static String scalingExcludedMaps;

	/**
	 * 已解析的冷却排除地图集合。
	 * Parsed set of cooldown-excluded map IDs.
	 */
	private static Set<Integer> cooldownExclusions = Collections.emptySet();

	/**
	 * 已解析的缩放排除地图集合。
	 * Parsed set of scaling-excluded map IDs.
	 */
	private static Set<Integer> scalingExclusions = Collections.emptySet();

	/**
	 * 校验配置并刷新排除地图集合。
	 * Validates config values and refreshes exclusion map sets.
	 */
	public static void refresh() {
		if (!Double.isFinite(COOLDOWN_RATE) || COOLDOWN_RATE < 0 || COOLDOWN_RATE > 1
				|| COOLDOWN_RATE > 0 && COOLDOWN_RATE < 0.01) {
			throw new IllegalArgumentException("Instance cooldown rate must be 0 or between 0.01 and 1");
		}
		if (DESTROY_DELAY_SECONDS < 0 || SOLO_DESTROY_DELAY_SECONDS < 0) {
			throw new IllegalArgumentException("Instance destroy delays must not be negative");
		}
		if (SCALING_HP_FLOOR < 0 || SCALING_HP_FLOOR > 1 || SCALING_DMG_FLOOR < 0 || SCALING_DMG_FLOOR > 1) {
			throw new IllegalArgumentException("Instance scaling floors must be between 0 and 1");
		}
		cooldownExclusions = parseMapIds(cooldownExcludedMaps);
		scalingExclusions = parseMapIds(scalingExcludedMaps);
	}

	/**
	 * 判断地图是否排除冷却倍率。
	 * Returns whether the map is excluded from cooldown rate.
	 */
	public static boolean isCooldownExcluded(int mapId) {
		return cooldownExclusions.contains(mapId);
	}

	/**
	 * 判断地图是否排除属性缩放。
	 * Returns whether the map is excluded from scaling.
	 */
	public static boolean isScalingExcluded(int mapId) {
		return scalingExclusions.contains(mapId);
	}

	/**
	 * 将逗号分隔的地图 ID 字符串解析为不可变集合。
	 * Parses a comma-separated map ID string into an unmodifiable set.
	 */
	private static Set<Integer> parseMapIds(String value) {
		if (value == null || value.isBlank()) {
			return Collections.emptySet();
		}
		return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(id -> !id.isEmpty() && !id.equals("0"))
				.map(Integer::valueOf)
				.collect(Collectors.toUnmodifiableSet());
	}
}
