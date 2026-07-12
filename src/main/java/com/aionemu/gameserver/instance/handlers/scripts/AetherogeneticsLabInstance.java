package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;

/**
 * 奥德遗传实验室副本事件处理器。
 * Instance event handler for Aetherogenetics Lab.
 *
 * @author Encom
 */

@InstanceID(310050000)
public class AetherogeneticsLabInstance extends GeneralInstanceHandler
{
    /** 门映射 / door map */
    private Map<Integer, StaticDoor> doors;
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 212341: //The Keykeeper.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000001, 1)); //Lepharist Research Center Key 1.
		    break;
			case 212175: //Expert Lab Scholar.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000002, 1)); //Lepharist Research Center Key 2.
		    break;
			case 212193: //Pretor Key Keeper.
				switch (Rnd.get(1, 2)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000003, 1)); //Lepharist Research Center Key 3.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000004, 1)); //Lepharist Research Center Key 4.
				    break;
				}
		    break;
			case 212202: //Gatekeeper.
			case 212342: //Key Eater.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000005, 1)); //Lepharist Research Center Key 5.
		    break;
			case 212211: //RM-78C.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
                    }
                }
            break;
        }
    }
	
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 212211: //RM-78C.
			    //sendMsg("Congratulation]: you finish <Aetherogenetics Lab>");
			break;
		}
    }
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000001, storage.getItemCountByItemId(185000001)); //Lepharist Research Center Key 1.
		storage.decreaseByItemId(185000002, storage.getItemCountByItemId(185000002)); //Lepharist Research Center Key 2.
		storage.decreaseByItemId(185000003, storage.getItemCountByItemId(185000003)); //Lepharist Research Center Key 3.
		storage.decreaseByItemId(185000004, storage.getItemCountByItemId(185000004)); //Lepharist Research Center Key 4.
		storage.decreaseByItemId(185000005, storage.getItemCountByItemId(185000005)); //Lepharist Research Center Key 5.
    }
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
}