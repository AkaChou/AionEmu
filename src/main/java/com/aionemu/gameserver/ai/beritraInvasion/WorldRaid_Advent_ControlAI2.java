package com.aionemu.gameserver.ai.beritraInvasion;


import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 贝里特拉入侵相关 NPC AI：World Raid Advent Control（@AIName "advent_control"），继承 NpcAI2。
 * Beritra-invasion related NPC AI: World Raid Advent Control (@AIName "advent_control"), extends NpcAI2.
 * @author Encom
 */
@AIName("advent_control")
public class WorldRaid_Advent_ControlAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(this::startLifeTask, 1000);
	}

	private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(WorldRaid_Advent_ControlAI2.this), 13800000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
