package com.aionemu.gameserver.ai.instance.padmarashkaCave;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Padmarashka Cave 副本 NPC AI：Padmarashka Eggs（@AIName "padmarashka_eggs"），继承 NpcAI2。
 * Padmarashka Cave instance NPC AI: Padmarashka Eggs (@AIName "padmarashka_eggs"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("padmarashka_eggs")
public class Padmarashka_EggsAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 5)) {
			case 1:
			    spawn(282615, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Neonate Drakan.
			    // 幼年德雷克。 / Neonate Drakan.
			break;
			case 2:
			    spawn(282616, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Neonate Drakan.
			    // 幼年德雷克。 / Neonate Drakan.
			break;
			case 3:
			    spawn(282617, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Neonate Drakan.
			    // 幼年德雷克。 / Neonate Drakan.
			break;
			case 4:
			    spawn(282618, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Neonate Drakan.
			    // 幼年德雷克。 / Neonate Drakan.
			break;
			case 5:
			    spawn(282619, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Neonate Drakan.
			    // 幼年德雷克。 / Neonate Drakan.
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
