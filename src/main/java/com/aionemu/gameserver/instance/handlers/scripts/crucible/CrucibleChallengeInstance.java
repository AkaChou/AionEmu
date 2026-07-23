package com.aionemu.gameserver.instance.handlers.scripts.crucible;

import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.playerreward.CruciblePlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

@InstanceID(300320000)
public class CrucibleChallengeInstance extends CrucibleInstance {

	@Override
	public void onDie(Npc npc) {
		super.onDie(npc);
		if (npc.getNpcId() != 217819) {
			return;
		}
		Player player = instance.getPlayer(instance.getSoloPlayerObj());
		if (player == null) {
			return;
		}
		QuestState quest = player.getQuestStateList().getQuestState(player.getRace() == Race.ASMODIANS ? 28208 : 18208);
		if (quest != null && quest.getStatus() == QuestStatus.START
				&& (quest.getQuestVarById(0) == 1 || quest.getQuestVarById(1) == 4)) {
			spawn(730459, 1765.7104f, 1281.2388f, 389.11743f, (byte) 0);
		}
	}

	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		super.onDie(player, lastAttacker);
		int position = revivePosition(player);
		runtimeState().put(playerKey(player.getObjectId(), "revive_position"), position);
		scheduleDeadline("revive." + player.getObjectId(), System.currentTimeMillis() + 13_000,
				() -> {
					if (player.getLifeStats().isAlreadyDead()) {
						onReviveEvent(player);
					}
				});
		return true;
	}

	@Override
	public boolean onReviveEvent(Player player) {
		super.onReviveEvent(player);
		switch (runtimeState().getInt(playerKey(player.getObjectId(), "revive_position"), 0)) {
			case 1 -> teleport(player, 380.35417f, 1663.3583f, 97.60156f, (byte) 0);
			case 2 -> teleport(player, 1819.8119f, 304.92932f, 469.4142f, (byte) 0);
			case 3 -> teleport(player, 1354.9386f, 1748.1531f, 318.6173f, (byte) 70);
			case 4 -> teleport(player, 1294.1417f, 234.49684f, 406.0368f, (byte) 0);
			case 5 -> teleport(player, 1307.3776f, 790.7324f, 437.29678f, (byte) 0);
			case 6 -> teleport(player, 381.7477f, 346.63913f, 96.74763f, (byte) 0);
			case 7 -> teleport(player, 1750.2677f, 1253.5453f, 389.11765f, (byte) 10);
		}
		return true;
	}

	@Override
	public void doReward(Player player) {
		CruciblePlayerReward reward = getPlayerReward(player.getObjectId());
		if (!reward.isRewarded()) {
			var plan = InstanceSettlementService.cruciblePlan(mapId, reward.getPoints());
			InstanceSettlementService.settleCrucible(instance, player, reward.getPoints());
			markRewarded(reward, Math.toIntExact(plan.itemCount(186000130)));
			instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		} else {
			TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		}
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(instanceReward, InstanceScoreType.END_PROGRESS));
	}

	@Override
	public void onPlayerLogin(Player player) {
		sendScore(player);
		if (getPlayerReward(player.getObjectId()).isRewarded()) {
			doReward(player);
		}
	}

	@Override
	public void onStopTraining(Player player) {
		doReward(player);
	}

	@Override
	public void onExitInstance(Player player) {
		removeItems(player);
		InstanceService.destroyInstance(instance);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400962, player.getName()));
	}

	private int revivePosition(Player player) {
		String[] zones = { "TRAINING_ROOM_01_300320000", "TRAINING_ROOM_02_300320000",
				"TRAINING_ROOM_03_300320000", "TRAINING_ROOM_04B_300320000", "TRAINING_ROOM_04A_300320000",
				"TRAINING_ROOM_05_300320000", "TRAINING_ROOM_06_300320000" };
		for (int i = 0; i < zones.length; i++) {
			if (player.isInsideZone(ZoneName.get(zones[i]))) {
				return i + 1;
			}
		}
		return 0;
	}

	private void teleport(Player player, float x, float y, float z, byte heading) {
		TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, heading);
	}

	private static void removeItems(Player player) {
		var inventory = player.getInventory();
		inventory.decreaseByItemId(186000134, inventory.getItemCountByItemId(186000134));
	}
}
