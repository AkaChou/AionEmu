package com.aionemu.gameserver.questEngine;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 每日/周可重复任务提醒广播：09:00 起每日触发一次的可重复任务刷新与提醒包。
 * Daily/weekly repeatable-quest reminder broadcast: fires once per day from 09:00, refreshing and
 * notifying repeatable-quest holders.
 */
final class QuestDailyReminder {

	/** 每日/周任务提醒定时任务 / Daily/weekly reminder scheduled task */
	private ScheduledFuture<?> task;

	/**
	 * 安排每日 9:00 的可重复任务提醒广播。
	 * Schedule the 09:00 daily broadcast for repeatable quest reminders.
	 */
	void schedule(QuestEngine engine) {
		Calendar sendingDate = Calendar.getInstance();
		sendingDate.set(Calendar.AM_PM, Calendar.AM);
		sendingDate.set(Calendar.HOUR, 9);
		sendingDate.set(Calendar.MINUTE, 0);
		sendingDate.set(Calendar.SECOND, 0);
		if (sendingDate.getTime().getTime() < System.currentTimeMillis()) {
			sendingDate.add(Calendar.HOUR, 24);
		}
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> {
			SM_SYSTEM_MESSAGE dailyMessage = new SM_SYSTEM_MESSAGE(1400854);
			SM_SYSTEM_MESSAGE weeklyMessage = new SM_SYSTEM_MESSAGE(1400856);
			for (Player player : GameWorldBootstrapServices.world().getAllPlayers()) {
				if (player == null) {
					continue;
				}
				boolean hasDaily = false;
				boolean hasWeekly = false;
				if (player.getQuestStateList() != null) {
					for (QuestState qs : player.getQuestStateList().getAllQuestState()) {
						var metadata = qs == null ? null : engine.questCatalog().findMetadata(qs.getQuestId()).orElse(null);
						if (qs != null && metadata != null && qs.canRepeat(metadata)) {
							if (metadata.repeatPolicy().daily()) {
								hasDaily = true;
							} else if (metadata.repeatPolicy().weekly()) {
								hasWeekly = true;
							}
						}
					}
				}
				if (hasDaily || hasWeekly) {
					player.getController().updateZone();
					player.getController().updateNearbyQuests();
				}
				if (hasDaily) {
					PacketSendUtility.sendPacket(player, dailyMessage);
				}
				if (hasWeekly) {
					PacketSendUtility.sendPacket(player, weeklyMessage);
				}
				if (player.getNpcFactions() != null) {
					player.getNpcFactions().sendDailyQuest();
				}
			}
		}, sendingDate.getTimeInMillis() - System.currentTimeMillis(), 1000 * 60 * 60 * 24);
	}

	/**
	 * 取消提醒任务（幂等）。
	 * Cancel the reminder task (idempotent).
	 */
	void cancel() {
		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}
}
