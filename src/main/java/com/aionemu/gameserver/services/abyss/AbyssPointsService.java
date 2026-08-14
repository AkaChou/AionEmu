package com.aionemu.gameserver.services.abyss;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 欧比斯点数服务：AP/GP 增减、军阶变更广播与低军阶回退校验。
 * Abyss points service: AP/GP gain/loss, rank-change broadcast, and low-rank fallback.
 */
public class AbyssPointsService {

	/**
	 * 带全局回调的 AP 增加入口（按击杀对象触发）。
	 * AP-add entry with global callback (keyed by killed object).
	 *
	 * @param player 玩家 / Player
	 * @param obj 关联可见对象 / related visible object
	 * @param value AP 变化量 / AP delta
	 */
	@GlobalCallback(AddAPGlobalCallback.class)
	public static void addAp(Player player, VisibleObject obj, int value) {
		addAp(player, value);
	}

	/**
	 * 带全局回调的 GP 增加入口（按击杀对象触发）。
	 * GP-add entry with global callback (keyed by killed object).
	 *
	 * @param player 玩家 / Player
	 * @param obj 关联可见对象 / related visible object
	 * @param value GP 变化量 / GP delta
	 */
	@GlobalCallback(AddGPGlobalCallback.class)
	public static void addGp(Player player, VisibleObject obj, int value) {
		addGp(player, value);
	}

