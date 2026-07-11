package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 龙脊深渊副本事件处理器。
 * Instance event handler for Drakenspire Depths.
 *
 * @author Encom
 */

@InstanceID(301390000)
public class DrakenspireDepthsInstance extends GeneralInstanceHandler {

	/** death char / death char */
		private int deathChar;
	/** sealscene 种族 / seal scene race */
		private Race sealSceneRace;
	/** drakenspire protector / drakenspire protector */
		private int drakenspireProtector;
	/** drakenspire 任务 / drakenspire task */
		private Future<?> drakenspireTask;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	/** 对象 / objects */
		private Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
	
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
		//** 破壁双生首领 / Breakwall Twin's Boss *//
		SpawnTemplate IDsealFire1st = SpawnEngine.addNewSingleTimeSpawn(301390000, 702695, 558.32593f, 152.40855f, 1683.0303f, (byte) 0);
		IDsealFire1st.setEntityId(408);
		objects.put(702695, SpawnEngine.spawnObject(IDsealFire1st, instanceId));
		//** 破壁双生首领 / Breakwall Twin's Boss *//
		SpawnTemplate IDsealFire2st = SpawnEngine.addNewSingleTimeSpawn(301390000, 702696, 558.32593f, 212.02460f, 1683.0303f, (byte) 0);
		IDsealFire2st.setEntityId(409);
		objects.put(702696, SpawnEngine.spawnObject(IDsealFire2st, instanceId));
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(236224, 850.943f, 343.2288f, 1723.6771f, (byte) 26); //Rapacious Kadena.
			break;
			case 2:
				spawn(236224, 879.0986f, 276.86612f, 1715.0032f, (byte) 76); //Rapacious Kadena.
			break;
			case 3:
				spawn(236224, 786.0551f, 363.54608f, 1697.079f, (byte) 86); //Rapacious Kadena.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(236244, 151.88565f, 518.48145f, 1749.5945f, (byte) 9); //Beritra.
			break;
			case 2:
				spawn(236245, 151.88565f, 518.48145f, 1749.5945f, (byte) 9); //Enraged Beritra.
			break;
			case 3:
				spawn(236246, 151.88565f, 518.48145f, 1749.5945f, (byte) 9); //Crazed Beritra.
			break;
		}
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
			case 236223: //Fetid Phantomscorch Chimera.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000219, 1)); //Crossroads Choice Key.
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
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		if (sealSceneRace == null) {
            sealSceneRace = player.getRace();
            GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				/**
				 * 处理 run。
				 * Handle run.
				 */
				@Override
				public void run() {
				    spawnIDSealScene01();
				}
			}, 20000);
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
			case 855621: //Magma Glutten.
			case 855622: //Flamekite Geist.
			    despawnNpc(npc);
			break;
			case 236106: //Deathchar Slayer.
			case 236109: //Elite Deathchar Patroler.
			case 236113: //Deathchar Slayer.
			case 236116: //Elite Deathchar Patroler.
				deathChar++;
				if (deathChar == 4) {
				    // 熔岩守护者与热风口守护者共享泉源。 / The Lava Protector and Heatvent Protector are sharing the Fount.
					sendMsgByRace(1402682, Race.PC_ALL, 0);
					// 同时击败两名守护者时泉源被毁，守护者无法再复活。 / When both Protectors are defeated at the same time, the Fount is destroyed and the Protectors can no longer resurrect.
					sendMsgByRace(1402683, Race.PC_ALL, 10000);
					if (player != null) {
				        switch (player.getRace()) {
					        case ELYOS:
							    spawn(209686, 412.57935f, 177.6678f, 1684.2161f, (byte) 0);
							    spawn(209687, 412.36823f, 187.22583f, 1684.2161f, (byte) 0);
						    break;
						    case ASMODIANS:
							    spawn(209751, 412.57935f, 177.6678f, 1684.2161f, (byte) 0);
							    spawn(209752, 412.36823f, 187.22583f, 1684.2161f, (byte) 0);
						    break;
						}
					}
				}
			break;
			case 236221: //Fetid Deathchar Patroler.
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209684, 498.12088f, 206.94075f, 1688.1917f, (byte) 0);
									spawn(209684, 498.19083f, 215.10863f, 1688.1915f, (byte) 0);
									spawn(209685, 504.2284f, 211.17325f, 1688.1797f, (byte) 1);
								}
							}, 5000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209749, 498.12088f, 206.94075f, 1688.1917f, (byte) 0);
									spawn(209749, 498.19083f, 215.10863f, 1688.1915f, (byte) 0);
									spawn(209750, 504.2284f, 211.17325f, 1688.1797f, (byte) 1);
								}
							}, 5000);
						break;
					}
				}
			break;
			case 236222: //Fetid Deathchar Necromancer.
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209684, 497.9484f, 147.90346f, 1688.2479f, (byte) 1);
									spawn(209684, 498.11224f, 155.47922f, 1688.2467f, (byte) 0);
									spawn(209685, 504.3097f, 152.23334f, 1688.1984f, (byte) 0);
								}
							}, 5000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209749, 497.9484f, 147.90346f, 1688.2479f, (byte) 1);
									spawn(209749, 498.11224f, 155.47922f, 1688.2467f, (byte) 0);
									spawn(209750, 504.3097f, 152.23334f, 1688.1984f, (byte) 0);
								}
							}, 5000);
						break;
					}
				}
			break;
			case 236225: //Fountless Lava Protector.
			case 236226: //Fountless Heatvent Protector.
				deleteNpc(702404); //Twin's Firewall.
				deleteNpc(702695); //Breakwall Twin's Boss.
				deleteNpc(702696); //Breakwall Twin's Boss.
				// 守护者之泉已被摧毁，他们无法再复活。 / The Protectors' Fount has been destroyed and they will not be resurrected.
				sendMsgByRace(1402688, Race.PC_ALL, 0);
				Npc lavaProtector = instance.getNpc(236225); //Fountless Lava Protector.
			    Npc heatventProtector = instance.getNpc(236226); //Fountless Heatvent Protector.
			    if (isDead(lavaProtector) && isDead(heatventProtector)) {
				    if (player != null) {
				        switch (player.getRace()) {
					        case ELYOS:
							    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							        /**
							         * 处理 run。
							         * Handle run.
							         */
							        @Override
								    public void run() {
										spawn(209690, 552.93365f, 155.68227f, 1683.7301f, (byte) 0);
										spawn(209690, 552.90247f, 215.51768f, 1683.7301f, (byte) 0);
										spawn(209693, 552.98486f, 148.64922f, 1683.7301f, (byte) 1);
										spawn(209693, 553.1255f, 208.44653f, 1683.7301f, (byte) 1);
								    }
							    }, 2000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								    /**
								     * 处理 run。
								     * Handle run.
								     */
								    @Override
									public void run() {
										// 天族/魔族精锐开始推进。 / The Empyrean Elite started to advance.
										sendMsgByRace(1402994, Race.PC_ALL, 0);
									    spawn(209694, 583.8417f, 177.45332f, 1683.7301f, (byte) 8);
										spawn(209695, 582.48083f, 183.74684f, 1683.7301f, (byte) 116);
										Npc PCGuard_Li_Talk_A = getNpc(209695);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li_Talk_A, 1402727, PCGuard_Li_Talk_A.getObjectId(), 0, 2000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li_Talk_A, 1402728, PCGuard_Li_Talk_A.getObjectId(), 0, 6000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li_Talk_A, 1402729, PCGuard_Li_Talk_A.getObjectId(), 0, 10000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li_Talk_A, 1402730, PCGuard_Li_Talk_A.getObjectId(), 0, 14000);
									}
								}, 10000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								    /**
								     * 处理 run。
								     * Handle run.
								     */
								    @Override
									public void run() {
									    deleteNpc(209694);
										deleteNpc(209695);
										// 消灭恶臭幻焰大师并选择前进路径。 / Eliminate the Fetid Phantomscorch Master and choose a path to proceed.
										sendMsgByRace(1402995, Race.PC_ALL, 0);
										// 获取恶臭幻焰大师携带的十字路口选择钥匙。 / Obtain the Crossroads Choice Key carried by the Fetid Phantomscorch Master.
										sendMsgByRace(1403121, Race.PC_ALL, 10000);
										spawn(209698, 583.8417f, 177.45332f, 1683.7301f, (byte) 8);
										spawn(209700, 582.48083f, 183.74684f, 1683.7301f, (byte) 116);
									}
								}, 15000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									/**
									 * 处理 run。
									 * Handle run.
									 */
									@Override
									public void run() {
										Npc Masionel = getNpc(209698);
										// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
										GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501314, Masionel.getObjectId(), 0, 0);
										// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
										GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501312, Masionel.getObjectId(), 0, 6000);
										// 让我炸开一条路…… / Just let me blast us a path...
										GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501310, Masionel.getObjectId(), 0, 12000);
									}
								}, 25000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									/**
									 * 处理 run。
									 * Handle run.
									 */
									@Override
									public void run() {
										killNpc(getNpcs(731580));
										Npc Masionel = getNpc(209698);
										// 分遣队爆破手已打开通往下一区域的道路。 / Detachment Demolisher has opened the path to the next area.
										sendMsgByRace(1402689, Race.PC_ALL, 0);
										// 现在可以通过了。 / We can get through now.
										GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501311, Masionel.getObjectId(), 0, 0);
										// 请多保重。 / Please take care.
										GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501313, Masionel.getObjectId(), 0, 6000);
									}
								}, 30000);
							break;
							case ASMODIANS:
							    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							        /**
							         * 处理 run。
							         * Handle run.
							         */
							        @Override
								    public void run() {
										spawn(209755, 552.93365f, 155.68227f, 1683.7301f, (byte) 0);
										spawn(209755, 552.90247f, 215.51768f, 1683.7301f, (byte) 0);
										spawn(209758, 552.98486f, 148.64922f, 1683.7301f, (byte) 1);
										spawn(209758, 553.1255f, 208.44653f, 1683.7301f, (byte) 1);
								    }
							    }, 2000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								    /**
								     * 处理 run。
								     * Handle run.
								     */
								    @Override
									public void run() {
										// 天族/魔族精锐开始推进。 / The Empyrean Elite started to advance.
										sendMsgByRace(1402994, Race.PC_ALL, 0);
									    spawn(209759, 583.8417f, 177.45332f, 1683.7301f, (byte) 8);
										spawn(209760, 582.48083f, 183.74684f, 1683.7301f, (byte) 116);
										Npc PCGuard_Da_Talk_A = getNpc(209760);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da_Talk_A, 1402727, PCGuard_Da_Talk_A.getObjectId(), 0, 2000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da_Talk_A, 1402728, PCGuard_Da_Talk_A.getObjectId(), 0, 6000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da_Talk_A, 1402729, PCGuard_Da_Talk_A.getObjectId(), 0, 10000);
										GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da_Talk_A, 1402730, PCGuard_Da_Talk_A.getObjectId(), 0, 14000);
									}
								}, 10000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								    /**
								     * 处理 run。
								     * Handle run.
								     */
								    @Override
									public void run() {
									    deleteNpc(209759);
										deleteNpc(209760);
										// 消灭恶臭幻焰大师并选择前进路径。 / Eliminate the Fetid Phantomscorch Master and choose a path to proceed.
										sendMsgByRace(1402995, Race.PC_ALL, 0);
										// 获取恶臭幻焰大师携带的十字路口选择钥匙。 / Obtain the Crossroads Choice Key carried by the Fetid Phantomscorch Master.
										sendMsgByRace(1403121, Race.PC_ALL, 10000);
										spawn(209763, 583.8417f, 177.45332f, 1683.7301f, (byte) 8);
										spawn(209765, 582.48083f, 183.74684f, 1683.7301f, (byte) 116);
									}
								}, 15000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									/**
									 * 处理 run。
									 * Handle run.
									 */
									@Override
									public void run() {
										Npc Parsia = getNpc(209763);
										// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
										GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501314, Parsia.getObjectId(), 0, 0);
										// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
										GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501312, Parsia.getObjectId(), 0, 6000);
										// 让我炸开一条路…… / Just let me blast us a path...
										GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501310, Parsia.getObjectId(), 0, 12000);
									}
								}, 25000);
								GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
									/**
									 * 处理 run。
									 * Handle run.
									 */
									@Override
									public void run() {
										killNpc(getNpcs(731580));
										Npc Parsia = getNpc(209763);
										// 分遣队爆破手已打开通往下一区域的道路。 / Detachment Demolisher has opened the path to the next area.
										sendMsgByRace(1402689, Race.PC_ALL, 0);
										// 现在可以通过了。 / We can get through now.
										GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501311, Parsia.getObjectId(), 0, 0);
										// 请多保重。 / Please take care.
										GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501313, Parsia.getObjectId(), 0, 6000);
									}
								}, 30000);
							break;
						}
					}
				}
			break;
			case 236159: //Phantomscorch Bonerival.
			case 236162: //Elite Phantomscorch Chimera.
			case 236165: //Phantomscorch Contender.
			    Npc phantomscorchBonerival = instance.getNpc(236159); //Phantomscorch Bonerival.
			    Npc elitePhantomscorchChimera = instance.getNpc(236162); //Elite Phantomscorch Chimera.
				Npc phantomscorchContender = instance.getNpc(236165); //Phantomscorch Contender.
			    if (isDead(phantomscorchBonerival) &&
				    isDead(elitePhantomscorchChimera) &&
					isDead(phantomscorchContender)) {
					if (player != null) {
				        switch (player.getRace()) {
					        case ELYOS:
							    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							        /**
							         * 处理 run。
							         * Handle run.
							         */
							        @Override
								    public void run() {
								        // 奥里桑开始通过飞升压制升空。 / Orissan begins to Ascend through Ascension Dominance.
										sendMsgByRace(1402693, Race.PC_ALL, 0);
										// 奥里桑已通过飞升压制升空。 / Orissan has Ascended through Ascension Dominance.
										sendMsgByRace(1402694, Race.PC_ALL, 8000);
										// 在下一次飞升压制开始前击杀奥里桑。 / Slay Orissan before the next Ascension Dominance begins.
										sendMsgByRace(1402698, Race.PC_ALL, 16000);
										spawn(209704, 821.6019f, 523.7112f, 1706.6428f, (byte) 33);
										spawn(209705, 815.1346f, 522.75665f, 1706.7778f, (byte) 32);
								    }
							    }, 2000);
						    break;
						    case ASMODIANS:
							    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							        /**
							         * 处理 run。
							         * Handle run.
							         */
							        @Override
								    public void run() {
								        // 奥里桑开始通过飞升压制升空。 / Orissan begins to Ascend through Ascension Dominance.
										sendMsgByRace(1402693, Race.PC_ALL, 0);
										// 奥里桑已通过飞升压制升空。 / Orissan has Ascended through Ascension Dominance.
										sendMsgByRace(1402694, Race.PC_ALL, 8000);
										// 在下一次飞升压制开始前击杀奥里桑。 / Slay Orissan before the next Ascension Dominance begins.
										sendMsgByRace(1402698, Race.PC_ALL, 16000);
										spawn(209769, 821.6019f, 523.7112f, 1706.6428f, (byte) 33);
										spawn(209770, 815.1346f, 522.75665f, 1706.7778f, (byte) 32);
								    }
							    }, 2000);
						    break;
						}
					}
			    }
			break;
			case 236229: //Orissan.
			case 236232: //Reverted Orissan.
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							sendMovie(player, 914);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209706, 810.4212f, 550.19934f, 1701.044f, (byte) 31);
									spawn(209707, 818.40704f, 552.7704f, 1701.044f, (byte) 36);
								}
							}, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209710, 807.78894f, 578.7186f, 1701.0446f, (byte) 34);
									spawn(209710, 815.84827f, 579.7431f, 1701.0446f, (byte) 30);
									spawn(209711, 814.1693f, 588.4347f, 1701.0449f, (byte) 34);
									spawn(209711, 806.99536f, 587.9815f, 1701.0448f, (byte) 30);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209712, 811.5f, 583.0642f, 1701.0447f, (byte) 32);
									spawn(209713, 810.85767f, 588.2299f, 1701.0449f, (byte) 32);
									Npc PCGuard_Li = getNpc(209713);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li, 1402727, PCGuard_Li.getObjectId(), 0, 2000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li, 1402728, PCGuard_Li.getObjectId(), 0, 6000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li, 1402729, PCGuard_Li.getObjectId(), 0, 10000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Li, 1402730, PCGuard_Li.getObjectId(), 0, 14000);
								}
							}, 20000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									Npc Masionel = getNpc(209712);
									// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501314, Masionel.getObjectId(), 0, 0);
									// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501312, Masionel.getObjectId(), 0, 6000);
									// 让我炸开一条路…… / Just let me blast us a path...
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501310, Masionel.getObjectId(), 0, 12000);
								}
							}, 35000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									killNpc(getNpcs(700546));
									Npc Masionel = getNpc(209712);
									// 分遣队继续推进。 / The detachment continues to advance.
									sendMsgByRace(1403000, Race.PC_ALL, 0);
									// 在魂消迷宫滑翔以使用风道。 / Glide at the Soulfade Labyrinth to use the wind road.
									sendMsgByRace(1402941, Race.PC_ALL, 5000);
									// 现在可以通过了。 / We can get through now.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501311, Masionel.getObjectId(), 0, 0);
									// 请多保重。 / Please take care.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501313, Masionel.getObjectId(), 0, 6000);
								}
							}, 50000);
						break;
						case ASMODIANS:
							sendMovie(player, 914);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209771, 810.4212f, 550.19934f, 1701.044f, (byte) 31);
									spawn(209772, 818.40704f, 552.7704f, 1701.044f, (byte) 36);
								}
							}, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209775, 807.78894f, 578.7186f, 1701.0446f, (byte) 34);
									spawn(209775, 815.84827f, 579.7431f, 1701.0446f, (byte) 30);
									spawn(209776, 814.1693f, 588.4347f, 1701.0449f, (byte) 34);
									spawn(209776, 806.99536f, 587.9815f, 1701.0448f, (byte) 30);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209777, 811.5f, 583.0642f, 1701.0447f, (byte) 32);
									spawn(209778, 810.85767f, 588.2299f, 1701.0449f, (byte) 32);
									Npc PCGuard_Da = getNpc(209778);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da, 1402727, PCGuard_Da.getObjectId(), 0, 2000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da, 1402728, PCGuard_Da.getObjectId(), 0, 6000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da, 1402729, PCGuard_Da.getObjectId(), 0, 10000);
									GameFeatureServices.npcShoutsService().sendMsg(PCGuard_Da, 1402730, PCGuard_Da.getObjectId(), 0, 14000);
								}
							}, 20000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									Npc Parsia = getNpc(209777);
									// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501314, Parsia.getObjectId(), 0, 0);
									// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501312, Parsia.getObjectId(), 0, 6000);
									// 让我炸开一条路…… / Just let me blast us a path...
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501310, Parsia.getObjectId(), 0, 12000);
								}
							}, 35000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									killNpc(getNpcs(700546));
									Npc Parsia = getNpc(209777);
									// 分遣队继续推进。 / The detachment continues to advance.
									sendMsgByRace(1403000, Race.PC_ALL, 0);
									// 在魂消迷宫滑翔以使用风道。 / Glide at the Soulfade Labyrinth to use the wind road.
									sendMsgByRace(1402941, Race.PC_ALL, 5000);
									// 现在可以通过了。 / We can get through now.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501311, Parsia.getObjectId(), 0, 0);
									// 请多保重。 / Please take care.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501313, Parsia.getObjectId(), 0, 6000);
								}
							}, 50000);
						break;
				    }
				}
			break;
			case 236166: //Phantomscorch Bonerival.
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209722, 631.6413f, 847.15717f, 1599.8486f, (byte) 90);
									spawn(209722, 639.45526f, 847.40265f, 1599.9614f, (byte) 90);
								}
							}, 5000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									spawn(209720, 635.39325f, 886.9716f, 1600.7146f, (byte) 90);
									spawn(209721, 635.0717f, 901.48553f, 1600.49f, (byte) 89);
									spawn(209731, 640.8788f, 891.53687f, 1600.5566f, (byte) 94);
									spawn(209731, 629.2198f, 891.1963f, 1600.5817f, (byte) 92);
								}
							}, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawnIDSeal4ThStageElyos();
								}
							}, 12000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									spawnEmpyreanLordsSiegeWeapon();
									// 主神的风暴加农正在为天火风暴充能。 / The Empyrean Lord's Stormcannon is being charged for the Empyrean Firestorm.
									sendMsgByRace(1402703, Race.PC_ALL, 0);
									// 保护分遣队及其攻城武器免受古赫纳军团攻击。 / Defend the Detachment and its siege weapons from the Guhena Legion.
									sendMsgByRace(1402705, Race.PC_ALL, 5000);
									// 若分遣队损失过多士兵，将无法在对抗贝里特拉时协助。 / If the Detachment loses too many soldiers, they will not be able to assist during the battle against Beritra.
									sendMsgByRace(1402706, Race.PC_ALL, 10000);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									spawnWaveDoor();
									// 古赫纳军团发现入侵者，即将进攻。 / The Guhena Legion has detected intruders and will begin attacking.
									sendMsgByRace(1402704, Race.PC_ALL, 0);
								}
							}, 25000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal1();
									doors.get(271).setOpen(true);
									// 古赫纳军团指挥官维尔沙已出现。必须击败所有队长与指挥官。 / The Guhena Legion's Commander Virtsha has appeared. You must defeat every captain and commander.
									sendMsgByRace(1402710, Race.PC_ALL, 0);
								}
							}, 30000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal2();
									// 古赫纳军团第二波进攻开始。还将有三波。 / The Guhena Legion's second wave of attack has started. There will be three more attack waves.
									sendMsgByRace(1402707, Race.PC_ALL, 0);
								}
							}, 65000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    startRaidSeal2_1();
								}
							}, 67000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal3();
									doors.get(267).setOpen(true);
									// 古赫纳军团第三波进攻开始。还将有两波。 / The Guhena Legion's third wave of attack has started. There will be two more attack waves.
									sendMsgByRace(1402708, Race.PC_ALL, 0);
								}
							}, 87000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawn(209787, 631.6413f, 847.15717f, 1599.8486f, (byte) 90);
									spawn(209787, 639.45526f, 847.40265f, 1599.9614f, (byte) 90);
								}
							}, 5000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									spawn(209785, 635.39325f, 886.9716f, 1600.7146f, (byte) 90);
									spawn(209786, 635.0717f, 901.48553f, 1600.49f, (byte) 89);
									spawn(209796, 640.8788f, 891.53687f, 1600.5566f, (byte) 94);
									spawn(209796, 629.2198f, 891.1963f, 1600.5817f, (byte) 92);
								}
							}, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawnIDSeal4ThStageAsmodians();
								}
							}, 12000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									spawnEmpyreanLordsSiegeWeapon();
									// 主神的风暴加农正在为天火风暴充能。 / The Empyrean Lord's Stormcannon is being charged for the Empyrean Firestorm.
									sendMsgByRace(1402703, Race.PC_ALL, 0);
									// 保护分遣队及其攻城武器免受古赫纳军团攻击。 / Defend the Detachment and its siege weapons from the Guhena Legion.
									sendMsgByRace(1402705, Race.PC_ALL, 5000);
									// 若分遣队损失过多士兵，将无法在对抗贝里特拉时协助。 / If the Detachment loses too many soldiers, they will not be able to assist during the battle against Beritra.
									sendMsgByRace(1402706, Race.PC_ALL, 10000);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    spawnWaveDoor();
									// 古赫纳军团发现入侵者，即将进攻。 / The Guhena Legion has detected intruders and will begin attacking.
									sendMsgByRace(1402704, Race.PC_ALL, 0);
								}
							}, 25000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal1();
									doors.get(271).setOpen(true);
									// 古赫纳军团指挥官维尔沙已出现。必须击败所有队长与指挥官。 / The Guhena Legion's Commander Virtsha has appeared. You must defeat every captain and commander.
									sendMsgByRace(1402710, Race.PC_ALL, 0);
								}
							}, 30000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal2();
									// 古赫纳军团第二波进攻开始。还将有三波。 / The Guhena Legion's second wave of attack has started. There will be three more attack waves.
									sendMsgByRace(1402707, Race.PC_ALL, 0);
								}
							}, 65000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
								    startRaidSeal2_1();
								}
							}, 67000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal3();
									doors.get(267).setOpen(true);
									// 古赫纳军团第三波进攻开始。还将有两波。 / The Guhena Legion's third wave of attack has started. There will be two more attack waves.
									sendMsgByRace(1402708, Race.PC_ALL, 0);
								}
							}, 87000);
						break;
					}
				}
			break;
			case 236235: //Flamesquelch Command Destroyer.
				despawnNpc(npc);
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal4();
									startRaidSeal5();
									doors.get(7).setOpen(true);
									doors.get(310).setOpen(true);
									// 古赫纳军团第四波进攻开始。还将有一波。 / The Guhena Legion's fourth wave of attack has started. There will be one more attack wave.
									sendMsgByRace(1402709, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal4();
									startRaidSeal5();
									doors.get(7).setOpen(true);
									doors.get(310).setOpen(true);
									// 古赫纳军团第四波进攻开始。还将有一波。 / The Guhena Legion's fourth wave of attack has started. There will be one more attack wave.
									sendMsgByRace(1402709, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
					}
				}
			break;
			case 236236: //Flamesquelch Command Destroyer.
				despawnNpc(npc);
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal6();
									doors.get(210).setOpen(true);
									doors.get(312).setOpen(true);
									// 分遣队损失惨重，无法再提供协助。 / The Detachment has suffered severe losses and will not be able to assist any further.
									sendMsgByRace(1402712, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal6();
									doors.get(210).setOpen(true);
									doors.get(312).setOpen(true);
									// 分遣队损失惨重，无法再提供协助。 / The Detachment has suffered severe losses and will not be able to assist any further.
									sendMsgByRace(1402712, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
					}
				}
			break;
			case 236237: //Flamesquelch Command Sorcerer.
				despawnNpc(npc);
				if (player != null) {
				    switch (player.getRace()) {
					    case ELYOS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal3();
									startRaidSeal4();
									// 分遣队损失严重，仅能有限协助。 / The Detachment has suffered heavy losses and can only assist in limited capacity.
									sendMsgByRace(1402713, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
						case ASMODIANS:
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    /**
							     * 处理 run。
							     * Handle run.
							     */
							    @Override
								public void run() {
									startRaidSeal3();
									startRaidSeal4();
									// 分遣队损失严重，仅能有限协助。 / The Detachment has suffered heavy losses and can only assist in limited capacity.
									sendMsgByRace(1402713, Race.PC_ALL, 0);
								}
							}, 15000);
						break;
					}
				}
			break;
			case 236238: //Flamesquelch Command Burnsmark.
			    despawnNpc(npc);
				if (player != null) {
				    switch (player.getRace()) {
				        case ELYOS:
						    // 灭焰军团因指挥官阵亡而陷入混乱。 / The Flamesquelch Legion is in disaray from the loss of its commanders.
							sendMsgByRace(1403007, Race.PC_ALL, 0);
						    // 分遣队有所损失，但仍几乎可全力协助。 / The Detachment has suffered some losses, but can assist at almost full capacity.
							sendMsgByRace(1402714, Race.PC_ALL, 5000);
							// 你成功保护了分遣队。他们将在对抗贝里特拉时协助你。 / You have successfully protected the Detachment. They will assist you during the battle against Beritra.
							sendMsgByRace(1402715, Race.PC_ALL, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									Npc Masionel = getNpc(209720);
									// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501314, Masionel.getObjectId(), 0, 0);
									// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501312, Masionel.getObjectId(), 0, 6000);
									// 黑暗阻挡了道路。是时候使用主神的攻城武器了。 / Darkness blocks our path It is time to use the Empyrean Lord's siege weapon.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501318, Masionel.getObjectId(), 0, 12000);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									killNpc(getNpcs(700545));
									Npc Masionel = getNpc(209720);
									spawnEternalAltarOfTormentEntrance();
									// 主神的风暴加农已轰开黑暗封印。 / The Empyrean Lord's Stormcannon has blown open the Seal of Darkness.
									sendMsgByRace(1402711, Race.PC_ALL, 0);
									// 充能完成！开火！！ / Charge complete! Fire!!
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501320, Masionel.getObjectId(), 0, 0);
									// 谢谢。现在，让我们把战斗带给贝里特拉！ / Thank you. Now, let's bring the fight to Beritra!
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501321, Masionel.getObjectId(), 0, 6000);
								}
							}, 30000);
						break;
						case ASMODIANS:
						    // 灭焰军团因指挥官阵亡而陷入混乱。 / The Flamesquelch Legion is in disaray from the loss of its commanders.
							sendMsgByRace(1403007, Race.PC_ALL, 0);
						    // 分遣队有所损失，但仍几乎可全力协助。 / The Detachment has suffered some losses, but can assist at almost full capacity.
							sendMsgByRace(1402714, Race.PC_ALL, 5000);
							// 你成功保护了分遣队。他们将在对抗贝里特拉时协助你。 / You have successfully protected the Detachment. They will assist you during the battle against Beritra.
							sendMsgByRace(1402715, Race.PC_ALL, 10000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									Npc Parsia = getNpc(209785);
									// 多亏你，分遣队毫无损失通过。干得好！ / Thanks to you, the Detachment got through without any losses. Excellent work!
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501314, Parsia.getObjectId(), 0, 0);
									// 此地受黑暗力量保护，无法摧毁。 / This place is protected by a dark power. It cannot be destroyed.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501312, Parsia.getObjectId(), 0, 6000);
									// 黑暗阻挡了道路。是时候使用主神的攻城武器了。 / Darkness blocks our path It is time to use the Empyrean Lord's siege weapon.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501318, Parsia.getObjectId(), 0, 12000);
								}
							}, 15000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									killNpc(getNpcs(700545));
									Npc Parsia = getNpc(209785);
									spawnEternalAltarOfTormentEntrance();
									// 主神的风暴加农已轰开黑暗封印。 / The Empyrean Lord's Stormcannon has blown open the Seal of Darkness.
									sendMsgByRace(1402711, Race.PC_ALL, 0);
									// 充能完成！开火！！ / Charge complete! Fire!!
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501320, Parsia.getObjectId(), 0, 0);
									// 谢谢。现在，让我们把战斗带给贝里特拉！ / Thank you. Now, let's bring the fight to Beritra!
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501321, Parsia.getObjectId(), 0, 6000);
								}
							}, 30000);
						break;
					}
				}
			break;
			case 236204: //Flamesquelch Destoyer.
			case 236205: //Flamesquelch Burnsmark.
			case 236206: //Flamesquelch Rangerblaze.
			case 236216: //Elite Flamesquelch Destroyer.
			case 236217: //Elite Flamesquelch Burnsmark.
			case 236218: //Elite Flamesquelch Rangerblaze.
			case 236219: //Elite Flamesquelch Sorcerer.
			case 236220: //Elite Flamesquelch Extinguisher.
			    despawnNpc(npc);
			break;
			case 855460: //Drakenspire Protector.
			case 855461: //Drakenspire Protector.
			case 855462: //Drakenspire Protector.
			case 855463: //Drakenspire Protector.
			case 855464: //Drakenspire Protector.
			case 855465: //Drakenspire Protector.
				Npc beritra1 = instance.getNpc(236244); //Beritra.
				Npc beritra2 = instance.getNpc(236245); //Enraged Beritra.
				Npc beritra3 = instance.getNpc(236246); //Crazed Beritra.
				drakenspireProtector++;
				if (beritra1 != null) {
				    if (drakenspireProtector == 2) {
						beritra1.getEffectController().removeEffect(21610); //Dark Affinity.
					} else if (drakenspireProtector == 4) {
						beritra1.getEffectController().removeEffect(21611); //Wall Of Blades.
					} else if (drakenspireProtector == 6) {
						deleteNpc(236244); //Beritra.
						killNpc(getNpcs(702695));
						killNpc(getNpcs(702697));
						killNpc(getNpcs(702699));
						beritra1.getEffectController().removeEffect(21612); //Everlasting Life.
						spawn(236247, 127.77517f, 508.3428f, 1749.8322f, (byte) 8); //Beritra [Dragon Form]
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
									startDrakenspireTimer();
									PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 420)); //7 Minutes.
								}
							}
						});
					}
				} else if (beritra2 != null) {
				    if (drakenspireProtector == 2) {
						beritra2.getEffectController().removeEffect(21610); //Dark Affinity.
					} else if (drakenspireProtector == 4) {
						beritra2.getEffectController().removeEffect(21611); //Wall Of Blades.
					} else if (drakenspireProtector == 6) {
						deleteNpc(236245); //Enraged Beritra.
						killNpc(getNpcs(702695));
						killNpc(getNpcs(702697));
						killNpc(getNpcs(702699));
						beritra2.getEffectController().removeEffect(21612); //Everlasting Life.
						spawn(236247, 127.77517f, 508.3428f, 1749.8322f, (byte) 8); //Beritra [Dragon Form]
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
									startDrakenspireTimer();
									PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 420)); //7 Minutes.
								}
							}
						});
					}
				} else if (beritra3 != null) {
				    if (drakenspireProtector == 2) {
						beritra3.getEffectController().removeEffect(21610); //Dark Affinity.
					} else if (drakenspireProtector == 4) {
						beritra3.getEffectController().removeEffect(21611); //Wall Of Blades.
					} else if (drakenspireProtector == 6) {
						deleteNpc(236246); //Crazed Beritra.
						killNpc(getNpcs(702695));
						killNpc(getNpcs(702697));
						killNpc(getNpcs(702699));
						beritra3.getEffectController().removeEffect(21612); //Everlasting Life.
						spawn(236247, 127.77517f, 508.3428f, 1749.8322f, (byte) 8); //Beritra [Dragon Form]
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
									startDrakenspireTimer();
									PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 420)); //7 Minutes.
								}
							}
						});
					}
				}
				despawnNpc(npc);
			break;
			/**
	 * 若玩家击杀贝里特拉将移出副本！须先移除 3 枚封印再战龙形态。 / If player kill <Beritra> they are removed from instance!!! Coz, player must removed <3 Seal> before fight "Beritra Dragon Form"
	 */
			case 236244: //Beritra.
			case 236245: //Enraged Beritra.
			case 236246: //Crazed Beritra.
			    despawnNpc(npc);
				sendMsg("[EPIC FAIL]: You should not kill <Beritra>, you had to remove the 3 seal :( ");
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						if (!isInstanceDestroyed) {
							for (Player player: instance.getPlayersInside()) {
								onExitInstance(player);
							}
						}
					}
				}, 10000);
			break;
			case 236247: //Beritra [Dragon Form]
				despawnNpc(npc);
			    if (player != null) {
				    switch (player.getRace()) {
				        case ELYOS:
						    sendMovie(player, 916);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									spawnIDSealSceneEnding();
									drakenspireTask.cancel(true);
									// 黑暗封印已被摧毁。 / The Seal of Darkness has been destroyed.
									sendMsgByRace(1403008, Race.PC_ALL, 0);
									// 代理人的武器内部正在发生反应。 / A reaction is taking place within the agent's weapon.
									sendMsgByRace(1403012, Race.PC_ALL, 5000);
									sendMsg("[Congratulation]: you finish <Drakenspire Depths>");
									spawn(731548, 147.01088f, 517.9374f, 1749.5007f, (byte) 2); //Drakenspire Depths Exit.
									spawn(702769, 152.10716f, 518.6436f, 1749.5945f, (byte) 68); //Ominous Darkness.
									spawn(731578, 152.10033f, 518.63507f, 1749.605f, (byte) 0, 211);
									spawn(833012, 152.30666f, 522.0088f, 1749.5466f, (byte) 88); //Cloak Of Balaur Lord Beritra.
									spawn(833015, 161.93251f, 518.47626f, 1749.4482f, (byte) 59); //Sacred Beast Of Balaur Lord Beritra.
									Npc Masionel = getNpc(209739);
									// 贝里特拉或许逃脱了，但我们已拿下龙脊深渊。干得好。 / Beritra may have gotten away, but we've taken Drakenspire Depths. You've done an excellent job.
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501326, Masionel.getObjectId(), 0, 12000);
									// 贝里特拉逃走了！他或许逃脱，但我们给他留下了深刻印象！ / Beritra has fled! He may have escaped, but we gave him something to remember us by!
									GameFeatureServices.npcShoutsService().sendMsg(Masionel, 1501327, Masionel.getObjectId(), 0, 22000);
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
								}
							}, 10000);
						break;
						case ASMODIANS:
						    sendMovie(player, 916);
						    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/**
								 * 处理 run。
								 * Handle run.
								 */
								@Override
								public void run() {
									spawnIDSealSceneEnding();
									drakenspireTask.cancel(true);
									// 黑暗封印已被摧毁。 / The Seal of Darkness has been destroyed.
									sendMsgByRace(1403008, Race.PC_ALL, 0);
									// 代理人的武器内部正在发生反应。 / A reaction is taking place within the agent's weapon.
									sendMsgByRace(1403012, Race.PC_ALL, 5000);
									sendMsg("[Congratulation]: you finish <Drakenspire Depths>");
									spawn(731548, 147.01088f, 517.9374f, 1749.5007f, (byte) 2); //Drakenspire Depths Exit.
									spawn(702769, 152.10716f, 518.6436f, 1749.5945f, (byte) 68); //Ominous Darkness.
									spawn(731578, 152.10033f, 518.63507f, 1749.605f, (byte) 0, 211);
									spawn(833012, 152.30666f, 522.0088f, 1749.5466f, (byte) 88); //Cloak Of Balaur Lord Beritra.
									spawn(833015, 161.93251f, 518.47626f, 1749.4482f, (byte) 59); //Sacred Beast Of Balaur Lord Beritra.
									Npc Parsia = getNpc(209804);
									// 贝里特拉或许逃脱了，但我们已拿下龙脊深渊。干得好。 / Beritra may have gotten away, but we've taken Drakenspire Depths. You've done an excellent job.
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501326, Parsia.getObjectId(), 0, 12000);
									// 贝里特拉逃走了！他或许逃脱，但我们给他留下了深刻印象！ / Beritra has fled! He may have escaped, but we gave him something to remember us by!
									GameFeatureServices.npcShoutsService().sendMsg(Parsia, 1501327, Parsia.getObjectId(), 0, 22000);
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
								}
							}, 10000);
						break;
					}
				}
			break;
		}
	}
	
	private void startDrakenspireTimer() {
		// 贝里特拉变身为龙。 / Beritra transforms into a dragon.
		sendMsgByRace(1402721, Race.PC_ALL, 0);
		// 遗物将在 7 分钟后提取完毕，贝里特拉将消失。 / Beritra will disappear when the relic is completely extracted in 7 minutes.
		sendMsgByRace(1402722, Race.PC_ALL, 10000);
		// 遗物即将提取完毕，贝里特拉将消失。 / Beritra will disappear when the relic is completely extracted in a moment.
		sendMsgByRace(1402726, Race.PC_ALL, 390000);
		// 遗物将在 5 分钟后提取完毕，贝里特拉将消失。 / Beritra will disappear when the relic is completely extracted in 5 minutes.
		this.sendMessage(1402724, 2 * 60 * 1000);
		// 遗物将在 1 分钟后提取完毕，贝里特拉将消失。 / Beritra will disappear when the relic is completely extracted in 1 minute.
		this.sendMessage(1402725, 6 * 60 * 1000);
		// 龙主遗物提取完成，贝里特拉已消失。 / The extraction of the Balaur Lord's Relic is complete and Beritra has disappeared.
		this.sendMessage(1402720, 7 * 60 * 1000);
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
				    drakenspireTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
									onExitInstance(player);
								}
							});
							onInstanceDestroy();
						}
					}, 420000); //7 Min.
				}
			}
		});
    }
	
	private void spawnIDSealScene01() {
		// 与盟友一起挺进龙脊深渊。 / Advance into Drakenspire Depths with your allies.
		sendMsgByRace(1402991, Race.PC_ALL, 10000);
		// 选择前进路径。 / Choose a path to proceed.
		sendMsgByRace(1402992, Race.PC_ALL, 60000);
        final int pcGuard1 = sealSceneRace == Race.ASMODIANS ? 209744 : 209679;
		final int pcGuard2 = sealSceneRace == Race.ASMODIANS ? 209744 : 209679;
        final int pcGuard3 = sealSceneRace == Race.ASMODIANS ? 209745 : 209680;
        final int pcGuard4 = sealSceneRace == Race.ASMODIANS ? 209746 : 209681;
		spawn(pcGuard1, 353.3932f, 185.61818f, 1684.2164f, (byte) 1);
		spawn(pcGuard2, 353.4143f, 179.24579f, 1684.2164f, (byte) 0);
        spawn(pcGuard3, 349.18713f, 192.32655f, 1684.2164f, (byte) 119);
        spawn(pcGuard4, 349.34637f, 173.6827f, 1684.2164f, (byte) 1);
    }
	
	private void spawnIDSealSceneEnding() {
        final int IDSealSceneEndingQuestNPC = sealSceneRace == Race.ASMODIANS ? 209804 : 209739; //Parsia/Masionel.
		final int IDSealSceneEndingPCGuard1 = sealSceneRace == Race.ASMODIANS ? 209807 : 209742;
        final int IDSealSceneEndingPCGuard2 = sealSceneRace == Race.ASMODIANS ? 209807 : 209742;
        final int IDSealSceneEndingPCGuard3 = sealSceneRace == Race.ASMODIANS ? 209807 : 209742;
		final int IDSealSceneEndingPCGuard4 = sealSceneRace == Race.ASMODIANS ? 209807 : 209742;
		spawn(IDSealSceneEndingQuestNPC, 156.73125f, 518.6457f, 1749.5138f, (byte) 0);
		spawn(IDSealSceneEndingPCGuard1, 155.51422f, 514.9752f, 1749.5044f, (byte) 106);
        spawn(IDSealSceneEndingPCGuard2, 155.3807f, 521.9788f, 1749.5118f, (byte) 15);
        spawn(IDSealSceneEndingPCGuard3, 148.80602f, 521.83466f, 1749.516f, (byte) 45);
		spawn(IDSealSceneEndingPCGuard4, 148.60063f, 515.0635f, 1749.5034f, (byte) 75);
    }
	
	private void spawnEmpyreanLordsSiegeWeapon() {
		final int empyreanLordsSiegeWeapon = sealSceneRace == Race.ASMODIANS ? 702720 : 702719; //Empyrean Lord's Siege Weapon.
		spawn(empyreanLordsSiegeWeapon, 635.24457f, 890.4639f, 1600.5914f, (byte) 90);
    }
	
	private void spawnEternalAltarOfTormentEntrance() {
		SpawnTemplate EnvSkyBoxObject = SpawnEngine.addNewSingleTimeSpawn(301390000, 804697, 635.69067f, 959.46039f, 1615.0714f, (byte) 0);
		EnvSkyBoxObject.setEntityId(50);
		objects.put(804697, SpawnEngine.spawnObject(EnvSkyBoxObject, instanceId));
    }
	
	private void spawnWaveDoor() {
	    SpawnTemplate AionFXPostGlow = SpawnEngine.addNewSingleTimeSpawn(301390000, 731581, 635.3889f, 784.05261f, 1596.7184f, (byte) 0);
		AionFXPostGlow.setEntityId(548);
		objects.put(731581, SpawnEngine.spawnObject(AionFXPostGlow, instanceId));
	}
	
	private void moveToSealForward(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	private void spawnIDSeal4ThStageElyos() {
		moveToSealForward((Npc)spawn(209722, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 632.21173f, 887.62164f, 1600.698f, false);
		moveToSealForward((Npc)spawn(209722, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 638.4854f, 889.0385f, 1600.6517f, false);
		moveToSealForward((Npc)spawn(209722, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 638.0168f, 896.49756f, 1600.4114f, false);
		moveToSealForward((Npc)spawn(209722, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 631.50134f, 895.16174f, 1600.5238f, false);
	}
	
	private void spawnIDSeal4ThStageAsmodians() {
		moveToSealForward((Npc)spawn(209787, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 632.21173f, 887.62164f, 1600.698f, false);
		moveToSealForward((Npc)spawn(209787, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 638.4854f, 889.0385f, 1600.6517f, false);
		moveToSealForward((Npc)spawn(209787, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 638.0168f, 896.49756f, 1600.4114f, false);
		moveToSealForward((Npc)spawn(209787, 636.07764f, 846.96954f, 1599.9142f, (byte) 30), 631.50134f, 895.16174f, 1600.5238f, false);
	}
	
	private void raidSeal(final Npc npc) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player: instance.getPlayersInside()) {
						npc.setTarget(player);
						((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
						npc.setState(1);
						npc.getMoveController().moveToTargetObject();
						PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
					}
				}
			}
		}, 1000);
	}
	/**
	 * 处理 startRaidSeal1。
	 * Handle startRaidSeal1.
	 */
	
	public void startRaidSeal1() {
	    raidSeal((Npc)spawn(236204, 632.9971f, 788.14307f, 1596.5493f, (byte) 28));
		raidSeal((Npc)spawn(236204, 637.02356f, 787.7114f, 1596.4082f, (byte) 29));
		raidSeal((Npc)spawn(236205, 686.8446f, 823.1334f, 1610.0796f, (byte) 46));
		raidSeal((Npc)spawn(236205, 689.8247f, 826.4868f, 1610.1107f, (byte) 46));
	}
	/**
	 * 处理 startRaidSeal2。
	 * Handle startRaidSeal2.
	 */
	
	public void startRaidSeal2() {
	    raidSeal((Npc)spawn(236206, 632.9971f, 788.14307f, 1596.5493f, (byte) 28));
		raidSeal((Npc)spawn(236206, 637.02356f, 787.7114f, 1596.4082f, (byte) 29));
		raidSeal((Npc)spawn(236235, 635.31866f, 789.59717f, 1596.6062f, (byte) 30));
	}
	/**
	 * 处理 startRaidSeal2_1。
	 * Handle startRaidSeal2_1.
	 */
	
	public void startRaidSeal2_1() {
		raidSeal((Npc)spawn(236206, 632.9971f, 788.14307f, 1596.5493f, (byte) 28));
		raidSeal((Npc)spawn(236206, 637.02356f, 787.7114f, 1596.4082f, (byte) 29));
		raidSeal((Npc)spawn(236206, 635.31866f, 789.59717f, 1596.6062f, (byte) 30));
	}
	/**
	 * 处理 startRaidSeal3。
	 * Handle startRaidSeal3.
	 */
	
	public void startRaidSeal3() {
	    raidSeal((Npc)spawn(236216, 686.8446f, 823.1334f, 1610.0796f, (byte) 46));
		raidSeal((Npc)spawn(236216, 689.8247f, 826.4868f, 1610.1107f, (byte) 46));
		raidSeal((Npc)spawn(236216, 579.17377f, 823.6274f, 1609.9344f, (byte) 14));
		raidSeal((Npc)spawn(236216, 582.5257f, 820.4343f, 1609.9154f, (byte) 15));
	}
	/**
	 * 处理 startRaidSeal4。
	 * Handle startRaidSeal4.
	 */
	
	public void startRaidSeal4() {
		raidSeal((Npc)spawn(236220, 632.9971f, 788.14307f, 1596.5493f, (byte) 28));
		raidSeal((Npc)spawn(236220, 637.02356f, 787.7114f, 1596.4082f, (byte) 29));
		raidSeal((Npc)spawn(236220, 635.31866f, 789.59717f, 1596.6062f, (byte) 30));
	}
	/**
	 * 处理 startRaidSeal5。
	 * Handle startRaidSeal5.
	 */
	
	public void startRaidSeal5() {
		raidSeal((Npc)spawn(236218, 574.2141f, 879.9431f, 1600.7627f, (byte) 0));
		raidSeal((Npc)spawn(236218, 574.22955f, 875.5384f, 1601.1173f, (byte) 0));
		raidSeal((Npc)spawn(236219, 703.2663f, 874.8648f, 1604.521f, (byte) 59));
		raidSeal((Npc)spawn(236219, 703.3903f, 880.198f, 1604.7985f, (byte) 62));
		raidSeal((Npc)spawn(236236, 702.1438f, 877.7707f, 1604.4375f, (byte) 59));
	}
	/**
	 * 处理 startRaidSeal6。
	 * Handle startRaidSeal6.
	 */
	
	public void startRaidSeal6() {
		raidSeal((Npc)spawn(236217, 576.9567f, 939.24054f, 1620.987f, (byte) 104));
		raidSeal((Npc)spawn(236217, 573.4911f, 935.99945f, 1621.0607f, (byte) 104));
		raidSeal((Npc)spawn(236237, 575.50415f, 937.4111f, 1620.9528f, (byte) 106));
		raidSeal((Npc)spawn(236217, 687.77435f, 933.2536f, 1617.8989f, (byte) 68));
		raidSeal((Npc)spawn(236217, 689.8956f, 929.0121f, 1617.4075f, (byte) 68));
		raidSeal((Npc)spawn(236238, 688.66656f, 931.058f, 1617.5339f, (byte) 72));
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
	
	private void sendMessage(final int msgId, long delay) {
        if (delay == 0) {
            this.sendMsg(msgId);
        } else {
            GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
                /**
                 * 处理 run。
                 * Handle run.
                 */
                public void run() {
                    sendMsg(msgId);
                }
            }, delay);
        }
    }
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	protected Npc getNpc(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpc(npcId);
		}
		return null;
	}
	
	protected void killNpc(List<Npc> npcs) {
        for (Npc npc: npcs) {
            npc.getController().die();
        }
    }
	
	protected List<Npc> getNpcs(int npcId) {
		if (!isInstanceDestroyed) {
			return instance.getNpcs(npcId);
		}
		return null;
	}
	
	private boolean isDead(Npc npc) {
		return (npc == null || npc.getLifeStats().isAlreadyDead());
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		doors.clear();
		isInstanceDestroyed = true;
	}
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
}