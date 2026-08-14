package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 拉克桑遗迹副本事件处理器。
 * Instance event handler for Raksang Ruins.
 *
 * @author Encom
 */

@InstanceID(300610000)
public class RaksangRuinsInstance extends GeneralInstanceHandler {

	// 恐怖宝库突袭 / Terror's Vault Raid
		/** raksangraid 任务 a1 / raksang raid task a1 */
		private Future<?> raksangRaidTaskA1;
		/** raksangraid 任务 a2 / raksang raid task a2 */
		private Future<?> raksangRaidTaskA2;
		/** raksha solo spakle a161an / raksha solo spakle a161an */
		private int rakshaSoloSpakleA161An;
		/** raksha solo skeleton s61an / raksha solo skeleton s61an */
		private int rakshaSoloSkeletonS61An;
		/** raksha solo grave witch sn61an / raksha solo grave witch sn61an */
		private int rakshaSoloGraveWitchSN61An;
	// 苦痛熔炉突袭 / Torment's Forge Raid
		/** raksangraid 任务 b1 / raksang raid task b1 */
		private Future<?> raksangRaidTaskB1;
		/** raksangraid 任务 b2 / raksang raid task b2 */
		private Future<?> raksangRaidTaskB2;
		/** raksha solo skeleton b161an / raksha solo skeleton b161an */
		private int rakshaSoloSkeletonB161An;
		/** raksha solo skeleton b261an / raksha solo skeleton b261an */
		private int rakshaSoloSkeletonB261An;
	// 地狱之路突袭 / Hellpath Raid
		/** raksangraid 任务 c1 / raksang raid task c1 */
		private Future<?> raksangRaidTaskC1;
		/** raksangraid 任务 c2 / raksang raid task c2 */
		private Future<?> raksangRaidTaskC2;
		/** raksha solo clodworm c161an / raksha solo clodworm c161an */
		private int rakshaSoloClodwormC161An;
		/** raksha solo clodworm c261an / raksha solo clodworm c261an */
		private int rakshaSoloClodwormC261An;
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	
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
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
        super.onEnterInstance(player); 
		if (spawnRace == null) {
			spawnRace = player.getRace();
			SpawnAbisoRace();
		}
    }
	
	private void SpawnAbisoRace() {
		final int abiso1 = spawnRace == Race.ASMODIANS ? 206395 : 206378;
        final int abiso2 = spawnRace == Race.ASMODIANS ? 206396 : 206379;
		final int abiso3 = spawnRace == Race.ASMODIANS ? 206397 : 206380;
		switch (Rnd.get(1, 3)) {
		    case 1:
				spawn(abiso1, 817.48f, 927.9041f, 1207.4312f, (byte) 19);
			break;
			case 2:
				spawn(abiso2, 817.48f, 927.9041f, 1207.4312f, (byte) 19);
			break;
			case 3:
				spawn(abiso3, 817.48f, 927.9041f, 1207.4312f, (byte) 19);
			break;
		}
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
			//http://aion.power.plaync.com/wiki/%EB%A6%AC%EB%A9%98%ED%88%AC+-+%EB%93%9C%EB%A1%AD+%EC%95%84%EC%9D%B4%ED%85%9C
			case 236306: //Reviver Nasto.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
				switch (Rnd.get(1, 7)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053678, 1)); //Nasto's Unique Weapon Box.
				    break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053679, 1)); //Nasto's Unique Jacket Box.
				    break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053680, 1)); //Nasto's Unique Pants Box.
				    break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053681, 1)); //Nasto's Unique Pauldrons Box.
				    break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053682, 1)); //Nasto's Unique Gloves Box.
				    break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053683, 1)); //Nasto's Unique Shoes Chest.
				    break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053684, 1)); //Nasto's Hero Accessory Box.
				    break;
				} switch (Rnd.get(1, 5)) {
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
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc NPC / npc
	 */
	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
		    case 236010: //Trained Porgus.
			    rakshaSoloSpakleA161An++;
				if (rakshaSoloSpakleA161An == 3) {
					startRaksangRaidA1Bis();
					//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
					sendMsgByRace(1402832, Race.PC_ALL, 0);
				} else if (rakshaSoloSpakleA161An == 6) {
				   startRaksangRaidA2();
				   raksangRaidTaskA1.cancel(true);
				   //再坚持一下就能活下来。 / Hold a little longer and you will survive.
				   sendMsgByRace(1402833, Race.PC_ALL, 0);
				   //只剩少数敌人！ / Only a few enemies left!
				   sendMsgByRace(1402834, Race.PC_ALL, 5000);
				}
			break;
			case 236012: //Crumbling Skelesword.
			    rakshaSoloSkeletonS61An++;
				if (rakshaSoloSkeletonS61An == 4) {
					startRaksangRaidA2Bis();
					//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
					sendMsgByRace(1402832, Race.PC_ALL, 0);
				}
			break;
			case 236014: //Ragelich Adept.
			    rakshaSoloGraveWitchSN61An++;
				if (rakshaSoloGraveWitchSN61An == 4) {
					raksangRaidTaskA2.cancel(true);
					// 使用已开启入口前往下一区域。 / Use the open entrance to move to the next area.
					sendMsgByRace(1402781, Race.PC_ALL, 0);
				}
			break;
			case 236019: //Trained Lava Petrahulk.
				hellpathFirstWave();
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402785, Race.PC_ALL, 0);
			break;
			case 236020: //Trained Clodworm.
			    rakshaSoloClodwormC161An++;
				if (rakshaSoloClodwormC161An == 6) {
					raksangRaidTaskC1.cancel(true);
					doors.get(107).setOpen(true);
				}
			break;
			case 236074: //Crumbling Skeleton.
			    rakshaSoloSkeletonB161An++;
				if (rakshaSoloSkeletonB161An == 6) {
					raksangRaidTaskB1.cancel(true);
					doors.get(457).setOpen(true);
				}
			break;
			case 236077: //Crumbling Skeleton.
			    rakshaSoloSkeletonB261An++;
				if (rakshaSoloSkeletonB261An == 6) {
					doors.get(64).setOpen(true);
					raksangRaidTaskB2.cancel(true);
					// 使用已开启入口前往下一区域。 / Use the open entrance to move to the next area.
					sendMsgByRace(1402784, Race.PC_ALL, 0);
				}
			break;
			case 236084: //Classified Drill Camp Instructor.
				startRaksangRaidA1();
				doors.get(307).setOpen(true);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402780, Race.PC_ALL, 0);
				// 门尚无法打开。 / The door cannot be opened yet.
				sendMsgByRace(1402831, Race.PC_ALL, 10000);
			break;
			case 236096: //Trained Clodworm.
			    rakshaSoloClodwormC261An++;
				if (rakshaSoloClodwormC261An == 4) {
					doors.get(324).setOpen(true);
					raksangRaidTaskC2.cancel(true);
					// 使用已开启入口前往下一区域。 / Use the open entrance to move to the next area.
					sendMsgByRace(1402786, Race.PC_ALL, 0);
				}
			break;
			case 236303: //Drill Instructor Diplito.
				doors.get(294).setOpen(true);
			break;
			case 236304: //Drill Instructor Pratica.
				doors.get(118).setOpen(true);
			break;
			case 236305: //Drill Instructor Exico.
				hellpathSecondWave();
			break;
			case 236306: //Reviver Nasto.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Raksang Ruins>");
				spawn(730445, 648.5508f, 700.05725f, 522.0487f, (byte) 80); //Raksang Exit.
			break;
		}
	}
	
	/**
	 * Terror's Vault Raid A1/A2
	 */
	private void startRaksangRaidA1() {
		raksangRaidTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
		        raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
			}
		}, 20000);
	}
	private void startRaksangRaidA1Bis() {
		raksangRaidTaskA1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
		        raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236010, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236011, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
			}
		}, 20000);
	}
	private void startRaksangRaidA2() {
		raksangRaidTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
		        raksangRaid((Npc)spawn(236012, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236013, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236014, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236012, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236013, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236014, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
			}
		}, 20000);
	}
	private void startRaksangRaidA2Bis() {
		raksangRaidTaskA2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
		        raksangRaid((Npc)spawn(236012, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236013, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236014, 581.06055f, 224.19353f, 927.9906f, (byte) 42));
				raksangRaid((Npc)spawn(236012, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236013, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
				raksangRaid((Npc)spawn(236014, 596.07947f, 241.60663f, 927.9906f, (byte) 57));
			}
		}, 20000);
	}
	
	/**
	 * Torment's Forge Raid B1/B2
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 702673: //Tombstone Of Liberation.
				despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB1();
				    }
			    }, 5000);
				raksangRaidTaskB1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB1Bis();
				    }
			    }, 60000);
			break;
			case 702674: //Tombstone Of Liberation.
			    despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB2();
				    }
			    }, 5000);
				raksangRaidTaskB1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB2Bis();
				    }
			    }, 60000);
			break;
			case 702675: //Tombstone Of Liberation.
			    despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB3();
				    }
			    }, 5000);
				raksangRaidTaskB1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB3Bis();
				    }
			    }, 60000);
			break;
			case 702690: //Tombstone Of Liberation.
				despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB4();
				    }
			    }, 5000);
				raksangRaidTaskB2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB4Bis();
				    }
			    }, 60000);
			break;
			case 702691: //Tombstone Of Liberation.
				despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB6();
				    }
			    }, 5000);
				raksangRaidTaskB2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB6Bis();
				    }
			    }, 60000);
			break;
			case 702692: //Tombstone Of Liberation.
				despawnNpc(npc);
				// 开关现已可用。 / The switch is now operational.
				sendMsgByRace(1402782, Race.PC_ALL, 0);
				//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
				sendMsgByRace(1402783, Race.PC_ALL, 4000);
				//再坚持一下就能活下来。 / Hold a little longer and you will survive.
				sendMsgByRace(1402833, Race.PC_ALL, 30000);
				//只剩少数敌人！ / Only a few enemies left!
				sendMsgByRace(1402834, Race.PC_ALL, 50000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB5();
				    }
			    }, 5000);
				raksangRaidTaskB2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startRaksangRaidB5Bis();
				    }
			    }, 60000);
			break;
		}
	}
	
	/**
	 * Torment's Forge Raid B
	 */
	public void startRaksangRaidB1() {
	    raksangRaid((Npc)spawn(236074, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
		raksangRaid((Npc)spawn(236075, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
		raksangRaid((Npc)spawn(236076, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
	}
	/**
	 * 处理 startRaksangRaidB1Bis。
	 * Handle startRaksangRaidB1Bis.
	 */
	public void startRaksangRaidB1Bis() {
	    raksangRaid((Npc)spawn(236074, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
		raksangRaid((Npc)spawn(236075, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
		raksangRaid((Npc)spawn(236076, 963.0322f, 791.4068f, 734.0461f, (byte) 53));
	}
	/**
	 * 处理 startRaksangRaidB2。
	 * Handle startRaksangRaidB2.
	 */
	public void startRaksangRaidB2() {
	    raksangRaid((Npc)spawn(236074, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
		raksangRaid((Npc)spawn(236075, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
		raksangRaid((Npc)spawn(236076, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
	}
	/**
	 * 处理 startRaksangRaidB2Bis。
	 * Handle startRaksangRaidB2Bis.
	 */
	public void startRaksangRaidB2Bis() {
	    raksangRaid((Npc)spawn(236074, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
		raksangRaid((Npc)spawn(236075, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
		raksangRaid((Npc)spawn(236076, 962.4403f, 775.7848f, 734.05475f, (byte) 38));
	}
	/**
	 * 处理 startRaksangRaidB3。
	 * Handle startRaksangRaidB3.
	 */
	public void startRaksangRaidB3() {
	    raksangRaid((Npc)spawn(236074, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
		raksangRaid((Npc)spawn(236075, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
		raksangRaid((Npc)spawn(236076, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
	}
	/**
	 * 处理 startRaksangRaidB3Bis。
	 * Handle startRaksangRaidB3Bis.
	 */
	public void startRaksangRaidB3Bis() {
	    raksangRaid((Npc)spawn(236074, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
		raksangRaid((Npc)spawn(236075, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
		raksangRaid((Npc)spawn(236076, 941.6077f, 774.7897f, 734.0187f, (byte) 30));
	}
	/**
	 * 处理 startRaksangRaidB4。
	 * Handle startRaksangRaidB4.
	 */
	public void startRaksangRaidB4() {
	    raksangRaid((Npc)spawn(236077, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
		raksangRaid((Npc)spawn(236078, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
		raksangRaid((Npc)spawn(236079, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
	}
	/**
	 * 处理 startRaksangRaidB4Bis。
	 * Handle startRaksangRaidB4Bis.
	 */
	public void startRaksangRaidB4Bis() {
	    raksangRaid((Npc)spawn(236077, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
		raksangRaid((Npc)spawn(236078, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
		raksangRaid((Npc)spawn(236079, 989.6738f, 877.95856f, 762.55774f, (byte) 8));
	}
	/**
	 * 处理 startRaksangRaidB5。
	 * Handle startRaksangRaidB5.
	 */
	public void startRaksangRaidB5() {
	    raksangRaid((Npc)spawn(236077, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
		raksangRaid((Npc)spawn(236078, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
		raksangRaid((Npc)spawn(236079, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
	}
	/**
	 * 处理 startRaksangRaidB5Bis。
	 * Handle startRaksangRaidB5Bis.
	 */
	public void startRaksangRaidB5Bis() {
	    raksangRaid((Npc)spawn(236077, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
		raksangRaid((Npc)spawn(236078, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
		raksangRaid((Npc)spawn(236079, 995.08215f, 899.61633f, 762.55774f, (byte) 102));
	}
	/**
	 * 处理 startRaksangRaidB6。
	 * Handle startRaksangRaidB6.
	 */
	public void startRaksangRaidB6() {
	    raksangRaid((Npc)spawn(236077, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
		raksangRaid((Npc)spawn(236078, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
		raksangRaid((Npc)spawn(236079, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
	}
	/**
	 * 处理 startRaksangRaidB6Bis。
	 * Handle startRaksangRaidB6Bis.
	 */
	public void startRaksangRaidB6Bis() {
	    raksangRaid((Npc)spawn(236077, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
		raksangRaid((Npc)spawn(236078, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
		raksangRaid((Npc)spawn(236079, 1006.8747f, 894.7426f, 762.55774f, (byte) 81));
	}
	
	/**
	 * Hellpath Raid C1
	 */
	private void hellpathFirstWave() {
		//再坚持一下就能活下来。 / Hold a little longer and you will survive.
		sendMsgByRace(1402833, Race.PC_ALL, 60000);
		//只剩少数敌人！ / Only a few enemies left!
		sendMsgByRace(1402834, Race.PC_ALL, 110000);
		raksangRaidTaskC1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				raksangRaid((Npc)spawn(236020, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
				raksangRaid((Npc)spawn(236020, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
				raksangRaid((Npc)spawn(236020, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
		        raksangRaid((Npc)spawn(236021, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
		        raksangRaid((Npc)spawn(236021, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
				raksangRaid((Npc)spawn(236021, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
			}
		}, 5000);
		raksangRaidTaskC1 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				raksangRaid((Npc)spawn(236021, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
				raksangRaid((Npc)spawn(236021, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
				raksangRaid((Npc)spawn(236021, 311.0131f, 607.05383f, 146.51385f, (byte) 13));
		        raksangRaid((Npc)spawn(236020, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
		        raksangRaid((Npc)spawn(236020, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
				raksangRaid((Npc)spawn(236020, 325.99796f, 635.8432f, 146.51385f, (byte) 93));
			}
		}, 120000);
	}
	private void hellpathSecondWave() {
		//准备战斗！更多敌人涌入！ / Prepare for combat! More enemies swarming in!
		sendMsgByRace(1402832, Race.PC_ALL, 4000);
		//再坚持一下就能活下来。 / Hold a little longer and you will survive.
		sendMsgByRace(1402833, Race.PC_ALL, 60000);
		//只剩少数敌人！ / Only a few enemies left!
		sendMsgByRace(1402834, Race.PC_ALL, 110000);
		raksangRaidTaskC2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				raksangRaid((Npc)spawn(236096, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
				raksangRaid((Npc)spawn(236097, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
				raksangRaid((Npc)spawn(236098, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
		        raksangRaid((Npc)spawn(236096, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
		        raksangRaid((Npc)spawn(236097, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
				raksangRaid((Npc)spawn(236098, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
			}
		}, 5000);
		raksangRaidTaskC2 = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				raksangRaid((Npc)spawn(236096, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
				raksangRaid((Npc)spawn(236097, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
				raksangRaid((Npc)spawn(236099, 322.56607f, 777.8472f, 148.35696f, (byte) 13));
		        raksangRaid((Npc)spawn(236096, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
		        raksangRaid((Npc)spawn(236097, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
				raksangRaid((Npc)spawn(236099, 334.89322f, 801.4367f, 146.65071f, (byte) 92));
			}
		}, 120000);
	}
	
	private void raksangRaid(final Npc npc) {
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
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private void stopInstanceTask() {
        if (raksangRaidTaskA1 != null) {
            raksangRaidTaskA1.cancel(true);
        }
        if (raksangRaidTaskA2 != null) {
            raksangRaidTaskA2.cancel(true);
        }
    
        if (raksangRaidTaskB1 != null) {
            raksangRaidTaskB1.cancel(true);
        }
        if (raksangRaidTaskB2 != null) {
            raksangRaidTaskB2.cancel(true);
        }
    
        if (raksangRaidTaskC1 != null) {
            raksangRaidTaskC1.cancel(true);
        }
        if (raksangRaidTaskC2 != null) {
            raksangRaidTaskC2.cancel(true);
       }
    }

	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		stopInstanceTask();
		doors.clear();
	}
}