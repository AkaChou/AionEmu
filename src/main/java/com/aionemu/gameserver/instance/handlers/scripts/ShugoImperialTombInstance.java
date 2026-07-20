package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Locale;

@InstanceID(300560000)
public class ShugoImperialTombInstance extends GeneralInstanceHandler {
	private static final String CONDITION_S2 = "Condition_S2";
	private static final String CONDITION_S3 = "Condition_S3";
	private static final String CONDITION_S4 = "Condition_S4";
	private static final int CONDITION_S2_END = 130;
	private static final int CONDITION_S3_END = 173;
	private static final int CONDITION_S4_END = 141;

	private boolean instanceDestroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		restoreConditionStage(CONDITION_S2, CONDITION_S2_END);
		restoreConditionStage(CONDITION_S3, CONDITION_S3_END);
		restoreConditionStage(CONDITION_S4, CONDITION_S4_END);
	}

	public void startConditionStage(String variable, int endValue) {
		String key = stageKey(variable);
		long startedAt = runtimeState().getLong(key + ".started_at", 0);
		if (startedAt == 0) {
			startedAt = System.currentTimeMillis();
			runtimeState().put(key + ".started_at", startedAt);
		}
		advanceConditionStage(variable, endValue, startedAt);
	}

	private void restoreConditionStage(String variable, int endValue) {
		long startedAt = runtimeState().getLong(stageKey(variable) + ".started_at", 0);
		if (startedAt > 0) {
			advanceConditionStage(variable, endValue, startedAt);
		}
	}

	private void advanceConditionStage(String variable, int endValue, long startedAt) {
		if (instanceDestroyed) {
			return;
		}
		int value = conditionValue(startedAt, System.currentTimeMillis(), endValue);
		RetailConditionSpawnEngine.setVariable(instance, variable, value, 0);
		if (value < endValue) {
			long nextDeadline = startedAt + value * 1_000L;
			scheduleDeadline(stageKey(variable), nextDeadline,
				() -> advanceConditionStage(variable, endValue, startedAt));
		}
	}

	static int conditionValue(long startedAt, long now, int endValue) {
		return (int) Math.min(endValue, Math.max(0, (now - startedAt) / 1_000L) + 1);
	}

	private String stageKey(String variable) {
		return "tomb.condition." + variable.toLowerCase(Locale.ROOT);
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() == 831095) {
			GameEngineServices.skillEngine().getSkill(npc, 21096, 60, player).useNoAnimationSkill();
		}
	}

	public void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182006989, storage.getItemCountByItemId(182006989));
		storage.decreaseByItemId(182006990, storage.getItemCountByItemId(182006990));
		storage.decreaseByItemId(182006991, storage.getItemCountByItemId(182006991));
		storage.decreaseByItemId(182006999, storage.getItemCountByItemId(182006999));
	}

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21096);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	@Override
	public void onInstanceDestroy() {
		instanceDestroyed = true;
		cancelDeadline(stageKey(CONDITION_S2));
		cancelDeadline(stageKey(CONDITION_S3));
		cancelDeadline(stageKey(CONDITION_S4));
	}
}
