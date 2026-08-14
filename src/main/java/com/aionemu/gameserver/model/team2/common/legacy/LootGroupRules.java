package com.aionemu.gameserver.model.team2.common.legacy;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.player.InRoll;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.services.drop.DropDistributionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Loot 队伍 Rules，用于团队2相关逻辑。
 * Loot Group Rules for team 2 logic.
 *
 * @author ATracer, xTz
 */
public class LootGroupRules {

	private LootRuleType lootRule;
	private LootDistribution autodistribution;
	private int common_item_above;
	private int superior_item_above;
	private int heroic_item_above;
	private int fabled_item_above;
	private int ethernal_item_above;
	private int misc;
	private int nrMisc;
	private int nrRoundRobin;
	private List<DropItem> itemsToBeDistributed = new ArrayList<DropItem>();

	public LootGroupRules() {
		lootRule = LootRuleType.ROUNDROBIN;
		autodistribution = LootDistribution.ROLL_DICE;
		common_item_above = 0;
		superior_item_above = 2;
		heroic_item_above = 2;
		fabled_item_above = 2;
		ethernal_item_above = 2;
	}

	public LootGroupRules(LootRuleType lootRule, LootDistribution autodistribution, int commonItemAbove,
			int superiorItemAbove, int heroicItemAbove, int fabledItemAbove, int ethernalItemAbove, int misc) {
		super();
		this.lootRule = lootRule;
		this.autodistribution = autodistribution;
		this.misc = misc;
		common_item_above = commonItemAbove;
		superior_item_above = superiorItemAbove;
		heroic_item_above = heroicItemAbove;
		fabled_item_above = fabledItemAbove;
		ethernal_item_above = ethernalItemAbove;
	}

	/**
	 * 按物品品质返回对应的分配规则。
	 * Return the distribution rule for the given item quality.
	 *
	 * @param quality 物品品质 / Item quality
	 * @return 是否按该品质规则分配 / Whether the rule applies
	 */
	public boolean getQualityRule(ItemQuality quality) {
		switch (quality) {
		case COMMON: // 白色 / White
			return common_item_above != 0;
		case RARE: // 绿色 / Green
			return superior_item_above != 0;
		case LEGEND: // 蓝色 / Blue
			return heroic_item_above != 0;
		case UNIQUE: // 黄色 / Yellow
			return fabled_item_above != 0;
		case MYTHIC: // 橙色 / Orange
			return ethernal_item_above != 0;
		case EPIC: // 紫色 / Purple
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * 判断是否为杂项物品（垃圾品质且开启杂项分配）。
	 * Whether the item is a misc item (junk quality with misc distribution enabled).
	 *
	 * @param quality 物品品质 / Item quality
	 * @return 是否为杂项 / Whether misc
	 */
	public boolean isMisc(ItemQuality quality) {
		return quality.equals(ItemQuality.JUNK) && misc == 1;
	}

	/**
	 * @return 拾取规则 / The loot rule
	 */
	public LootRuleType getLootRule() {
		return lootRule;
	}

	/**
	 * @return 自动分配方式 / The autodistribution
	 */
	public LootDistribution getAutodistribution() {
		return autodistribution;
	}

	/**
	 * @return 普通品质分配门槛 / The common item above
	 */
	public int getCommonItemAbove() {
		return common_item_above;
	}

	/**
	 * @return 稀有品质分配门槛 / The superior item above
	 */
	public int getSuperiorItemAbove() {
		return superior_item_above;
	}

	/**
	 * @return 传颂品质分配门槛 / The heroic item above
	 */
	public int getHeroicItemAbove() {
		return heroic_item_above;
	}

	/**
	 * @return 唯一品质分配门槛 / The fabled item above
	 */
	public int getFabledItemAbove() {
		return fabled_item_above;
	}

	/**
	 * @return 神话品质分配门槛 / The ethernal item above
	 */
	public int getEthernalItemAbove() {
		return ethernal_item_above;
	}

	/**
	 * @return 杂项分配数量 / The nrMisc
	 */
	public int getNrMisc() {
		return nrMisc;
	}

	/**
	 * @param nrMisc 设置的杂项分配数量 / The nrMisc to set
	 */
	public void setNrMisc(int nrMisc) {
		this.nrMisc = nrMisc;
	}

	/** 设置 players in roll / Sets the players in roll */
	public void setPlayersInRoll(final Collection<Player> players, int time, final int index, final int npcId) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				for (Player player : players) {
					if (player.isInPlayerMode(PlayerMode.IN_ROLL)) {
						InRoll inRoll = player.inRoll;
						switch (inRoll.getRollType()) {
						case 2:
							if (inRoll.getIndex() == index && inRoll.getNpcId() == npcId) {
								GameFeatureServices.dropDistributionService().handleRoll(player, 0, inRoll.getItemId(),
										inRoll.getNpcId(), inRoll.getIndex());
							}
							break;
						case 3:
							if (inRoll.getIndex() == index && inRoll.getNpcId() == npcId) {
								GameFeatureServices.dropDistributionService().handleBid(player, 0, inRoll.getItemId(),
										inRoll.getNpcId(), inRoll.getIndex());
							}
							break;
						}
					}
				}
			}
		}, time);
	}

	/**
	 * @return 轮转分配次数 / The nrRoundRobin
	 */
	public int getNrRoundRobin() {
		return nrRoundRobin;
	}

	/**
	 * @param nrRoundRobin 设置的轮转分配次数 / The nrRoundRobin to set
	 */
	public void setNrRoundRobin(int nrRoundRobin) {
		this.nrRoundRobin = nrRoundRobin;
	}

	/** 返回 misc / Returns the misc */
	public int getMisc() {
		return misc;
	}

	/** 添加 item to be distributed / Adds item to be distributed */
	public void addItemToBeDistributed(DropItem dropItem) {
		itemsToBeDistributed.add(dropItem);
	}

	/** 包含掉落物品 / Contain Drop Item */
	public boolean containDropItem(DropItem dropItem) {
		return itemsToBeDistributed.contains(dropItem);
	}

	/** 移除 item to be distributed / Removes item to be distributed */
	public void removeItemToBeDistributed(DropItem dropItem) {
		itemsToBeDistributed.remove(dropItem);
	}

	/** 返回 items to be distributed / Returns the items to be distributed */
	public List<DropItem> getItemsToBeDistributed() {
		return itemsToBeDistributed;
	}
}
