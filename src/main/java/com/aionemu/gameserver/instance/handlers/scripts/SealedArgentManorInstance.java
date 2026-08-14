package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SealedArgentManorReward;
import com.aionemu.gameserver.model.instance.playerreward.SealedArgentManorPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 封印银庄园副本事件处理器。
 * Instance event handler for Sealed Argent Manor.
 *
 * @author Encom
 */

@InstanceID(301510000)
public class SealedArgentManorInstance extends GeneralInstanceHandler
{
		/** 军阶 / rank */
		private int rank;
	/** 开始时间 / start time */
	private long startTime;
		/** 准备计时器 / timer prepare */
		private Future<?> timerPrepare;
		/** 副本计时器 / timer instance */
		private Future<?> timerInstance;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	// 准备时间。 / Preparation Time.
		/** 准备计时秒数 / prepare timer seconds */
		private int prepareTimerSeconds = 60000; //…1 分钟 / ...1Min
	// 副本持续计时。 / Duration Instance Time.
		/** 副本计时秒数 / instance timer seconds */
		private int instanceTimerSeconds = 900000; //...15Min
	/** 副本奖励对象 / instance reward object */
	private SealedArgentManorReward instanceReward;
		/** sealed 任务 / sealed task */
		private final List<Future<?>> sealedTask = new ArrayList<Future<?>>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * @param object 可见对象 / visible object
	 * @return 结果 / result
	 */
	
