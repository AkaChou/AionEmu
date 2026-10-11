package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * PvP 击杀奖励、连杀与限制相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * PvP kill rewards, killing spree and restriction related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author Rinzler (Encom)
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
public class PvPConfig {

	/**
	 * 每日可获得完整 AP 的最大击杀数。
	 * Max daily PvP kills allowed for full AP.
	 */
	@Property(key = "gameserver.pvp.maxkills", defaultValue = "5")
	public static int MAX_DAILY_PVP_KILLS;

	/**
	 * 是否启用 PvP 点券奖励。
	 * Whether PvP toll rewards are enabled.
	 */
	@Property(key = "gameserver.pvp.toll.rewarding.enable", defaultValue = "false")
	public static boolean ENABLE_TOLL_REWARD;
	/**
	 * PvP 点券奖励触发概率。
	 * Chance to grant PvP toll reward.
	 */
	@Property(key = "gameserver.pvp.toll.reward.chance", defaultValue = "50")
	public static int TOLL_CHANCE;
	/**
	 * PvP 点券奖励数量。
	 * Quantity of PvP toll reward.
	 */
	@Property(key = "gameserver.pvp.toll.reward.quantity", defaultValue = "5")
	public static int TOLL_QUANTITY;

	/**
	 * 是否启用连杀系统。
	 * Whether killing spree system is enabled.
	 */
	@Property(key = "gameserver.pvp.killingspree.enable", defaultValue = "false")
	public static boolean ENABLE_KILLING_SPREE_SYSTEM;
	/**
	 * Spree 连杀所需击杀数。
	 * Kill count required for Spree.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.spree", defaultValue = "5")
	public static int SPREE_KILL_COUNT;
	/**
	 * Carnage 连杀所需击杀数。
	 * Kill count required for Carnage.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.carnage", defaultValue = "10")
	public static int CARNAGE_KILL_COUNT;
	/**
	 * Genocide 连杀所需击杀数。
	 * Kill count required for Genocide.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.genocide", defaultValue = "15")
	public static int GENOCIDE_KILL_COUNT;
	/**
	 * Rampage 连杀所需击杀数。
	 * Kill count required for Rampage.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.rampage", defaultValue = "20")
	public static int RAMPAGE_KILL_COUNT;
	/**
	 * Dominating 连杀所需击杀数。
	 * Kill count required for Dominating.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.dominating", defaultValue = "25")
	public static int DOMINATING_KILL_COUNT;
	/**
	 * Unstoppable 连杀所需击杀数。
	 * Kill count required for Unstoppable.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.unstoppable", defaultValue = "30")
	public static int UNSTOPPABLE_KILL_COUNT;
	/**
	 * Insane Monster 连杀所需击杀数。
	 * Kill count required for Insane Monster.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.uniquemonster", defaultValue = "35")
	public static int INSANEMONSTER_KILL_COUNT;
	/**
	 * Godlike 连杀所需击杀数。
	 * Kill count required for Godlike.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.godlike", defaultValue = "40")
	public static int GODLIKE_KILL_COUNT;
	/**
	 * Wicked Sick 连杀所需击杀数。
	 * Kill count required for Wicked Sick.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.wickedsick", defaultValue = "45")
	public static int WICKEDSICK_KILL_COUNT;
	/**
	 * Muthafakaaas 连杀所需击杀数。
	 * Kill count required for Muthafakaaas.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.muthafakaaas", defaultValue = "50")
	public static int MUTHAFAKAAAS_KILL_COUNT;

	/**
	 * 连杀奖励 1 的数量。
	 * Quantity of spree reward item 1.
	 */
	@Property(key = "gameserver.pvp.spree_reward1.count", defaultValue = "10")
	public static int SPREE_REWARD_COUNT1;
	/**
	 * 连杀奖励 2 的数量。
	 * Quantity of spree reward item 2.
	 */
	@Property(key = "gameserver.pvp.spree_reward2.count", defaultValue = "10")
	public static int SPREE_REWARD_COUNT2;
	/**
	 * 连杀奖励 1 的物品 ID。
	 * Item ID of spree reward 1.
	 */
	@Property(key = "gameserver.pvp.spree_reward1.item", defaultValue = "166020000")
	public static int SPREE_REWARD_ITEM1;
	/**
	 * 连杀奖励 2 的物品 ID。
	 * Item ID of spree reward 2.
	 */
	@Property(key = "gameserver.pvp.spree_reward2.item", defaultValue = "166030005")
	public static int SPREE_REWARD_ITEM2;