	/**
	 * 增减 AP，提示玩家并同步军团贡献。
	 * Add or subtract AP, notify the player, and sync legion contribution.
	 *
	 * @param player 玩家 / Player
	 * @param value AP 变化量 / AP delta
	 */
	public static void addAp(Player player, int value) {
		if (player == null) {
			return;
		}
		if (value > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN(value));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300965, value * -1));
		}
		setAp(player, value);
		if (player.isLegionMember() && value > 0) {
			player.getLegion().addContributionPoints(value);
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_EDIT(0x03, player.getLegion()));
		}
	}

	/**
	 * 增减 GP 并提示玩家。
	 * Add or subtract GP and notify the player.
	 *
	 * @param player 玩家 / Player
	 * @param value GP 变化量 / GP delta
	 */
	public static void addGp(Player player, int value) {
		if (player == null) {
			return;
		}
		if (value > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GLORY_POINT_GAIN(value));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402219, value * -1));
		}
		setGp(player, value);
	}

	/**
	 * 同时增减 AP 与 GP。
	 * Add or subtract both AP and GP in one call.
	 *
	 * @param player 玩家 / Player
	 * @param ap AP 变化量 / AP delta
	 * @param gp GP 变化量 / GP delta
	 */
	public static void addAGp(Player player, int ap, int gp) {
		if (player == null) {
			return;
		}
		if (ap > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN(ap));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300965, ap * -1));
		}
		setAp(player, ap);
		if (player.isLegionMember() && ap > 0) {
			player.getLegion().addContributionPoints(ap);
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_EDIT(0x03, player.getLegion()));
		}
		if (gp > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GLORY_POINT_GAIN(gp));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402219, gp * -1));
		}
		setGp(player, gp);
	}

	/**
	 * 将 AP 变化应用到军阶并在变更时广播。
	 * Apply AP delta to the rank and broadcast on rank change.
	 *
	 * @param player 玩家 / Player
	 * @param value AP 变化量 / AP delta
	 */
	public static void setAp(Player player, int value) {
		if (player == null) {
			return;
		}
		AbyssRank rank = player.getAbyssRank();
		AbyssRankEnum oldAbyssRank = rank.getRank();
		rank.addAp(value, player);
		AbyssRankEnum newAbyssRank = rank.getRank();
		checkRankChanged(player, oldAbyssRank, newAbyssRank);
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
	}

	/**
	 * 将 GP 变化应用到军阶并下发军阶包。
	 * Apply GP delta to the rank and send the abyss-rank packet.
	 *
	 * @param player 玩家 / Player
	 * @param value GP 变化量 / GP delta
	 */
	public static void setGp(Player player, int value) {
		if (player == null) {
			return;
		}
		AbyssRank rank = player.getAbyssRank();
		rank.addGp(value);
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
	}

	/**
	 * 军阶变化时广播外观、刷新军阶包并校验军阶限装。
	 * On rank change: broadcast appearance, refresh rank packet, check rank-limited gear.
	 *
	 * @param player 玩家 / Player
	 * @param oldAbyssRank 旧军阶 / old rank
	 * @param newAbyssRank 新军阶 / new rank
	 */
	public static void checkRankChanged(Player player, AbyssRankEnum oldAbyssRank, AbyssRankEnum newAbyssRank) {
		if (oldAbyssRank == newAbyssRank) {
			return;
		}
		PacketSendUtility.broadcastPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
		player.getEquipment().checkRankLimitItems();
	}

	/**
	 * 荣耀军阶变化时广播、刷新限装并更新欧比斯技能。
	 * On glory-rank change: broadcast, refresh gear limits, and update abyss skills.
	 *
	 * @param player 玩家 / Player
	 * @param oldGloryRank 旧荣耀军阶 / Old glory rank
	 * @param newGloryRank 新荣耀军阶 / New glory rank
	 */
	public static void checkRankGpChanged(Player player, AbyssRankEnum oldGloryRank, AbyssRankEnum newGloryRank) {
		if (oldGloryRank == newGloryRank) {
			return;
		}
		PacketSendUtility.broadcastPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
		PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
		player.getEquipment().checkRankLimitItems();
		AbyssSkillService.updateSkills(player);
	}

	/**
	 * GP 不足军官门槛时，按 AP 区间回退到士兵军阶。
	 * When GP is below officer threshold, fall back to soldier ranks by AP bands.
	 *
	 * @param player 玩家 / Player
	 */
	public static void AbyssRankCheck(Player player) {
		if (player == null) {
			return;
		}
		if (player.getAbyssRank().getGp() < AbyssRankEnum.STAR1_OFFICER.getGpRequired()) {
			if (player.getAbyssRank().getAp() < 1200) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE9_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 1200 && player.getAbyssRank().getAp() < 4220) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE8_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 4220 && player.getAbyssRank().getAp() < 10990) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE7_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 10990 && player.getAbyssRank().getAp() < 23500) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE6_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 23500 && player.getAbyssRank().getAp() < 42780) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE5_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 42780 && player.getAbyssRank().getAp() < 69700) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE4_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 69700 && player.getAbyssRank().getAp() < 105600) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE3_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 105600 && player.getAbyssRank().getAp() < 150800) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE2_SOLDIER);
			} else if (player.getAbyssRank().getAp() >= 150800) {
				player.getAbyssRank().setRank(AbyssRankEnum.GRADE1_SOLDIER);
			}
			PacketSendUtility.broadcastPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
			PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK_UPDATE(0, player));
			PacketSendUtility.sendPacket(player, new SM_ABYSS_RANK(player.getAbyssRank()));
		}
	}

	/**
	 * AP 增加全局回调：在玩家/攻城 NPC（非和平）击杀后通知扩展点。
	 * AP-add global callback: notifies extensions after player/siege-NPC (non-peace) kills.
	 */
	@SuppressWarnings("rawtypes")
	public abstract static class AddAPGlobalCallback implements Callback {
		@Override
		public CallbackResult beforeCall(Object obj, Object[] args) {
			return CallbackResult.newContinue();
		}

		@Override
		public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
			Player player = (Player) args[0];
			VisibleObject creature = (VisibleObject) args[1];
			int abyssPoints = (Integer) args[2];
			if ((creature instanceof Player)) {
				onAbyssPointsAdded(player, abyssPoints);
			} else if (((creature instanceof SiegeNpc)) && (!((SiegeNpc) creature).getSpawn().isPeace())) {
				onAbyssPointsAdded(player, abyssPoints);
			}
			return CallbackResult.newContinue();
		}

		@Override
		public Class<? extends Callback> getBaseClass() {
			return AddAPGlobalCallback.class;
		}

		/**
		 * AP 已增加后的扩展钩子。
		 * Extension hook after AP was added.
		 *
		 * @param player 玩家 / Player
		 * @param abyssPoints AP 数量 / AP amount
		 */
		public abstract void onAbyssPointsAdded(Player player, int abyssPoints);
	}

	/**
	 * GP 增加全局回调：在玩家/攻城 NPC（非和平）击杀后通知扩展点。
	 * GP-add global callback: notifies extensions after player/siege-NPC (non-peace) kills.
	 */
	@SuppressWarnings("rawtypes")
	public abstract static class AddGPGlobalCallback implements Callback {
		@Override
		public CallbackResult beforeCall(Object obj, Object[] args) {
			return CallbackResult.newContinue();
		}

		@Override
		public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
			Player player = (Player) args[0];
			VisibleObject creature = (VisibleObject) args[1];
			int gloryPoints = (Integer) args[2];
			if ((creature instanceof Player)) {
				onGloryPointsAdded(player, gloryPoints);
			} else if (((creature instanceof SiegeNpc)) && (!((SiegeNpc) creature).getSpawn().isPeace())) {
				onGloryPointsAdded(player, gloryPoints);
			}
			return CallbackResult.newContinue();
		}

		@Override
		public Class<? extends Callback> getBaseClass() {
			return AddGPGlobalCallback.class;
		}

		/**
		 * GP 已增加后的扩展钩子。
		 * Extension hook after GP was added.
		 *
		 * @param player 玩家 / Player
		 * @param gloryPoints GP 数量 / GP amount
		 */
		public abstract void onGloryPointsAdded(Player player, int gloryPoints);
	}
}
