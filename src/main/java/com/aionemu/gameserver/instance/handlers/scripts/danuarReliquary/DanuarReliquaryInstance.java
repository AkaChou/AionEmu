package com.aionemu.gameserver.instance.handlers.scripts.danuarReliquary;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * 达努亚尔圣物匣副本事件处理器。
 * Instance event handler for Danuar Reliquary.
 *
 * @author Encom
 */

@InstanceID(301110000)
public class DanuarReliquaryInstance extends GeneralInstanceHandler
{
	/** 理念击杀 / idean killed */
		private int ideanKilled;
	/** 克隆莫多尔已击杀 / clone modor killed */
		private int cloneModorKilled;
	/** danuarreliquary 任务 / danuar reliquary task */
		private Future<?> danuarReliquaryTask;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc 注册掉落的 NPC / registered NPC
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
            case 701795: //Danuar Reliquary Box.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052388, 1)); //Modor's Equipment Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053099, 1)); //Pure Modor's Equipment Crux Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052951, 1)); //[Event] Prestige Supplies.
                    }
                }
            break;
			case 802183: //Danuar Reliquary Opportunity Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
			break;
        }
    }
	
   /**
	 * 莫多尔启动了达努亚怨念炸弹 / Modor activated the Danuar Bomb of grudge
	 */
	private void startDanuarReliquaryTimer() {
		// 莫多尔激活了怨恨的达努阿尔炸弹。你有 15 分钟击败她。 / Modor activated the Danuar Bomb of grudge. You have 15 minutes to defeat her.
		sendMsgByRace(1401676, Race.PC_ALL, 5000);
		this.sendMessage(1401677, 10 * 60 * 1000); //10 minutes elapsed.
		this.sendMessage(1401678, 15 * 60 * 1000); //The bomb has detonated.
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
				    danuarReliquaryTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
									onExitInstance(player);
								}
							});
							onInstanceDestroy();
						}
					}, 900000); //15 Minutes.
				}
			}
		});
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc 死亡的 NPC / dead NPC
	 */
	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
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
				if (ideanKilled == 1) {
				} else if (ideanKilled == 2) {
				} else if (ideanKilled == 3) {
				    spawn(231304, 256.45197f, 257.91986f, 241.78688f, (byte) 90); //Cursed Queen's Modor.
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
							    startDanuarReliquaryTimer();
							    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900)); //15 Minutes.
						    }
					    }
				    });
				}
				despawnNpc(npc);
			break;
			case 284383: //Clone's Modor.
				cloneModorKilled ++;
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
				danuarReliquaryTask.cancel(true);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Danuar Reliquary>");
				spawn(730843, 256.45197f, 257.91986f, 241.78688f, (byte) 90); //Danuar Reliquary Exit.
				spawn(701795, 256.39725f, 255.52034f, 241.78006f, (byte) 90); //Danuar Reliquary Box.
				spawn(802183, 251.97578f, 256.2998f, 241.7948f, (byte) 68); //Danuar Reliquary Opportunity Bundle.
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
						    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
					    }
				    }
			    });
			break;
		}
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
	 * @param msg 消息 ID / message id
	 * @param race 阵营 / race
	 * @param time 延迟时间 / delay time
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
	
	private void sendMessage(final int msgId, long delay) {
        if (delay == 0) {
            this.sendMsg(msgId);
        } else {
            GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
                /**
                 * 处理 run。
                 * Handle run.
                 */
                public void run() {
                    sendMsg(msgId);
                }
            }, delay);
        }
    }
	
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