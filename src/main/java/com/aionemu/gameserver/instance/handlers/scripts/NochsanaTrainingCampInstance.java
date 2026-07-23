package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Nochsana's world, NPC AI and static data own the dungeon flow.
 * The artifact interaction remains here because the current Retail AI runtime
 * does not dispatch its NPC skill group through a data-driven item-use action.
 */
@InstanceID(300030000)
public class NochsanaTrainingCampInstance extends GeneralInstanceHandler {

    @Override
    public void handleUseItemFinish(Player player, Npc npc) {
        if (npc.getNpcId() == 700437) {
            GameEngineServices.skillEngine().getSkill(npc, 276, 16, player).useNoAnimationSkill();
        }
    }
}
