package com.aionemu.gameserver.ai.instance.steelRake;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Steel Rake 副本 NPC AI：Genies Incense Burner（@AIName "geniesincenseburner"），继承 ActionItemNpcAI2。
 * Steel Rake instance NPC AI: Genies Incense Burner (@AIName "geniesincenseburner"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("geniesincenseburner")
public class GeniesIncenseBurnerAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		AI2Actions.targetSelf(this);
		AI2Actions.useSkill(this, 18465);
	}
}
