package com.aionemu.gameserver.ai.instance.contaminedUnderpath;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * Contamined Underpath 副本 NPC AI：MAAD S（@AIName "maad_s"），继承 AggressiveNpcAI2。
 * Contamined Underpath instance NPC AI: MAAD S (@AIName "maad_s"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("maad_s")
public class MAAD_SAI2 extends AggressiveNpcAI2
{
	private boolean canThink = true;
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
	}
	
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 245575: //MAAD-S.
			case 248525: //IDEvent_Def_ZombieKing_65.
				spawnMAD99SCore(246352);
			break;
		}
		super.handleDied();
	}

	/**
	 * 死亡后在附近随机生成 6 个 MAD99S 核心。
	 * Spawn 6 MAD99S cores at random nearby positions on death.
	 *
	 * @param npcId 核心 NPC ID / core NPC ID
	 */
	private void spawnMAD99SCore(int npcId) {
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
	}

	/**
	 * 在指定 NPC 周围随机方向、随机距离处生成一个 NPC。
	 * Spawn an NPC at a random direction and distance around the owner.
	 *
	 * @param npcId 要生成的 NPC ID / NPC ID to spawn
	 * @param distance 距离 / distance
	 * @return 生成的 NPC / spawned NPC
	 */
	private Npc rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		return (Npc) spawn(npcId, p.getX() + x1, p.getY() + y1, p.getZ(), (byte) 0);
	}
}
