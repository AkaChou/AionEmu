package com.aionemu.gameserver.instance.handlers.scripts.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.IDEventDefReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.IDEventDefPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.item.ItemService;
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
 * 活动·污染暗道副本事件处理器。
 * Instance event handler for Event Contaminated Underpath.
 *
 * @author Encom
 */

@InstanceID(301631000)
public class Event_ContaminatedUnderpathInstance extends GeneralInstanceHandler
{
	/** 开始时间 / start time */
	private long startTime;
	/** 技能种族 / skill race */
		private Race skillRace;
	/** 准备计时器 / timer prepare */
		private Future<?> timerPrepare;
	/** 副本计时器 / timer instance */
		private Future<?> timerInstance;
	/** underpath 任务 A1 / underpath task a1 */
		private Future<?> underpathTaskA1;
	/** underpath 任务 A2 / underpath task a2 */
		private Future<?> underpathTaskA2;
	/** underpath 任务 A3 / underpath task a3 */
		private Future<?> underpathTaskA3;
	/** underpath 任务 A4 / underpath task a4 */
		private Future<?> underpathTaskA4;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** idevent def zombie vampire1 / idevent def zombie vampire1 */
		private int IDEventDefZombieVampire1;
	/** idevent def zombie vampire2 / idevent def zombie vampire2 */
		private int IDEventDefZombieVampire2;
	/** idevent def zombie vampire3 / idevent def zombie vampire3 */
		private int IDEventDefZombieVampire3;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	// 准备时间。 / Preparation Time.
	/** 准备计时秒数 / prepare timer seconds */
		private int prepareTimerSeconds = 60000; //…1 分钟 / ...1Min
	// 副本持续计时。 / Duration Instance Time.
	/** 副本计时秒数 / instance timer seconds */
		private int instanceTimerSeconds = 1200000; //...20Min
	/** 副本奖励对象 / instance reward object */
	private IDEventDefReward instanceReward;
	/** ideventdef 任务 / idevent def task */
		private final List<Future<?>> IDEventDefTask = new ArrayList<Future<?>>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * visible object
	 * result
	 */
	