	/**
	 * 是否启用 PvP 荣耀点奖励。
	 * Whether PvP glory point rewards are enabled.
	 */
	@Property(key = "gameserver.enable.gp.reward", defaultValue = "false")
	public static boolean ENABLE_GP_REWARD;
	/**
	 * 是否启用 PvP 荣耀点损失。
	 * Whether PvP glory point loss is enabled.
	 */
	@Property(key = "gameserver.enable.gp.lose", defaultValue = "false")
	public static boolean ENABLE_GP_LOSE;
	/**
	 * 是否使用固定荣耀点损失值。
	 * Whether fixed glory point loss is used.
	 */
	@Property(key = "gameserver.enable.gp.lose.fixed", defaultValue = "false")
	public static boolean ENABLE_GP_FIXED_LOSE;
	/**
	 * 固定荣耀点损失数值。
	 * Fixed glory point loss amount.
	 */
	@Property(key = "gameserver.gp.lose", defaultValue = "100")
	public static int GP_LOSE;

	/**
	 * 连续击杀时间限制（秒，0 表示不限制）。
	 * Chain-kill time restriction in seconds (0 disables).
	 */
	@Property(key = "gameserver.pvp.chainkill.time.restriction", defaultValue = "0")
	public static int CHAIN_KILL_TIME_RESTRICTION;
	/**
	 * 连续击杀次数限制。
	 * Chain-kill number restriction.
	 */
	@Property(key = "gameserver.pvp.chainkill.number.restriction", defaultValue = "30")
	public static int CHAIN_KILL_NUMBER_RESTRICTION;
	/**
	 * 允许获得奖励的最大等级差。
	 * Max authorized level difference for PvP rewards.
	 */
	@Property(key = "gameserver.pvp.max.leveldiff.restriction", defaultValue = "9")
	public static int MAX_AUTHORIZED_LEVEL_DIFF;

	/**
	 * 是否启用 PvP 勋章奖励。
	 * Whether PvP medal rewards are enabled.
	 */
	@Property(key = "gameserver.pvp.medal.rewarding.enable", defaultValue = "false")
	public static boolean ENABLE_MEDAL_REWARDING;
	/**
	 * PvP 勋章奖励触发概率。
	 * Chance to grant PvP medal reward.
	 */
	@Property(key = "gameserver.pvp.medal.reward.chance", defaultValue = "10")
	public static float MEDAL_REWARD_CHANCE;
	/**
	 * Genocide 特殊奖励类型。
	 * Special reward type for Genocide spree.
	 */
	@Property(key = "gameserver.pvp.special_reward.type", defaultValue = "0")
	public static int GENOCIDE_SPECIAL_REWARDING;
	/**
	 * 特殊奖励触发概率。
	 * Chance to grant special PvP reward.
	 */
	@Property(key = "gameserver.pvp.special_reward.chance", defaultValue = "2")
	public static float SPECIAL_REWARD_CHANCE;

	/** PvP 绑定树 / PvP binding tree. */
	@Getter
	private final PvpProperties pvp = new PvpProperties();
	/** 荣耀点开关绑定树 / glory-point switch binding tree. */
	@Getter
	private final EnableProperties enable = new EnableProperties();
	/** 荣耀点损失值绑定树 / glory-point loss amount binding tree. */
	@Getter
	private final GpAmountProperties gp = new GpAmountProperties();

