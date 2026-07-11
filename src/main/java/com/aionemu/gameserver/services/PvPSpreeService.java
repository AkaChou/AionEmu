package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.main.PvPConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * PvP 连杀（Spree）服务，统计连杀、升级公告与连杀终结。
 * PvP spree service tracking kill streaks, tier announcements, and spree ends.
 *
 * @author Rinzler (Encom)
 */
@Slf4j(topic = "PVP_LOG")
public class PvPSpreeService {
	private static final String STRING_SPREE1 = "Bloody Storm";
	private static final String STRING_SPREE2 = "Carnage";
	private static final String STRING_SPREE3 = "Genocide";
	private static final String STRING_SPREE4 = "Rampage";
	private static final String STRING_SPREE5 = "Dominating";
	private static final String STRING_SPREE6 = "Unstoppable";
	private static final String STRING_SPREE7 = "InsaneMonster";
	private static final String STRING_SPREE8 = "Godlike";
	private static final String STRING_SPREE9 = "WickedSick";
	private static final String STRING_SPREE10 = "Muthafakaaas";

	/**
	 * 增加胜利者原始连杀数，并在达到阈值时升级连杀等级与奖励。
	 * Increments the winner's raw kill count and upgrades spree level/rewards at thresholds.
	 *
	 * winner
	 */
	public static void increaseRawKillCount(Player winner) {
		int currentRawKillCount = winner.getRawKillCount();
		winner.setRawKillCount(currentRawKillCount + 1);
		int newRawKillCount = currentRawKillCount + 1;
		PacketSendUtility.sendWhiteMessageOnCenter(winner, "You killed " + newRawKillCount + " players in a row!");

		if ((newRawKillCount == PvPConfig.SPREE_KILL_COUNT) || (newRawKillCount == PvPConfig.CARNAGE_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.GENOCIDE_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.RAMPAGE_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.DOMINATING_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.UNSTOPPABLE_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.INSANEMONSTER_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.GODLIKE_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.WICKEDSICK_KILL_COUNT)
				|| (newRawKillCount == PvPConfig.MUTHAFAKAAAS_KILL_COUNT)) {
			if (newRawKillCount == PvPConfig.SPREE_KILL_COUNT)
				updateSpreeLevel(winner, 1);
			ItemService.addItem(winner, PvPConfig.SPREE_REWARD_ITEM1, PvPConfig.SPREE_REWARD_COUNT1);
			ItemService.addItem(winner, PvPConfig.SPREE_REWARD_ITEM2, PvPConfig.SPREE_REWARD_COUNT2);
			if (newRawKillCount == PvPConfig.CARNAGE_KILL_COUNT)
				updateSpreeLevel(winner, 2);
			if (newRawKillCount == PvPConfig.GENOCIDE_KILL_COUNT)
				updateSpreeLevel(winner, 3);
			if (newRawKillCount == PvPConfig.RAMPAGE_KILL_COUNT)
				updateSpreeLevel(winner, 4);
			if (newRawKillCount == PvPConfig.DOMINATING_KILL_COUNT)
				updateSpreeLevel(winner, 5);
			if (newRawKillCount == PvPConfig.UNSTOPPABLE_KILL_COUNT)
				updateSpreeLevel(winner, 6);
			if (newRawKillCount == PvPConfig.INSANEMONSTER_KILL_COUNT)
				updateSpreeLevel(winner, 7);
			if (newRawKillCount == PvPConfig.GODLIKE_KILL_COUNT)
				updateSpreeLevel(winner, 8);
			if (newRawKillCount == PvPConfig.WICKEDSICK_KILL_COUNT)
				updateSpreeLevel(winner, 9);
			if (newRawKillCount == PvPConfig.MUTHAFAKAAAS_KILL_COUNT)
				updateSpreeLevel(winner, 10);
		}
	}

	/**
	 * 更新连杀等级并广播公告。
	 * Updates spree level and broadcasts the announcement.
	 *
	 * winner
	 * @param level 连杀等级 / spree level
	 */
	private static void updateSpreeLevel(Player winner, int level) {
		winner.setSpreeLevel(level);
		sendUpdateSpreeMessage(winner, level);
	}

	/**
	 * 向全服广播连杀等级升级消息。
	 * Broadcasts spree-level upgrade message to all players.
	 *
	 * winner
	 * @param level 连杀等级 / spree level
	 */
	private static void sendUpdateSpreeMessage(Player winner, int level) {
		for (Player p : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
			if (level == 1)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " has started a " + STRING_SPREE1 + " !");
			if (level == 2)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is performing a true " + STRING_SPREE2 + " ! Stop him fast !");
			if (level == 3)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is doing a " + STRING_SPREE3 + " ! Watch Out !");
			if (level == 4)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is now on a " + STRING_SPREE4 + " ! ");
			if (level == 5)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is now " + STRING_SPREE5 + " ! ");
			if (level == 6)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase() + " is "
								+ STRING_SPREE6 + " ! Run away if you can! ");
			if (level == 7)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p, winner.getName() + " Of "
						+ winner.getCommonData().getRace().toString().toLowerCase() + " is a " + STRING_SPREE7 + " ! ");
			if (level == 8)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is now " + STRING_SPREE8 + " ! ");
			if (level == 9)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " is now on " + STRING_SPREE9 + " ! ");
			if (level == 10)
				PacketSendUtility.sendBrightYellowMessageOnCenter(p,
						winner.getName() + " Of " + winner.getCommonData().getRace().toString().toLowerCase()
								+ " CHUUCHUU " + STRING_SPREE10 + " ! IS NOW A TRUE PVP FIGHTER!!!!!!!");
		}
		log.info(I18n.get("log.ecaed2ab0d0d", winner.getName(), level));
	}

	/**
	 * 重置受害者连杀状态；若有连杀等级则广播终结消息。
	 * Resets the victim's spree state; broadcasts end message if a spree level was active.
	 *
	 * victim
	 * killer
	 * whether PvP death
	 */
	public static void cancelSpree(Player victim, Creature killer, boolean isPvPDeath) {
		int killsBeforeDeath = victim.getRawKillCount();
		victim.setRawKillCount(0);
		if (victim.getSpreeLevel() > 0) {
			victim.setSpreeLevel(0);
			sendEndSpreeMessage(victim, killer, isPvPDeath, killsBeforeDeath);
		}
	}

	/**
	 * 向全服广播连杀被终结的消息。
	 * Broadcasts the spree-ended message to all players.
	 *
	 * victim
	 * killer
	 * whether PvP death
	 * @param killsBeforeDeath 死前连杀数 / kills before death
	 */
	private static void sendEndSpreeMessage(Player victim, Creature killer, boolean isPvPDeath, int killsBeforeDeath) {
		String spreeEnder = isPvPDeath ? ((Player) killer).getName() : "A monster";
		for (Player p : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
			PacketSendUtility.sendWhiteMessageOnCenter(p, "The killing spree of " + victim.getName()
					+ " has been stopped by " + spreeEnder + " after " + killsBeforeDeath + " uninterrupted murders !");
		}
		log.info(I18n.get("log.5666f236eeb6", victim.getName(), spreeEnder));
	}
}