	protected IDEventDefPlayerReward getPlayerReward(Integer object) {
		return (IDEventDefPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	/**
	 * 处理 addPlayerReward。
	 * Handle addPlayerReward.
	 *
	 * @param player 玩家 / player
	 */
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new IDEventDefPlayerReward(player.getObjectId()));
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
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(186000470, storage.getItemCountByItemId(186000470)); //战争点数。 / War Points.
		storage.decreaseByItemId(186000495, storage.getItemCountByItemId(186000495)); //Key.
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
		switch (npc.getNpcId()) {
		    case 703473: //IDEVENT_SWSwitch_01a.
				despawnNpc(npc);
				spawnPrototype();
				startContaminedUnderPath1();
				sendMsg("[START]: Wave <1/4>");
				// 1     ?.
				sendMsgByRace(1404504, Race.PC_ALL, 0);
			break;
			case 703474: //IDEVENT_SWSwitch_02a.
			    despawnNpc(npc);
			    startContaminedUnderPath2();
				underpathTaskA1.cancel(true);
				sendMsg("[START]: Wave <2/4>");
				// 2     ?.
				sendMsgByRace(1404505, Race.PC_ALL, 0);
			break;
			case 703475: //IDEVENT_SWSwitch_03a.
			    despawnNpc(npc);
			    startContaminedUnderPath3();
				underpathTaskA2.cancel(true);
				sendMsg("[START]: Wave <3/4>");
				// 3     ?.
				sendMsgByRace(1404506, Race.PC_ALL, 0);
			break;
		   /**
	 * 5. 击杀最终 Boss：共有多级控制单元。 / 5. Kill The Final Boss Monster: A total of "4 levels of control units" appear, and monsters of different characteristics come in each stage. Be careful that the character dies when entering the contaminated floor. Step 4 After you click on the controller, kill the dead boss monster, the body resuscitator Voodoo, and the attack will be completed
	 */
			case 703476: //IDEVENT_SWSwitch_04a.
			    despawnNpc(npc);
			    startContaminedUnderPath4();
				underpathTaskA3.cancel(true);
				sendMsg("[START]: Wave <4/4>");
				//？？？ III / ?? ? III
				sendMsgByRace(1404507, Race.PC_ALL, 0);
			break;
			case 836149: //IDEvent_Def_In_Door.
				if (player.getInventory().decreaseByItemId(186000495, 1)) {
					killNpc(getNpcs(836149));
			    } else {
					//?    .
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404524));
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
		int points = 0;
		int npcId = npc.getNpcId();
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 248502: //IDEvent_Def_ZombieVampireD_65_1st.
			    points = 600;
			    IDEventDefZombieVampire1++;
				if (IDEventDefZombieVampire1 == 4) {
					deleteNpc(703473);
					underpathTaskA1.cancel(true);
					sendMsg("[START]: Wave 1 End");
					//2    .
					sendMsgByRace(1404508, Race.PC_ALL, 0);
					spawn(703474, 230.04181f, 206.23842f, 160.28148f, (byte) 30);
				}
			break;
			case 248512: //IDEvent_Def_ZombieVampireD_65_2nd.
			    points = 600;
			    IDEventDefZombieVampire2++;
				if (IDEventDefZombieVampire2 == 4) {
					deleteNpc(703474);
					underpathTaskA2.cancel(true);
					sendMsg("[START]: Wave 2 End");
					//3    .
					sendMsgByRace(1404509, Race.PC_ALL, 0);
					spawn(703475, 230.04181f, 206.23842f, 160.28148f, (byte) 30);
				}
			break;
			case 248522: //IDEvent_Def_ZombieVampireD_65_3rd.
			    points = 600;
			    IDEventDefZombieVampire3++;
				if (IDEventDefZombieVampire3 == 4) {
					deleteNpc(703475);
					underpathTaskA3.cancel(true);
					sendMsg("[START]: Wave 3 End");
					//4    .
					sendMsgByRace(1404510, Race.PC_ALL, 0);
					spawn(703476, 230.04181f, 206.23842f, 160.28148f, (byte) 30);
				}
			break;
			case 248495:
			case 248496:
			case 248497:
			case 248498:
			case 248499:
			case 248500:
			case 248501:
			case 248503:
			case 248504:
			case 248505:
			case 248506:
			case 248507:
			case 248508:
			case 248509:
			case 248510:
			case 248511:
			case 248513:
			case 248514:
			case 248515:
			case 248516:
			case 248517:
			case 248518:
			case 248519:
			case 248520:
			case 248521:
			case 248523:
			case 248524:
			    points = 600;
			break;
			case 248923: //IDEvent_Def_MutantBeast_65.
			    ItemService.addItem(player, 186000470, 50); //战争点数。 / War Points.
			break;
			case 248525:
				points = 500000;
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
								underpathTaskA4.cancel(true);
						    }
					    });
					}
				}, 5000);
			break;
			case 246352:
			    player.getCommonData().addExp(50000, RewardType.QUEST);
			break;
		} if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		}
	}
	
	private void startContaminedUnderPath1() {
		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248495, 222.78767f, 276.12140f, 160.4131f, (byte) 89, 1000, "IDEVENT_Def_1");
				sp(248496, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248497, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248498, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248499, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248500, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 1000);
		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248501, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248502, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248503, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248504, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248495, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248496, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 30000);
		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248501, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248502, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248503, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248504, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248495, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248496, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 60000);
		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248497, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248498, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248499, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248500, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248501, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248502, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 90000);
		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248497, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248498, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248499, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248500, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248501, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248502, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 120000);
	}
	
	private void startContaminedUnderPath2() {
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248505, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248506, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248507, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248508, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248509, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248510, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 1000);
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248511, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248512, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248513, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248514, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248505, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248506, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 30000);
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248511, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248512, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248513, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248514, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248505, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248506, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 60000);
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248507, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248508, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248509, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248510, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248511, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248512, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 90000);
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248507, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248508, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248509, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248510, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248511, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248512, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 120000);
	}
	
	private void startContaminedUnderPath3() {
		underpathTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248515, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248516, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248517, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248518, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248519, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248520, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 1000);
		underpathTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248521, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248522, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248523, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248524, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248515, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248516, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 30000);
		underpathTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248521, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248522, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248523, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248524, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248515, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248516, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 60000);
		underpathTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248517, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248518, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248519, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248520, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248521, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248522, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 90000);
		underpathTaskA3 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248517, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "IDEVENT_Def_1");
				sp(248518, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "IDEVENT_Def_1");
				sp(248519, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "IDEVENT_Def_1");
				sp(248520, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "IDEVENT_Def_1");
				sp(248521, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "IDEVENT_Def_1");
				sp(248522, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "IDEVENT_Def_1");
			}
		}, 120000);
	}
	
	private void startContaminedUnderPath4() {
		underpathTaskA4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(248525, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 1000, "IDEVENT_Def_1");
			}
		}, 1000);
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
	 * 3. 安装炮塔：可在指定位置安装各类炮塔。 / 3. Installing The Turret: You can install various types of turrets on an empty turret, or upgrade the installed turret using the "Aura Of Patience". The turrets become increasingly stronger turrets as you upgrade
	 */
	private void spawnPrototype() {
		sp(836050, 235.97508f, 215.58057f, 160.34032f, (byte) 30, 2000, 0, null);
		sp(836050, 231.52914f, 215.41585f, 160.28148f, (byte) 30, 2500, 0, null);
		sp(836050, 227.33563f, 215.30028f, 160.28148f, (byte) 30, 3000, 0, null);
		sp(836050, 223.48090f, 215.20303f, 160.28148f, (byte) 30, 3500, 0, null);
		sp(836050, 223.44165f, 226.83798f, 160.28148f, (byte) 31, 4000, 0, null);
		sp(836050, 227.34960f, 226.85701f, 160.28148f, (byte) 30, 4500, 0, null);
		sp(836050, 231.63043f, 226.90276f, 160.28148f, (byte) 30, 5000, 0, null);
		sp(836050, 236.04927f, 226.95053f, 160.28148f, (byte) 30, 5500, 0, null);
		sp(836050, 232.67305f, 236.96098f, 160.28148f, (byte) 30, 6000, 0, null);
		sp(836050, 226.52002f, 236.88406f, 160.28148f, (byte) 30, 6500, 0, null);
		sp(836050, 223.52928f, 247.17563f, 159.90181f, (byte) 30, 7000, 0, null);
		sp(836050, 227.39925f, 247.25684f, 159.90181f, (byte) 30, 7500, 0, null);
		sp(836050, 231.61705f, 247.26353f, 159.90181f, (byte) 22, 8000, 0, null);
		sp(836050, 236.08197f, 247.40280f, 159.90181f, (byte) 30, 8500, 0, null);
	}
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
	protected void startInstanceTask() {
		IDEventDefTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
						deleteNpc(248525);
					    stopInstance(player);
				    }
			    });
            }
        }, 1200000)); //...20Min
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
		if (doorId == 57) {
			startInstanceTask();
			doors.get(57).setOpen(true);
			// 片刻后第一通道将强制开放。请准备。 / After a while, the first passage is forcibly released. Please prepare.
			sendMsgByRace(1404511, Race.PC_ALL, 0);
			// 使用空���塔召唤炮塔。 / Summon the turret using an empty turret.
			sendMsgByRace(1404528, Race.PC_ALL, 5000);
			// 使用强力生命的守护者石像可获得强大力量。 / You can get strong strength by using Deva stone statue of powerful life.
			sendMsgByRace(1404530, Race.PC_ALL, 10000);
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
		IDEventDefPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		startPrepareTimer();
	   /**
	 * 2. 开始战斗：点击入口后战斗开始。 / 2. Start Combat: When you click on the entrance, the battle begins, and after a certain time, the monster starts to gather. If you kill a monster, you can acquire a 'Guardian energy', which can be used to build a turret or to strengthen your skills. Tip 1. Let's use 'prison keys' to get items faster. After the battle begins, you can use your key to open the prison door between the stairs to kill the contaminated Dog. It can help you to shorten your attack time because you can acquire 50 points of 'Guardian Power' when you deal with the contaminated Dog. The 'prison key' will be paid through three surveys. Additional purchases can be made through a dedicated store if necessary. Tip 2. Let's get rid of the gangs! Gold stems often appear inside the interior. At the time of the treatment, you can acquire the 'Power of Suho' at random, so let's do not miss it. 4. Enhance Your Skills: If you click on the stone statue located on the entrance side, you can strengthen your ability by using the power of guardian
	 */
		ItemService.addItem(player, 186000495, 1); //?  (Open Door Prison)
	   /**
	 * 1. 变身：进入污染地下通道时自动变为传送形态。 / 1. Transformation: When entering the contaminated underground passage, it automatically transforms into a form of transfer. Basically, you can use the 'Berta' skill, you cannot use your skills
	 */
		final int IDEventDef = skillRace == Race.ASMODIANS ? 4940 : 4935;
		GameEngineServices.skillEngine().applyEffectDirectly(IDEventDef, player, player, 1200000 * 1);
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
		// sendMsg("[成功]：你活下来了！！！"); / sendMsg("[SUCCES]: You survived !!! :) ");
		sendPacket(0, 0);
	}
	
   /**
	 * 主要补偿/奖励信息：开启 S 级与 A 级宝箱等。 / Major compensation information Major rewards open the "S Rank" treasure box and "A Rank" treasure box, and the following item comes out
	 */
	@Override
	public void doReward(Player player) {
		IDEventDefPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			int IDEventDefRank = instanceReward.getRank();
			RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, IDEventDefRank);
			playerReward.setScoreAP(plan.ap());
			playerReward.setWrapCashIDEventDefLiveSRank(Math.toIntExact(plan.itemCount(188058265)));
			playerReward.setWrapCashIDEventDefLiveARank(Math.toIntExact(plan.itemCount(188058266)));
			playerReward.setWrapCashIDEventDefLiveBRank(Math.toIntExact(plan.itemCount(188058267)));
			InstanceSettlementService.settleTimeAttack(instance, player, IDEventDefRank);
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
		instanceReward = new IDEventDefReward(mapId, instanceId);
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
		stopInstanceTask();
		isInstanceDestroyed = true;
		instanceReward.clear();
		doors.clear();
	}
	
	private void stopInstanceTask() {
        for (Future<?> task : IDEventDefTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * entity id
	 * time
	 * message
	 * 阵营 / race
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        IDEventDefTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * walkerId
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        IDEventDefTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	 * npc
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
	 * npcs
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
	 * NPC
	 * result
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
		effectController.removeEffect(4935);
		effectController.removeEffect(4936);
		effectController.removeEffect(4937);
		effectController.removeEffect(4938);
		effectController.removeEffect(4939);
		effectController.removeEffect(4940);
		effectController.removeEffect(4941);
		effectController.removeEffect(4942);
		effectController.removeEffect(4943);
		effectController.removeEffect(4944);
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
