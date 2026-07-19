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
import com.aionemu.gameserver.model.instance.instancereward.SmolderingReward;
import com.aionemu.gameserver.model.instance.playerreward.SmolderingPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 闷燃火焰神殿副本事件处理器。
 * Instance event handler for Smoldering Fire Temple.
 *
 * @author Encom
 */

@InstanceID(302000000)
public class SmolderingFireTempleInstance extends GeneralInstanceHandler
{
	/** 开始时间 / start time */
	private long startTime;
	/** vengeful obscura / vengeful obscura */
		private int vengefulObscura;
	/** 准备计时器 / timer prepare */
		private Future<?> timerPrepare;
	/** 副本计时器 / timer instance */
		private Future<?> timerInstance;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本奖励对象 / instance reward object */
	private SmolderingReward instanceReward;
	// 准备时间。 / Preparation Time.
	/** 准备计时秒数 / prepare timer seconds */
		private int prepareTimerSeconds = 60000; //…1 分钟 / ...1Min
	// 副本持续计时。 / Duration Instance Time.
	/** 副本计时秒数 / instance timer seconds */
		private int instanceTimerSeconds = 600000; //...10Min
	/** smoldering 任务 / smoldering task */
		private final List<Future<?>> smolderingTask = new ArrayList<Future<?>>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * visible object
	 * result
	 */
	
