package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.CubeExpandTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 背包扩展服务，处理 NPC 付费扩展与任务/道具扩展。
 * Cube expand service handling paid NPC expands and quest/ticket expands.
 */
@Slf4j
public class CubeExpandService {
	/** 最小扩展等级。 / Minimum expand level. */
	private static final int MIN_EXPAND = 0;
	/** 最大扩展等级。 / Maximum expand level. */
	private static final int MAX_EXPAND = 15;

	/**
	 * 通过 NPC 发起背包扩展确认与扣费。
	 * Starts a cube expand confirmation and kinah charge via NPC.
	 *
	 * 玩家 / player
	 * expand NPC
	 */
	public static void expandCube(final Player player, Npc npc) {
		final CubeExpandTemplate expandTemplate = DataManager.CUBEEXPANDER_DATA
				.getCubeExpandListTemplate(npc.getNpcId());
		if (expandTemplate == null) {
			log.error(I18n.get("log.a3b71cc3084c", npc.getObjectId()));
			return;
		}
		if (npcCanExpandLevel(expandTemplate, player.getNpcExpands() + 1) && canExpand(player)) {
			if (player.getNpcExpands() >= 15) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_EXTEND_INVENTORY_CANT_EXTEND_MORE);
				return;
			}
			final int price = getPriceByLevel(expandTemplate, player.getNpcExpands() + 1);
			RequestResponseHandler responseHandler = new RequestResponseHandler(npc) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (price > player.getInventory().getKinah()) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_WAREHOUSE_EXPAND_NOT_ENOUGH_MONEY);
						return;
					}
					expand(responder, true);
					player.getInventory().decreaseKinah(price);
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
				}
			};
			boolean result = player.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_WAREHOUSE_EXPAND_WARNING,
					responseHandler);
			if (result) {
				PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(
						SM_QUESTION_WINDOW.STR_WAREHOUSE_EXPAND_WARNING, 0, 0, String.valueOf(price)));
			}
		} else
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300430));
	}

	/**
	 * 实际增加背包扩展次数并同步客户端。
	 * Actually increases cube expands and syncs the client size.
	 *
	 * @param player 玩家 / player
	 * @param isNpcExpand true 为 NPC 扩展，false 为任务扩展 / true for NPC expand, false for quest expand
	 */
	public static void expand(Player player, boolean isNpcExpand) {
		if (!canExpand(player)) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300431, "9"));
		if (isNpcExpand) {
			player.setNpcExpands(player.getNpcExpands() + 1);
		} else {
			player.setQuestExpands(player.getQuestExpands() + 1);
		}
		PacketSendUtility.sendPacket(player, SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, player));
	}

	/**
	 * 判断玩家是否还能继续扩展背包。
	 * Returns whether the player can expand the cube further.
	 *
	 * @param player 玩家 / player
	 * @return 可扩展返回 true / true if expandable
	 */
	public static boolean canExpand(Player player) {
		return validateNewSize(player.getNpcExpands() + player.getQuestExpands() + 1);
	}

	/**
	 * 判断玩家是否可用指定等级的扩展票继续扩展。
	 * Returns whether the player can expand further with a ticket of the given level.
	 *
	 * 玩家 / player
	 * ticket level
	 *
	 * @return 若 allowed 则为 true / true if allowed
	 */
	public static boolean canExpandByTicket(Player player, int ticketLevel) {
		if (!canExpand(player))
			return false;
		int ticketExpands = player.getQuestExpands() - getCompletedCubeQuests(player);
		return ticketExpands < ticketLevel;
	}

	/**
	 * 校验新的总扩展等级是否在合法区间。
	 * Validates that the new total expand level is within bounds.
	 *
	 * new level
	 *
	 * @param level 若 valid 则为 true / true if valid
	 */
	private static boolean validateNewSize(int level) {
		if (level < MIN_EXPAND || level > MAX_EXPAND)
			return false;
		return true;
	}

	/**
	 * 判断 NPC 模板是否支持指定扩展等级。
	 * Returns whether the NPC template supports the given expand level.
	 *
	 * @param clist 扩展模板 / expand template
	 * @param level 目标等级 / target level
	 * @return 若 supported 则为 true / true if supported
	 */
	private static boolean npcCanExpandLevel(CubeExpandTemplate clist, int level) {
		if (!clist.contains(level)) {
			return false;
		}
		return true;
	}

	/**
	 * 统计已完成的背包扩展任务数（上限 2）。
	 * Counts completed cube expand quests (capped at 2).
	 *
	 * 玩家 / player
	 * completed count
	 */
	private static int getCompletedCubeQuests(Player player) {
		int result = 0;
		QuestStateList qs = player.getQuestStateList();
		int[] questIds = { 1800, 1947, 2833, 2937, 1797 };
		for (int q : questIds) {
			if (qs.getQuestState(q) != null && qs.getQuestState(q).getStatus().equals(QuestStatus.COMPLETE))
				result++;
		}
		return result > 2 ? 2 : result;
	}

	/**
	 * 按扩展等级获取价格。
	 * Returns the expand price for the given level.
	 *
	 * @param clist 扩展模板 / expand template
	 * @param level 目标等级 / target level
	 * price
	 */
	private static int getPriceByLevel(CubeExpandTemplate clist, int level) {
		return clist.get(level).getPrice();
	}
}
