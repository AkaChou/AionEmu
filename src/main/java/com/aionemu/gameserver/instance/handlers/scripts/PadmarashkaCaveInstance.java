package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * 帕德玛拉什卡洞穴副本事件处理器。
 * Instance event handler for Padmarashka Cave.
 *
 * @author Encom
 */

@InstanceID(320150000)
public class PadmarashkaCaveInstance extends GeneralInstanceHandler
{
	/** dramata egg55 / dramata egg55 */
		private int dramataEgg55;
	/** dramata fi55ae / dramata fi55ae */
		private int dramataFi55Ae;
	/** dramata 任务 / dramata task */
		private Future<?> dramataTask;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		super.onInstanceCreate(instance);
		// 须在时限内击败守护者以唤醒处于防护沉眠的帕德玛拉什卡。 / You must defeat the protector within the time limit to wake Padmarashka from the Protective Slumber.
		sendMsgByRace(1400711, Race.PC_ALL, 10000);
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
					startPadmarashkaTimer();
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 7200)); //2Hrs.
				}
			}
		});
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
		Npc npc = instance.getNpc(218756); //Padmarashka.
		if (npc != null) {
			npc.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			GameEngineServices.skillEngine().getSkill(npc, 19186, 60, npc).useNoAnimationSkill(); //Protective Slumber.
		}
	}
	
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
			case 218756: //Padmarashka.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057935, 1)); //Padmarashka's Raging Weapon Box.
					    break;
						case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188057935, 1)); //Padmarashka's Weapon Chest.
					    break;
					} switch (Rnd.get(1, 14)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100001640, 1)); //Padmarashka's Raging Sword Skin.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100101258, 1)); //Padmarashka's Raging Warhammer Skin.
					    break;
						case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100201433, 1)); //Padmarashka's Raging Dagger Skin.
					    break;
						case 4:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100501248, 1)); //Padmarashka's Raging Jewel Skin.
					    break;
						case 5:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100601352, 1)); //Padmarashka's Raging Spellbook Skin.
					    break;
						case 6:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 100901276, 1)); //Padmarashka's Raging Greatsword Skin.
					    break;
						case 7:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 101301191, 1)); //Padmarashka's Raging Polearm Skin.
					    break;
						case 8:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 101501280, 1)); //Padmarashka's Raging Staff Skin.
					    break;
						case 9:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 101701299, 1)); //Padmarashka's Raging Longbow Skin.
					    break;
						case 10:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 101801148, 1)); //Padmarashka's Raging Pistol Skin.
					    break;
						case 11:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 101901059, 1)); //Padmarashka's Raging Aethercannon Skin.
					    break;
						case 12:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 102001175, 1)); //Padmarashka's Raging Harp Skin.
					    break;
						case 13:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 115001680, 1)); //Padmarashka's Raging Shield Skin.
					    break;
						case 14:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 115001794, 1)); //Padmarashka's Raging Shield Skin.
					    break;
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
			case 218756: //Padmarashka.
			    dramataTask.cancel(true);
				// 帕德玛拉什卡已死亡。30 分钟后将离开其洞穴。 / Padmarashka has died. You will be removed from Padmarashka's Cave in 30 minutes.
				sendMsgByRace(1400675, Race.PC_ALL, 10000);
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Padmarashka Cave>");
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
			break;
			case 282613: //Padmarashka's Eggs.
			case 282614: //Huge Padmarashka's Eggs.
			    dramataEgg55++;
				if (dramataEgg55 == 2) {
					// 帕德玛拉什卡即将产卵。 / Padmarashka is about to lay eggs.
					sendMsgByRace(1400526, Race.PC_ALL, 0);
				} else if (dramataEgg55 == 5) {
					// 帕德玛拉什卡因大量卵被毁而暴怒。 / Padmarashka is furious after seeing so many of her eggs destroyed.
					sendMsgByRace(1401213, Race.PC_ALL, 0);
				}
			break;
			case 218670: //Padmarashka's Elite Commander.
			case 218671: //Padmarashka Sartip.
			case 218673: //Padmarashka's Elite Captain.
			case 218674: //Padmarashka's Chief Medic.
				Npc dramata55Al = instance.getNpc(218756); //Padmarashka.
				dramataFi55Ae++;
				if (dramata55Al != null) {
					if (dramataFi55Ae == 1) {
					} else if (dramataFi55Ae == 2) {
					} else if (dramataFi55Ae == 3) {
					} else if (dramataFi55Ae == 4) {
						deleteNpc(282123); //Dramata Shield.
						// 帕德玛拉什卡已从防护沉眠中苏醒。 / Padmarashka has awoken from the Protective Slumber.
						sendMsgByRace(1400728, Race.PC_ALL, 10000);
						dramata55Al.getEffectController().removeEffect(19186); //Protective Slumber.
					}
				}
			break;
		}
    }
	
	private void startPadmarashkaTimer() {
        // 帕德玛拉什卡施放防御魔法。2 小时后将离开其洞穴。 / Padmarashka has cast defensive magic. You will be removed from Padmarashka's Cave in 2 hours.
		sendMsg(1400506);
		// 你将在 1 小时 30 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 1 hour and 30 minutes.
        this.sendMessage(1400507, 30 * 60 * 1000);
		// 你将在 1 小时后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 1 hour.
		this.sendMessage(1400508, 60 * 60 * 1000);
		// 你将在 30 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 30 minutes.
		this.sendMessage(1400509, 90 * 60 * 1000);
		// 你将在 15 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 15 minutes.
		this.sendMessage(1400510, 105 * 60 * 1000);
		// 你将在 10 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 10 minutes.
		this.sendMessage(1400511, 110 * 60 * 1000);
		// 你将在 5 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 5 minutes.
		this.sendMessage(1400512, 115 * 60 * 1000);
		// 你将在 3 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 3 minutes.
		this.sendMessage(1400513, 117 * 60 * 1000);
		// 你将在 2 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 2 minutes.
		this.sendMessage(1400514, 118 * 60 * 1000);
		// 你将在 1 分钟后被移出帕德玛拉什卡洞穴。 / You will be removed from Padmarashka's Cave in 1 minute.
		this.sendMessage(1400515, 119 * 60 * 1000);
        dramataTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
            @Override
            public void run() {
				// 你被帕德玛拉什卡的防御魔法强制移出洞穴。 / You have been forcibly removed from Padmarashka's Cave by Padmarashka's defensive magic.
				sendMsgByRace(1400524, Race.PC_ALL, 0);
				deleteNpc(218756); //Padmarashka.
            }
        }, 7200000);
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
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
            movies.add(movie);
            PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
	
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("PADMARASHKAS_NEST_320150000")) {
			sendMovie(player, 488);
	    }
    }
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
		movies.clear();
    }
}