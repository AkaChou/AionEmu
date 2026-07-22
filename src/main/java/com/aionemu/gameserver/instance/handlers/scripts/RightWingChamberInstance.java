package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(300090000)
public class RightWingChamberInstance extends GeneralInstanceHandler {
	private static final long CHEST_DURATION = 15 * 60_000L;
	private static final long EXIT_DELAY = CHEST_DURATION + 10_000L;
	private static final int[] TREASURE_BOX_IDS = { 700469, 700470, 700471, 701481, 701486,
		702800, 702801, 702802, 702805, 702806 };

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (!runtimeState().getBoolean("rightwing.started", false)) {
			return;
		}
		long chestDeadline = runtimeState().getLong("rightwing.chest_deadline", 0);
		if (runtimeState().getBoolean("rightwing.chests_expired", false)
				|| chestDeadline <= System.currentTimeMillis()) {
			expireChests();
		} else {
			scheduleDeadline("chests", chestDeadline, this::expireChests);
		}
		long exitDeadline = runtimeState().getLong("rightwing.exit_deadline", 0);
		if (exitDeadline > 0) {
			scheduleDeadline("exit", exitDeadline, this::exitPlayers);
		}
		long messageDeadline = runtimeState().getLong("rightwing.all_chests_message_deadline", 0);
		if (messageDeadline > 0 && !runtimeState().getBoolean("rightwing.all_chests_message_sent", false)) {
			scheduleDeadline("all_chests_message", messageDeadline, this::sendAllChestsExpired);
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		if (!runtimeState().getBoolean("rightwing.started", false)) {
			runtimeState().put("rightwing.started", true);
			long now = System.currentTimeMillis();
			long chestDeadline = now + CHEST_DURATION;
			runtimeState().put("rightwing.chest_deadline", chestDeadline);
			runtimeState().put("rightwing.exit_deadline", now + EXIT_DELAY);
			scheduleDeadline("chests", chestDeadline, this::expireChests);
			scheduleDeadline("exit", runtimeState().getLong("rightwing.exit_deadline", 0), this::exitPlayers);
		}
		long chestDeadline = runtimeState().getLong("rightwing.chest_deadline", 0);
		if (chestDeadline > System.currentTimeMillis()
				&& !runtimeState().getBoolean("rightwing.chests_expired", false)) {
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(0, (int) ((chestDeadline - System.currentTimeMillis()) / 1000)));
		}
	}

	private void expireChests() {
		deleteTreasureBoxes();
		if (runtimeState().getBoolean("rightwing.chests_expired", false)) {
			return;
		}
		runtimeState().put("rightwing.chests_expired", true);
		sendMsg(1400245);
		long messageDeadline = System.currentTimeMillis() + 4_000;
		runtimeState().put("rightwing.all_chests_message_deadline", messageDeadline);
		scheduleDeadline("all_chests_message", messageDeadline, this::sendAllChestsExpired);
	}

	private void deleteTreasureBoxes() {
		for (int npcId : TREASURE_BOX_IDS) {
			instance.getNpcs(npcId).forEach(npc -> npc.getController().onDelete());
		}
	}

	private void sendAllChestsExpired() {
		if (runtimeState().getBoolean("rightwing.all_chests_message_sent", false)) {
			return;
		}
		runtimeState().put("rightwing.all_chests_message_sent", true);
		sendMsg(1400244);
	}

	private void exitPlayers() {
		if (runtimeState().getBoolean("rightwing.exited", false)) {
			return;
		}
		runtimeState().put("rightwing.exited", true);
		instance.doOnAllPlayers(this::onExitInstance);
	}

	@Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
