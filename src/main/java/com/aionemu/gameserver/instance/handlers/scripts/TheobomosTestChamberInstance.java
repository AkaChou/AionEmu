package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301610000)
public class TheobomosTestChamberInstance extends GeneralInstanceHandler {
	private static final String REWARD_SPAWNED = "theobomos_test_chamber.reward_spawned";

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		if (runtimeState().getBoolean(REWARD_SPAWNED, false)) {
			spawnReward();
		}
	}

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 220426 && !runtimeState().getBoolean(REWARD_SPAWNED, false)) {
			runtimeState().put(REWARD_SPAWNED, true);
			spawnReward();
		}
	}

	private void spawnReward() {
		spawn(806221, 284.93094f, 119.47065f, 196.01285f, (byte) 1);
	}
}
