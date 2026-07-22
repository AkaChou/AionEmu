package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.crucible.EmpyreanCrucibleInstance;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@AIName("empyrean_crucible_recordkeeper")
public class EmpyreanCrucibleRecordkeeperAI2 extends NpcAI2 {

	private boolean transitioned;

	@Override
	protected void handleSpawned() {
		Integer stage = switch (getNpcId()) {
			case 799567 -> 0;
			case 799568 -> 1;
			case 799569 -> 2;
			case 205331 -> 3;
			case 205332 -> 4;
			case 205333 -> 5;
			case 205334 -> 6;
			case 205335 -> 7;
			case 205336 -> 8;
			case 205337 -> 9;
			default -> null;
		};
		if (stage != null) {
			set("STAGE", stage, 0);
		} else if (getNpcId() >= 205338 && getNpcId() <= 205344) {
			sayToAll(342684);
			if (getNpcId() == 205344) {
				sayToAll(342685);
			}
		}
		super.handleSpawned();
	}

	@Override
	protected void handleCreatureSee(Creature creature) {
		if (creature instanceof Player player) {
			if (getNpcId() >= 205331 && getNpcId() <= 205337 && startStage(player)) {
				AI2Actions.deleteOwner(this);
			} else if (getNpcId() >= 205338 && getNpcId() <= 205343) {
				finishStage();
			}
		}
	}

	@Override
	protected void handleDialogStart(Player player) {
		if (startStage(player)) {
			AI2Actions.deleteOwner(this);
		} else if (getNpcId() >= 205338 && getNpcId() <= 205343) {
			finishStage();
		} else {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (getNpcId() != 205344 || dialogId != 10000) {
			return false;
		}
		getPosition().getWorldMapInstance().getInstanceHandler().doReward(player);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		return true;
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}

	private synchronized boolean startStage(Player player) {
		if (transitioned) {
			return false;
		}
		switch (getNpcId()) {
			case 799567 -> {
				changeWorldSceneStatus(101000);
				boolean elyos = player.getRace() == Race.ELYOS;
				set(elyos ? "Condition_S1_L" : "Condition_S1_D", 0, 1);
				sayToAll(elyos ? 342681 : 342682);
			}
			case 799568 -> {
				changeWorldSceneStatus(102000);
				set("STAGE2_START", 0, 1);
				set("Condition_S2_L", 0, 1);
				sayToAll(342683);
			}
			case 799569 -> {
				changeWorldSceneStatus(103000);
				set("Condition_S3_L", 0, 1);
				sayToAll(342683);
			}
			case 205331 -> {
				changeWorldSceneStatus(104000);
				set("Condition_S4", 0, 1);
				sayToAll(342683);
			}
			case 205332 -> start(205000, "Condition_S5_L");
			case 205333 -> start(306000, "Condition_S6");
			case 205334 -> start(407000, "Condition_S7_L");
			case 205335 -> start(508000, "Condition_S8");
			case 205336 -> start(609000, "Condition_S9");
			case 205337 -> start(710000, "Condition_S10");
			default -> {
				return false;
			}
		}
		transitioned = true;
		return true;
	}

	private void start(int sceneStatus, String variable) {
		changeWorldSceneStatus(sceneStatus);
		set(variable, 0, 1);
		sayToAll(342683);
	}

	private synchronized void finishStage() {
		if (transitioned) {
			return;
		}
		StageEnd stage = switch (getNpcId()) {
			case 205338 -> new StageEnd(4, "STAGE5_START");
			case 205339 -> new StageEnd(5, "STAGE6_START");
			case 205340 -> new StageEnd(6, "STAGE7_START");
			case 205341 -> new StageEnd(7, "STAGE8_START");
			case 205342 -> new StageEnd(8, "STAGE9_START");
			case 205343 -> new StageEnd(9, "STAGE10_START");
			default -> null;
		};
		if (stage == null) {
			return;
		}
		transitioned = true;
		set("STAGE", stage.stage(), 0);
		set(stage.nextVariable(), 1, 0);
	}

	private void changeWorldSceneStatus(int status) {
		if (getPosition().getWorldMapInstance().getInstanceHandler() instanceof EmpyreanCrucibleInstance handler) {
			handler.changeWorldSceneStatus(status);
		}
	}

	private void sayToAll(int stringId) {
		for (Player player : getPosition().getWorldMapInstance().getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, stringId, getObjectId(), 1));
		}
	}

	private void set(String variable, int set, int modify) {
		RetailConditionSpawnEngine.setVariable(getPosition().getWorldMapInstance(), variable, set, modify);
	}

	private record StageEnd(int stage, String nextVariable) {
	}
}
