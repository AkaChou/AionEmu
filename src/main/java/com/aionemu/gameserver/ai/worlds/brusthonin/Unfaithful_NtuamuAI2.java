package com.aionemu.gameserver.ai.worlds.brusthonin;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Brusthonin 区域 NPC AI：Unfaithful Ntuamu（@AIName "unfaithfulntuamu"），继承 AggressiveNpcAI2。
 * Brusthonin zone NPC AI: Unfaithful Ntuamu (@AIName "unfaithfulntuamu"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("unfaithfulntuamu")
public class Unfaithful_NtuamuAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50) {
			spawn(214583, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			AI2Actions.deleteOwner(this);
		}
	}
}
