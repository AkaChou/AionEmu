package com.aionemu.gameserver.instance.handlers.scripts.danuarReliquary;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.Set;

/**
 * 幸运达努亚尔圣物匣副本事件处理器。
 * Instance event handler for Lucky Danuar Reliquary.
 *
 * @author Encom
 */

@InstanceID(301330000)
public class Lucky_DanuarReliquaryInstance extends GeneralInstanceHandler
{
	/** 理念击杀 / idean killed */
		private int ideanKilled;
	/** 克隆莫多尔已击杀 / clone modor killed */
		private int cloneModorKilled;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		ideanKilled = runtimeState().getInt("danuar.idean_killed", 0);
		cloneModorKilled = runtimeState().getInt("danuar.clone_killed", 0);
		restoreDeadIdeans();
		if (runtimeState().getBoolean("danuar.complete", false)) {
			spawnCompletion();
		} else if (cloneModorKilled >= 5) {
			spawn(231305, 256.45197f, 257.91986f, 241.78688f, (byte) 90);
		} else if (ideanKilled >= 3) {
			spawn(231304, 256.45197f, 257.91986f, 241.78688f, (byte) 90);
		}
		restoreBombDeadlines();
	}
	
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
            case 701795: //Lucky Danuar Reliquary Box.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052388, 1)); //Modor's Equipment Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053353, 1)); //Lucky Danuar Reliquary Bundle.
                    }
                }
            break;
        }
    }
	
   /**
	 * 莫多尔启动了达努亚怨念炸弹 / Modor activated the Danuar Bomb of grudge
	 */
	private void startLuckyReliquaryTimer() {
		if (runtimeState().getLong("danuar.bomb_deadline", 0) != 0) {
			return;
		}
		long deadline = System.currentTimeMillis() + 900_000;
		runtimeState().put("danuar.bomb_deadline", deadline);
		restoreBombDeadlines();
    }

	private void restoreBombDeadlines() {
		long deadline = runtimeState().getLong("danuar.bomb_deadline", 0);
		if (deadline == 0 || runtimeState().getBoolean("danuar.complete", false)
				|| runtimeState().getBoolean("danuar.expired", false)) {
			return;
		}
		scheduleDeadline("bomb_warning", deadline - 895_000, () -> sendRaceMessage(1401676));
		scheduleDeadline("bomb_ten_minutes", deadline - 300_000, () -> sendRaceMessage(1401677));
		scheduleDeadline("bomb_expire", deadline, this::expireBomb);
	}

	private void expireBomb() {
		runtimeState().put("danuar.expired", true);
		sendRaceMessage(1401678);
		instance.doOnAllPlayers((Visitor<Player>) this::onExitInstance);
		onInstanceDestroy();
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(Npc npc) {
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 284380:
			case 284381:
			case 284382:
			case 284659:
			case 284660:
			case 284662:
			case 284663:
			case 284664:
			    despawnNpc(npc);
			break;
			case 284377: //Danuar Reliquary Novun.
			case 284378: //Idean Lapilima.
			case 284379: //Idean Obscura.
				ideanKilled ++;
				runtimeState().put("danuar.idean_killed", ideanKilled);
				runtimeState().put("danuar.dead." + npc.getNpcId(), true);
				if (ideanKilled == 1) {
				} else if (ideanKilled == 2) {
				} else if (ideanKilled == 3) {
				    spawn(231304, 256.45197f, 257.91986f, 241.78688f, (byte) 90); //Cursed Queen's Modor.
					startLuckyReliquaryTimer();
					instance.doOnAllPlayers(new Visitor<Player>() {
					    /**
					     * 处理 visit。
					     * Handle visit.
					     *
					     * @param player 玩家 / player
					     */
					    @Override
					    public void visit(Player player) {
						    if (player.isOnline()) {
							    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900)); //15 Minutes.
						    }
					    }
				    });
				}
				despawnNpc(npc);
			break;
			case 284383: //Clone's Modor.
				cloneModorKilled ++;
				runtimeState().put("danuar.clone_killed", cloneModorKilled);
				if (cloneModorKilled == 1) {
				} else if (cloneModorKilled == 2) {
				} else if (cloneModorKilled == 3) {
				} else if (cloneModorKilled == 4) {
				} else if (cloneModorKilled == 5) {
				    spawn(231305, 256.45197f, 257.91986f, 241.78688f, (byte) 90); //Enraged Queen's Modor.
				}
				despawnNpc(npc);
			break;
			case 231305: //Enraged Queen's Modor.
				completeReliquary();
			break;
		}
	}

	private void completeReliquary() {
		if (runtimeState().getBoolean("danuar.complete", false)) {
			return;
		}
		runtimeState().put("danuar.complete", true);
		cancelDeadline("bomb_warning");
		cancelDeadline("bomb_ten_minutes");
		cancelDeadline("bomb_expire");
		spawnCompletion();
		instance.doOnAllPlayers(player -> {
			if (player.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
			}
		});
	}

	private void spawnCompletion() {
		spawn(730907, 256.45197f, 257.91986f, 241.78688f, (byte) 90);
		spawn(701795, 256.39725f, 255.52034f, 241.78006f, (byte) 90);
	}

	private void restoreDeadIdeans() {
		for (int npcId : new int[] { 284377, 284378, 284379 }) {
			if (runtimeState().getBoolean("danuar.dead." + npcId, false)) {
				Npc npc = getNpc(npcId);
				if (npc != null) {
					npc.getController().onDelete();
				}
			}
		}
	}

	private void sendRaceMessage(int messageId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId)));
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
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
