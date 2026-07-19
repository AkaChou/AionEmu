package com.aionemu.gameserver.ai.instance.treasureIsland;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.TreasureIslandOfCourageInstance;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

@AIName("idrun_sensory_score")
public class IdRunSensoryScoreAI2 extends NpcAI2 {

	@Override
	protected void handleCreatureSee(Creature creature) {
		registerStage(creature);
	}

	@Override
	protected void handleCreatureMoved(Creature creature) {
		registerStage(creature);
	}

	private void registerStage(Creature creature) {
		if (!(creature instanceof Player player) || !MathUtil.isIn3dRange(getOwner(), player, 20)) {
			return;
		}
		if (getPosition().getWorldMapInstance().getInstanceHandler() instanceof TreasureIslandOfCourageInstance handler) {
			handler.onStageSensor(player, getNpcId());
		}
	}
}
