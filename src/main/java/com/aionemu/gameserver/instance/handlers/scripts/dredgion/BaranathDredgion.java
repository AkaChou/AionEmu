package com.aionemu.gameserver.instance.handlers.scripts.dredgion;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.DredgionReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.DredgionPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 巴拉纳特无渊号副本事件处理器。
 * Instance event handler for Baranath Dredgion.
 *
 * @author Encom
 * @author MATTY
 */

@InstanceID(300110000)
public class BaranathDredgion extends GeneralInstanceHandler
{
	/** 隔板 / bulkhead */
		private int bulkhead;
	/** 秘密缓存 / secret cache */
		private int secretCache;
	/** 苏卡纳击杀 / surkana kills */
		private int surkanaKills;
	/** 副本时间戳 / instance timestamp */
		private long instanceTime;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 无畏舰奖励 / dredgion reward */
		protected DredgionReward dredgionReward;
	/** 败方倍率 / losing-group multiplier */
		private float loosingGroupMultiplier = 1;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed = false;
	/** 副本是否已开始 / whether the instance started */
		protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
	/** baranath 任务 / baranath task */
		private final List<Future<?>> baranathTask = new ArrayList<Future<?>>();
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * 玩家 / player
	 * result
	 */
	
	protected DredgionPlayerReward getPlayerReward(Player player) {
		Integer object = player.getObjectId();
		if (dredgionReward.getPlayerReward(object) == null) {
			addPlayerToReward(player);
		}
		return (DredgionPlayerReward) dredgionReward.getPlayerReward(object);
	}
	/**
	 * 处理 captureRoom。
	 * Handle captureRoom.
	 *
	 * 阵营 / race
	 * roomId
	 */
	
	protected void captureRoom(Race race, int roomId) {
		dredgionReward.getDredgionRoomById(roomId).captureRoom(race);
	}
	
