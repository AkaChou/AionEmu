package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 坠落伤害相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Fall damage related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	@Getter
	private final FalldamageProperties falldamage = new FalldamageProperties();

	@Getter
	public static class FalldamageProperties {
		private final DistanceProperties distance = new DistanceProperties();

		public float getPercentage() {
			return FALL_DAMAGE_PERCENTAGE;
		}

		public void setPercentage(float percentage) {
			FALL_DAMAGE_PERCENTAGE = percentage;
		}

		@Getter
		public static class DistanceProperties {
			public int getMinimum() {
				return MINIMUM_DISTANCE_DAMAGE;
			}

			public void setMinimum(int minimum) {
				MINIMUM_DISTANCE_DAMAGE = minimum;
			}

			public int getMaximum() {
				return MAXIMUM_DISTANCE_DAMAGE;
			}

			public void setMaximum(int maximum) {
				MAXIMUM_DISTANCE_DAMAGE = maximum;
			}

			public int getMidair() {
				return MAXIMUM_DISTANCE_MIDAIR;
			}

			public void setMidair(int midair) {
				MAXIMUM_DISTANCE_MIDAIR = midair;
			}
		}
	}
}
