package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Crucible Challenge 副本 NPC AI：Barrel（@AIName "barrel"），继承 NpcAI2。
 * Crucible Challenge instance NPC AI: Barrel (@AIName "barrel"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("barrel")
public class BarrelAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		super.handleDied();
		int npcId = 0;
		switch (getNpcId()) {
			case 217840: // 肉桶 / Meat Barrel.
				npcId = 217841; // 超薄肉片 / Wafer Thin Meat.
			break;
			case 218560: // 奥德桶 / Aether Barrel.
				npcId = 218561; // 奥德块 / Aether Lump.
			break;
		}
		spawn(npcId, 1298.4448f, 1728.3262f, 316.8472f, (byte) 63);
		AI2Actions.deleteOwner(this);
	}
}
