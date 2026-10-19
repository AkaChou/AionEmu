package com.aionemu.gameserver.instance.handlers.scripts;



import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.TransformEffect;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 术古皇陵副本事件与场景任务的所有者；调度、执行和销毁清理在处理器监视器内串行化。
 * Owns Shugo Imperial Tomb events and scene tasks; scheduling, execution and teardown share the handler monitor.
 * @author Encom
 */

@InstanceID(300560000)
public class ShugoImperialTombInstance extends GeneralInstanceHandler {
	/** 术古变身面板 / Shugo transformation panels. */
	private static final Set<Integer> SHUGO_TRANSFORM_PANELS = Set.of(25, 26, 27);
	/** 光族术古变身技能 / Elyos Shugo transformation skills. */
	private static final Set<Integer> LIGHT_TRANSFORM_SKILLS = Set.of(21094, 21095, 21096);
	/** 暗族术古变身技能 / Asmodian Shugo transformation skills. */
	private static final Set<Integer> DARK_TRANSFORM_SKILLS = Set.of(21103, 21104, 21105);
	/** 术古战士变身装置 / Shugo warrior transformation device. */
	private static final int TRANSFORMATION_DEVICE = 831095;
	/** A 阶段的后续波次任务 / Follow-up wave tasks for stage A. */
	private final List<Future<?>> tombRaidTaskA1 = new ArrayList<>();
	/** B 阶段的后续波次任务 / Follow-up wave tasks for stage B. */
	private final List<Future<?>> tombRaidTaskB1 = new ArrayList<>();
	/** C 阶段首轮后续波次任务 / First follow-up wave tasks for stage C. */
	private final List<Future<?>> tombRaidTaskC1 = new ArrayList<>();
	/** C 阶段次轮后续波次任务 / Second follow-up wave tasks for stage C. */
	private final List<Future<?>> tombRaidTaskC2 = new ArrayList<>();
    /** strong kobold worker / strong kobold worker */
		private int strongKoboldWorker;
		/** diligent kobold worker / diligent kobold worker */
		private int diligentKoboldWorker;
		/** swift krall graverobber / swift krall graverobber */
		private int swiftKrallGraverobber;
		/** krall lookout commander / krall lookout commander */
		private int krallLookoutCommander;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 当前绑定副本代次 / current instance binding generation */
	private long lifecycleGeneration;
	/** 当前波次任务组；派生的 sp/sendMsgByRace 自动归属该组 / current wave group for nested tasks */
	private final ThreadLocal<List<Future<?>>> currentWaveTasks = new ThreadLocal<>();
	/** 本实例拥有的全部场景任务 / All scene tasks owned by this instance. */
	private final List<Future<?>> imperialTombTask = new ArrayList<>();
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 * @param npc NPC / npc
	 */

	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 219508: //Diligent Kobold Worker.
			case 219514: //Strong Kobold Worker.
			case 219521: //Swift Krall Graverobber.
			case 219528: //Krall Lookout Commander.
			    for (Player player: instance.getPlayersInside()) {
					if (player.isOnline()) {
					    dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002160, 1)); //Repair.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002158, 1)); //Cursed Chill.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002157, 1)); //Powerful Trickster's Essence.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002156, 1)); //Trickster's Essence.
						switch (Rnd.get(1, 4)) {
					        case 1:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100087, 1)); //Treasure Room Map Piece 1.
				            break;
					        case 2:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100088, 1)); //Treasure Room Map Piece 2.
				            break;
					        case 3:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100089, 1)); //Treasure Room Map Piece 3.
				            break;
					        case 4:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100090, 1)); //Treasure Room Map Piece 4.
				            break;
						}
					}
				}
			break;
			case 219530: //Letu Erezat.
			case 219531: //Captain Lediar.
			case 219544: //Awakened Guardian.
			    for (Player player: instance.getPlayersInside()) {
					if (player.isOnline()) {
					    dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002160, 1)); //Repair.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002158, 1)); //Cursed Chill.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002157, 1)); //Powerful Trickster's Essence.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164002156, 1)); //Trickster's Essence.
						switch (Rnd.get(1, 4)) {
					        case 1:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100087, 1)); //Treasure Room Map Piece 1.
				            break;
					        case 2:
						        dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100088, 1)); //Treasure Room Map Piece 2.
				            break;
					        case 3:
						        dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100089, 1)); //Treasure Room Map Piece 3.
						    break;
						    case 4:
						        dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188100090, 1)); //Treasure Room Map Piece 4.
						    break;
						} switch (Rnd.get(1, 2)) {
						    case 1:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 185000129, 1)); //Common Treasure Chest Key.
						    break;
						    case 2:
				                dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 185000129, 2)); //Common Treasure Chest Key.
						    break;
						}
					}
				}
			break;
		}
	}

	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public synchronized void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		lifecycleGeneration++;
		isInstanceDestroyed = false;
		spawn(831110, 183.95969f, 237.51074f, 536.16974f, (byte) 71); // 称颂皇太子的人 / Crown Prince's Admirer.
		spawn(831095, 218.27571f, 287.24326f, 550.68805f, (byte) 74); //Shugo Warrior Transformation Device.
	}

	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 * @param npc NPC / npc
	 */
	@Override
	public synchronized void onDie(Npc npc) {
		if (!isActiveLifecycle() || npc == null) {
			return;
		}
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 219508: //Diligent Kobold Worker.
			    diligentKoboldWorker++;
				if (diligentKoboldWorker == 6) {
					startTombRaidA1_1();
					// 第二波掠夺者将在 10 秒后到达！ / A second wave of pillagers will arrive in 10 seconds!
					sendMsgByRace(1401586, Race.PC_ALL, 0);
					//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				    sendMsgByRace(1402833, Race.PC_ALL, 5000);
				    //只剩少数敌人！ / Only a few enemies left!
				    sendMsgByRace(1402834, Race.PC_ALL, 10000);
				} else if (diligentKoboldWorker == 20) {
					cancelTasks(tombRaidTaskA1);
					spawn(831095, 344.28635f, 425.418f, 294.75867f, (byte) 56); //Shugo Warrior Transformation Device.
					spawn(831114, 183.95969f, 237.51074f, 536.16974f, (byte) 71); // 称颂喜悦的皇太子的人 / Crown Prince's Delighted Admirer.
					spawn(831111, 340.27893f, 426.2435f, 294.7574f, (byte) 56); // A地区NPC战斗开始 / Empress' Admirer.
				}
			break;
			case 219514: //Strong Kobold Worker.
			    strongKoboldWorker++;
				if (strongKoboldWorker == 6) {
					startTombRaidB1_1();
					// 第二波掠夺者将在 10 秒后到达！ / A second wave of pillagers will arrive in 10 seconds!
					sendMsgByRace(1401586, Race.PC_ALL, 0);
					//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				    sendMsgByRace(1402833, Race.PC_ALL, 5000);
				    //只剩少数敌人！ / Only a few enemies left!
				    sendMsgByRace(1402834, Race.PC_ALL, 10000);
				} else if (strongKoboldWorker == 40) {
					spawnFairyGuardian();
					cancelTasks(tombRaidTaskB1);
					//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
					sendMsgByRace(1402832, Race.PC_ALL, 0);
				}
			break;
			case 219521: //Swift Krall Graverobber.
			    swiftKrallGraverobber++;
				if (swiftKrallGraverobber == 6) {
					startTombRaidC1_1();
					// 第二波掠夺者将在 10 秒后到达！ / A second wave of pillagers will arrive in 10 seconds!
					sendMsgByRace(1401586, Race.PC_ALL, 0);
					//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				    sendMsgByRace(1402833, Race.PC_ALL, 5000);
				    //只剩少数敌人！ / Only a few enemies left!
				    sendMsgByRace(1402834, Race.PC_ALL, 10000);
				} else if (swiftKrallGraverobber == 30) {
					startLetuErezat();
					cancelTasks(tombRaidTaskC1);
					//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				    sendMsgByRace(1402833, Race.PC_ALL, 5000);
				    //只剩少数敌人！ / Only a few enemies left!
				    sendMsgByRace(1402834, Race.PC_ALL, 10000);
				}
			break;
			case 219528: //Krall Lookout Commander.
			    krallLookoutCommander++;
			    if (krallLookoutCommander == 30) {
					startCaptainLediar();
					//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				    sendMsgByRace(1402833, Race.PC_ALL, 5000);
				    //只剩少数敌人！ / Only a few enemies left!
				    sendMsgByRace(1402834, Race.PC_ALL, 10000);
					cancelTasks(tombRaidTaskC2);
				}
			break;
			case 219530: //Letu Erezat.
			    startTombRaidC1_2();
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 0);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 5000);
			break;
			case 219531: //Captain Lediar.
			    deleteNpc(831130); // 皇太子的石碑 / Crown Prince's Monument.
			    spawn(831116, 443.322f, 110.39832f, 212.20023f, (byte) 92); // 称颂喜悦的皇帝的人 / Emperor's Delighted Admirer.
				spawn(831119, 440.2393f, 109.80865f, 212.20023f, (byte) 94); // 马里安·雷林 / Marayrinerk.
				spawn(831350, 452.43765f, 106.14462f, 212.20023f, (byte) 68); // 神圣祭坛 / Imperial Shrine.
			break;
			case 219544: //Awakened Guardian.
			    spawn(831095, 465.13556f, 111.26043f, 214.702f, (byte) 8); //Shugo Warrior Transformation Device.
				spawn(831115, 329.33588f, 432.96265f, 294.76144f, (byte) 100); // 称颂喜悦的皇后的人 / Empress's Delighted Admirer.
			    spawn(831112, 452.43765f, 106.14462f, 212.20023f, (byte) 68); // 称颂皇帝的人 / Emperor's Admirer.
			break;
		}
	}

	private void spawnFairyGuardian() {
		spawn(219544, 315.94565f, 431.73035f, 294.58875f, (byte) 116);
        spawn(219505, 314.94418f, 428.22006f, 294.58875f, (byte) 115);
        spawn(219505, 318.11328f, 427.66050f, 294.58875f, (byte) 115);
        spawn(219505, 319.69778f, 434.42917f, 294.58875f, (byte) 115);
        spawn(219505, 316.08636f, 435.35806f, 294.58875f, (byte) 115);
        spawn(219505, 321.05954f, 430.44263f, 294.58875f, (byte) 115);
	}

	/**
	 * TOMB RAID A
	 */
	private void startTombRaidA1_1() {
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 10000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 30000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 50000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 70000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 90000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 110000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 130000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 150000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 170000);
		scheduleTask(tombRaidTaskA1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219508, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 1000, "ImperialTombUnderpath1");
		        sp(219509, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 5000, "ImperialTombUnderpath1");
		        sp(219510, 199.53075f, 270.43457f, 550.5646f, (byte) 77, 9000, "ImperialTombUnderpath1");
		        sp(219508, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 1000, "ImperialTombUnderpath2");
		        sp(219509, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 5000, "ImperialTombUnderpath2");
		        sp(219510, 209.68540f, 263.57240f, 550.5646f, (byte) 78, 9000, "ImperialTombUnderpath2");
			}
		}, 190000);
	}

	/**
	 * TOMB RAID B
	 */
	private void startTombRaidB1_1() {
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 10000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 30000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 50000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 70000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 90000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 110000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 130000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 150000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 170000);
		scheduleTask(tombRaidTaskB1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219514, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 1000, "ImperialTombUnderpath3");
		        sp(219515, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 5000, "ImperialTombUnderpath3");
		        sp(219516, 307.80344f, 434.2390f, 298.31903f, (byte) 25, 9000, "ImperialTombUnderpath3");
		        sp(219514, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 1000, "ImperialTombUnderpath4");
		        sp(219515, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 5000, "ImperialTombUnderpath4");
		        sp(219516, 307.02597f, 433.8582f, 298.31903f, (byte) 88, 9000, "ImperialTombUnderpath4");
		        sp(219514, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 1000, "ImperialTombUnderpath5");
		        sp(219515, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 5000, "ImperialTombUnderpath5");
		        sp(219516, 359.85450f, 421.5649f, 292.48206f, (byte) 30, 9000, "ImperialTombUnderpath5");
		        sp(219514, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 1000, "ImperialTombUnderpath6");
		        sp(219515, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 5000, "ImperialTombUnderpath6");
		        sp(219516, 359.61240f, 421.5032f, 292.48206f, (byte) 82, 9000, "ImperialTombUnderpath6");
			}
		}, 190000);
	}

	/**
	 * TOMB RAID C-1
	 */
	private void startTombRaidC1_1() {
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 10000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 30000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 50000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 70000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 90000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 110000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 130000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 150000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 170000);
		scheduleTask(tombRaidTaskC1, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219521, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219522, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219523, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219521, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219522, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219523, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219521, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219522, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219523, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 190000);
	}

	/**
	 * TOMB RAID C-2
	 */
	private void startTombRaidC1_2() {
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 10000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 30000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 50000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 70000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 90000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 110000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 130000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 150000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 170000);
		scheduleTask(tombRaidTaskC2, new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				//更多掠夺者将在 5 秒后到达！ / More pillagers will arrive in 5 seconds!
				sendMsgByRace(1401607, Race.PC_ALL, 0);
				sp(219527, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath7");
		        sp(219528, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath7");
		        sp(219529, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath7");
		        sp(219527, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 1000, "ImperialTombUnderpath8");
		        sp(219528, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 5000, "ImperialTombUnderpath8");
		        sp(219529, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 9000, "ImperialTombUnderpath8");
		        sp(219527, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 1000, "ImperialTombUnderpath9");
		        sp(219528, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 5000, "ImperialTombUnderpath9");
		        sp(219529, 419.37616f, 90.95251f, 214.33856f, (byte) 8, 9000, "ImperialTombUnderpath9");
			}
		}, 190000);
	}
	private void startLetuErezat() {
		sp(219530, 398.80435f, 81.94784f, 223.16089f, (byte) 8, 2000, "ImperialTombUnderpath7"); //Letu Erezat.
	}
	private void startCaptainLediar() {
		sp(219531, 398.66214f, 81.80799f, 223.16089f, (byte) 8, 2000, "ImperialTombUnderpath8"); //Captain Lediar.
	}

	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 * @param player 玩家 / player
	 * @param npc NPC / npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (player == null || npc == null || npc.getNpcId() != TRANSFORMATION_DEVICE
			|| !isActiveLifecycle()) {
			return;
		}

		// 设备技能组同时包含光/暗族技能，按玩家阵营选择同一数据源。
		// The device skill list contains both factions; select from the same data source by player race.
		Race race = player.getRace();
		Set<Integer> factionSkills = race == Race.ELYOS ? LIGHT_TRANSFORM_SKILLS
			: race == Race.ASMODIANS ? DARK_TRANSFORM_SKILLS : Set.of();
		if (factionSkills.isEmpty()) {
			return;
		}
		NpcSkillEntry selected = null;
		for (int index = 0; index < npc.getSkillList().size(); index++) {
			NpcSkillEntry candidate = npc.getSkillList().getSkillByIndex(index);
			if (candidate != null && factionSkills.contains(candidate.getSkillId())
				&& (selected == null || candidate.getSkillId() > selected.getSkillId())) {
				selected = candidate;
			}
		}
		if (selected == null) {
			return;
		}
		var skill = SkillEngine.getInstance().getSkill(npc, selected.getSkillId(), selected.getSkillLevel(), player);
		if (skill != null) {
			skill.useNoAnimationSkill();
		}
	}
	/**
	 * 移除相关物品。
	 * Remove related items.
	 * @param player 玩家 / player
	 */

	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(182006989, storage.getItemCountByItemId(182006989)); //Emperor's Golden Tag.
		storage.decreaseByItemId(182006990, storage.getItemCountByItemId(182006990)); //Empress' Silver Tag.
        storage.decreaseByItemId(182006991, storage.getItemCountByItemId(182006991)); //Crown Prince's Brass Tag.
		storage.decreaseByItemId(182006999, storage.getItemCountByItemId(182006999)); //Shugo Coin.
    }

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		for (int skillId : LIGHT_TRANSFORM_SKILLS) {
			effectController.removeEffect(skillId);
		}
		for (int skillId : DARK_TRANSFORM_SKILLS) {
			effectController.removeEffect(skillId);
		}
	}


	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}

	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
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
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}


	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public synchronized void onInstanceDestroy() {
		if (isInstanceDestroyed) {
			return;
		}
		isInstanceDestroyed = true;
		cancelTasks(imperialTombTask);
		tombRaidTaskA1.clear();
		tombRaidTaskB1.clear();
		tombRaidTaskC1.clear();
		tombRaidTaskC2.clear();
		currentWaveTasks.remove();
	}

	/**
	 * 将场景任务绑定到副本而不是会在开战时删除的对话 NPC；销毁后不再调度或执行。
	 * Binds scene tasks to the instance rather than the dialogue NPC deleted at battle start; ignores work after teardown.
	 * @param task 场景动作 / Scene action
	 * @param delay 延迟毫秒 / Delay in milliseconds
	 */
	public void scheduleTask(Runnable task, int delay) {
		scheduleTask(currentWaveTasks.get(), task, delay);
	}

	private synchronized void scheduleTask(List<Future<?>> waveTasks, Runnable task, int delay) {
		Objects.requireNonNull(task, "task");
		if (!isActiveLifecycle()) {
			return;
		}
		imperialTombTask.removeIf(Future::isDone);
		long generation = lifecycleGeneration;
		Future<?> future = scheduleTaskExecution(() -> runTask(task, waveTasks, generation), delay);
		if (future == null) {
			return;
		}
		imperialTombTask.add(future);
		if (waveTasks != null) {
			waveTasks.add(future);
		}
	}

	/**
	 * 调度边界；测试可覆盖以推进任务而不启动线程池。
	 * Scheduling seam overridable by tests to advance tasks without a thread pool.
	 */
	protected Future<?> scheduleTaskExecution(Runnable task, int delay) {
		return ThreadPoolManager.getInstance().schedule(task, delay);
	}

	/**
	 * 将已出队回调与销毁串行化，取消 Future 之外仍需阻止晚到的回调。
	 * Serializes dequeued callbacks with teardown; cancelling their futures alone cannot suppress late callbacks.
	 */
	private void runTask(Runnable task, List<Future<?>> waveTasks, long generation) {
		List<Future<?>> previousWaveTasks = currentWaveTasks.get();
		synchronized (this) {
			if (!isCurrentLifecycle(generation)) {
				return;
			}
			currentWaveTasks.set(waveTasks);
		}
		try {
			synchronized (this) {
				if (!isCurrentLifecycle(generation)) {
					return;
				}
				task.run();
			}
		} finally {
			if (previousWaveTasks == null) {
				currentWaveTasks.remove();
			} else {
				currentWaveTasks.set(previousWaveTasks);
			}
		}
	}

	private boolean isActiveLifecycle() {
		return instance != null && !isInstanceDestroyed && isCurrentInstance();
	}

	/**
	 * 通过对象身份确认仍绑定当前注册实例，避免实例 ID 复用后旧处理器继续运行。
	 * Confirms the handler still owns the registered instance object, not merely a reused id.
	 */
	protected boolean isCurrentInstance() {
		return World.getInstance().getWorldMap(mapId).getWorldMapInstanceById(instanceId) == instance;
	}

	private synchronized boolean isCurrentLifecycle(long generation) {
		return isActiveLifecycle() && lifecycleGeneration == generation;
	}

	private synchronized void cancelTasks(List<Future<?>> tasks) {
		for (Future<?> task : List.copyOf(tasks)) {
			task.cancel(false);
			imperialTombTask.remove(task);
		}
		tasks.clear();
	}


	/**
	 * 延迟生成并启动巡逻；副本销毁或生成被底层拒绝时不再操作 NPC。
	 * Spawns a delayed walker; does not touch an NPC after teardown or a rejected spawn.
	 */
	protected void sp(int npcId, float x, float y, float z, byte h, int time, String walkerId) {
		scheduleTask(currentWaveTasks.get(), () -> {
			Npc npc = (Npc) spawn(npcId, x, y, z, h);
			if (npc != null) {
				npc.getSpawn().setWalkerId(walkerId);
				WalkManager.startWalking((NpcAI2) npc.getAi2());
			}
		}, time);
	}

	protected void sendMsgByRace(final int msg, final Race race, int time) {
		scheduleTask(currentWaveTasks.get(), new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<>() {
					/**
					 * 处理 visit。
					 * Handle visit.
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
	 * 判断玩家是否具有当前阵营的有效术古变身。
	 * Whether the player has an active Shugo transformation matching their faction.
	 *
	 * @param player 玩家 / player
	 * @return 满足开战条件时为 true / true when battle prerequisites are met
	 */
	public boolean hasValidShugoTransform(Player player) {
		if (player == null || player.getTransformModel() == null) {
			return false;
		}
		Effect activeEffect = player.getTransformModel().getActiveTransformEffect();
		int modelPanelId = player.getTransformModel().getPanelId();
		if (activeEffect == null || !SHUGO_TRANSFORM_PANELS.contains(modelPanelId)) {
			return false;
		}
		TribeClass expectedTribe = expectedTransformTribe(player.getRace());
		if (expectedTribe == null || player.getTransformModel().getTribe() != expectedTribe) {
			return false;
		}
		for (EffectTemplate effectTemplate : activeEffect.getEffectTemplates()) {
			if (effectTemplate instanceof TransformEffect transform
				&& transform.getPanelId() == modelPanelId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 开战门禁：实例内所有在线参与者都必须拥有有效术古变身。
	 * Battle gate: every online participant in the instance must hold a valid Shugo transformation.
	 *
	 * @param player 触发开战的玩家 / player starting the battle
	 * @return 可以开战时为 true / true when the battle can start
	 */
	public boolean canStartTombBattle(Player player) {
		if (!isActiveLifecycle() || player == null || !hasValidShugoTransform(player)) {
			return false;
		}
		for (Player participant : instance.getPlayersInside()) {
			if (participant != null && participant.isOnline() && !hasValidShugoTransform(participant)) {
				return false;
			}
		}
		return true;
	}

	private static TribeClass expectedTransformTribe(Race race) {
		return race == Race.ELYOS ? TribeClass.IDEVENT01_POLYMORPHL
			: race == Race.ASMODIANS ? TribeClass.IDEVENT01_POLYMORPHD : null;
	}
}
