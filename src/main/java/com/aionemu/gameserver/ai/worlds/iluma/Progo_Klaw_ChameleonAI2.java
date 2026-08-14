package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Progo Klaw Chameleon（@AIName "progo_klaw_chameleon"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Progo Klaw Chameleon (@AIName "progo_klaw_chameleon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("progo_klaw_chameleon")
public class Progo_Klaw_ChameleonAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(243003, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // Progo Klaw 幼兽 / Progo Klaw Fledgling.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