	private void addPlayerToReward(Player player) {
		dredgionReward.addPlayerReward(new DredgionPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return dredgionReward.containPlayer(object);
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
			case 214823: //Captain Adhati.
				for (Player player: instance.getPlayersInside()) {
					if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 215082: //Technician Sarpa.
			case 215083: //Navigator Nevikah.
			case 215084: //Assistant Malakun.
			case 215085: //Adjutant Kundhan.
			case 215086: //First Mate Aznaya.
			case 215089: //Air Captain Girana.
			case 215090: //Vice Air Captain Kai.
			case 215091: //Vice Gun Captain Zha.
			case 215092: //Gun Captain Ankrana.
			case 215390: //Auditor Nirshaka.
			case 215391: //Quartermaster Vujara.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 215087: //Sentinel Garkusa.
				for (Player player: instance.getPlayersInside()) {
					dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000040, 1)); //Brig Key.
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 215088: //Prison Guard Mahnena.
				for (Player player: instance.getPlayersInside()) {
					dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000072, 1)); //Secondary Brig Key.
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					}
				}
			break;
			case 215093: //Adjutant Kalanadi.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						switch (Rnd.get(1, 4)) {
							case 1:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121000836, 1)); //Kalanadi's Necklace.
							break;
							case 2:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001053, 1)); //Kalanadi's Ring.
							break;
							case 3:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123000941, 1)); //Kalanadi's Belt.
							break;
							case 4:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123000942, 1)); //Kalanadi's Band.
							break;
						}
					}
				}
			break;
			case 215427: //Supervisor Lakhane.
				for (Player player: instance.getPlayersInside()) {
				    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000189, 1)); //Secret Cache Key.
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //龙之征服者印记箱。 / Dragon's Conquerer Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						switch (Rnd.get(1, 3)) {
							case 1:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125001995, 1)); //Lakhane's Kerchief.
							break;
							case 2:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121000837, 1)); //Lakhane's Necklace.
							break;
							case 3:
							    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001054, 1)); //Lakhane's Ring.
							break;
						}
					}
				}
			break;
		}
	}
	
	private void onDieSurkan(Npc npc, Player mostPlayerDamage, int points) {
        Race race = mostPlayerDamage.getRace();
        captureRoom(race, npc.getNpcId() + 14 - 700498); //Captain's Cabin Power Surkana.
        for (Player player: instance.getPlayersInside()) {
            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400199, new DescriptionId(race.equals(Race.ASMODIANS) ? 1800483 : 1800481), new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1)));
        } if (++surkanaKills == 5) {
            // 阿达蒂船长已出现在船长室。 / Captain Adhati has appeared in the Captain's Cabin.
			sendMsgByRace(1400405, Race.PC_ALL, 0);
			spawn(214823, 485.47916f, 812.4957f, 416.68475f, (byte) 31); //Captain Adhati.
        }
		getPlayerReward(mostPlayerDamage).captureZone();
        updateScore(mostPlayerDamage, npc, points, false);
        npc.getController().onDelete();
    }
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
	protected void startInstanceTask() {
		instanceTime = System.currentTimeMillis();
		baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				openFirstDoors();
				// 舱壁已激活，第一军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the First Armory and Gravity Control has been sealed.
				sendMsgByRace(1400595, Race.PC_ALL, 5000);
				// 舱壁已激活，第二军械库与重力控制室之间的通道已封闭。 / The bulkhead has been activated and the passage between the Second Armory and Gravity Control has been sealed.
				sendMsgByRace(1400596, Race.PC_ALL, 10000);
				dredgionReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
				sendPacket();
				switch (Rnd.get(1, 2)) {
					case 1:
					    spawn(215391, 415.2769f, 282.0216f, 409.7311f, (byte) 118); //Quartermaster Vujara.
					break;
					case 2:
					    spawn(215391, 556.53534f, 279.2918f, 409.7311f, (byte) 33); //Quartermaster Vujara.
					break;
				} switch (Rnd.get(1, 2)) {
					case 1:
					    spawn(215086, 485.25455f, 877.04614f, 405.01407f, (byte) 90); //First Mate Aznaya.
					break;
					case 2:
					    spawn(215390, 485.25455f, 877.04614f, 405.01407f, (byte) 90); //Auditor Nirshaka.
					break;
				}
			}
		}, 60000));
	   /**
	 * 巴拉纳斯战舰内有多处传送装置。 / Baranath Dredgion Teleportation Devices: There are numerous teleportation devices located inside the Baranath Dredgion. These teleportation devices allow players to teleport to different areas of the Dredgion with ease. Central Teleporter: This teleporter activates 10 minutes after the Instanced Dungeon has begun
	 */
		baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				// 紧急出口已生成核控制室传送器。 / A Nuclear Control Room Teleporter has been created at the Emergency Exit.
				sendMsgByRace(1400265, Race.PC_ALL, 0);
				spawn(730187, 398.45651f, 160.15234f, 432.2988f, (byte) 0, 10); //Portside Central Teleporter.
				spawn(730188, 571.88f, 160.62f, 432.29999f, (byte) 0, 9); //Starboard Central Teleporter. 
			}
		}, 600000));
		baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!dredgionReward.isRewarded()) {
					Race winningRace = dredgionReward.getWinningRaceByScore();
					stopInstance(winningRace);
				}
			}
		}, 3600000));
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		int point = 0;
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		switch (npc.getObjectTemplate().getTemplateId()) {
		   /**
	 * 解救囚犯：击杀囚室命名怪可获得房间钥匙。 / Rescue Prisoners: olding the named monster of a prisoner receiving chamber can accommodate prisoners get a room key. When you open the container chamber prisoner standing in the room with the key to rescue the prisoners to obtain a score of 100 points. Conversely, it is possible to obtain a 100-point touch the opponent, like captive species
	 */
		    case 798323: //Captured Elyos Scholar.
            case 798324: //Captured Guardian.
            case 798325: //Captured Guardian.
			case 798327: //Captured Asmodian Scholar.
            case 798328: //Captured Archon.
            case 798329: //Captured Archon.
                point = 100;
				despawnNpc(npc);
            break;
		   /**
	 * 苏卡纳：摧毁各房间苏卡纳可获得更高分数。 / The Surkana: 1. Destroy Surkana in each room can obtain a higher score. 2. When you add monsters to attack Surkana is around 20m range. First, it is safe to be cleaned up monsters. 3. When you destroy a race that destroyed Surkana is displayed on the map, it is through you can guess the path of the opposing faction
	 */
			case 700485: //Armory Maintenance Surkana.
			case 700486: //Armory Maintenance Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 500);
			break;
			case 700487: //Gravity Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 900);
			break;
			case 700488: //Nuclear Control Surkana.
			case 700489: //Nuclear Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 1100);
			break;
			case 700490: //Main Cannon Control Surkana.
			case 700491: //Main Cannon Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 800);
			break;
			case 700492: //Drop Device Surkana.
			case 700493: //Drop Device Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 600);
			break;
			case 700494: //Fighter Enhancing Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 700);
			break;
			case 700495: //Brig Power Surkana.
			case 700496: //Brig Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 500);
			break;
			case 700497: //Bridge Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 700);
			break;
			case 700498: //Captain's Cabin Power Surkana.
				despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 1100);
			break;
			case 700503: //Portside Door Of Captain's Cabin.
				// 左舷船长室门已被摧毁。 / The Port Captain's Cabin Door has been destroyed.
				sendMsgByRace(1400230, Race.PC_ALL, 0);
			break;
			case 700504: //Starboard Door Of Captain's Cabin.
				// 右舷船长室门已被摧毁。 / The Starboard Captain's Cabin Door has been destroyed.
				sendMsgByRace(1400231, Race.PC_ALL, 0);
			break;
		   /**
	 * 船长室传送装置：在兵营击败监督者拉卡内后激活。 / Captain’s Cabin Teleport Device: This teleporter activates when "Supervisor Lakhane" is defeated in the Barracks. Only the race that defeated "Supervisor Lakhane" can use this teleporter
	 */
			case 215427: //Supervisor Lakhane.
				point = 1000;
				// 中庭尽头生成了可持续 3 分钟的船长室传送装置。 / A Captain's Cabin Teleport Device that lasts for 3 minutes has been generated at the end of the Atrium.
				sendMsgByRace(1400234, Race.PC_ALL, 0);
				spawn(730197, 484.72f, 761.41998f, 388.66f, (byte) 0, 91); //Captain's Cabin Teleport Device.
            break;
		   /**
	 * 补给室传送器：兵营中传送发生器被摧毁后激活 / Supply Room Teleporter: This teleporter activates after the destruction of the Teleporter Generator in the Barracks
	 */
			case 700505: //Portside Teleporter Generator.
                despawnNpc(npc);
				// 左舷中央传送器已在逃生舱口生成。 / A Portside Central Teleporter has been generated at the Escape Hatch.
				sendMsgByRace(1400228, Race.PC_ALL, 0);
				spawn(730213, 402.33429f, 175.11707f, 432.2988f, (byte) 0, 64); //No.1 Nuclear Control Room Teleporter.
            break;
			case 700506: //Starboard Teleporter Generator.
                despawnNpc(npc);
				// 右舷中央传送器已在副逃生舱口生成。 / A Starboard Central Teleporter has been generated at the Secondary Escape Hatch.
				sendMsgByRace(1400229, Race.PC_ALL, 0);
				spawn(730214, 567.59119f, 175.19655f, 432.29999f, (byte) 0, 65); //No.2 Nuclear Control Room Teleporter.
            break;
		   /**
	 * 每台护盾发生器需要 3 个理念物品，共 12 个 / Defense Shield Generator: When the Defense Shield Generator on the Weapons Deck or Lower Weapons deck is demolished, a shield appears in Ready Room 1 or 2. This shield blocks access to the center of the Baranath Dredgion. The Ready Room is the shortest route to the center of the Dredgion, and the quickest route to the opposing race’s area. Different tactics can be used in this area to maximize the Group’s accumulation of points. For example, if one Group decides to destroy the opposing Group’s Shield Generator, it will make it difficult for the opposing Group to reach the center of the Dredgion. In some cases, it might wiser for one Group to destroy their own Defense Shield Generator, and delay engagement with the opposing race in order to accumulate more points
	 */
			case 700501: //Portside Defense Shield.
			case 700502: //Starboard Defense Shield.
				despawnNpc(npc);
			break;
			case 700507: //Portside Defense Shield Generator.
				despawnNpc(npc);
				// 左舷防御护盾已在准备室 1 生成。 / The Portside Defense Shield has been generated in Ready Room 1.
				sendMsgByRace(1400226, Race.PC_ALL, 0);
				spawn(700501, 448.39151f, 493.64182f, 394.13174f, (byte) 0, 12);  // spawn barier
	
			break;
			case 700508: //Starboard Defense Shield Generator.
				despawnNpc(npc);
				// 右舷防御护盾已在准备室 2 生成。 / The Starboard Defense Shield has been generated in Ready Room 2.
				sendMsgByRace(1400227, Race.PC_ALL, 0);
				spawn(700502, 520.87555f, 493.40115f, 394.43292f, (byte) 0, 16);  // spawn barier
				
			break;
		   /**
	 * 舱壁：哨兵开战时激活护盾，阻挡入口。 / The Bulkhead: These shields are activated by the Baranath Churl when first encountered at the beginning of the battle. These shields block the entrance from the Armories to Gravity Control, and can be demolished with attacks, but also have a significant amount of health. Groups often opt to move around the shields instead of demolishing them. It’s worth noting that after a certain amount of time has passed, Technician Sarpa spawns in the Gravity Control room, and gives 1,000 points when defeated. There is also a chance that Adjutant Kalanadi, a Hero grade Named Monster, will spawn. Adjutant Kalanadi has a chance to drop Fabled and Heroic accessories
	 */
			case 700598: //Port Bulkhead.
			case 700599: //Starboard Bulkhead.
				bulkhead++;
				if (bulkhead == 2) {
				    switch (Rnd.get(1, 2)) {
					    case 1:
					        spawn(215082, 456.3946f, 319.65912f, 402.69315f, (byte) 28); //Technician Sarpa.
					    break;
					    case 2:
					        spawn(215093, 513.9867f, 319.86224f, 402.68634f, (byte) 4); //Adjutant Kalanadi.
					    break;
				    }
				}
				despawnNpc(npc);
			break;
			case 215083: //Navigator Nevikah.
			case 215084: //Assistant Malakun.
			case 215085: //Adjutant Kundhan.
			case 215087: //Sentinel Garkusa.
			case 215088: //Prison Guard Mahnena.
			case 215089: //Air Captain Girana.
			case 215090: //Vice Air Captain Kai.
			case 215091: //Vice Gun Captain Zha.
			case 215092: //Gun Captain Ankrana.
			    secretCache++;
				if (secretCache == 5) {
				    // 战舰宝箱已出现在投放区！ / A Dredgion Treasure Chest has appeared in the Drop Zone!
					sendMsgByRace(1401421, Race.PC_ALL, 0);
					spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92); //Dredgion Opportunity Bundle.
				}
				point = 200;
            break;
			case 215082: //Technician Sarpa.
			case 215086: //First Mate Aznaya.
			case 215093: //Adjutant Kalanadi.
			case 215390: //Auditor Nirshaka.
			case 215391: //Quartermaster Vujara.
                point = 1000;
            break;
            case 214823: //Captain Adhati.
                point = 1000;
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
					public void run() {
						if (!dredgionReward.isRewarded()) {
							Race winningRace = dredgionReward.getWinningRaceByScore();
							stopInstance(winningRace);
						}
					}
				}, 30000);
			break;
        }
		updateScore(mostPlayerDamage, npc, point, false);
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 处理 openFirstDoors。
	 * Handle openFirstDoors.
	 */
	
    protected void openFirstDoors() {
        openDoor(17);
        openDoor(18);
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		if (!containPlayer(player.getObjectId())) {
			addPlayerToReward(player);
		}
		sendPacket();
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
		dredgionReward = new DredgionReward(mapId, instanceId);
		dredgionReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
		startInstanceTask();
	}
	/**
	 * 停止副本并结算。
	 * Stop the instance and settle.
	 *
	 * @param race 阵营 / race
	 */
	
	protected void stopInstance(Race race) {
		stopInstanceTask();
		dredgionReward.setWinningRace(race);
		dredgionReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward();
		sendPacket();
	}
	/**
	 * 结算并发放奖励。
	 * Settle and grant rewards.
	 */
	
	public void doReward() {
		for (Player player : instance.getPlayersInside()) {
			InstancePlayerReward playerReward = getPlayerReward(player);
			float abyssPoint = playerReward.getPoints() * RateConfig.DREDGION_REWARD_RATE;
			if (player.getRace().equals(dredgionReward.getWinningRace())) {
				abyssPoint += dredgionReward.getWinnerPoints();
			} else {
				abyssPoint += dredgionReward.getLooserPoints();
			}
			AbyssPointsService.addAp(player, (int) abyssPoint);
			QuestEnv env = new QuestEnv(null, player, 0, 0);
			GameEngineServices.questEngine().onDredgionReward(env);
		}
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player : instance.getPlayersInside()) {
						if (PlayerActions.isAlreadyDead(player)) {
							PlayerReviveService.duelRevive(player);
						}
						onExitInstance(player);
					}
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
				}
			}
		}, 120000);
	}
	
	private int getTime() {
		long result = System.currentTimeMillis() - instanceTime;
		if (result < 60000) {
			return (int) (60000 - result);
		} else if (result < 3600000) {
			return (int) (3600000 - (result - 60000));
		}
		return 0;
	}
	
	/**
	 * 处理玩家复活事件。
	 * Handle a player revive event.
	 *
	 * 玩家 / player
	 * result
	 */
	@Override
    public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INSTANT_DUNGEON_RESURRECT, 0, 0));
        dredgionReward.portToPosition(player);
		return true;
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * 玩家 / player
	 * @param lastAttacker 最后攻击者 / last attacker
	 * result
	 */
	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		int points = 60;
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
        PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), false, 0, 8));
		if (lastAttacker instanceof Player) {
			if (lastAttacker.getRace() != player.getRace()) {
				InstancePlayerReward playerReward = getPlayerReward(player);
				if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
					points *= loosingGroupMultiplier;
				} else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
					points = 0;
				}
			    updateScore((Player) lastAttacker, player, points, true);
			}
		}
		updateScore(player, player, -points, false);
		return true;
	}
	
	private MutableInt getPointsByRace(Race race) {
		return dredgionReward.getPointsByRace(race);
	}
	
	private void addPointsByRace(Race race, int points) {
		dredgionReward.addPointsByRace(race, points);
	}
	
	private void addPointToPlayer(Player player, int points) {
		getPlayerReward(player).addPoints(points);
	}
	
	private void addPvPKillToPlayer(Player player) {
		getPlayerReward(player).addPvPKillToPlayer();
	}
	
	private void addBalaurKillToPlayer(Player player) {
		getPlayerReward(player).addMonsterKillToPlayer();
	}
	/**
	 * 处理 updateScore。
	 * Handle updateScore.
	 *
	 * 玩家 / player
	 * target
	 * points
	 * pvpKill
	 */
	
	protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
		if (points == 0) {
			return;
		}
		addPointsByRace(player.getRace(), points);
		List<Player> playersToGainScore = new ArrayList<Player>();
		if (target != null && player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (member.getLifeStats().isAlreadyDead()) {
					continue;
				} if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
					playersToGainScore.add(member);
				}
			}
		} else {
			playersToGainScore.add(player);
		}
		for (Player playerToGainScore : playersToGainScore) {
			addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
			if (target instanceof Npc) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
			}
		}
		int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
		if (pointDifference < 0) {
			pointDifference *= -1;
		} if (pointDifference >= 3000) {
			loosingGroupMultiplier = 10;
		} else if (pointDifference >= 1000) {
			loosingGroupMultiplier = 1.5f;
		} else {
			loosingGroupMultiplier = 1;
		} if (pvpKill && points > 0) {
			addPvPKillToPlayer(player);
		} else if (target instanceof Npc && ((Npc) target).getRace().equals(Race.DRAKAN)) {
			addBalaurKillToPlayer(player);
		}
		sendPacket();
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		dredgionReward.clear();
		stopInstanceTask();
		doors.clear();
	}
	/**
	 * 打开指定门。
	 * Open the given door.
	 *
	 * doorId
	 */
	
	protected void openDoor(int doorId) {
		StaticDoor door = doors.get(doorId);
		if (door != null) {
			door.setOpen(true);
		}
	}
	
	private void sendPacket() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), dredgionReward, instance.getPlayersInside()));
			}
		});
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
        baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        baranathTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        }, time));
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
	
	private void stopInstanceTask() {
        for (Future<?> task : baranathTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return dredgionReward;
	}
	
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onExitInstance(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onLeaveInstance(Player player) {
        stopInstanceTask();
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
        if (player.isInGroup2()) {
            PlayerGroupService.removePlayer(player);
        }
    }
}
