package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(320080000)
public class DraupnirCaveInstance extends GeneralInstanceHandler {

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		restorePhantasmDeadline();
	}

	@Override
	public void onEnterInstance(Player player) {
		if (runtimeState().getLong("draupnir.phantasm_deadline", 0) != 0) {
			return;
		}
		sendMsg(1400757, 0, false, 25, 10_000);
		long deadline = System.currentTimeMillis() + 10_000;
		runtimeState().put("draupnir.phantasm_deadline", deadline);
		scheduleDeadline("phantasm", deadline,
			() -> spawn(237276, 495.48535f, 392.0867f, 616.5717f, (byte) 89));
	}

	private void restorePhantasmDeadline() {
		long deadline = runtimeState().getLong("draupnir.phantasm_deadline", 0);
		if (deadline != 0) {
			scheduleDeadline("phantasm", deadline,
				() -> spawn(237276, 495.48535f, 392.0867f, 616.5717f, (byte) 89));
		}
	}
}
