package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("empyrean_record_keeper")
public class Empyrean_Record_KeeperAI2 extends NpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
			switch (getNpcId()) {
				case 799568 -> setRandom("condition_s2_l", "condition_s2_d");
				case 799569 -> set("condition_s3_l");
				case 205331 -> set("condition_s4");
				case 205338 -> move("stage5_start", 1260.15f, 812.34f, 358.6056f, (byte) 90);
				case 205332 -> setRandom("condition_s5_l", "condition_s5_d");
				case 205339 -> move("stage6_start", 1616.0248f, 154.43837f, 126f, (byte) 10);
				case 205333 -> set("condition_s6");
				case 205340 -> move("stage7_start", 1793.9233f, 796.92f, 469.36542f, (byte) 60);
				case 205334 -> set(player.getRace() == Race.ELYOS ? "condition_s7_d" : "condition_s7_l");
				case 205341 -> move("stage8_start", 1776.4169f, 1749.9952f, 303.69553f, (byte) 0);
				case 205335 -> set("condition_s8");
				case 205342 -> move("stage9_start", 1328.935f, 1742.0771f, 316.74188f, (byte) 0);
				case 205336 -> set("condition_s9");
				case 205343 -> move("stage10_start", 1760.9441f, 1278.033f, 394.23764f, (byte) 0);
				case 205337 -> set("condition_s10");
			}
		}
		AI2Actions.deleteOwner(this);
		return true;
	}

	@Override
	protected void handleSpawned() {
		if (getNpcId() == 799568) {
			shout(1111451, 6000);
		} else if (getNpcId() == 799569) {
			shout(1111452, 6000);
		} else if (getNpcId() == 205331) {
			shout(1111453, 6000);
		}
		int message = switch (getNpcId()) {
			case 799568 -> 1111460;
			case 799569 -> 1111461;
			case 205331 -> 1111462;
			case 205332 -> 1111454;
			case 205333 -> 1111455;
			case 205334 -> 1111456;
			case 205335 -> 1111457;
			case 205336 -> 1111458;
			case 205337 -> 1111459;
			case 205338 -> 1111463;
			case 205339 -> 1111464;
			case 205340 -> 1111465;
			case 205341 -> 1111466;
			case 205342 -> 1111467;
			case 205343 -> 1111468;
			default -> 0;
		};
		if (message != 0) {
			shout(message, 2000);
		}
		super.handleSpawned();
	}

	private void shout(int message, int delay) {
		GameFeatureServices.npcShoutsService().sendMsg(
				getPosition().getWorldMapInstance(), message, getObjectId(), false, 0, delay);
	}

	private void move(String variable, float x, float y, float z, byte heading) {
		set(variable);
		for (Player player : getPosition().getWorldMapInstance().getPlayersInside()) {
			TeleportService2.teleportTo(player, getPosition().getWorldMapInstance().getMapId(),
					getPosition().getInstanceId(), x, y, z, heading);
		}
	}

	private void setRandom(String first, String second) {
		set(Rnd.nextBoolean() ? first : second);
	}

	private void set(String variable) {
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), variable, 0, 1);
	}
}
