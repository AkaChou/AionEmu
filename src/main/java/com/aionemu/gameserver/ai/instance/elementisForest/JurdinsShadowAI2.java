package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Elementis Forest 副本 NPC AI：Jurdins Shadow（@AIName "jurdinshadow"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Jurdins Shadow (@AIName "jurdinshadow"), extends AggressiveNpcAI2.
 *
 * @author Luzien
 */
@AIName("jurdinshadow")
public class JurdinsShadowAI2 extends AggressiveNpcAI2 {
	
	@Override
	public void handleSpawned() {
		super.handleSpawned();
		AI2Actions.useSkill(this, 19404);
	}
}
