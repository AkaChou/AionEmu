package com.aionemu.gameserver.instance.handlers.scripts;

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
import com.aionemu.gameserver.model.instance.instancereward.ShugoEmperorVaultReward;
import com.aionemu.gameserver.model.instance.playerreward.ShugoEmperorVaultPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 皇帝特里鲁内克保险箱副本事件处理器。
 * Instance event handler for Emperor Trillirunerk Safe.
 *
 * @author Encom
 */

@InstanceID(301590000)
public class EmperorTrillirunerkSafeInstance extends GeneralInstanceHandler
{
		/** 军阶 / rank */
		private int rank;
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
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
		private int instanceTimerSeconds = 600000; //...10Min
	/** 副本奖励对象 / instance reward object */
	private ShugoEmperorVaultReward instanceReward;
		/** 宝库任务 / vault task */
		private final List<Future<?>> vaultTask = new ArrayList<Future<?>>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * @param object 可见对象 / visible object
	 * @return 结果 / result
	 */
	
	protected ShugoEmperorVaultPlayerReward getPlayerReward(Integer object) {
		return (ShugoEmperorVaultPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	/**
	 * 处理 addPlayerReward。
	 * Handle addPlayerReward.
	 *
	 * @param player 玩家 / player
	 */
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new ShugoEmperorVaultPlayerReward(player.getObjectId()));
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
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 235643: //Indirunerk Jonakak's Supply Box.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002079, 2)); //Shugo Warrior Secret Remedy.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002080, 2)); //High-Quality Shugo Warrior Secret Remedy.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002081, 2)); //Shugo Monk Secret Remedy.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002082, 2)); //High-Quality Shugo Monk Secret Remedy.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002083, 2)); //Shugo Warrior Secret Remedy.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 162002084, 2)); //High-Quality Shugo Warrior Secret Remedy.
					}
				}
			break;
			case 832929: //Emperor's Treasure Box.
			case 832930: //Emperor's Quality Treasure Box.
				switch (Rnd.get(1, 2)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054631, 1)); //Middle Grade Reward Bundle.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054632, 1)); //Low Grade Reward Bundle.
				    break;
				}
			break;
			case 832931: //Emperor's Premium Treasure Box.
				switch (Rnd.get(1, 4)) {
					case 1:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054629, 1)); //Highest Grade Reward Bundle.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054630, 1)); //High Grade Reward Bundle.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054631, 1)); //Middle Grade Reward Bundle.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054632, 1)); //Low Grade Reward Bundle.
					break;
				}
			break;
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000222, storage.getItemCountByItemId(185000222)); //Rusted Vault Key.
		storage.decreaseByItemId(162002031, storage.getItemCountByItemId(162002079)); //Shugo Warrior Secret Remedy.
		storage.decreaseByItemId(162002032, storage.getItemCountByItemId(162002080)); //High-Quality Shugo Warrior Secret Remedy.
		storage.decreaseByItemId(162002033, storage.getItemCountByItemId(162002081)); //Shugo Monk Secret Remedy.
		storage.decreaseByItemId(162002034, storage.getItemCountByItemId(162002082)); //High-Quality Shugo Monk Secret Remedy.
		storage.decreaseByItemId(162002035, storage.getItemCountByItemId(162002083)); //Shugo Warrior Secret Remedy.
		storage.decreaseByItemId(162002036, storage.getItemCountByItemId(162002084)); //High-Quality Shugo Warrior Secret Remedy.
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
		    case 235629: //Intruder Skirmisher.
			case 235630: //Intruder Scout.
				points = 180;
				despawnNpc(npc);
			break;
			case 235631: //Brainwashed Peon.
			    points = 160;
				despawnNpc(npc);
			break;
			case 235633: //Intruder Marksman.
			    points = 1070;
				despawnNpc(npc);
			break;
			case 235634: //Watchman Hokuruki.
				points = 2040;
				despawnNpc(npc);
				// 使用已开启入口前往下一区域。 / Use the open entrance to move to the next area.
				sendMsgByRace(1402781, Race.PC_ALL, 0);
				spawn(832924, 469.53888f, 657.56543f, 396.91852f, (byte) 0, 432);
				spawn(235643, 486.0f, 638.0f, 395.875f, (byte) 108); //Indirunerk Jonakak's Supply Box.
			break;
			case 235635: //Intruder Challenger.
			case 235650: //Intruder Assassin.
				points = 700;
				despawnNpc(npc);
			break;
			case 235637: //Intruder Guard.
				points = 820;
				despawnNpc(npc);
			break;
			case 235640: //Captain Mirez.
				points = 12000;
				despawnNpc(npc);
				// 格拉迪第二军官已出现！准备迎战长刀佐迪卡！ / Gradi's second officer has appeared! Prepare for Longknife Zodica!
				sendMsgByRace(1402679, Race.PC_ALL, 0);
				// 格拉迪的第二随从出现！ / The Second Henchman of Gradi appears!
				sendMsgByRace(1402885, Race.PC_ALL, 2000);
				spawn(235685, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //Longknife Zodica.
			break;
			case 235641: //Shugo Turncoat.
				points = 660;
				despawnNpc(npc);
			break;
			case 235647: //Grand Commander Gradi.
				points = 400000;
				despawnNpc(npc);
				// 所有入侵者已逃离。你肃清了宝库！ / All the intruders have fled. You've cleared the Vault!
				sendMsgByRace(1402681, Race.PC_ALL, 2000);
                spawn(832932, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //The Shugo Emperor's Butler.
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
			case 235649: //Intruder Sniper.
				points = 760;
				despawnNpc(npc);
			break;
			case 235651: //Intruder Gladiator.
				points = 1400;
				despawnNpc(npc);
			break;
			case 235652: //Intruder Warrior.
			case 235653: //Intruder Sharpeye.
				points = 250;
				despawnNpc(npc);
			break;
			case 235660: //Ruthless Jabaraki.
				points = 1740;
				despawnNpc(npc);
				// 格拉迪第一军官已出现！准备迎战队长米雷兹！ / Gradi's first officer has appeared! Prepare for Captain Mirez!
				sendMsgByRace(1402678, Race.PC_ALL, 0);
				// 格拉迪的第一随从出现！ / The First Henchman of Gradi appears!
				sendMsgByRace(1402884, Race.PC_ALL, 2000);
				spawn(235640, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //Captain Mirez.
			break;
			case 235680: //Intruder Brawler.
			case 235681: //Intruder Lookout.
				points = 530;
				despawnNpc(npc);
			break;
			case 235683: //Elite Captain Rupasha.
				points = 272000;
				despawnNpc(npc);
				// 贪婪的格拉迪（入侵指挥官）已出现。准备战斗！ / Greedy Gradi, the intruder commander, has appeared. Get ready for a fight!
				sendMsgByRace(1402743, Race.PC_ALL, 0);
				// 格拉迪的第五随从出现！ / The Fifth Henchman of Gradi appears!
				sendMsgByRace(1402888, Race.PC_ALL, 2000);
				spawn(235647, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //Grand Commander Gradi.
			break;
			case 235684: //Sorcerer Budyn.
				points = 48000;
				despawnNpc(npc);
				// 格拉迪最终军官已出现！准备迎战精英队长鲁帕沙！ / Gradi's final officer has appeared! Prepare for Elite Captain Rupasha!
				sendMsgByRace(1402742, Race.PC_ALL, 0);
				// 格拉迪的第四随从出现！ / The Fourth Henchman of Gradi appears!
				sendMsgByRace(1402887, Race.PC_ALL, 2000);
				spawn(235683, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //Elite Captain Rupasha.
			break;
			case 235685: //Longknife Zodica.
				points = 14400;
				despawnNpc(npc);
				// 格拉迪第三军官已出现！准备迎战巫师布丁！ / Gradi's third officer has appeared! Prepare for Sorcerer Budyn!
				sendMsgByRace(1402680, Race.PC_ALL, 0);
				// 格拉迪的第三随从出现！ / The Third Henchman of Gradi appears!
				sendMsgByRace(1402886, Race.PC_ALL, 2000);
				spawn(235684, 360.03033f, 757.95233f, 398.42203f, (byte) 104); //Sorcerer Budyn.
			break;
		} if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21829);
		effectController.removeEffect(21830);
		effectController.removeEffect(21831);
		effectController.removeEffect(21832);
		effectController.removeEffect(21833);
		effectController.removeEffect(21834);
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
		if (totalPoints >= 878600) { //Rank S.
			rank = 1;
		} else if (totalPoints >= 463800) { //Rank A.
			rank = 2;
		} else if (totalPoints >= 165100) { //Rank B.
			rank = 3;
		} else if (totalPoints >= 54000) { //Rank C.
			rank = 4;
		} else if (totalPoints >= 180) { //Rank D.
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
		vaultTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
				spawn(832950, 362.71112f, 760.5198f, 398.42203f, (byte) 104); //The Shugo Emperor's Exit.
            }
        }, 600000));
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
		if (doorId == 430) {
			startInstanceTask();
			doors.get(430).setOpen(true);
			// 成员招募窗口已过，无法再招募成员。 / The member recruitment window has passed. You cannot recruit any more members.
			sendMsgByRace(1401181, Race.PC_ALL, 5000);
			// 宝库检测到入侵者！ / Intruders detected in the Vault!
			sendMsgByRace(1402677, Race.PC_ALL, 10000);
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
		ShugoEmperorVaultPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		} if (spawnRace == null) {
			spawnRace = player.getRace();
			spawnVaultRace();
		}
		startPrepareTimer();
	}
	
	private void spawnVaultRace() {
	    final int templarerk = spawnRace == Race.ASMODIANS ? 833494 : 833491; //Brave Templarerk's Soul.
        final int gladiatorerk = spawnRace == Race.ASMODIANS ? 833495 : 833492; //Furious Gladiatorerk's Soul.
        final int sorcererk = spawnRace == Race.ASMODIANS ? 833496 : 833493; //Roiling Sorcererk's Soul.
		spawn(templarerk, 541.1751f, 302.90582f, 400.49493f, (byte) 76);
		spawn(templarerk, 465.46735f, 638.66113f, 395.375f, (byte) 100);
        spawn(templarerk, 420.09814f, 688.6983f, 398.42203f, (byte) 14);
		spawn(gladiatorerk, 543.03845f, 302.22098f, 400.48618f, (byte) 89);
		spawn(gladiatorerk, 467.3807f, 640.5601f, 395.41f, (byte) 112);
        spawn(gladiatorerk, 417.19376f, 691.69653f, 398.42203f, (byte) 14);
		spawn(sorcererk, 544.87866f, 302.53723f, 400.55246f, (byte) 98);
		spawn(sorcererk, 467.43195f, 643.59753f, 395.5f, (byte) 6);
        spawn(sorcererk, 414.0031f, 694.8936f, 398.42203f, (byte) 14);
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
		// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Emperor Trillirunerk's Safe>");
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
		ShugoEmperorVaultPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int vaultRank = instanceReward.getRank();
			switch (vaultRank) {
				case 1: //Rank S
					playerReward.setRustedVaultKey(6);
					ItemService.addItem(player, 185000222, 6); //Rusted Vault Key.
				break;
				case 2: //Rank A
					playerReward.setRustedVaultKey(4);
					ItemService.addItem(player, 185000222, 4); //Rusted Vault Key.
				break;
				case 3: //Rank B
					playerReward.setRustedVaultKey(3);
					ItemService.addItem(player, 185000222, 3); //Rusted Vault Key.
				break;
				case 4: //Rank C
					playerReward.setRustedVaultKey(2);
					ItemService.addItem(player, 185000222, 2); //Rusted Vault Key.
				break;
			}
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
		instanceReward = new ShugoEmperorVaultReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : vaultTask) {
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
	 * @param npc NPC / npc
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
}
