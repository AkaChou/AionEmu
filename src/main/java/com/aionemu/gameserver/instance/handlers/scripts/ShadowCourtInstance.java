package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 暗影法庭副本事件处理器。
 * Instance event handler for Shadow Court.
 *
 * @author Encom
 */

@InstanceID(320120000)
public class ShadowCourtInstance extends GeneralInstanceHandler
{
    /** 已播放动画集合 / played-movie set */
    private List<Integer> movies = new ArrayList<Integer>();
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		if (movies.contains(423)) {
            return;
        }
		sendMovie(player, 423);
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc NPC / npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 214347: //Unfest Guard Captain.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000014, 1)); //Arena Basement Level 3 Key 1.
		    break;
			case 214349: //Dysceptic Karnif.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000011, 1)); //Arena Basement Level 2 Key 1.
		    break;
			case 214351: //Dysceptic Taiga.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000012, 1)); //Arena Basement Level 2 Key 2.
		    break;
			case 214353: //Bejeweled Mosbear.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000013, 1)); //Arena Basement Level 2 Key 3.
		    break;
			case 214357: //Cleric Wraith.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000009, 1)); //Arena Basement Level 1 Key 2.
		    break;
			case 214360: //Ranger Spirit.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000010, 1)); //Arena Basement Level 1 Key 3.
		    break;
			case 214531: //Prison Guard.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000008, 1)); //Arena Basement Level 1 Key 1.
		    break;
		}
	}
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
        movies.clear();
	}
}