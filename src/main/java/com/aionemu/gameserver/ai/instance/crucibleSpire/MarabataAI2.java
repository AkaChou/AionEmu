package com.aionemu.gameserver.ai.instance.crucibleSpire;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Crucible Spire 副本 NPC AI：Marabata（@AIName "IDInfinity_Named_12"），继承 AggressiveNpcAI2。
 * Crucible Spire instance NPC AI: Marabata (@AIName "IDInfinity_Named_12"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("IDInfinity_Named_12")
public class MarabataAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		// 死亡时在原地生成两个替身，随后删除自身。 / Spawn two replicas at the death position, then delete self.
		spawn(247361, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		spawn(247361, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