	/**
	 * PvP 域节点：绑定 gameserver.pvp.*。
	 * PvP domain node binding gameserver.pvp.*.
	 */
	@Getter
	public static class PvpProperties {
		private final TollProperties toll = new TollProperties();
		private final KillingspreeProperties killingspree = new KillingspreeProperties();
		private final RawProperties raw = new RawProperties();
		private final SpreeReward1Properties spreeReward1 = new SpreeReward1Properties();
		private final SpreeReward2Properties spreeReward2 = new SpreeReward2Properties();
		private final ChainkillProperties chainkill = new ChainkillProperties();
		private final MaxProperties max = new MaxProperties();
		private final MedalProperties medal = new MedalProperties();
		private final SpecialRewardProperties specialReward = new SpecialRewardProperties();

		public int getMaxkills() {
			return MAX_DAILY_PVP_KILLS;
		}

		public void setMaxkills(int maxkills) {
			MAX_DAILY_PVP_KILLS = maxkills;
		}
	}

	/**
	 * 点券节点：绑定 gameserver.pvp.toll.*。
	 * Toll node binding gameserver.pvp.toll.*.
	 */
	@Getter
	public static class TollProperties {
		private final TollRewardingProperties rewarding = new TollRewardingProperties();
		private final TollRewardProperties reward = new TollRewardProperties();
	}

	/**
	 * 点券奖励开关父节点：对应配置元素 gameserver.pvp.toll.rewarding。
	 * Toll reward switch parent node matching the gameserver.pvp.toll.rewarding element.
	 */
	public static class TollRewardingProperties {
		public boolean isEnable() {
			return ENABLE_TOLL_REWARD;
		}

		public void setEnable(boolean enable) {
			ENABLE_TOLL_REWARD = enable;
		}
	}

	/**
	 * 点券奖励节点：绑定 gameserver.pvp.toll.reward.*。
	 * Toll reward node binding gameserver.pvp.toll.reward.*.
	 */
	public static class TollRewardProperties {
		public int getChance() {
			return TOLL_CHANCE;
		}

		public void setChance(int chance) {
			TOLL_CHANCE = chance;
		}

		public int getQuantity() {
			return TOLL_QUANTITY;
		}

		public void setQuantity(int quantity) {
			TOLL_QUANTITY = quantity;
		}
	}

	/**
	 * 连杀开关节点：绑定 gameserver.pvp.killingspree.enable。
	 * Killing-spree switch node binding gameserver.pvp.killingspree.enable.
	 */
	public static class KillingspreeProperties {
		public boolean isEnable() {
			return ENABLE_KILLING_SPREE_SYSTEM;
		}

		public void setEnable(boolean enable) {
			ENABLE_KILLING_SPREE_SYSTEM = enable;
		}
	}

	/**
	 * 连杀阈值父节点：对应配置元素 gameserver.pvp.raw。
	 * Raw kill-count parent node matching the gameserver.pvp.raw element.
	 */
	@Getter
	public static class RawProperties {
		private final KillcountProperties killcount = new KillcountProperties();
	}

	/**
	 * 连杀阈值节点：绑定 gameserver.pvp.raw.killcount.*。
	 * Raw kill-count node binding gameserver.pvp.raw.killcount.*.
	 */
	public static class KillcountProperties {
		public int getSpree() {
			return SPREE_KILL_COUNT;
		}

		public void setSpree(int spree) {
			SPREE_KILL_COUNT = spree;
		}

		public int getCarnage() {
			return CARNAGE_KILL_COUNT;
		}

		public void setCarnage(int carnage) {
			CARNAGE_KILL_COUNT = carnage;
		}

		public int getGenocide() {
			return GENOCIDE_KILL_COUNT;
		}

		public void setGenocide(int genocide) {
			GENOCIDE_KILL_COUNT = genocide;
		}

		public int getRampage() {
			return RAMPAGE_KILL_COUNT;
		}

		public void setRampage(int rampage) {
			RAMPAGE_KILL_COUNT = rampage;
		}

		public int getDominating() {
			return DOMINATING_KILL_COUNT;
		}

		public void setDominating(int dominating) {
			DOMINATING_KILL_COUNT = dominating;
		}

		public int getUnstoppable() {
			return UNSTOPPABLE_KILL_COUNT;
		}

		public void setUnstoppable(int unstoppable) {
			UNSTOPPABLE_KILL_COUNT = unstoppable;
		}

