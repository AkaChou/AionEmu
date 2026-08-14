package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Spirit Forest Worg Morpher（@AIName "spirit_forest_worg_morpher"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Spirit Forest Worg Morpher (@AIName "spirit_forest_worg_morpher"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("spirit_forest_worg_morpher")
public class Spirit_Forest_Worg_MorpherAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242843, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 灵森 Worg 幼崽 / Spirit Forest Worg Pup.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