	protected SmolderingPlayerReward getPlayerReward(Integer object) {
		return (SmolderingPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	/**
	 * 处理 addPlayerReward。
	 * Handle addPlayerReward.
	 *
	 * @param player 玩家 / player
	 */
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new SmolderingPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
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
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 244435: //Potion Chest.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002085, 2)); //Hero GM’s Secret Remedy Of Recovery.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002086, 2)); //Hero GM’s Quality Secret Remedy Of Recovery.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002087, 2)); //Hero GM’s Secret Remedy Of DP Recovery.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002088, 2)); //Hero GM’s Quality Secret Remedy Of DP Recovery.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002089, 2)); //Hero GM’s Secret Remedy Of Recovery.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002090, 2)); //Hero GM’s Quality Secret Remedy Of Recovery.
					}
				}
			break;
			case 834058: //Smoldering Fire Temple Treasure Chest.
			case 834059: //Smoldering Fire Temple Premium Treasure Chest.
			case 834060: //Smoldering Fire Temple Treasure Chest.
			case 834061: //Smoldering Fire Temple Quality Treasure Chest.
				switch (Rnd.get(1, 4)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054631, 1)); //Middle Grade Reward Bundle.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054632, 1)); //Low Grade Reward Bundle.
				    break;
					case 3:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054629, 1)); //Highest Grade Reward Bundle.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054630, 1)); //High Grade Reward Bundle.
					break;
				}
			break;
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000270, storage.getItemCountByItemId(185000270)); //Nostalgic Fire Temple Treasure Chest Key.
		storage.decreaseByItemId(162002031, storage.getItemCountByItemId(162002085)); //Hero GM’s Secret Remedy Of Recovery.
		storage.decreaseByItemId(162002032, storage.getItemCountByItemId(162002086)); //Hero GM’s Quality Secret Remedy Of Recovery.
		storage.decreaseByItemId(162002033, storage.getItemCountByItemId(162002087)); //Hero GM’s Secret Remedy Of DP Recovery.
		storage.decreaseByItemId(162002034, storage.getItemCountByItemId(162002088)); //Hero GM’s Quality Secret Remedy Of DP Recovery.
		storage.decreaseByItemId(162002035, storage.getItemCountByItemId(162002089)); //Hero GM’s Secret Remedy Of Recovery.
		storage.decreaseByItemId(162002036, storage.getItemCountByItemId(162002090)); //Hero GM’s Quality Secret Remedy Of Recovery.
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(Npc npc) {
		int points = 0;
		int npcId = npc.getNpcId();
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 244084: //Flame Spirit.
			case 244085: //Fire Spirit.
			case 244091: //Flame Spirit.
				points = 180;
				despawnNpc(npc);
			break;
			case 244086: //Kalgolem.
			case 244092: //Fire Spirit.
				points = 160;
				despawnNpc(npc);
			break;
			case 244087: //Enhanced Kalgolem.
			case 244088: //Vengeful Obscura.
				points = 250;
				despawnNpc(npc);
			break;
			case 244093: //Vengeful Obscura.
				points = 250;
				despawnNpc(npc);
				vengefulObscura++;
				if (vengefulObscura == 12) {
					spawn(244097, 416.1324f, 97.165924f, 117.19401f, (byte) 50); //Temple Guardian.
				}
			break;
			case 244089: //Vengeful Obscura.
				points = 660;
				despawnNpc(npc);
			break;
			case 244094: //Enhanced Orange Crystal Molgat.
				points = 1740;
				despawnNpc(npc);
			break;
			case 244095: //Enhanced Silver Blade Rotan.
				points = 2040;
				despawnNpc(npc);
				doors.get(8).setOpen(true);
			break;
			case 244096: //Enhanced Tough Sipus.
				points = 12000;
				despawnNpc(npc);
				spawn(834067, 292.34671f, 166.54131f, 119.53692f, (byte) 0, 40);
			break;
			case 244097: //Temple Guardian.
				points = 14400;
				despawnNpc(npc);
				spawn(834066, 169.24069f, 417.35110f, 140.77321f, (byte) 0, 3);
				spawn(244098, 416.1324f, 97.165924f, 117.19401f, (byte) 50); //Enraged Lady Angerr.
			break;
			case 244098: //Enraged Lady Angerr.
				points = 48000;
				despawnNpc(npc);
				spawn(244099, 416.1324f, 97.165924f, 117.19401f, (byte) 50); //Enraged Judge Kaliga.
			break;
			case 244099: //Enraged Judge Kaliga.
				points = 272000;
				despawnNpc(npc);
				spawn(244100, 416.1324f, 97.165924f, 117.19401f, (byte) 50); //Enraged Kromede.
			break;
			case 244100: //Enraged Kromede.
				points = 500000;
				despawnNpc(npc);
				spawn(834068, 416.1324f, 97.165924f, 117.19401f, (byte) 50); //Old Fire Temple Fortune Server.
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
			break;
		} if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		PlayerEffectController effectController = player.getEffectController();
		switch (npc.getNpcId()) {
			case 834055: //GM Stomper.
				if (player.getCommonData().getRace() == Race.ELYOS) {
				    effectController.removeEffect(21376);
				    effectController.removeEffect(21377);
				    GameEngineServices.skillEngine().getSkill(npc, 21375, 1, player).useNoAnimationSkill();
				} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
					effectController.removeEffect(21379);
				    effectController.removeEffect(21380);
				    GameEngineServices.skillEngine().getSkill(npc, 21378, 1, player).useNoAnimationSkill();
				}
			break;
			case 834056: //GM Shine.
			    if (player.getCommonData().getRace() == Race.ELYOS) {
				    effectController.removeEffect(21375);
				    effectController.removeEffect(21377);
				    GameEngineServices.skillEngine().getSkill(npc, 21376, 1, player).useNoAnimationSkill();
				} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
				    effectController.removeEffect(21378);
				    effectController.removeEffect(21380);
				    GameEngineServices.skillEngine().getSkill(npc, 21379, 1, player).useNoAnimationSkill();
				}
			break;
			case 834057: //GM Iris.
			    if (player.getCommonData().getRace() == Race.ELYOS) {
				    effectController.removeEffect(21375);
				    effectController.removeEffect(21376);
				    GameEngineServices.skillEngine().getSkill(npc, 21377, 1, player).useNoAnimationSkill();
				} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
				    effectController.removeEffect(21378);
				    effectController.removeEffect(21379);
				    GameEngineServices.skillEngine().getSkill(npc, 21380, 1, player).useNoAnimationSkill();
				}
			break;
		}
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21375);
		effectController.removeEffect(21376);
		effectController.removeEffect(21377);
		effectController.removeEffect(21378);
		effectController.removeEffect(21379);
		effectController.removeEffect(21380);
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
		removeEffects(player);
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
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
		removeEffects(player);
	}
	
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
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
				Math.max(0, System.currentTimeMillis() - startTime) / 1000);
	}
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
	protected void startInstanceTask() {
		smolderingTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        }, 600000));
    }
	
	/**
	 * 玩家打开门时处理。
	 * Handle a player opening a door.
	 *
	 * 玩家 / player
	 * doorId
	 */
	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId == 2) {
			startInstanceTask();
			doors.get(2).setOpen(true);
			// 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
			sendMsgByRace(1401181, Race.PC_ALL, 0);
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
		SmolderingPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
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
		// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Smoldering Fire Temple>");
		sendPacket(0, 0);
	}
	
	private void rewardGroup() {
		for (Player p: instance.getPlayersInside()) {
			doReward(p);
		}
	}
	
	/**
	 * 结算并发放奖励。
	 * Settle and grant rewards.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void doReward(Player player) {
		SmolderingPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			int smolderingRank = instanceReward.getRank();
			RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, smolderingRank);
			playerReward.setSmolderingKey(Math.toIntExact(plan.itemCount(185000270)));
			InstanceSettlementService.settleTimeAttack(instance, player, smolderingRank);
			playerReward.setRewarded();
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
		instanceReward = new SmolderingReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : smolderingTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
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
	 * 移除指定 NPC。
	 * Despawn the given NPC.
	 *
	 * npc
	 */
	
	protected void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
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
}
