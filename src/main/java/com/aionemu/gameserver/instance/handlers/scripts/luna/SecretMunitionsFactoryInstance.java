package com.aionemu.gameserver.instance.handlers.scripts.luna;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SecretMunitionsFactoryReward;
import com.aionemu.gameserver.model.instance.playerreward.SecretMunitionsFactoryPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
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
 * 秘密军工厂副本事件处理器。
 * Instance event handler for Secret Munitions Factory.
 *
 * @author Encom
 */

@InstanceID(301640000)
public class SecretMunitionsFactoryInstance extends GeneralInstanceHandler
{
	/** 军阶 / rank */
		private int rank;
	/** 开始时间 / start time */
	private long startTime;
	/** 技能种族 / skill race */
		private Race skillRace;
	/** 准备计时器 / timer prepare */
		private Future<?> timerPrepare;
	/** factory 任务 A1 / factory task a1 */
		private Future<?> factoryTaskA1;
	/** factory 任务 A2 / factory task a2 */
		private Future<?> factoryTaskA2;
	/** factory 任务 A3 / factory task a3 */
		private Future<?> factoryTaskA3;
	/** 副本计时器 / timer instance */
		private Future<?> timerInstance;
	/** mecha infantryman killed / mecha infantryman killed */
		private int mechaInfantrymanKilled;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	// 准备时间。 / Preparation Time.
	/** 准备计时秒数 / prepare timer seconds */
		private int prepareTimerSeconds = 60000; //…1 分钟 / ...1Min
	// 副本持续计时。 / Duration Instance Time.
	/** 副本计时秒数 / instance timer seconds */
		private int instanceTimerSeconds = 3600000; //...1 小时 / ...1Hr
	/** 副本奖励对象 / instance reward object */
	private SecretMunitionsFactoryReward instanceReward;
	/** factory task1 / factory task1 */
		private final List<Future<?>> factoryTask1 = new ArrayList<>();
	/** factory task2 / factory task2 */
		private final List<Future<?>> factoryTask2 = new ArrayList<>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * @param object 可见对象 / visible object
	 * @return 奖励记录 / result
	 */
	
