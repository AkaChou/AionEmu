package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 哈拉梅尔副本事件处理器。
 * Instance event handler for Haramel.
 *
 * @author Encom
 */

@InstanceID(300200000)
public class HaramelInstance extends GeneralInstanceHandler
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
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/haramel.xhtml"));
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 217025: //Keymaster MuMu Dang.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000103, 1)); //Rusty Key.
			break;
			case 217108: //MuMu Mechanic.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000107, 1)); //Lubricating Oil.
			break;
			case 700829: //Ancient Treasure Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188052574, 1)); //Hamerun's Special Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
				switch (Rnd.get(1, 5)) {
					case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080005, 2)); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080006, 2)); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080007, 2)); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080008, 2)); //可爱随从契约。 / Cute Minion Contract.
					break;
					case 5:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190200000, 50)); //Minium.
					break;
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
			case 700855: //Prison Doors.
			    despawnNpc(npc);
			break;
			case 216922: //Hamerun The Bleeder.
                despawnNpc(npc);
				sendMovie(player, 457);
				// 哈梅伦掉落了宝箱。 / Hamerun has dropped a treasure chest.
				sendMsgByRace(1400713, Race.PC_ALL, 0);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Haramel>");
			    spawn(700829, 224.137f, 268.608f, 144.898f, (byte) 90); //Ancient Treasure Box.
				spawn(700852, 223.93062f, 337.5487f, 142.43079f, (byte) 90); //Opened Dimensional Gate.
				if (player != null) {
					ItemService.addItem(player, 188900008, 1); //Secret Remedy Of Growth II.
				}
			break;
        }
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private boolean sendMovie(Player player, int movie) {
		if (player == null || movies.contains(movie)) {
			return false;
		}
		movies.add(movie);
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
		return true;
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
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
						}
					}
				});
			}
		}, time);
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
