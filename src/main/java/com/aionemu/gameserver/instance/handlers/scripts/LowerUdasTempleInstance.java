package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 下乌达斯神殿副本事件处理器。
 * Instance event handler for Lower Udas Temple.
 *
 * @author Encom
 */

@InstanceID(300160000)
public class LowerUdasTempleInstance extends GeneralInstanceHandler
{
    /**
	 * whether timer1 started
	 */
        private boolean isStartTimer1 = false;
	/** 是否启动计时器2 / is start timer2 */
		private boolean isStartTimer2 = false;
	/** 是否启动计时器3 / is start timer3 */
		private boolean isStartTimer3 = false;
	/** 是否启动计时器4 / is start timer4 */
		private boolean isStartTimer4 = false;
	/** 是否启动计时器5 / is start timer5 */
		private boolean isStartTimer5 = false;
	/** 是否启动计时器6 / is start timer6 */
		private boolean isStartTimer6 = false;
	/** 是否启动计时器7 / is start timer7 */
		private boolean isStartTimer7 = false;
	/** 是否启动计时器8 / is start timer8 */
		private boolean isStartTimer8 = false;
	/** 是否启动计时器9 / is start timer9 */
		private boolean isStartTimer9 = false;
	/** 是否启动计时器10 / is start timer10 */
		private boolean isStartTimer10 = false;
	/** 是否启动计时器11 / is start timer11 */
		private boolean isStartTimer11 = false;
	/** 是否启动计时器12 / is start timer12 */
		private boolean isStartTimer12 = false;
	/** chestudastemple 任务 / chest udas temple task */
		private Future<?> chestUdasTempleTask;
	/** udas temple chest / udas temple chest */
		private List<Npc> udasTempleChest = new ArrayList<Npc>();
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 215796: //Gradarim The Collector.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000087, 1)); //Jotun Vault Key.
		    break;
			case 215786: //Garha The Punisher.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000086, 1)); //Shadowy Prison Key.
		    break;
			case 215797: //Bergrisar.
			case 216149: //Udas Temple Treasure Box.
			case 216150: //Udas Temple Treasure Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188052306, 1)); //Udas Temple Contribution Bundle.
		    break;
			case 215783: //Nexus.
			case 215795: //Debilkarim The Maker.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundlele.
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
			case 215795: //Debilkarim The Maker.
			    chestUdasTempleTask.cancel(true);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Lower Udas Temple>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 575.1232f, 1295.7212f, 187.85898f, (byte) 113); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 575.1232f, 1295.7212f, 187.85898f, (byte) 113); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
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
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		super.onInstanceCreate(instance);
		if (!isStartTimer1) {
			isStartTimer1 = true;
			System.currentTimeMillis();
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
						PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			udasTempleChest.add((Npc) spawn(216149, 445.99957f, 1178.3578f, 193.02937f, (byte) 21));
            udasTempleChest.add((Npc) spawn(216149, 448.85532f, 1205.2148f, 191.59023f, (byte) 15));
            udasTempleChest.add((Npc) spawn(216149, 452.71637f, 1180.77f, 190.47333f, (byte) 85));
            udasTempleChest.add((Npc) spawn(216149, 440.6775f, 1198.4562f, 191.70049f, (byte) 50));
            udasTempleChest.add((Npc) spawn(216149, 449.19788f, 1197.8282f, 190.50172f, (byte) 24));
            udasTempleChest.add((Npc) spawn(216149, 436.17404f, 1185.6791f, 190.22073f, (byte) 13));
			udasTempleChest.add((Npc) spawn(216150, 442.38748f, 1186.572f, 190.88919f, (byte) 14));
            udasTempleChest.add((Npc) spawn(216150, 433.22824f, 1198.147f, 192.34004f, (byte) 0));
			udasTempleChest.add((Npc) spawn(216150, 462.2652f, 1180.8121f, 191.70518f, (byte) 85));
            udasTempleChest.add((Npc) spawn(216150, 455.50082f, 1176.3575f, 192.6768f, (byte) 34));
			udasTempleChest.add((Npc) spawn(216150, 436.63177f, 1192.1348f, 190.88254f, (byte) 119));
            udasTempleChest.add((Npc) spawn(216150, 438.38586f, 1202.9849f, 192.8323f, (byte) 105));
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer2();
					sendMsg(1400245);
					udasTempleChest.get(0).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer2() {
        if (!isStartTimer2) {
			isStartTimer2 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer3();
					sendMsg(1400245);
					udasTempleChest.get(1).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer3() {
	    if (!isStartTimer3) {
			isStartTimer3 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer4();
					sendMsg(1400245);
					udasTempleChest.get(2).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer4() {
	    if (!isStartTimer4) {
			isStartTimer4 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer5();
					sendMsg(1400245);
					udasTempleChest.get(3).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer5() {
	    if (!isStartTimer5) {
			isStartTimer5 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer6();
					sendMsg(1400245);
					udasTempleChest.get(4).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer6() {
	    if (!isStartTimer6) {
			isStartTimer6 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer7();
					sendMsg(1400245);
					udasTempleChest.get(5).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer7() {
	    if (!isStartTimer7) {
			isStartTimer7 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer8();
					sendMsg(1400245);
					udasTempleChest.get(6).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer8() {
	    if (!isStartTimer8) {
			isStartTimer8 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer9();
					sendMsg(1400245);
					udasTempleChest.get(7).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer9() {
	    if (!isStartTimer9) {
			isStartTimer9 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer10();
					sendMsg(1400245);
					udasTempleChest.get(8).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer10() {
	    if (!isStartTimer10) {
			isStartTimer10 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer11();
					sendMsg(1400245);
					udasTempleChest.get(9).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer11() {
	    if (!isStartTimer11) {
			isStartTimer11 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer12();
					sendMsg(1400245);
					udasTempleChest.get(10).getController().onDelete();
				}
			}, 300000);
		}
	}
	
	private void StartTimer12() {
	    if (!isStartTimer12) {
			isStartTimer12 = true;
			System.currentTimeMillis();
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
					    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 300));
					}
				}
			});
			chestUdasTempleTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					sendMsg(1400244);
					sendMsg(1400245);
					udasTempleChest.get(11).getController().onDelete();
				}
			}, 300000);
		}
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
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000086, storage.getItemCountByItemId(185000086)); //Jotun Vault Key.
		storage.decreaseByItemId(185000087, storage.getItemCountByItemId(185000087)); //Shadowy Prison Key.
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