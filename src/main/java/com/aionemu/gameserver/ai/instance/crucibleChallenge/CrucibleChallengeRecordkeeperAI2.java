package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("crucible_challenge_recordkeeper")
public class CrucibleChallengeRecordkeeperAI2 extends NpcAI2 {

	private boolean transitioned;

	@Override
	protected void handleSpawned() {
		if (isStageStart()) {
			set("STAGE", 0, 0);
		}
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
		super.handleSpawned();
	}

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public synchronized boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId != 10000) {
			return false;
		}
		if (getNpcId() == 205679) {
			set("CLEAR", 1, 0);
			getPosition().getWorldMapInstance().getInstanceHandler().doReward(player);
		} else if (!transitioned && transition(player)) {
			transitioned = true;
			AI2Actions.deleteOwner(this);
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}

	private boolean transition(Player player) {
		switch (getNpcId()) {
			case 205668 -> set(player.getRace() == Race.ELYOS ? "Condition_S1_L" : "Condition_S1_D", 1, 0);
			case 205674 -> finish(1, "STAGE2_START");
			case 205669 -> set("Condition_S2", 1, 0);
			case 205675 -> finish(2, "STAGE3_START");
			case 205670 -> set("Condition_S3", 1, 0);
			case 205676 -> {
				set("STAGE", Rnd.nextBoolean() ? 3 : 4, 0);
				set("STAGE4_START", 1, 0);
			}
			case 205666 -> set("Condition_S4B", 1, 0);
			case 205671 -> set("Condition_S4A", 1, 0);
			case 205667, 205677 -> finish(5, "STAGE5_START");
			case 205672 -> set(player.getRace() == Race.ELYOS ? "Condition_S5_L" : "Condition_S5_D", 1, 0);
			case 205678 -> finish(6, "STAGE6_START");
			case 205673 -> set("Condition_S6", 1, 0);
			default -> {
				return false;
			}
		}
		return true;
	}

	private void finish(int stage, String nextVariable) {
		set("STAGE", stage, 0);
		set(nextVariable, 1, 0);
	}

	private boolean isStageStart() {
		return getNpcId() == 205666 || getNpcId() >= 205668 && getNpcId() <= 205673;
	}

	private void set(String variable, int set, int modify) {
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), variable, set, modify);
	}
}