	protected SealedArgentManorPlayerReward getPlayerReward(Integer object) {
		return (SealedArgentManorPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	/**
	 * 处理 addPlayerReward。
	 * Handle addPlayerReward.
	 *
	 * @param player 玩家 / player
	 */
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new SealedArgentManorPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * @return 结果 / result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
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
		switch (npcId) {
			case 237190: //Manor Usher.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000242, 1)); //Rechargeable Electric Fuel.
			break;
			/**
			 * 除军阶奖励外，银白庄园宝箱中还有多种额外物品。 / Apart from the rank rewards there are many additional items awaiting in the "Argent Manor Treasure Box"
			 */
			case 702816: //Argent Manor Treasure Box.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054117, 1)); //Argent Manor Composite Manastone Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054118, 1)); //Argent Manor Ancient Coin Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166100008, 5)); //Greater Supplements (Eternal).
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166100011, 5)); //Greater Supplements (Mythic).
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000119, 2)); //Superior Life Potion.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000122, 2)); //Superior Life Serum.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000120, 2)); //Superior Mana Potion.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 162000123, 2)); //Superior Mana Serum.
			break;
			case 237193: //Forgotten Zadra.
			case 237194: //Lost Zadra.
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
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
    public void handleUseItemFinish(Player player, Npc npc) {
        switch (npc.getNpcId()) {
            case 701001: //Transformation Bonfire.
                GameEngineServices.skillEngine().getSkill(npc, 19316, 60, player).useNoAnimationSkill();
            break;
            case 701002: //Spirit's Bucket.
                GameEngineServices.skillEngine().getSkill(npc, 19317, 60, player).useNoAnimationSkill();
            break;
            case 701003: //Magic Pinwheel.
                GameEngineServices.skillEngine().getSkill(npc, 19318, 60, player).useNoAnimationSkill();
            break;
            case 701004: //Magical Soil Mound.
                GameEngineServices.skillEngine().getSkill(npc, 19319, 60, player).useNoAnimationSkill();
            break;
			case 856547: //Drained Hetgolem.
				if (player.getInventory().decreaseByItemId(185000242, 1)) { //Rechargeable Electric Fuel.
					despawnNpc(npc);
					// 已使用电力燃料。 / Electric fuel used.
					sendMsgByRace(1402978,  Race.PC_ALL, 0);
					// 赫特魔像已激活。 / Hetgolem activated.
					sendMsgByRace(1402977,  Race.PC_ALL, 2000);
                    spawn(237196, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); //Perfectly Restored Hetgolem.
			    } else {
					// 需要电力燃料。 / Electrical fuel required.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402976));
				}
            break;
        }
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc NPC / npc
	 */
	@Override
	public void onDie(Npc npc) {
		int points = 0;
		int npcId = npc.getNpcId();
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 282208: //Eldritch Surkana.
				despawnNpc(npc);
            break;
			case 237195: //Elemental Iron Prison.
				despawnNpc(npc);
				deleteNpc(701000); //Nameless Wall.
            break;
			case 237180: //Sturdy Hetgolem.
			case 237181: //Agile Hetgolem.
			case 237182: //Mystic Hetgolem.
			    points = 300;
			break;
			case 237200: //Hard Hetgolem.
				points = 400;
			break;
			case 237196: //Perfectly Restored Hetgolem.
			    points = 1000;
			break;
			case 237193: //Forgotten Zadra.
			case 237194: //Lost Zadra.
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
							    stopInstance(player);
						    }
					    });
					}
				}, 3000);
				points = 1500;
				despawnNpc(npc);
			break;
		} if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}
	
	/**
	 * @return 你：have up to 15min to finish the instance。 / You have up to 15min to finish the instance
	 */
	private int getTime() {
		long result = (int) (System.currentTimeMillis() - startTime);
		return instanceTimerSeconds - (int) result;
	}
	
	private void sendPacket(final int nameId, final int point) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (nameId != 0) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), point));
				}
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
			}
		});
	}
	
	private int checkRank(int totalPoints) {
		if (totalPoints >= 16000) { //Rank S.
			rank = 1;
		} else if (totalPoints >= 11500) { //Rank A.
			rank = 2;
		} else if (totalPoints >= 11500) { //Rank B.
			rank = 3;
		} else if (totalPoints >= 10100) { //Rank C.
			rank = 4;
		} else if (totalPoints >= 8100) { //Rank D.
			rank = 5;
		} else {
			rank = 6;
		}
		return rank;
	}
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
	protected void startInstanceTask() {
		sealedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
					    stopInstance(player);
				    }
			    });
            }
        }, 900000));
    }
	
	/**
	 * 玩家打开门时处理。
	 * Handle a player opening a door.
	 *
	 * @param player 玩家 / player
	 * @param doorId 门 ID / doorId
	 */
	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId == 14) {
			startInstanceTask();
			doors.get(14).setOpen(true);
			// 玩家有 1 分钟准备！！！【红色计时】 / The player has 1 min to prepare !!! [Timer Red]
			if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
				// 开始副本计时！！！【白色计时】 / Start the instance time !!! [Timer White]
				startMainInstanceTimer();
			}
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
		if (!instanceReward.containPlayer(player.getObjectId())) {
			addPlayerReward(player);
		}
		SealedArgentManorPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		} switch (player.getPlayerClass()) {
			case RANGER:
			case CLERIC:
			case TEMPLAR:
            case CHANTER:
			case ASSASSIN:
			case GLADIATOR:
				spawn(237193, 819.55664f, 1420.614f, 194.97882f, (byte) 30); //Forgotten Zadra.
			break;
			case SORCERER:
			case GUNSLINGER:
			case SONGWEAVER:
			case AETHERTECH:
            case SPIRIT_MASTER:
				spawn(237194, 819.55664f, 1420.614f, 194.97882f, (byte) 30); //Lost Zadra.
			break;
		}
		startPrepareTimer();
	}
	
	private void startPrepareTimer() {
		if (timerPrepare == null) {
			timerPrepare = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
					startMainInstanceTimer();
				}
			}, prepareTimerSeconds);
		}
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(prepareTimerSeconds, instanceReward, null));
			}
		});
	}
	
	private void startMainInstanceTimer() {
		if (!timerPrepare.isDone()) {
			timerPrepare.cancel(false);
		}
		startTime = System.currentTimeMillis();
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendPacket(0, 0);
	}
	/**
	 * 停止副本并结算。
	 * Stop the instance and settle.
	 *
	 * @param player 玩家 / player
	 */
	
	protected void stopInstance(Player player) {
        stopInstanceTask();
        instanceReward.setRank(6);
		instanceReward.setRank(checkRank(instanceReward.getPoints()));
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward(player);
		// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Sealed Argent Manor>");
		sendPacket(0, 0);
	}
	
	/**
	 * 结算并发放奖励。
	 * Settle and grant rewards.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void doReward(Player player) {
		SealedArgentManorPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int manorRank = instanceReward.getRank();
			switch (manorRank) {
				case 1: //Rank S
					playerReward.setScoreAP(14000);
					playerReward.setGreaterArgentManorBox(1);
					ItemService.addItem(player, 188054114, 1); //Greater Argent Manor Box.
				break;
				case 2: //Rank A
				    playerReward.setScoreAP(12000);
					playerReward.setArgentManorBox(1);
					ItemService.addItem(player, 188054115, 1); //Argent Manor Box.
				break;
				case 3: //Rank B
				    playerReward.setScoreAP(10000);
					playerReward.setLesserArgentManorBox(1);
					ItemService.addItem(player, 188054116, 1); //Lesser Argent Manor Box.
				break;
				case 4: //Rank C
				    playerReward.setScoreAP(5000);
					playerReward.setLesserArgentManorBox(1);
					ItemService.addItem(player, 188054116, 1); //Lesser Argent Manor Box.
				break;
				case 5: //Rank D
				    playerReward.setScoreAP(2500);
					playerReward.setLesserArgentManorBox(1);
					ItemService.addItem(player, 188054116, 1); //Lesser Argent Manor Box.
				break;
			}
			AbyssPointsService.addAp(player, playerReward.getScoreAP());
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
	 * @param msg 消息 / message
	 * @param race 阵营 / race
	 * @param time 时间 / time
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
		if (timerInstance != null) {
			timerInstance.cancel(false);
		} if (timerPrepare != null) {
			timerPrepare.cancel(false);
		}
		stopInstanceTask();
		isInstanceDestroyed = true;
		instanceReward.clear();
		doors.clear();
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
		instanceReward = new SealedArgentManorReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
		Npc npc = instance.getNpc(237195); //Elemental Iron Prison.
		if (npc != null) {
			switch (Rnd.get(1, 4)) {
				case 1: //Resistance: Water.
				    GameEngineServices.skillEngine().getSkill(npc, 19312, 60, npc).useNoAnimationSkill();
				break;
				case 2: //Resistance: Fire.
				    GameEngineServices.skillEngine().getSkill(npc, 19313, 60, npc).useNoAnimationSkill();
				break;
				case 3: //Resistance: Earth.
				    GameEngineServices.skillEngine().getSkill(npc, 19314, 60, npc).useNoAnimationSkill();
				break;
				case 4: //Resistance: Wind.
				    GameEngineServices.skillEngine().getSkill(npc, 19315, 60, npc).useNoAnimationSkill();
				break;
			}
		}
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : sealedTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onPlayerLogOut(Player player) {
        removeEffects(player);
    }
	
    /**
     * 玩家离开副本时处理。
     * Handle a player leaving the instance.
     *
     * @param player 玩家 / player
     */
    @Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}
	
    private void removeEffects(Player player) {
        PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(19316);
		effectController.removeEffect(19317);
		effectController.removeEffect(19318);
		effectController.removeEffect(19319);
    }
}
