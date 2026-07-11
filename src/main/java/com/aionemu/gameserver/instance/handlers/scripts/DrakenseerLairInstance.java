package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 龙先知巢穴副本事件处理器。
 * Instance event handler for Drakenseer Lair.
 *
 * @author Encom
 */

@InstanceID(301620000)
public class DrakenseerLairInstance extends GeneralInstanceHandler
{
	/** abyss gate enhancer killed / abyss gate enhancer killed */
		private int abyssGateEnhancerKilled;
	/** 是否启动计时器 / is start timer */
		private boolean isStartTimer = false;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed = false;
	/** drakenseerlair 任务 / drakenseer lair task */
		private final List<Future<?>> drakenseerLairTask = new ArrayList<Future<?>>();
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 220450: //Akhal The Oracle.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166030005, 5)); //淬炼溶液。 / Tempering Solution.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166040001, 1)); //Essence Core Solution.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188058413, 1)); //? ?  ??.
                        switch (Rnd.get(1, 4)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057624, 1)); //Oracle's Illusion Godstone Bundle.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057625, 1)); //Oracle Greater Enchant Supplement Bundle.
				            break;
							case 3:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057626, 1)); //Oracle Ancient Relic Bundle.
				            break;
							case 4:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057627, 1)); //Arkhal's Accessory Box.
				            break;
						} switch (Rnd.get(1, 2)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054910, 1)); //Akhal's Weapon Box.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054911, 1)); //Akhal's Armor Box.
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
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		spawnDrakenseerLairRings();
		// 你已进入龙视者之巢。 / You have entered Drakenseer's Lair.
		sendMsgByRace(1403376, Race.PC_ALL, 5000);
		Npc npc = instance.getNpc(220450); //Akhal The Oracle.
		if (npc != null) {
			GameEngineServices.skillEngine().getSkill(npc, 21791, 60, npc).useNoAnimationSkill(); //Turning Tide.
		}
	}
	
	/**
	 * 玩家通过飞行环时处理。
	 * Handle a player passing a flying ring.
	 *
	 * 玩家 / player
	 * @param flyingRing 飞行环标识 / flying-ring id
	 * result
	 */
	@Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
        if (flyingRing.equals("DRAKENSEER_LAIR")) {
		    if (!isStartTimer) {
			    isStartTimer = true;
			    System.currentTimeMillis();
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
							startDrakenseerLairTimer();
							PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 600));
							// 10 分钟内摧毁护盾导管并击败阿卡哈尔。 / Destroy the Shielding Conduits within 10 minutes and defeat Akhal.
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403377));
						}
					}
				});
			}
		}
		return false;
	}
	
	private void spawnDrakenseerLairRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("DRAKENSEER_LAIR", mapId,
        new Point3D(283.44757, 342.6241, 336.25607),
        new Point3D(276.73062, 339.42966, 345.29074),
        new Point3D(270.43948, 340.3889, 336.3338), 93), instanceId);
        f1.spawn();
    }
	/**
	 * 处理 startDrakenseerLairTimer。
	 * Handle startDrakenseerLairTimer.
	 */
	
	protected void startDrakenseerLairTimer() {
		// 进入龙视者之巢并摧毁护盾导管。 / Enter Drakenseer's Lair and destroy the Shielding Conduits.
		this.sendMessage(1403375, 1 * 60 * 1000);
		// 你还剩 1 分钟摧毁剩余护盾导管。 / You have one minute left to destroy the remaining Shielding Conduits.
		this.sendMessage(1403382, 9 * 60 * 1000);
		drakenseerLairTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        }, 600000)); //10 Minutes.
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
			case 857974: //Balaur Abyss Gate Enhancer A.
			case 857975: //Balaur Abyss Gate Enhancer B.
			case 857976: //Balaur Abyss Gate Enhancer C.
				abyssGateEnhancerKilled++;
				if (abyssGateEnhancerKilled == 1) {
					// 还剩两个护盾导管。 / Two Shielding Conduits remain.
				    sendMsgByRace(1403379, Race.PC_ALL, 0);
				} else if (abyssGateEnhancerKilled == 2) {
					// 还剩一个护盾导管。 / One Shielding Conduit remains.
					sendMsgByRace(1403380, Race.PC_ALL, 0);
				} else if (abyssGateEnhancerKilled == 3) {
					stopDrakenseerLairTimer(player);
					// 全部护盾导管被摧毁后，阿卡哈尔终于出现。 / With all the Shielding Conduits destroyed, Akhal finally appears.
				    sendMsgByRace(1403381, Race.PC_ALL, 2000);
					Npc akhalTheOracle = instance.getNpc(220450); //Akhal The Oracle.
					akhalTheOracle.getEffectController().removeEffect(21791); //Turning Tide.
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
				despawnNpc(npc);
			break;
			case 220450: //Akhal The Oracle.
			    spawn(806240, 299.1905f, 258.07004f, 319.67477f, (byte) 110); //Drakenseer's Lair Exit.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Drakenseer's Lair>");
			break;
		}
	}
	/**
	 * 处理 stopDrakenseerLairTimer。
	 * Handle stopDrakenseerLairTimer.
	 *
	 * @param player 玩家 / player
	 */
	
	protected void stopDrakenseerLairTimer(Player player) {
        stopDrakenseerLairTask();
	}
	
	private void stopDrakenseerLairTask() {
        for (Future<?> task : drakenseerLairTask) {
			if (task != null) {
				task.cancel(true);
			}
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
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		stopDrakenseerLairTask();
		isInstanceDestroyed = true;
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
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
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}
}