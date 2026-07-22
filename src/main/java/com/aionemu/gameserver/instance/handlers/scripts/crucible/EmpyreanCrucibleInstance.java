package com.aionemu.gameserver.instance.handlers.scripts.crucible;

import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.playerreward.CruciblePlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_STAGE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

@InstanceID(300300000)
public class EmpyreanCrucibleInstance extends CrucibleInstance {

	@Override
	public void onEnterInstance(Player player) {
		super.onEnterInstance(player);
		CruciblePlayerReward reward = getPlayerReward(player.getObjectId());
		if (reward.isRewarded()) {
			doReward(player);
		}
	}

	@Override
	public boolean onReviveEvent(Player player) {
		super.onReviveEvent(player);
		moveToReadyRoom(player);
		for (Player other : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(other, other == player
					? new SM_SYSTEM_MESSAGE(1400932)
					: new SM_SYSTEM_MESSAGE(1400933, player.getName()));
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
		} else {
			TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		}
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(instanceReward, InstanceScoreType.END_PROGRESS));
	}

	@Override
	public void onStopTraining(Player player) {
		doReward(player);
	}

	public void changeWorldSceneStatus(int status) {
		for (Player player : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_INSTANCE_STAGE_INFO(2, status & 0xffff, status >>> 16));
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}

	@Override
	public void onExitInstance(Player player) {
		removeItems(player);
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400962, player.getName()));
	}

	private void moveToReadyRoom(Player player) {
		if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_05_300300000"))) {
			teleport(player, 1260.9495f, 832.87317f, 358.60562f, (byte) 92);
		} else if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_06_300300000"))) {
			teleport(player, 1592.8813f, 149.78166f, 128.81355f, (byte) 117);
		} else if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_07_300300000"))) {
			teleport(player, 1820.8805f, 795.80914f, 470.18304f, (byte) 51);
		} else if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_08_300300000"))) {
			teleport(player, 1780.103f, 1723.458f, 304.039f, (byte) 53);
		} else if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_09_300300000"))) {
			teleport(player, 1359.5046f, 1751.7952f, 319.59406f, (byte) 30);
		} else if (player.isInsideZone(ZoneName.get("TRAINING_ROOM_10_300300000"))) {
			teleport(player, 1755.709f, 1253.4136f, 394.2378f, (byte) 33);
		} else {
			teleport(player, 381.41684f, 346.78162f, 96.74763f, (byte) 43);
		}
	}

	private void teleport(Player player, float x, float y, float z, byte heading) {
		TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, heading);
	}

	private static void removeItems(Player player) {
		Storage storage = player.getInventory();
		for (int itemId : new int[] { 186000124, 186000125, 186000134 }) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}
}
