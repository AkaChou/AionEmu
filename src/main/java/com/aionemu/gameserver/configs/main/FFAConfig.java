package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * FFA（自由混战）活动相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Free-for-all (FFA) event related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author Rinzler (Encom)
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class FFAConfig {

	/**
	 * 是否启用 FFA 模式。
	 * Whether FFA mode is enabled.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.enabled", defaultValue = "true")
	public static boolean FFA_ENABLED;
	/**
	 * 连杀奖励通行币数量。
	 * Toll quantity rewarded for FFA spree.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree.toll.quantity", defaultValue = "10")
	public static int FFA_SPREE_REWARD_TOLL_QUANTITY;
	/**
	 * 连杀奖励道具 ID。
	 * Item ID rewarded for FFA spree.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree.reward.item", defaultValue = "166030005")
	public static int FFA_SPREE_REWARD_ITEM;

	/**
	 * FFA 连杀公告文案 1。
	 * FFA killing spree announce message 1.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree1", defaultValue = " <is now on a Killing Spree> !")
	public static String FFA_SPREE_1;
	/**
	 * FFA 连杀公告文案 2。
	 * FFA killing spree announce message 2.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree2", defaultValue = " <is now on Rampage> !")
	public static String FFA_SPREE_2;
	/**
	 * FFA 连杀公告文案 3。
	 * FFA killing spree announce message 3.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree3", defaultValue = " <is now Dominating> !")
	public static String FFA_SPREE_3;
	/**
	 * FFA 连杀公告文案 4。
	 * FFA killing spree announce message 4.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree4", defaultValue = " <Unstoppable> !")
	public static String FFA_SPREE_4;
	/**
	 * FFA 连杀公告文案 5。
	 * FFA killing spree announce message 5.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree5", defaultValue = " <CHUUCHUU MUTHAFAKAAASS> !")
	public static String FFA_SPREE_5;
	/**
	 * FFA 连杀公告文案 6。
	 * FFA killing spree announce message 6.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree6", defaultValue = " <is now Getting Crazzyyy> !")
	public static String FFA_SPREE_6;
	/**
	 * FFA 连杀公告文案 7。
	 * FFA killing spree announce message 7.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree7", defaultValue = " <is now GODLIKE> !")
	public static String FFA_SPREE_7;
	/**
	 * FFA 连杀公告文案 8。
	 * FFA killing spree announce message 8.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree8", defaultValue = " <is now on WICKED SICKKKKKK> !")
	public static String FFA_SPREE_8;
	/**
	 * FFA 连杀公告文案 9。
	 * FFA killing spree announce message 9.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree9", defaultValue = " <Really knows how to kill players> !")
	public static String FFA_SPREE_9;
	/**
	 * FFA 连杀公告文案 10。
	 * FFA killing spree announce message 10.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree10", defaultValue = " <IS NOW A TRUE PVP FIGHTER> !")
	public static String FFA_SPREE_10;

	@Getter
	private final PvpProperties pvp = new PvpProperties();

	@Getter
	public static class PvpProperties {
		private final ModProperties mod = new ModProperties();

		@Getter
		public static class ModProperties {
			private final FfaProperties ffa = new FfaProperties();

			@Getter
			public static class FfaProperties {
				private final SpreeProperties spree = new SpreeProperties();

				public boolean isEnabled() {
					return FFA_ENABLED;
				}

				public void setEnabled(boolean enabled) {
					FFA_ENABLED = enabled;
				}

				public String getSpree1() {
					return FFA_SPREE_1;
				}

				public void setSpree1(String spree1) {
					FFA_SPREE_1 = spree1;
				}

				public String getSpree2() {
					return FFA_SPREE_2;
				}

				public void setSpree2(String spree2) {
					FFA_SPREE_2 = spree2;
				}

				public String getSpree3() {
					return FFA_SPREE_3;
				}

				public void setSpree3(String spree3) {
					FFA_SPREE_3 = spree3;
				}

				public String getSpree4() {
					return FFA_SPREE_4;
				}

				public void setSpree4(String spree4) {
					FFA_SPREE_4 = spree4;
				}

				public String getSpree5() {
					return FFA_SPREE_5;
				}

				public void setSpree5(String spree5) {
					FFA_SPREE_5 = spree5;
				}

				public String getSpree6() {
					return FFA_SPREE_6;
				}

				public void setSpree6(String spree6) {
					FFA_SPREE_6 = spree6;
				}

				public String getSpree7() {
					return FFA_SPREE_7;
				}

				public void setSpree7(String spree7) {
					FFA_SPREE_7 = spree7;
				}

				public String getSpree8() {
					return FFA_SPREE_8;
				}

				public void setSpree8(String spree8) {
					FFA_SPREE_8 = spree8;
				}

				public String getSpree9() {
					return FFA_SPREE_9;
				}

				public void setSpree9(String spree9) {
					FFA_SPREE_9 = spree9;
				}

				public String getSpree10() {
					return FFA_SPREE_10;
				}

				public void setSpree10(String spree10) {
					FFA_SPREE_10 = spree10;
				}

				@Getter
				public static class SpreeProperties {
					private final TollProperties toll = new TollProperties();
					private final RewardProperties reward = new RewardProperties();

					public static class TollProperties {
						public int getQuantity() {
							return FFA_SPREE_REWARD_TOLL_QUANTITY;
						}

						public void setQuantity(int quantity) {
							FFA_SPREE_REWARD_TOLL_QUANTITY = quantity;
						}
					}

					public static class RewardProperties {
						public int getItem() {
							return FFA_SPREE_REWARD_ITEM;
						}

						public void setItem(int item) {
							FFA_SPREE_REWARD_ITEM = item;
						}
					}
				}
			}
		}
	}
}
