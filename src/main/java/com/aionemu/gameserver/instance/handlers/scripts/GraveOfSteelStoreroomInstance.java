package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 钢之墓储藏室副本事件处理器。
 * Instance event handler for Grave Of Steel Storeroom.
 *
 * @author Encom
 */

@InstanceID(300120000)
public class GraveOfSteelStoreroomInstance extends GeneralInstanceHandler
{
	/** gravesteel 储藏室任务 / grave of steel storeroom task */
		private Future<?> graveOfSteelStoreroomTask;
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
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** grave of steel storeroom chest / grave of steel storeroom chest */
		private List<Npc> graveOfSteelStoreroomChest = new ArrayList<Npc>();
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch(npc.getNpcId()) {
			case 215414: //Kysis Chamber Artifact.
				sendMsg("You win effect <Shield Of Compassion>");
				GameEngineServices.skillEngine().getSkill(npc, 276, 10, player).useNoAnimationSkill();
			break;
		}
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
			case 215173: //Treasurer Darmaraja.
			case 215174: //Treasurer Swaraja.
			case 215175: //Treasurer Chandra.
			case 215176: //Treasurer Dragagh.
				switch (Rnd.get(1, 3)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000061, 1)); //Kysis Armory Key.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000062, 1)); //Kysis Supply Base Key.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000063, 1)); //Kysis Operations Room Key.
					break;
				}
			break;
			case 215178: //Weakened Kysis Duke.
			case 215179: //Awakened Kysis Duke.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000065, 1)); //Kysis Gold Room Key.
			break;
        }
    }
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        doors = instance.getDoors();
		switch (Rnd.get(1, 4)) {
			case 1:
				spawn(215173, 527.769f, 212.12146f, 178.46744f, (byte) 90); //Treasurer Darmaraja.
			break;
			case 2:
				spawn(215174, 527.769f, 212.12146f, 178.46744f, (byte) 90); //Treasurer Swaraja.
			break;
			case 3:
				spawn(215175, 527.769f, 212.12146f, 178.46744f, (byte) 90); //Treasurer Chandra.
			break;
			case 4:
				spawn(215176, 527.769f, 212.12146f, 178.46744f, (byte) 90); //Treasurer Dragagh.
			break;
		} switch (Rnd.get(1, 2)) {
			case 1:
				sendMsg("<Weakened Kysis Duke> appear!!!");
				spawn(215178, 526.6656f, 845.7792f, 199.44875f, (byte) 90); //Weakened Kysis Duke.
			break;
			case 2:
				sendMsg("<Awakened Kysis Duke> appear!!!");
				spawn(215179, 526.6656f, 845.7792f, 199.44875f, (byte) 90); //Awakened Kysis Duke.
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
			case 215147: //Ranx Deathbow.
			    //某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						deleteNpc(731580);
				    }
			    }, 5000);
			break;
			case 215159: //Ranx High Mage.
			    //某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						deleteNpc(700545);
				    }
			    }, 5000);
			break;
			case 215172: //Ranx Medico.
			    //某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						deleteNpc(700546);
				    }
			    }, 5000);
			break;
			case 215177: //Ebonlord Nukuam.
			    //某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						deleteNpc(700547);
				    }
			    }, 5000);
			break;
			case 215178: //Weakened Kysis Duke.
			case 215179: //Awakened Kysis Duke.
				doors.get(11).setOpen(true);
				doors.get(15).setOpen(true);
				doors.get(17).setOpen(true);
				doors.get(18).setOpen(true);
				doors.get(19).setOpen(true);
				doors.get(20).setOpen(true);
				doors.get(28).setOpen(true);
				doors.get(74).setOpen(true);
				doors.get(76).setOpen(true);
				doors.get(79).setOpen(true);
				doors.get(80).setOpen(true);
				graveOfSteelStoreroomTask.cancel(true);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Kysis Chamber>");
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
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 478.56662f, 815.6565f, 199.76048f, (byte) 70));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 471.32745f, 834.5498f, 199.76048f, (byte) 63));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 470.52844f, 854.9471f, 199.76048f, (byte) 56));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 477.76843f, 873.94354f, 199.76036f, (byte) 50));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 490.90323f, 889.6053f, 199.76036f, (byte) 43));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 508.64328f, 899.91547f, 199.76036f, (byte) 36));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 528.42053f, 903.5909f, 199.76036f, (byte) 29));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 548.2363f, 900.31604f, 199.76036f, (byte) 23));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 565.53644f, 890.173f, 199.76036f, (byte) 16));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 578.9111f,  874.7958f, 199.76036f, (byte) 9));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 585.83545f, 855.7736f, 199.76036f, (byte) 3));
			graveOfSteelStoreroomChest.add((Npc) spawn(254574, 586.7527f, 835.4556f, 199.76036f, (byte) 116));
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer2();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(0).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer3();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(1).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer4();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(2).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer5();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(3).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer6();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(4).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer7();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(5).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer8();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(6).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer9();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(7).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer10();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(8).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer11();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(9).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					StartTimer12();
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(10).getController().onDelete();
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
			graveOfSteelStoreroomTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					sendMsg(1400244);
					sendMsg(1400245);
					graveOfSteelStoreroomChest.get(11).getController().onDelete();
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
		storage.decreaseByItemId(185000061, storage.getItemCountByItemId(185000061));
		storage.decreaseByItemId(185000062, storage.getItemCountByItemId(185000062));
		storage.decreaseByItemId(185000063, storage.getItemCountByItemId(185000063));
		storage.decreaseByItemId(185000064, storage.getItemCountByItemId(185000064));
		storage.decreaseByItemId(185000065, storage.getItemCountByItemId(185000065));
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
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("KYSIS_ARTIFACT_CONTROL_ROOM_300120000")) {
            sendMsg("Use <Kysis Chamber Artifact> to receive a skill");
	    }
    }
}