package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Beshmundir Temple 副本 NPC AI：The Plaguebearer（@AIName "theplaguebearer"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: The Plaguebearer (@AIName "theplaguebearer"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("theplaguebearer")
public class ThePlaguebearerAI2 extends AggressiveNpcAI2
{
	private boolean isStart = false;
	
	private void checkPercentage (int hpPercentage) {
		if (hpPercentage == 90) {
			isStart = true;
			summons();
		} if (hpPercentage == 70) {
			isStart = true;
			summons();
		} if (hpPercentage == 50) {
			isStart = true;
			summons();
		} if (hpPercentage == 30) {
			isStart = true;
			summons();
		}
	}
	
	private void summons() {
		if (getPosition().isSpawned() && !isAlreadyDead() && isStart) {
			for (int i = 0; i < 1; i++) {
				int distance = Rnd.get(4, 10);
				int nrNpc = Rnd.get(1, 2);
				switch (nrNpc) {
				    case 1:
					    nrNpc = 281808; // 瘟疫使者残片 / Plaguebearer Fragment.
					break;
				    case 2:
					    nrNpc = 281809; // 瘟疫使者残片 / Plaguebearer Fragment.
					break;
				}
				rndSpawnInRange(nrNpc, distance);
			}
		}
	}
	
	private void rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		spawn(npcId, getPosition().getX() + x1, getPosition().getY() + y1, getPosition().getZ(), (byte) 0);
	}
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	@Override
	protected void handleBackHome() {
		isStart = false;
		super.handleBackHome();
	}
}
