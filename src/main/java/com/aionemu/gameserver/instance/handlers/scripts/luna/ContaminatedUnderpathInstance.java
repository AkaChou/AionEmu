package com.aionemu.gameserver.instance.handlers.scripts.luna;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

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
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.ContaminatedUnderpathReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.ContaminatedUnderpathPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
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
 * 污染暗道副本事件处理器。
 * Instance event handler for Contaminated Underpath.
 *
 * @author Rinzler (Encom
 * @author MATTY (ADev Team
 */

@InstanceID(301630000)
public class ContaminatedUnderpathInstance extends GeneralInstanceHandler
{
	/** 排名 / rank */
	private int rank;
	/** 开始时间 / start time */
	private long startTime;
	/** 技能种族 / skill race */
		private Race skillRace;
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
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
	/** underpath 任务 A5 / underpath task a5 */
		private Future<?> underpathTaskA5;
	/** underpath 任务 A6 / underpath task a6 */
		private Future<?> underpathTaskA6;
	/** underpath 任务 A7 / underpath task a7 */
		private Future<?> underpathTaskA7;
	/** underpath 任务 A8 / underpath task a8 */
		private Future<?> underpathTaskA8;
	/** underpath 任务 A9 / underpath task a9 */
		private Future<?> underpathTaskA9;
	/** underpath 任务 A10 / underpath task a10 */
		private Future<?> underpathTaskA10;
	/** underpath 任务 a11 / underpath task a11 */
		private Future<?> underpathTaskA11;
	/** underpath 任务 a12 / underpath task a12 */
		private Future<?> underpathTaskA12;
	/** underpath 任务 a13 / underpath task a13 */
		private Future<?> underpathTaskA13;
	/** underpath 任务 a14 / underpath task a14 */
		private Future<?> underpathTaskA14;
	/** underpath 任务 a15 / underpath task a15 */
		private Future<?> underpathTaskA15;
	/** underpath 任务 a16 / underpath task a16 */
		private Future<?> underpathTaskA16;
	/** underpath 任务 a17 / underpath task a17 */
		private Future<?> underpathTaskA17;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 准备计时秒数 / prepare timer seconds */
		private int prepareTimerSeconds = 180000; // Красное время. Ожидание начала данжа 3 минуты.
	/** 副本计时秒数 / instance timer seconds */
		private int instanceTimerSeconds = 3600000; // Длительность данжа. 1 час.
	/** 副本奖励对象 / instance reward object */
	private ContaminatedUnderpathReward instanceReward;
	/** contamined 任务 / contamined task */
		private final List<Future<?>> contaminedTask = new ArrayList<Future<?>>();
	
	protected ContaminatedUnderpathPlayerReward getPlayerReward(Integer object) {
		return (ContaminatedUnderpathPlayerReward) instanceReward.getPlayerReward(object);
	}
	
	@SuppressWarnings("unchecked")
	protected void addPlayerReward(Player player) {
		instanceReward.addPlayerReward(new ContaminatedUnderpathPlayerReward(player.getObjectId()));
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
		switch (npcId) {
			case 703384: //Infected Bone Mound.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182216109, 1)); //Maad-S Molar.
			break;
			case 703385: //Infected Flesh Lump.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182216110, 1)); //Maad-S Skin Tissue.
			break;
			case 833866: //Unstable Aether Energy.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182007405, 3)); //Bright Aether.
			break;
			case 834253: //Maedrunerk Legion Treasures.
			break;
		}
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(182007405, storage.getItemCountByItemId(182007405)); //Bright Aether.
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
		    case 833812: //Flame Switch.
			    despawnNpc(npc);
				spawn(833813, 232.31128f, 239.1524f, 160.36285f, (byte) 90); //Contaminated Underpath Fire.
                spawn(833813, 225.83708f, 239.09781f, 160.36285f, (byte) 90); //Contaminated Underpath Fire.
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
			case 243647: //MAD-74C.
			    points = 50;
			break;
			case 245543: //Experimental Daeva M.
			case 245544: //Experimental Daeva F.
			case 245545: //Transfectant Crawler.
			case 245549: //Cursed Brave Warrior.
			case 245550: //Crazy Priest.
			case 245551: //Infected Zaif.
			case 245552: //Transfectant Flyer.
			case 245553: //Transfectant Flyer.
			case 245554: //Infected Saam King.
			case 245555: //Transfectant Crawler.
			case 245559: //Cursed Brave Warrior.
			case 245560: //Crazy Priest.
			case 245561: //Infected Zaif.
			case 245562: //Transfectant Flyer.
			case 245563: //Transfectant Flyer.
			case 245564: //Infected Saam King.
			case 245565: //Transfectant Crawler.
			case 245569: //Cursed Brave Warrior.
			case 245570: //Crazy Priest.
			case 246175: //Aetherflame.
			    points = 150;
			break;
			case 245547: //Experimental Reian.
			case 245548: //Experimental Reian.
			case 245557: //Experimental Reian.
			case 245558: //Experimental Reian.
			case 245567: //Experimental Reian.
			case 245568: //Experimental Reian.
			    points = 600;
			break;
			case 245546: //Gringol The Devourer.
			    points = 2500;
			break;
			case 245575: //MAAD-S.
				points = 540000;
				
				spawn(703384, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); //Infected Bone Mound.
				spawn(703385, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()); //Infected Flesh Lump.
				
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
								underpathTaskA11.cancel(true);
						    }
					    });
					}
				}, 5000);
			break;
			case 246352: //MAD-99S' Core.
			    // 击杀“核心”给予玩家 50000 经验。 / Give 50.000 Exp to player by "Core" kill.
			    player.getCommonData().addExp(50000, RewardType.QUEST);
			break;
		} if (instanceReward.getInstanceScoreType().isStartProgress()) {
			instanceReward.addNpcKill();
			instanceReward.addPoints(points);
			sendPacket(npc.getObjectTemplate().getNameId(), points);
		} switch (npcId) {
			case 833863: //Frontal Barricade.
			    // 前方路障被摧毁。 / The barricade in the front was destroyed.
				sendMsgByRace(1403609, Race.PC_ALL, 0);
			    instanceReward.addPoints(-500);
			break;
			case 833864: //Rear Barricade.
			    // 后方路障被摧毁。阿特雷亚危在旦夕。 / The barricade in the back was destroyed. Atreia is in danger.
				sendMsgByRace(1403610, Race.PC_ALL, 0);
			    instanceReward.addPoints(-25000);
			break;
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
				sp(245549, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245549, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245549, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
			}
		}, 1000);

		underpathTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245549, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath1");
				sp(245549, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath2");
				sp(245549, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath3");
			}
		}, 30000);
	}
	private void startContaminedUnderPath2() {
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);
		underpathTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath3() {
		underpathTaskA5 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(243647, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 1000, "ContaminedUnderpath4");
			}
		}, 1000);
	}
	
	private void startContaminedUnderPath4() {
		underpathTaskA4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245546, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1000, "ContaminedUnderpath2");
				sp(245546, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath4");
				sp(245546, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath6");
			}
		}, 1000);
		underpathTaskA4 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245546, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1000, "ContaminedUnderpath2");
				sp(245546, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath4");
				sp(245546, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath6");
			}
		}, 30000);
	}
	private void startContaminedUnderPath5() {
		underpathTaskA5 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath13");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath14");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath15");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath16");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath17");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath18");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath19");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath20");
			}
		}, 1000);
	}
	private void startContaminedUnderPath6() {

		underpathTaskA6 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245552, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath10");
				sp(245552, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath11");
				sp(245552, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath12");
				sp(245552, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath13");
				sp(245552, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath14");
				sp(245552, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath15");
				sp(245553, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath16");
				sp(245553, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath17");
				sp(245553, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath18");
				sp(245553, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath19");
				sp(245553, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath20");
			}
		}, 1000);
	}
	private void startContaminedUnderPath7() {

		underpathTaskA7 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath13");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath14");
				sp(245555, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath15");
				sp(245555, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath16");
				sp(245555, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath17");
				sp(245555, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath18");
				sp(245555, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath19");
				sp(245555, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath20");
			}
		}, 1000);
		underpathTaskA7 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath8() {
		underpathTaskA8 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(243647, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 1000, "ContaminedUnderpath4");
			}
		}, 1000);
		underpathTaskA8 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
		underpathTaskA8 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245553, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245553, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245553, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245553, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245553, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245553, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
			}
		}, 30000);
	}
	private void startContaminedUnderPath9() {

		underpathTaskA9 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA9 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath10() {

		underpathTaskA10 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA10 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath11() {

		underpathTaskA11 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245575, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 1000, "ContaminedUnderpath4");
			}
		}, 1000);
	}
	private void startContaminedUnderPath12() {

		underpathTaskA12 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA12 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath13() {

		underpathTaskA13 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA13 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath14() {

		underpathTaskA14 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA14 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath15() {

		underpathTaskA15 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA15 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath16() {

		underpathTaskA16 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA16 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
	}
	private void startContaminedUnderPath17() {

		underpathTaskA17 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 1000);

		underpathTaskA17 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(245543, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath1");
				sp(245543, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath2");
				sp(245543, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath3");
				sp(245543, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath4");
				sp(245543, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath5");
				sp(245543, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath6");
				sp(245544, 222.78767f, 276.12140f, 160.4131f, (byte) 90, 1000, "ContaminedUnderpath7");
				sp(245544, 225.05133f, 275.86157f, 160.3114f, (byte) 89, 1500, "ContaminedUnderpath8");
				sp(245544, 227.54712f, 275.85287f, 160.3114f, (byte) 89, 2000, "ContaminedUnderpath9");
				sp(245544, 229.59123f, 275.84586f, 160.3114f, (byte) 89, 2500, "ContaminedUnderpath10");
				sp(245544, 232.00526f, 275.83752f, 160.3114f, (byte) 89, 3000, "ContaminedUnderpath11");
				sp(245544, 234.10661f, 275.83023f, 160.3114f, (byte) 89, 3500, "ContaminedUnderpath12");
			}
		}, 30000);
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
		if (totalPoints >= 549000) { //Rank S.
			rank = 1;
		} else if (totalPoints >= 544000) { //Rank A.
			rank = 2;
		} else if (totalPoints >= 50) { //Rank B.
			rank = 3;
		} else if (totalPoints >= 50) { //Rank C.
			rank = 4;
		} else if (totalPoints >= 50) { //Rank D.
			rank = 5;
		} else {
			rank = 6;
		}
		return rank;
	}
	
	protected void startInstanceTask() {
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
					startMainInstanceTimer();
				}
				startContaminedUnderPath1();
				sendMsgByRace(1403628, Race.PC_ALL, 0);
            }
			
        }, 30000)); // 30 Секунд после старта времени первая волна
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath2();
				underpathTaskA1.cancel(true);
				sendMsgByRace(1403657, Race.PC_ALL, 0);
            }
        }, 60000)); // Через 1 минуту вторая волна
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath3();
				underpathTaskA2.cancel(true);
				sendMsgByRace(1403655, Race.PC_ALL, 0);
            }
        }, 90000)); // 1.5 минуты после старта
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath4();
				underpathTaskA3.cancel(true);
				sendMsgByRace(1403662, Race.PC_ALL, 0);
            }
        }, 120000)); // 2 минуты после старта
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath5();
				underpathTaskA4.cancel(true);
				sendMsgByRace(1403656, Race.PC_ALL, 0);
            }
        }, 150000)); // 2.5 минуты
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath6();
				underpathTaskA5.cancel(true);
				sendMsgByRace(1403661, Race.PC_ALL, 0);
            }
        }, 180000)); // 3 минуты
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath7();
				underpathTaskA6.cancel(true);
				sendMsgByRace(1403659, Race.PC_ALL, 0);
            }
        }, 210000)); //...3.5
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath8();
				underpathTaskA7.cancel(true);
				sendMsgByRace(1403660, Race.PC_ALL, 0);
            }
        }, 240000)); //4 минуты
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath9();
				underpathTaskA8.cancel(true);
				sendMsgByRace(1403657, Race.PC_ALL, 0);
            }
        }, 300000)); // 5 минута
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath10();
				underpathTaskA9.cancel(true);
				sendMsgByRace(1403655, Race.PC_ALL, 0);
            }
        }, 330000)); //...5.5
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath11();
				underpathTaskA10.cancel(true);
				//Волна с РБ
				sendMsgByRace(1403658, Race.PC_ALL, 0);
            }
        }, 360000)); // 6 минут
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath12();
				underpathTaskA11.cancel(true);
				sendMsgByRace(1403659, Race.PC_ALL, 0);
            }
        }, 420000)); //...7 мин
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath13();
				underpathTaskA12.cancel(true);
				sendMsgByRace(1403660, Race.PC_ALL, 0);
            }
        }, 480000)); //...8
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath14();
				underpathTaskA13.cancel(true);
				sendMsgByRace(1403661, Race.PC_ALL, 0);
            }
        }, 540000)); //...9
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath15();
				underpathTaskA14.cancel(true);
				sendMsgByRace(1403662, Race.PC_ALL, 0);
            }
        }, 600000)); //...31Min
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath16();
				underpathTaskA15.cancel(true);
				sendMsgByRace(1403655, Race.PC_ALL, 0);
            }
        }, 660000)); //...11 мин
		contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				startContaminedUnderPath17();
				underpathTaskA16.cancel(true);
				sendMsgByRace(1403656, Race.PC_ALL, 0);
            }
        }, 720000)); //... 12 Минут
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
		if (doorId == 28) {
			startInstanceTask();
			doors.get(28).setOpen(true);
			sendMsgByRace(1403696, Race.PC_ALL, 0);
			// Остановка таймера при преждивременном открытии двери
			if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
				//	Запуск основного таймера инстанса
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
		ContaminatedUnderpathPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			doReward(player);
		}
		startPrepareTimer();
		
		final int lunaDetachement = skillRace == Race.ASMODIANS ? 21346 : 21345;
		//if (player.getRace().equals(Race.ASMODIANS)) {
		//	GameEngineServices.skillEngine().applyEffectDirectly(21346, player, player, 0);
		//} else {
		//	GameEngineServices.skillEngine().applyEffectDirectly(21345, player, player, 0);
		//}
		GameEngineServices.skillEngine().applyEffectDirectly(lunaDetachement, player, player, 3000000 * 1);
		//final int lunaHeal = spawnRace == Race.ASMODIANS ? 703477 : 703477;
		final int lunaHeal = spawnRace == Race.ASMODIANS ? 834491 : 834491;
		
		spawn(lunaHeal, 236.77367f, 212.99107f, 160.28148f, (byte) 60);
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
	
	protected void stopInstance(Player player) {
		stopInstanceTask();
        instanceReward.setRank(6);
		instanceReward.setRank(checkRank(instanceReward.getPoints()));
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward(player);
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
		ContaminatedUnderpathPlayerReward playerReward = getPlayerReward(player.getObjectId());
		if (!playerReward.isRewarded()) {
			playerReward.setRewarded();
			int contaminatedRank = instanceReward.getRank();
			switch (contaminatedRank) {
				case 1: //Rank S
					playerReward.setContaminatedPremiumRewardBundle(1);
					// 污染高级奖励包。 / Contaminated Premium Reward Bundle.
					ItemService.addItem(player, 188055598, 1);
				break;
				case 2: //Rank A
				    playerReward.setContaminatedHighestRewardBundle(1);
					// 污染最高奖励包。 / Contaminated Highest Reward Bundle.
					ItemService.addItem(player, 188055599, 1);
				break;
				case 3: //Rank B
				    playerReward.setContaminatedUnderpathSpecialPouch(1);
					// 污染地下通道特殊袋。 / Contaminated Underpath Special Pouch.
					ItemService.addItem(player, 188055664, 1);
				break;
				case 4: //Rank C
				    playerReward.setContaminatedUnderpathSpecialPouch(1);
					// 污染地下通道特殊袋。 / Contaminated Underpath Special Pouch.
					ItemService.addItem(player, 188055664, 1);
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
		instanceReward = new ContaminatedUnderpathReward(mapId, instanceId);
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
        for (Future<?> task : contaminedTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        contaminedTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
		effectController.removeEffect(21345);
		effectController.removeEffect(21346);
		effectController.removeEffect(22741);
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