		public int getUniquemonster() {
			return INSANEMONSTER_KILL_COUNT;
		}

		public void setUniquemonster(int uniquemonster) {
			INSANEMONSTER_KILL_COUNT = uniquemonster;
		}

		public int getGodlike() {
			return GODLIKE_KILL_COUNT;
		}

		public void setGodlike(int godlike) {
			GODLIKE_KILL_COUNT = godlike;
		}

		public int getWickedsick() {
			return WICKEDSICK_KILL_COUNT;
		}

		public void setWickedsick(int wickedsick) {
			WICKEDSICK_KILL_COUNT = wickedsick;
		}

		public int getMuthafakaaas() {
			return MUTHAFAKAAAS_KILL_COUNT;
		}

		public void setMuthafakaaas(int muthafakaaas) {
			MUTHAFAKAAAS_KILL_COUNT = muthafakaaas;
		}
	}

	/**
	 * 连杀奖励 1 节点：绑定 gameserver.pvp.spree_reward1.*。
	 * Spree reward 1 node binding gameserver.pvp.spree_reward1.*.
	 */
	public static class SpreeReward1Properties {
		public int getCount() {
			return SPREE_REWARD_COUNT1;
		}

		public void setCount(int count) {
			SPREE_REWARD_COUNT1 = count;
		}

		public int getItem() {
			return SPREE_REWARD_ITEM1;
		}

		public void setItem(int item) {
			SPREE_REWARD_ITEM1 = item;
		}
	}

	/**
	 * 连杀奖励 2 节点：绑定 gameserver.pvp.spree_reward2.*。
	 * Spree reward 2 node binding gameserver.pvp.spree_reward2.*.
	 */
	public static class SpreeReward2Properties {
		public int getCount() {
			return SPREE_REWARD_COUNT2;
		}

		public void setCount(int count) {
			SPREE_REWARD_COUNT2 = count;
		}

		public int getItem() {
			return SPREE_REWARD_ITEM2;
		}

		public void setItem(int item) {
			SPREE_REWARD_ITEM2 = item;
		}
	}

	/**
	 * 连续击杀限制父节点：绑定 gameserver.pvp.chainkill.*。
	 * Chain-kill restriction parent node binding gameserver.pvp.chainkill.*.
	 */
	@Getter
	public static class ChainkillProperties {
		private final ChainkillTimeProperties time = new ChainkillTimeProperties();
		private final ChainkillNumberProperties number = new ChainkillNumberProperties();
	}

	/**
	 * 时间限制父节点：对应配置元素 gameserver.pvp.chainkill.time。
	 * Time restriction parent node matching the gameserver.pvp.chainkill.time element.
	 */
	@Getter
	public static class ChainkillTimeProperties {
		public int getRestriction() {
			return CHAIN_KILL_TIME_RESTRICTION;
		}

		public void setRestriction(int restriction) {
			CHAIN_KILL_TIME_RESTRICTION = restriction;
		}
	}

	/**
	 * 次数限制父节点：对应配置元素 gameserver.pvp.chainkill.number。
	 * Number restriction parent node matching the gameserver.pvp.chainkill.number element.
	 */
	@Getter
	public static class ChainkillNumberProperties {
		public int getRestriction() {
			return CHAIN_KILL_NUMBER_RESTRICTION;
		}

		public void setRestriction(int restriction) {
			CHAIN_KILL_NUMBER_RESTRICTION = restriction;
		}
	}

	/**
	 * 等级差父节点：对应配置元素 gameserver.pvp.max。
	 * Level-difference parent node matching the gameserver.pvp.max element.
	 */
	@Getter
	public static class MaxProperties {
		private final LeveldiffProperties leveldiff = new LeveldiffProperties();
	}

	/**
	 * 等级差父节点：对应配置元素 gameserver.pvp.max.leveldiff。
	 * Level-difference parent node matching the gameserver.pvp.max.leveldiff element.
	 */
	@Getter
	public static class LeveldiffProperties {
		public int getRestriction() {
			return MAX_AUTHORIZED_LEVEL_DIFF;
		}

