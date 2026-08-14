package com.aionemu.gameserver.ai.worlds.inggison;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Inggison 区域 NPC AI：Huge Egg（@AIName "huge_egg"），继承 NpcAI2。
 * Inggison zone NPC AI: Huge Egg (@AIName "huge_egg"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("huge_egg")
public class Huge_EggAI2 extends NpcAI2
{
    @Override
	protected void handleDied() {
		spawn(217097, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); // 光翼 Coiren / Lightwing Coiren.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	public int modifyOwnerDamage(int damage) {
		return 1;
	}
	
	@Override
	public int modifyDamage(int damage) {
		return 1;
	}
}
