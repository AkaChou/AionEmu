package com.aionemu.gameserver.services.abyss;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

	/** 欧比斯点数监听器接口 / Abyss points listener interface */
	public interface AbyssPointsListener {
		void onAbyssPointsAdded(Player player, int abyssPoints);
	}

	/** 荣耀点数监听器接口 / Glory points listener interface */
	public interface GloryPointsListener {
		void onGloryPointsAdded(Player player, int gloryPoints);
	}

	private static final List<AbyssPointsListener> apListeners = new CopyOnWriteArrayList<>();
	private static final List<GloryPointsListener> gpListeners = new CopyOnWriteArrayList<>();

	public static void addListener(AbyssPointsListener listener) {
		apListeners.add(listener);
	}

	public static void addListener(GloryPointsListener listener) {
		gpListeners.add(listener);
	}

	public static void removeListener(GloryPointsListener listener) {
		gpListeners.remove(listener);
	}

	public static void removeListener(AbyssPointsListener listener) {
		apListeners.remove(listener);
	}

	/**
	 * 带全局回调的 AP 增加入口（按击杀对象触发）。
	 * AP-add entry with listener dispatch (keyed by killed object).
	 * @param player 玩家 / Player
	 * @param obj 关联可见对象 / related visible object
	 * @param value AP 变化量 / AP delta
	 */
	public static void addAp(Player player, VisibleObject obj, int value) {
		addAp(player, value);
		notifyAbyssPointsAdded(player, obj, value);
	}

	/**
	 * 带全局回调的 GP 增加入口（按击杀对象触发）。
	 * GP-add entry with listener dispatch (keyed by killed object).
	 * @param player 玩家 / Player
	 * @param obj 关联可见对象 / related visible object
	 * @param value GP 变化量 / GP delta
	 */
	public static void addGp(Player player, VisibleObject obj, int value) {
		addGp(player, value);
		notifyGloryPointsAdded(player, obj, value);
	}

	private static void notifyAbyssPointsAdded(Player player, VisibleObject obj, int value) {
		if (apListeners.isEmpty() || !isKillObject(obj)) {
			return;
		}
		for (AbyssPointsListener listener : apListeners) {
			listener.onAbyssPointsAdded(player, value);
		}
	}

	private static void notifyGloryPointsAdded(Player player, VisibleObject obj, int value) {
		if (gpListeners.isEmpty() || !isKillObject(obj)) {
			return;
		}
		for (GloryPointsListener listener : gpListeners) {
			listener.onGloryPointsAdded(player, value);
		}
	}

	private static boolean isKillObject(VisibleObject obj) {
		return obj instanceof Player || (obj instanceof SiegeNpc && !((SiegeNpc) obj).getSpawn().isPeace());
	}

	/**
	 * 增减 AP，提示玩家并同步军团贡献。
	 * Add or subtract AP, notify the player, and sync legion contribution.
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
}
