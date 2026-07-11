package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.*;

/**
 * 活动事件 NPC AI：Event Witch Flower Agrint（@AIName "Event_Witch_Flower_Agrint"），继承 AggressiveNpcAI2。
 * Event NPC AI: Event Witch Flower Agrint (@AIName "Event_Witch_Flower_Agrint"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Event_Witch_Flower_Agrint")
public class Event_Witch_Flower_AgrintAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 248365: //?  I.
			case 248366: //?  II.
				spawn(835678, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
			case 248367: //?  I.
			case 248368: //?  II.
				spawn(835679, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
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