		public void setRestriction(int restriction) {
			MAX_AUTHORIZED_LEVEL_DIFF = restriction;
		}
	}

	/**
	 * 勋章节点：绑定 gameserver.pvp.medal.*。
	 * Medal node binding gameserver.pvp.medal.*.
	 */
	@Getter
	public static class MedalProperties {
		private final MedalRewardingProperties rewarding = new MedalRewardingProperties();
		private final MedalRewardProperties reward = new MedalRewardProperties();
	}

	/**
	 * 勋章奖励开关父节点：对应配置元素 gameserver.pvp.medal.rewarding。
	 * Medal reward switch parent node matching the gameserver.pvp.medal.rewarding element.
	 */
	public static class MedalRewardingProperties {
		public boolean isEnable() {
			return ENABLE_MEDAL_REWARDING;
		}

		public void setEnable(boolean enable) {
			ENABLE_MEDAL_REWARDING = enable;
		}
	}

	/**
	 * 勋章奖励节点：绑定 gameserver.pvp.medal.reward.chance。
	 * Medal reward node binding gameserver.pvp.medal.reward.chance.
	 */
	public static class MedalRewardProperties {
		public float getChance() {
			return MEDAL_REWARD_CHANCE;
		}

		public void setChance(float chance) {
			MEDAL_REWARD_CHANCE = chance;
		}
	}

	/**
	 * 特殊奖励节点：绑定 gameserver.pvp.special_reward.*。
	 * Special reward node binding gameserver.pvp.special_reward.*.
	 */
	public static class SpecialRewardProperties {
		public int getType() {
			return GENOCIDE_SPECIAL_REWARDING;
		}

		public void setType(int type) {
			GENOCIDE_SPECIAL_REWARDING = type;
		}

		public float getChance() {
			return SPECIAL_REWARD_CHANCE;
		}

		public void setChance(float chance) {
			SPECIAL_REWARD_CHANCE = chance;
		}
	}

	/**
	 * 荣耀点开关父节点：对应配置元素 gameserver.enable。
	 * Glory-point switch parent node matching the gameserver.enable element.
	 */
	@Getter
	public static class EnableProperties {
		private final EnableGpProperties gp = new EnableGpProperties();
	}

	/**
	 * 荣耀点开关节点：绑定 gameserver.enable.gp.*。
	 * Glory-point switch node binding gameserver.enable.gp.*.
	 */
	@Getter
	public static class EnableGpProperties {
		private final LoseProperties lose = new LoseProperties();

		public boolean isReward() {
			return ENABLE_GP_REWARD;
		}

		public void setReward(boolean reward) {
			ENABLE_GP_REWARD = reward;
		}
	}

	/**
	 * 荣耀点损失节点：绑定 gameserver.enable.gp.lose.fixed 与兼容标量 gameserver.enable.gp.lose。
	 * GP-loss node binding gameserver.enable.gp.lose.fixed and the legacy scalar gameserver.enable.gp.lose.
	 */
	@Getter
	public static class LoseProperties {
		/**
		 * 兼容旧版裸标量 gameserver.enable.gp.lose=false 的绑定入口。
		 * Binding entry for the legacy bare scalar gameserver.enable.gp.lose=false.
		 */
		public static LoseProperties valueOf(String value) {
			try {
				ENABLE_GP_LOSE = Boolean.parseBoolean(value);
			} catch (RuntimeException ignored) {
			}
			return new LoseProperties();
		}

		public boolean isValue() {
			return ENABLE_GP_LOSE;
		}

		public void setValue(boolean value) {
			ENABLE_GP_LOSE = value;
		}

		public boolean isFixed() {
			return ENABLE_GP_FIXED_LOSE;
		}

		public void setFixed(boolean fixed) {
			ENABLE_GP_FIXED_LOSE = fixed;
		}
	}

	/**
	 * 荣耀点损失值节点：绑定 gameserver.gp.lose。
	 * GP-loss amount node binding gameserver.gp.lose.
	 */
	@Getter
	public static class GpAmountProperties {
		public int getLose() {
			return GP_LOSE;
		}

		public void setLose(int lose) {
			GP_LOSE = lose;
		}
	}
}
