package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.PvPConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * PvP 奖励服务，按职业与连杀状态计算勋章/代币奖励。
 * PvP reward service computing medal/toll rewards by class and spree state.
 *
 * @author Rinzler (Encom)
 */
@Slf4j(topic = "PVP_LOG")

public class PvPRewardService {

	private static final String plate = "188055157,188055160";
	private static final String chain = "188055157,188055162";
	private static final String leather = "188055157,188055164";
	private static final String cloth = "188055157,188055166";

	/**
	 * 按职业返回高级奖励物品 ID 列表。
	 * Returns advanced reward item ids for the given player class.
	 *
	 * @param pc 玩家职业 / player class
	 * item id list
	 */
	private static List<Integer> getRewardList(PlayerClass pc) {
		List<Integer> rewardList = new ArrayList<Integer>();
		String rewardString = "";
		switch (pc) {
		case TEMPLAR:
		case GLADIATOR:
			rewardString = plate;
			break;
		case CLERIC:
		case CHANTER:
		case AETHERTECH:
			rewardString = chain;
			break;
		case RANGER:
		case ASSASSIN:
		case GUNSLINGER:
			rewardString = leather;
			break;
		case SORCERER:
		case SONGWEAVER:
		case SPIRIT_MASTER:
			rewardString = cloth;
			break;
		default:
			rewardString = null;
		}
		if (rewardString != null) {
			String[] parts = rewardString.split(",");
			for (int i = 0; i < parts.length; i++) {
				rewardList.add(Integer.valueOf(Integer.parseInt(parts[i])));
			}
		} else {
			log.warn(I18n.get("log.0efb21e8756c", pc.toString()));
		}
		return rewardList;
	}

	/**
	 * 计算击杀奖励物品 ID（普通勋章或高级随机奖励）。
	 * Computes the kill reward item id (normal medal or advanced random reward).
	 *
	 * winner
	 * victim
	 * @param isAdvanced 是否高级奖励 / whether advanced reward
	 * item id
	 */
	public static int getRewardId(Player winner, Player victim, boolean isAdvanced) {
		int itemId = 0;
		if (victim.getSpreeLevel() > 2) {
			isAdvanced = true;
		}
		if (!isAdvanced) {
			int lvl = victim.getLevel();
			if (lvl >= 25 && lvl <= 83) {
				itemId = 186000469; // 페트라 공훈 훈장.
			}
		} else {
			List<Integer> abyssItemsList = getAdvancedReward(winner);
			itemId = ((Integer) abyssItemsList.get(Rnd.get(abyssItemsList.size()))).intValue();
		}
		return itemId;
	}

	/**
	 * 计算勋章奖励概率（受连杀与等级差影响）。
	 * Computes medal reward chance (influenced by spree and level difference).
	 *
	 * winner
	 * victim
	 * @return 概率百分比 / chance percent
	 */
	public static float getMedalRewardChance(Player winner, Player victim) {
		float chance = PvPConfig.MEDAL_REWARD_CHANCE;
		chance += 1.5F * winner.getRawKillCount();
		int diff = victim.getLevel() - winner.getLevel();
		if (diff * diff > 100) {
			if (diff < 0) {
				diff = -10;
			} else {
				diff = 10;
			}
		}
		chance += 2.0F * diff;
		if ((victim.getSpreeLevel() > 0) || (chance > 100.0F)) {
			chance = 100.0F;
		}
		return chance;
	}

	/**
	 * 计算勋章奖励数量。
	 * Computes medal reward quantity.
	 *
	 * winner
	 * victim
	 * quantity
	 */
	public static int getRewardQuantity(Player winner, Player victim) {
		int rewardQuantity = winner.getSpreeLevel() + 1;
		switch (victim.getSpreeLevel()) {
		case 1:
			rewardQuantity += 2;
			break;
		case 2:
			rewardQuantity += 4;
			break;
		case 3:
			rewardQuantity += 6;
			break;
		}
		return rewardQuantity;
	}

	/**
	 * 计算代币（Toll）奖励概率。
	 * Computes toll reward chance.
	 *
	 * winner
	 * victim
	 * @return 概率百分比 / chance percent
	 */
	public static float getTollRewardChance(Player winner, Player victim) {
		float chance = PvPConfig.TOLL_CHANCE;
		chance += 1.5F * winner.getRawKillCount();
		int diff = victim.getLevel() - winner.getLevel();
		if (diff * diff > 100) {
			if (diff < 0) {
				diff = -10;
			} else {
				diff = 10;
			}
		}
		chance += 2.0F * diff;
		if ((victim.getSpreeLevel() > 0) || (chance > 100.0F)) {
			chance = 100.0F;
		}
		return chance;
	}

	/**
	 * 计算代币（Toll）奖励数量。
	 * Computes toll reward quantity.
	 *
	 * winner
	 * victim
	 * quantity
	 */
	public static int getTollQuantity(Player winner, Player victim) {
		int tollQuantity = winner.getSpreeLevel() + 1;
		switch (victim.getSpreeLevel()) {
		case 1:
			tollQuantity += 2;
			break;
		case 2:
			tollQuantity += 4;
			break;
		case 3:
			tollQuantity += 6;
			break;
		}
		return tollQuantity;
	}

	/**
	 * 获取击杀者当前等级段的高级奖励列表。
	 * Returns advanced rewards for the winner's current level band.
	 *
	 * winner
	 * item id list
	 */
	private static List<Integer> getAdvancedReward(Player winner) {
		int lvl = winner.getLevel();
		PlayerClass pc = winner.getPlayerClass();
		List<Integer> rewardList = new ArrayList<Integer>();
		if (lvl >= 25 && lvl <= 83) {
			rewardList.addAll(getFilteredRewardList(pc, 25, 83));
		}
		return rewardList;
	}

	/**
	 * 按职业与物品等级过滤奖励列表。
	 * Filters reward list by class and item level range.
	 *
	 * @param pc 玩家职业 / player class
	 * @param minLevel 最小物品等级 / min item level
	 * @param maxLevel 最大物品等级（不含） / max item level (exclusive)
	 * @return 过滤后的物品 ID 列表 / filtered item id list
	 */
	private static List<Integer> getFilteredRewardList(PlayerClass pc, int minLevel, int maxLevel) {
		List<Integer> filteredRewardList = new ArrayList<Integer>();
		List<Integer> rewardList = getRewardList(pc);
		for (Iterator<Integer> i = rewardList.iterator(); i.hasNext();) {
			int id = i.next();
			ItemTemplate itemTemp = DataManager.ITEM_DATA.getItemTemplate(id);
			if (itemTemp == null) {
				log.warn(I18n.get("log.55ee25a26830", id, pc.toString()));
			}
			int itemLevel = itemTemp.getLevel();
			if (itemLevel >= minLevel && itemLevel < maxLevel) {
				filteredRewardList.add(id);
			}
		}
		return filteredRewardList.size() > 0 ? filteredRewardList : new ArrayList<Integer>();
	}
}
