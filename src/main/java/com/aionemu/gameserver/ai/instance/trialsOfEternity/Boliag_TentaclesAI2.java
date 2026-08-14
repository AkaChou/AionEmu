package com.aionemu.gameserver.ai.instance.trialsOfEternity;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.*;

/**
 * Trials Of Eternity 副本 NPC AI：Boliag Tentacles（@AIName "Dimension_Boss_Portal"），继承 AggressiveNpcAI2。
 * Trials Of Eternity instance NPC AI: Boliag Tentacles (@AIName "Dimension_Boss_Portal"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Dimension_Boss_Portal")
public class Boliag_TentaclesAI2 extends AggressiveNpcAI2
{
	/**
	 * 触手死亡时在原位置刷新对应的传送门，随后移除自身。
	 * On death, spawns the corresponding portal at this tentacle's position, then removes itself.
	 */
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 246937:
			    spawn(246724, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0, 1018);
		    break;
			case 247024:
			    spawn(246724, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0, 991);
			break;
			case 247025:
			    spawn(246724, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0, 1016);
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
