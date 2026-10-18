package com.aionemu.gameserver.ai.instance.secretMunitionsFactory;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Secret Munitions Factory 副本 NPC AI：Mechaturerk（@AIName "Mechaturerk"），继承 AggressiveNpcAI2。
 * 死亡产物由原版 AI pattern 负责，本类仅保留注册以兼容 NPC 模板引用。
 * Secret Munitions Factory instance NPC AI: Mechaturerk (@AIName "Mechaturerk"), extends AggressiveNpcAI2.
 * Death spawns belong to the retail AI pattern; this class stays registered only for the NPC template reference.
 *
 * @author Rinzler
 * @author Ranastic (Encom)
 */
@AIName("Mechaturerk")
public class MechaturerkAI2 extends AggressiveNpcAI2 {
	@Override
	public boolean canThink() {
		return true;
	}
}
