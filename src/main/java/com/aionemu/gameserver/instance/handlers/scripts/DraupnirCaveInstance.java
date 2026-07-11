package com.aionemu.gameserver.instance.handlers.scripts;

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
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * 德劳普尼尔洞穴副本事件处理器。
 * Instance event handler for Draupnir Cave.
 *
 * @author Encom
 */

@InstanceID(320080000)
public class DraupnirCaveInstance extends GeneralInstanceHandler
{
	//** NPC 4.9 / NPC 4.9 *//
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	/** bakarma charger / bakarma charger */
		private int bakarmaCharger;
	/** adjutants killed / adjutants killed */
		private int adjutantsKilled;
	/** 欧比斯 gate 任务 / abyss gate task */
		private Future<?> abyssGateTask;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		super.onInstanceCreate(instance);
		// 须击杀阿弗兰、萨拉斯瓦蒂、拉克希米与宁巴卡，指挥官巴卡尔玛才会出现。 / You must kill Afrane, Saraswati, Lakshmi, and Nimbarka to make Commander Bakarma appear.
		sendMsgByRace(1400757, Race.PC_ALL, 10000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				spawn(237276, 495.48535f, 392.0867f, 616.5717f, (byte) 89); //Akhal's Phantasm.
			}
		}, 10000);
		if (spawnRace == null) {
			spawnRace = player.getRace();
			SpawnIDDF3DragonSP();
		}
	}
	
	private void SpawnIDDF3DragonSP() {
		final int npc1 = spawnRace == Race.ASMODIANS ? 805737 : 805736;
		spawn(npc1, 498.74973f, 379.33267f, 621.2866f, (byte) 54);
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
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 213780: //Commander Bakarma.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053265, 1)); //Bakarma's Fabled Weapon Box.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053271, 1)); //Bakarma's Weapon Box.
					    break;
					}
				}
			break;
			case 237275: //Akhal.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054175, 1)); //Master Bakarma's Weapon Box.
					}
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
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 213776: //Instructor Afrane.
			case 237264:
			case 213778: //Beautiful Lakshmi.
			case 237265:
			case 213779: //Commander Nimbarka.
			case 237266:
			case 213802: //Kind Saraswati.
			case 237267:
				adjutantsKilled++;
				if (adjutantsKilled == 1) {
					// 还须再击杀 3 名副官，指挥官巴卡尔玛才会出现。 / You must kill 3 more Adjutants to make Commander Bakarma appear.
				    sendMsgByRace(1400758, Race.PC_ALL, 0);
				} else if (adjutantsKilled == 2) {
					// 还须再击杀 2 名副官，指挥官巴卡尔玛才会出现。 / You must kill 2 more Adjutants to make Commander Bakarma appear.
				    sendMsgByRace(1400759, Race.PC_ALL, 0);
				} else if (adjutantsKilled == 3) {
					// 还须再击杀 1 名副官，指挥官巴卡尔玛才会出现。 / You must kill 1 more Adjutant to make Commander Bakarma appear.
				    sendMsgByRace(1400760, Race.PC_ALL, 0);
				} else if (adjutantsKilled == 4) {
					spawnCommanderBakarma();
					// 指挥官巴卡尔玛已出现在贝里特拉神谕处。 / Commander Bakarma has appeared at Beritra's Oracle.
				    sendMsgByRace(1400751, Race.PC_ALL, 0);
					deleteNpc(214026); //Deputy Brigade General Yavant.
				}
			break;
			case 236929: //Commander Bakarma.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Draupnir Cave>");
				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 787.32513f, 431.49173f, 319.62155f, (byte) 33); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 787.32513f, 431.49173f, 319.62155f, (byte) 33); //高级修道院箱子。 / Noble Abbey Box.
					break;
				}
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					/**
					 * 处理 run。
					 * Handle run.
					 */
					@Override
					public void run() {
						spawnAkhal();
						// 贝里特拉神谕室出现了强大龙族。 / A powerful Balaur has appeared in Beritra's Oracle Chamber.
						sendMsgByRace(1403068, Race.PC_ALL, 0);
					}
				}, 60000);
			break;
			case 236900: //Bakarma Charger.
			    bakarmaCharger++;
				if (bakarmaCharger == 18) {
					abyssGateTask.cancel(true);
					// 欧比斯之门增强器已被中和。 / The Abyss Gate Enhancer has been neutralized.
					sendMsgByRace(1403065, Race.PC_ALL, 0);
				}
			break;
        }
    }
	
   /**
	 * Central Control Room Raid
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 702857: //Balaur Abyss Gate Enhancer.
				despawnNpc(npc);
				// 龙族蜂拥而至，保卫欧比斯之门增强器。 / Balaur are swarming to defend the Abyss Gate Enhancer.
				sendMsgByRace(1403063, Race.PC_ALL, 0);
				// 龙族已察觉入侵者的存在。 / The Balaur have been alerted to the presence of intruders.
				sendMsgByRace(1403064, Race.PC_ALL, 4000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						startAbyssGateRaid1();
				    }
			    }, 5000);
				abyssGateTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    /**
				     * 处理 run。
				     * Handle run.
				     */
				    @Override
				    public void run() {
						// 龙族蜂拥而至，保卫欧比斯之门增强器。 / Balaur are swarming to defend the Abyss Gate Enhancer.
						sendMsgByRace(1403063, Race.PC_ALL, 0);
						startAbyssGateRaid2();
				    }
			    }, 60000);
			break;
			case 702858: //Balaur Abyss Gate Booster.
			    despawnNpc(npc);
				// 在中央控制室找到并过载欧比斯之门增强器。 / Find and overload the Abyss Gate Enhancer in the Central Control Room.
				sendMsgByRace(1403058, Race.PC_ALL, 0);
				// 龙族的欧比斯之门增强器已激活。 / The Balaur's Abyss Gate Enhancer is active.
				// 增强器防护装置将在 3 分钟后激活，防止被摧毁。 / The enhancer protection device will activate in 3 minutes, preventing it from being destroyed.
				sendMsgByRace(1403081, Race.PC_ALL, 5000);
				spawn(702857, 469.00000f, 563.0000f, 510.49686f, (byte) 29); //Balaur Abyss Gate Enhancer.
				spawn(702857, 511.36166f, 591.0183f, 510.60300f, (byte) 60); //Balaur Abyss Gate Enhancer.
				spawn(702857, 466.00000f, 617.0000f, 511.22543f, (byte) 96); //Balaur Abyss Gate Enhancer.
			break;
		}
	}
	
	private void spawnCommanderBakarma() {
		spawn(236929, 777.46985f, 431.09888f, 321.7541f, (byte) 62); //Commander Bakarma.
	}
	
	private void spawnAkhal() {
		spawn(237275, 777.46985f, 431.09888f, 321.7541f, (byte) 62); //Akhal.
	}
	/**
	 * 处理 startAbyssGateRaid1。
	 * Handle startAbyssGateRaid1.
	 */
	
	public void startAbyssGateRaid1() {
	    abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
	}
	/**
	 * 处理 startAbyssGateRaid2。
	 * Handle startAbyssGateRaid2.
	 */
	
	public void startAbyssGateRaid2() {
	    abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
		abyssGateRaid((Npc)spawn(236900, 514.45465f, 614.66077f, 515.35785f, (byte) 67));
	}
	
	private void abyssGateRaid(final Npc npc) {
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