	protected SecretMunitionsFactoryPlayerReward getPlayerReward(Integer object) {
		return (SecretMunitionsFactoryPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	/**
	 * 处理 addPlayerReward。
	 * Handle addPlayerReward.
	 *
	 * @param player 玩家 / player
	 */
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new SecretMunitionsFactoryPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * @return 奖励记录 / result
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
			case 245185: // 机械图尔克的核心 / Mechaturerk’s Core.
			    switch (Rnd.get(1, 7)) {
				    case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150000, 2)); // 未切割的水晶 / Uncut Crystal.
				    break;
					case 2:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150001, 2)); // 有缺口的水晶 / Chipped Crystal.
				    break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150002, 2)); // 浑浊的水晶 / Cloudy Crystal.
				    break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150003, 2)); // 透明的水晶 / Clear Crystal.
				    break;
					case 5:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150004, 2)); // 无瑕的水晶 / Flawless Crystal.
				    break;
					case 6:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150005, 2)); // 露娜之光 / Luna’s Light.
				    break;
					case 7:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 152150006, 2)); // 露娜的祝福 / Luna’s Blessing.
				    break;
			    }
			break;
			case 834443: // 机械图尔克的宝物箱 / Mechaturerk’s Treasure Box.
			case 834444: // 机械图尔克的特殊宝物箱 / Mechaturerk’s Special Treasure Box.
			break;
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000418, storage.getItemCountByItemId(164000418)); // 臭气弹 / Stink Bomb.
		storage.decreaseByItemId(164002362, storage.getItemCountByItemId(164002362)); // 机械图尔克油桶 / Mechaturerk Oil Cask.
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
			case 243993: // 机械图尔克大炮 / Mechaturerk’s Cannon.
				despawnNpc(npc);
				spawn(833835, 231.14809f, 258.98563f, 191.01645f, (byte) 59); // 机械图尔克大炮 / Mechaturerk's Cannon.
			break;
			case 245759: // 攻城工厂监视者 / Siege Factory Watcher.
				startFactoryTask1();
			break;
			case 243663: // 机械图尔克机械怪物 / Mechaturerk Machine Monster.
				despawnNpc(npc);
				killNpc(getNpcs(833896)); // 工厂大门 / Factory Gate.
				// 破坏魔像的储物箱已出现在军需工厂内。 / The Destruction Golem has appeared!
				sendMsgByRace(1403649, Race.PC_ALL, 0);
				// 机械怪物的储物箱已出现在军需工厂内。 / The Machine Monster’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403645, Race.PC_ALL, 5000);
		        spawn(703380, 138.84042f, 256.166f, 191.8727f, (byte) 0); // 机械怪物的储物箱 / Machine Monster’s Footlocker.
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
					    spawn(243664, 163.01869f, 259.16562f, 192.11992f, (byte) 1); // 机械图尔克 / Mechaturerk.
					}
				}, 5000);
			break;
			case 243664: // 机械图尔克 / Mechaturerk.
				points = 878600;
				// 你击杀了机械图尔克！ / You killed Mechaturerk!
				sendMsgByRace(1403653, Race.PC_ALL, 0);
				// 机械图尔克的特殊宝箱。 / Mechaturerk’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403646, Race.PC_ALL, 5000);
				// 机械图尔克的特殊宝箱。 / Mechaturerk’s Core has appeared inside the Munitions Factory.
				sendMsgByRace(1403647, Race.PC_ALL, 10000);
				// 破坏魔像的储物箱已出现在军需工厂内。 / The Destruction Golem’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403648, Race.PC_ALL, 15000);
				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(834443, 149.65579f, 260.02966f, 191.8727f, (byte) 0); // 机械图尔克的宝物箱 / Mechaturerk’s Treasure Box.
					break;
					case 2:
					    spawn(834444, 149.65579f, 260.02966f, 191.8727f, (byte) 0); // 机械图尔克的特殊宝物箱 / Mechaturerk’s Special Treasure Box.
					break;
				}
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
							    stopInstance1(player);
								stopInstance2(player);
						    }
					    });
					}
				}, 5000);
			break;
			case 243968: // 雷米鲁内克 / Remirunerk.
			    points = 500;
			    // 雷米伦伦克的储物箱已出现在军需工厂内。 / Remirunrunerk’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403643, Race.PC_ALL, 3000);
		        spawn(703378, 138.79507f, 263.1448f, 191.8727f, (byte) 0); // 雷米鲁内克的储物箱 / Remirunrunerk’s Footlocker.
			break;
			case 243969: // 波米鲁内克 / Bomirunrunerk.
			    points = 500;
			    // 博米伦伦克的储物箱已出现在军需工厂内。 / Bomirunrunerk’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403644, Race.PC_ALL, 3000);
		        spawn(703379, 138.76562f, 259.84332f, 191.8727f, (byte) 0); // 波米鲁内克的储物箱 / Bomirunrunerk’s Footlocker.
			break;
			case 244028: // 机械图尔克炮手 / Mechaturerk Gunner.
			    // 炮手的储物箱已出现在军需工厂内。 / The Gunner’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403642, Race.PC_ALL, 3000);
		        spawn(703377, 138.77333f, 266.49652f, 191.8727f, (byte) 0); // 炮手的储物箱 / Gunner’s Footlocker.
			break;
			case 244035: // 受损的机甲步兵 / Damaged Mecha Infantryman.
			    mechaInfantrymanKilled++;
				if (mechaInfantrymanKilled == 2) {
					// 装甲士兵的储物箱已出现在军需工厂内。 / The Armored Soldier’s Footlocker has appeared inside the Munitions Factory.
					sendMsgByRace(1403640, Race.PC_ALL, 3000);
					spawn(703375, 138.73476f, 272.44095f, 191.8727f, (byte) 0); // 装甲士兵的储物箱 / Armored Soldier’s Footlocker.
				}
			break;
			case 244135: // 近战支援破坏魔像 / Melee Support Destruction Golem.
				// 恢复植物已出现。 / The recovery plant has emerged.
				sendMsgByRace(1403824, Race.PC_ALL, 1000);
				spawn(836090, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); // 治疗舱 / Health Pod.
			break;
			case 244136: // 远程支援破坏魔像 / Ranged Support Destruction Golem.
				// 恢复植物已出现。 / The recovery plant has emerged.
				sendMsgByRace(1403824, Race.PC_ALL, 1000);
				spawn(836090, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); // 治疗舱 / Health Pod.
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
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 833833: // 炸弹箱 / Bomb Chest.
			    if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
			    ItemService.addItem(player, 164000418, 1); // 臭气弹 / Stink Bomb.
			break;
			case 836090: // 治疗舱 / Health Pod.
			    despawnNpc(npc);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 50000);
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.MP, 50000);
			break;
			case 243660: // 油桶 / Oil Cask.
			    despawnNpc(npc);
				if (player.getInventory().isFull()) {
					sendMsgByRace(1390149, Race.PC_ALL, 0);
				}
				ItemService.addItem(player, 164002362, 5); // 机械图尔克油桶 / Mechaturerk Oil Cask.
			break;
		}
	}
	
	private void startFactoryRaid1() {
		// 机械图尔克维修士兵。 / Mechaturerk Maintenance Soldier.
		factoryTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(243853, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243853, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 1000);
		// 机械图尔克维修士兵。 / Mechaturerk Maintenance Soldier.
		factoryTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(243853, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243853, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 30000);
		// 机械图尔克维修士兵。 / Mechaturerk Maintenance Soldier.
		factoryTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(243853, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243853, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 60000);
	}
	
	private void startFactoryRaid2() {
		// 近战支援破坏魔像。 / Melee Support Destruction Golem.
		factoryTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(244135, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(244135, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 1000);
		// 远程支援破坏魔像。 / Ranged Support Destruction Golem.
		factoryTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(244136, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(244136, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 30000);
		// 近战+远程支援破坏魔像。 / Melee + Ranged Support Destruction Golem.
		factoryTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(244135, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(244136, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 60000);
	}
	
	private void startFactoryRaid3() {
		// 青色活体炸弹。 / Azure Living Bomb.
		factoryTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 青色活体炸弹已出现！ / The Azure Living bomb has appeared!
				sendMsgByRace(1403650, Race.PC_ALL, 0);
				// 使用蓝色机械装置！ / Use the blue mechanical device!
				sendMsgByRace(1403663, Race.PC_ALL, 5000);
				sp(243661, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243661, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 1000);
		// 金色活体炸弹。 / Golden Living Bomb.
		factoryTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 金色活体炸弹已出现！ / The Golden Living bomb has appeared!
				sendMsgByRace(1403651, Race.PC_ALL, 0);
				// 使用黄色机械装置！ / Use the yellow mechanical device!
				sendMsgByRace(1403663, Race.PC_ALL, 5000);
				sp(243662, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243662, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 30000);
		// 青色活体炸弹。 / Azure Living Bomb.
		factoryTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 青色活体炸弹已出现！ / The Azure Living bomb has appeared!
				sendMsgByRace(1403650, Race.PC_ALL, 0);
				// 使用蓝色机械装置！ / Use the blue mechanical device!
				sendMsgByRace(1403663, Race.PC_ALL, 5000);
				sp(243661, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243661, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 60000);
		// 金色活体炸弹。 / Golden Living Bomb.
		factoryTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 金色活体炸弹已出现！ / The Golden Living bomb has appeared!
				sendMsgByRace(1403651, Race.PC_ALL, 0);
				// 使用黄色机械装置！ / Use the yellow mechanical device!
				sendMsgByRace(1403663, Race.PC_ALL, 5000);
				sp(243662, 133.37782f, 229.28152f, 191.94075f, (byte) 15, 1000, "MunitionFactory1");
				sp(243662, 132.91176f, 289.63672f, 191.98668f, (byte) 106, 2000, "MunitionFactory2");
			}
		}, 90000);
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
		if (totalPoints >= 878600) { // S 级 / Rank S.
			rank = 1;
		} else {
			rank = 6;
		}
		return rank;
	}
	
   /**
	 * 副本实例。 / Raid Instance
	 */
	protected void startFactoryTask1() {
		factoryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startFactoryRaid1();
				//sendMsg("[START]: Wave <1/3>");
            }
        }, 120000)); //...2 分钟 / ...2Min
		factoryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startFactoryRaid2();
				factoryTaskA1.cancel(true);
				//sendMsg("[START]: Wave <2/3>");
				// 维修士兵的储物箱已出现在军需工厂内。 / The Maintenance Soldier’s Footlocker has appeared inside the Munitions Factory.
				sendMsgByRace(1403641, Race.PC_ALL, 3000);
				spawn(703376, 138.75412f, 269.4629f, 191.8727f, (byte) 0); // 维护士兵的储物箱 / Maintenance Soldier’s Footlocker.
            }
        }, 240000)); //...4 分钟 / ...4Min
		factoryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startFactoryRaid3();
				factoryTaskA2.cancel(true);
				//sendMsg("[START]: Wave <3/3>");
            }
        }, 360000)); //...6 分钟 / ...6Min
		factoryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
						stopInstance1(player);
						factoryTaskA3.cancel(true);
				    }
			    });
            }
        }, 480000)); //...8 分钟 / ...8Min
	}
	
   /**
	 * 副本计时器 / Instance Timer
	 */
	protected void startFactoryTask2() {
		factoryTask2.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
					    stopInstance2(player);
				    }
			    });
            }
        }, 3600000)); // 1 小时 / 1 Hour.
    }
	
	/**
	 * 玩家打开门时处理。
	 * Handle a player opening a door.
	 *
	 * 玩家 / player
	 * @param doorId 门 id / doorId
	 */
	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId == 27) {
			startFactoryTask2();
			doors.get(27).setOpen(true);
			killNpc(getNpcs(833868)); // 岩石堆 / Rock Pile.
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
		SecretMunitionsFactoryPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		startPrepareTimer();
		//spawnLunaDetachment();
		final int lunaDetachement = skillRace == Race.ASMODIANS ? 21348 : 21347;
		GameEngineServices.skillEngine().applyEffectDirectly(lunaDetachement, player, player, 3000000 * 1);
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
		spawnLunaDetachment();
		sendPacket(0, 0);
	}
	
	private void spawnLunaDetachment() {
		sp(833829, 382.25574f, 283.81686f, 198.50284f, (byte) 7, 5, "NPCPathAlly_NPC_Path2");// 赫雷兹 / Herez.
		sp(833827, 386.10965f, 282.91656f, 198.24266f, (byte) 11, 5, "NPCPathAlly_NPC_Path3");// 马克 / Mak.
		sp(833897, 388.17896f, 279.8141f, 197.98882f, (byte) 14, 5, "NPCPathAlly_NPC_Path4");// 乔尔 / Joel.
		sp(833826, 385.30814f, 286.88065f, 198.56099f, (byte) 6, 5, "NPCPathAlly_NPC_Path5");// 罗克西 / Roxy.
		sp(833828, 386.33496f, 290.52594f, 198.5f, (byte) 115, 5, "NPCPathAlly_NPC_Path6");// 马纳德 / Manad.
	}
	/**
	 * 处理 stopInstance1。
	 * Handle stopInstance1.
	 *
	 * @param player 玩家 / player
	 */
	
	protected void stopInstance1(Player player) {
		stopInstanceTask1();
		// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You survived !!! :) ");
	}
	/**
	 * 处理 stopInstance2。
	 * Handle stopInstance2.
	 *
	 * @param player 玩家 / player
	 */
	
	protected void stopInstance2(Player player) {
        stopInstanceTask2();
		instanceReward.setRank(6);
		instanceReward.setRank(checkRank(instanceReward.getPoints()));
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward(player);
		// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Secret Munitions Factory>");
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
		SecretMunitionsFactoryPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int factoryRank = instanceReward.getRank();
			switch (factoryRank) {
				case 1: // S 级 / Rank S
					playerReward.setMechaturerkSecretBox(1);
					// 机械图尔克的特殊宝箱。 / Mechaturerk's Secret Box.
					ItemService.addItem(player, 188055475, 1);
				break;
				case 2: // A 级 / Rank A
				    playerReward.setMechaturerkNormalTreasureChest(1);
					// 机械图尔克的特殊宝箱。 / Mechaturerk’s Normal Treasure Chest.
					ItemService.addItem(player, 188055647, 1);
				break;
				case 3: // B 级 / Rank B
				    playerReward.setMechaturerkSpecialTreasureBox(1);
					// 机械图尔克的特殊宝箱。 / Mechaturerk’s Special Treasure Box.
					ItemService.addItem(player, 188055648, 1);
				break;
				case 4: // C 级 / Rank C
				    playerReward.setMechaturerkSpecialTreasureBox(1);
					// 机械图尔克的特殊宝箱。 / Mechaturerk’s Special Treasure Box.
					ItemService.addItem(player, 188055648, 1);
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
		instanceReward = new SecretMunitionsFactoryReward(mapId, instanceId);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
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
		stopInstanceTask1();
		stopInstanceTask2();
		isInstanceDestroyed = true;
		instanceReward.clear();
		doors.clear();
	}
	
	private void stopInstanceTask1() {
		for (Future<?> task : factoryTask1) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	private void stopInstanceTask2() {
		for (Future<?> task : factoryTask2) {
			if (task != null) {
				task.cancel(true);
			}
		}
	}
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * @param npcId NPC id / NPC id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * @param time 时间 / time
	 * @param walkerId 路径 id / walkerId
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        factoryTask1.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
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
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	/**
	 * 处理 killNpc。
	 * Handle killNpc.
	 *
	 * @param npcs NPC 列表 / npcs
	 */
	
	protected void killNpc(List<Npc> npcs) {
        for (Npc npc: npcs) {
            npc.getController().die();
        }
    }
	/**
	 * 返回 npcs。
	 * Return the npcs.
	 *
	 * @param npcId NPC id / NPC id
	 * @return NPC 列表 / result
	 */
	
	protected List<Npc> getNpcs(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpcs(npcId);
		}
		return null;
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
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		effectController.removeEffect(21347);
		effectController.removeEffect(21348);
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
	 * @param message 消息 / message
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
