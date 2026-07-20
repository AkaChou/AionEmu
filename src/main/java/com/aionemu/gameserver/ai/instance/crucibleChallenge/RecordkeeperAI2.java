package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

@AIName("recordkeeper")
public class RecordkeeperAI2 extends NpcAI2 {

	private final AtomicBoolean greeted = new AtomicBoolean();

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			switch (getNpcId()) {
				case 205668 -> setRace(player, "condition_s1_l", "condition_s1_d");
				case 205674 -> move(player, "stage2_start", 1796.5513f, 306.9967f, 469.25f, (byte) 60);
				case 205669 -> setRandom("condition_s2a", "condition_s2b");
				case 205675 -> move(player, "stage3_start", 1324.433f, 1738.2279f, 316.476f, (byte) 70);
				case 205670 -> setRandom("condition_s3a", "condition_s3b");
				case 205676 -> {
					set("stage4_start");
					if (Rnd.nextBoolean()) {
						teleport(player, 1283.1246f, 791.6683f, 436.6403f, (byte) 60);
					} else {
						teleport(player, 1270.8877f, 237.93307f, 405.38028f, (byte) 60);
					}
				}
				case 205666 -> set("condition_s4b");
				case 205671 -> set("condition_s4a");
				case 205667, 205677 -> move(player, "stage5_start", 357.98798f, 349.19116f, 96.09108f, (byte) 60);
				case 205672 -> setRace(player, "condition_s5_l", "condition_s5_d");
				case 205678 -> move(player, "stage6_start", 1759.5004f, 1273.5414f, 389.11743f, (byte) 10);
				case 205673 -> set("condition_s6");
				case 205679 -> getPosition().getWorldMapInstance().getInstanceHandler().doReward(player);
			}
			if (getNpcId() != 205679) {
				AI2Actions.deleteOwner(this);
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}

	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player && MathUtil.getDistance(getOwner(), creature) <= 30
				&& greeted.compareAndSet(false, true)) {
			int message = switch (getNpcId()) {
				case 205668 -> 1111470;
				case 205669 -> 1111471;
				case 205670 -> 1111472;
				case 205666, 205671 -> 1111473;
				case 205672 -> 1111474;
				case 205673 -> 1111475;
				case 205674 -> 1111476;
				case 205675 -> 1111477;
				case 205676 -> 1111478;
				case 205667, 205677 -> 1111479;
				case 205678 -> 1111480;
				case 205679 -> 1111481;
				default -> 0;
			};
			if (message != 0) {
				GameFeatureServices.npcShoutsService().sendMsg(
						getPosition().getWorldMapInstance(), message, getObjectId(), false, 0, 2000);
			}
		}
	}

	private void move(Player player, String variable, float x, float y, float z, byte heading) {
		set(variable);
		teleport(player, x, y, z, heading);
	}

	private void teleport(Player player, float x, float y, float z, byte heading) {
		TeleportService2.teleportTo(player, getPosition().getWorldMapInstance().getMapId(),
				getPosition().getInstanceId(), x, y, z, heading);
	}

	private void setRandom(String first, String second) {
		set(Rnd.nextBoolean() ? first : second);
	}

	private void setRace(Player player, String elyos, String asmodians) {
		set(player.getRace() == Race.ELYOS ? elyos : asmodians);
	}

	private void set(String variable) {
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), variable, 0, 1);
	}
}
