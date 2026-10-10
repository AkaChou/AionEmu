package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * AI 行为相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * AI behavior related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author ATracer
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class AIConfig {

	/**
	 * 是否启用 AI 移动调试日志。
	 * Whether AI move debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.move.debug", defaultValue = "true")
	public static boolean MOVE_DEBUG;

	/**
	 * 是否启用 AI 事件调试日志。
	 * Whether AI event debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.event.debug", defaultValue = "false")
	public static boolean EVENT_DEBUG;

	/**
	 * 是否启用 AI 创建时调试日志。
	 * Whether AI on-create debug logging is enabled.
	 */
	@Property(key = "gameserver.ai.oncreate.debug", defaultValue = "false")
	public static boolean ONCREATE_DEBUG;

	/**
	 * 对仇恨免疫的等级差阈值。
	 * Level difference to be immune to aggro.
	 */
	@Property(key = "gameserver.ai.aggro.level.immune", defaultValue = "10")
	public static int AGGRO_LEVEL_IMMUNE;

	/**
	 * 是否让等级不超过 2、生命值低于 10 且原 AI 不是 aggressive 的野兽使用胆怯 AI。
	 * Whether level-2-or-lower beasts with less than 10 HP and a non-aggressive original AI use the fearful-beast AI.
	 */
	@Property(key = "gameserver.npc.fearful_beast.enable", defaultValue = "true")
	public static boolean ENABLE_FEARFUL_BEAST_AI;

	/**
	 * 是否启用 NPC 移动。
	 * Whether NPC movement is enabled.
	 */
	@Property(key = "gameserver.npcmovement.enable", defaultValue = "true")
	public static boolean ACTIVE_NPC_MOVEMENT;

	/**
	 * 是否启用更平滑但成本更高的 NPC 回家移动。
	 * Whether smoother but more expensive NPC home-return movement is enabled.
	 */
	@Property(key = "gameserver.ai.home.return.enhanced.enable", defaultValue = "true")
	public static boolean ENHANCED_HOME_RETURN;

	/**
	 * NPC 移动最小延迟（秒）。
	 * Minimum NPC movement delay in seconds.
	 */
	@Property(key = "gameserver.npcmovement.delay.minimum", defaultValue = "3")
	public static int MINIMIMUM_DELAY;

	/**
	 * NPC 移动最大延迟（秒）。
	 * Maximum NPC movement delay in seconds.
	 */
	@Property(key = "gameserver.npcmovement.delay.maximum", defaultValue = "15")
	public static int MAXIMUM_DELAY;

	/**
	 * 是否启用 NPC 喊话。
	 * Whether NPC shouts are enabled.
	 */
	@Property(key = "gameserver.npcshouts.enable", defaultValue = "false")
	public static boolean SHOUTS_ENABLE;

	@Getter
	private final AiProperties ai = new AiProperties();
	@Getter
	private final NpcProperties npc = new NpcProperties();
	@Getter
	private final NpcmovementProperties npcmovement = new NpcmovementProperties();
	@Getter
	private final NpcshoutsProperties npcshouts = new NpcshoutsProperties();

	@Getter
	public static class AiProperties {
		private final MoveProperties move = new MoveProperties();
		private final EventProperties event = new EventProperties();
		private final OncreateProperties oncreate = new OncreateProperties();
		private final AggroProperties aggro = new AggroProperties();
		private final HomeProperties home = new HomeProperties();

		public static class MoveProperties {
			public boolean isDebug() {
				return MOVE_DEBUG;
			}

			public void setDebug(boolean debug) {
				MOVE_DEBUG = debug;
			}
		}

		public static class EventProperties {
			public boolean isDebug() {
				return EVENT_DEBUG;
			}

			public void setDebug(boolean debug) {
				EVENT_DEBUG = debug;
			}
		}

		public static class OncreateProperties {
			public boolean isDebug() {
				return ONCREATE_DEBUG;
			}

			public void setDebug(boolean debug) {
				ONCREATE_DEBUG = debug;
			}
		}

		@Getter
		public static class AggroProperties {
			private final LevelProperties level = new LevelProperties();

			public static class LevelProperties {
				public int getImmune() {
					return AGGRO_LEVEL_IMMUNE;
				}

				public void setImmune(int immune) {
					AGGRO_LEVEL_IMMUNE = immune;
				}
			}
		}

		public static class HomeProperties {
			private final ReturnProperties returnProperties = new ReturnProperties();

			public ReturnProperties getReturn() {
				return returnProperties;
			}

			@Getter
			public static class ReturnProperties {
				private final EnhancedProperties enhanced = new EnhancedProperties();

				public static class EnhancedProperties {
					public boolean isEnable() {
						return ENHANCED_HOME_RETURN;
					}

					public void setEnable(boolean enable) {
						ENHANCED_HOME_RETURN = enable;
					}
				}
			}
		}
	}

	@Getter
	public static class NpcProperties {
		private final FearfulBeastProperties fearfulBeast = new FearfulBeastProperties();

		public static class FearfulBeastProperties {
			public boolean isEnable() {
				return ENABLE_FEARFUL_BEAST_AI;
			}

			public void setEnable(boolean enable) {
				ENABLE_FEARFUL_BEAST_AI = enable;
			}
		}
	}

	@Getter
	public static class NpcmovementProperties {
		private final DelayProperties delay = new DelayProperties();

		public boolean isEnable() {
			return ACTIVE_NPC_MOVEMENT;
		}

		public void setEnable(boolean enable) {
			ACTIVE_NPC_MOVEMENT = enable;
		}

		public static class DelayProperties {
			public int getMinimum() {
				return MINIMIMUM_DELAY;
			}

			public void setMinimum(int minimum) {
				MINIMIMUM_DELAY = minimum;
			}

			public int getMaximum() {
				return MAXIMUM_DELAY;
			}

			public void setMaximum(int maximum) {
				MAXIMUM_DELAY = maximum;
			}
		}
	}

	public static class NpcshoutsProperties {
		public boolean isEnable() {
			return SHOUTS_ENABLE;
		}

		public void setEnable(boolean enable) {
			SHOUTS_ENABLE = enable;
		}
	}
}
