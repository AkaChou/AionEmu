package com.aionemu.gameserver.ai.instance.secretMunitionsFactory;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * Secret Munitions Factory 副本 NPC AI：Mechaturerk（@AIName "Mechaturerk"），继承 AggressiveNpcAI2。
 * Secret Munitions Factory instance NPC AI: Mechaturerk (@AIName "Mechaturerk"), extends AggressiveNpcAI2.
 *
 * @author Rinzler
 * @author Ranastic (Encom)
 */
@AIName("Mechaturerk")
public class MechaturerkAI2 extends AggressiveNpcAI2
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
	/**
	 * 死亡后随机散布 Mechaturerk 核心，并生成掉落宝箱、通往外界的通道与任务 NPC。
	 * On death, scatters Mechaturerk cores randomly and spawns loot footlockers, the corridor out, and a quest NPC.
	 */
	protected void handleDied() {
		spawnMechaturerkCore(245185);
		spawn(703381, 138.86005f, 253.14404f, 191.8727f, (byte) 0); //Mechaturerk’s Footlocker.
		spawn(703382, 138.84244f, 249.96141f, 191.8727f, (byte) 0); //Mechaturerk’s Core.
		spawn(703383, 138.83214f, 246.4382f, 191.8727f, (byte) 0); //Destruction Golem’s Footlocker.
		spawn(833998, 152.87827f, 268.53104f, 191.8727f, (byte) 106); //通往亚特雷亚的通道。 / Corridor To Atreia.
		spawn(834167, 149.93068f, 255.50876f, 191.8727f, (byte) 6); //Jay.
		super.handleDied();
	}
	
	private void spawnMechaturerkCore(int npcId) {
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
		rndSpawnInRange(npcId, Rnd.get(1, 5));
	}
	
	private Npc rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		return (Npc) spawn(npcId, p.getX() + x1, p.getY() + y1, p.getZ(), (byte) 0);
	}
}
