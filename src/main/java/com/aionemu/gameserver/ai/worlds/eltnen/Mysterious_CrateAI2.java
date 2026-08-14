package com.aionemu.gameserver.ai.worlds.eltnen;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Eltnen 区域 NPC AI：Mysterious Crate（@AIName "mysterious_crate"），继承 NpcAI2。
 * Eltnen zone NPC AI: Mysterious Crate (@AIName "mysterious_crate"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("mysterious_crate")
public class Mysterious_CrateAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 7)) {
			case 1:
				spawn(211793, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 姆姆·蒙 / MuMu Mon.
			break;
			case 2:
				spawn(211794, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 姆姆·苏 / MuMu Zoo.
			break;
			case 3:
				spawn(211795, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 被诅咒的卡穆 / Cursed Camu.
			break;
			case 4:
				spawn(211796, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 被诅咒的米库 / Cursed Miku.
			break;
			case 5:
			  	spawn(211797, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 被诅咒的穆库 / Cursed Muku.
			break;
			case 6:
				spawn(211798, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 傲慢的阿穆鲁 / Arrogant Amurru.
			break;
			case 7:
				spawn(211800, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 混沌的德拉库斯 / Chaos Dracus.
			break;
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
