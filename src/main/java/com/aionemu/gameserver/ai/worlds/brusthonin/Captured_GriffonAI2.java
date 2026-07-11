package com.aionemu.gameserver.ai.worlds.brusthonin;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Brusthonin 区域 NPC AI：Captured Griffon（@AIName "captured_griffon"），继承 NpcAI2。
 * Brusthonin zone NPC AI: Captured Griffon (@AIName "captured_griffon"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("captured_griffon")
public class Captured_GriffonAI2 extends NpcAI2
{
	@Override
    protected void handleDespawned() {
        super.handleDespawned();
		AI2Actions.scheduleRespawn(this);
    }
}
