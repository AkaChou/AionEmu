package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 坠落伤害相关配置。
 * Fall damage related configuration.
 */
public class FallDamageConfig {

	/**
	 * 每米坠落伤害百分比。
	 * Percentage of damage per meter fallen.
	 */
	@Property(key = "gameserver.falldamage.percentage", defaultValue = "1.0")
	public static float FALL_DAMAGE_PERCENTAGE;

	/**
	 * 产生坠落伤害的最小距离。
	 * Minimum fall distance that causes damage.
	 */
	@Property(key = "gameserver.falldamage.distance.minimum", defaultValue = "10")
	public static int MINIMUM_DISTANCE_DAMAGE;

	/**
	 * 落地必死的最大坠落距离。
	 * Maximum fall distance after which landing is fatal.
	 */
	@Property(key = "gameserver.falldamage.distance.maximum", defaultValue = "50")
	public static int MAXIMUM_DISTANCE_DAMAGE;

	/**
	 * 空中必死的最大坠落距离。
	 * Maximum fall distance after which death occurs mid-air.
	 */
	@Property(key = "gameserver.falldamage.distance.midair", defaultValue = "200")
	public static int MAXIMUM_DISTANCE_MIDAIR;
}
