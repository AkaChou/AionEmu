package com.aionemu.gameserver.model.team2.common.legacy;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Collection;

import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.player.InRoll;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemQuality;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Loot 队伍 Rules，用于团队2相关逻辑。
 * Loot Group Rules for team 2 logic.
 *
 * @author ATracer, xTz
 */
@Getter
@Setter
public class LootGroupRules {

	/**
	 * @return 拾取规则 / The loot rule
	 */
	private final LootRuleType lootRule;
	/**
	 * @return 自动分配方式 / The autodistribution
	 */
	private final LootDistribution autodistribution;
	private final int common_item_above;
	private final int superior_item_above;
	private final int heroic_item_above;
	private final int fabled_item_above;
	private final int ethernal_item_above;
	/** 返回 misc / Returns the misc */
	private int misc;
	/**
	 * @return 杂项分配数量 / The nrMisc
	 */
	private int nrMisc;
	/**
	 * @return 轮转分配次数 / The nrRoundRobin
	 */
	private int nrRoundRobin;
	/** 返回 items to be distributed / Returns the items to be distributed */
	private final List<DropItem> itemsToBeDistributed = new ArrayList<>();

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
		return switch (quality) {
			case COMMON -> // 白色 / White
				common_item_above != 0;
			case RARE -> // 绿色 / Green
				superior_item_above != 0;
			case LEGEND -> // 蓝色 / Blue
				heroic_item_above != 0;
			case UNIQUE -> // 黄色 / Yellow
				fabled_item_above != 0;
			case MYTHIC -> // 橙色 / Orange
				ethernal_item_above != 0;
			case EPIC -> // 紫色 / Purple
				true;
			default -> false;
		};
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
}
