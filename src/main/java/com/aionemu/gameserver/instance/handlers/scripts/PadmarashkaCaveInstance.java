package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

@InstanceID(320150000)
public class PadmarashkaCaveInstance extends GeneralInstanceHandler
{
	/** dramata egg55 / dramata egg55 */
		private int dramataEgg55;
	/** dramata fi55ae / dramata fi55ae */
		private int dramataFi55Ae;
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onEnterInstance(Player player) {
		// 须在时限内击败守护者以唤醒处于防护沉眠的帕德玛拉什卡。 / You must defeat the protector within the time limit to wake Padmarashka from the Protective Slumber.
		sendMsg(1400711, 0, false, 25, 10000);
		if (runtimeState().getLong("padma.deadline", 0) == 0
				&& !runtimeState().getBoolean("padma.complete", false)) {
			startPadmarashkaTimer();
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
				if (player.isOnline()) {
					long remaining = Math.max(0, runtimeState().getLong("padma.deadline", 0) - System.currentTimeMillis());
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, (int) (remaining / 1000)));
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
		dramataEgg55 = runtimeState().getInt("padma.eggs", 0);
		dramataFi55Ae = runtimeState().getInt("padma.protectors", 0);
		Npc npc = instance.getNpc(218756); //Padmarashka.
		if (npc != null && dramataFi55Ae < 4 && !runtimeState().getBoolean("padma.complete", false)
				&& !runtimeState().getBoolean("padma.expired", false)) {
			npc.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			GameEngineServices.skillEngine().getSkill(npc, 19186, 60, npc).useNoAnimationSkill(); //Protective Slumber.
		}
		restorePadmarashkaTimer();
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
				runtimeState().put("padma.complete", true);
				cancelPadmarashkaTimer();
				// 帕德玛拉什卡已死亡。30 分钟后将离开其洞穴。 / Padmarashka has died. You will be removed from Padmarashka's Cave in 30 minutes.
				sendMsg(1400675, 0, false, 25, 10000);
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
				runtimeState().put("padma.eggs", dramataEgg55);
				if (dramataEgg55 == 2) {
					// 帕德玛拉什卡即将产卵。 / Padmarashka is about to lay eggs.
					sendMsg(1400526, 0, false, 25, 0);
				} else if (dramataEgg55 == 5) {
					// 帕德玛拉什卡因大量卵被毁而暴怒。 / Padmarashka is furious after seeing so many of her eggs destroyed.
					sendMsg(1401213, 0, false, 25, 0);
				}
			break;
			case 218670: //Padmarashka's Elite Commander.
			case 218671: //Padmarashka Sartip.
			case 218673: //Padmarashka's Elite Captain.
			case 218674: //Padmarashka's Chief Medic.
				Npc dramata55Al = instance.getNpc(218756); //Padmarashka.
				dramataFi55Ae++;
				runtimeState().put("padma.protectors", dramataFi55Ae);
				if (dramata55Al != null) {
					if (dramataFi55Ae == 1) {
					} else if (dramataFi55Ae == 2) {
					} else if (dramataFi55Ae == 3) {
					} else if (dramataFi55Ae == 4) {
						deleteNpc(282123); //Dramata Shield.
						// 帕德玛拉什卡已从防护沉眠中苏醒。 / Padmarashka has awoken from the Protective Slumber.
						sendMsg(1400728, 0, false, 25, 10000);
						dramata55Al.getEffectController().removeEffect(19186); //Protective Slumber.
					}
				}
			break;
		}
    }
	
	private void startPadmarashkaTimer() {
        // 帕德玛拉什卡施放防御魔法。2 小时后将离开其洞穴。 / Padmarashka has cast defensive magic. You will be removed from Padmarashka's Cave in 2 hours.
		sendMsg(1400506);
		long deadline = System.currentTimeMillis() + 7_200_000;
		runtimeState().put("padma.deadline", deadline);
		restorePadmarashkaTimer();
    }

	private void restorePadmarashkaTimer() {
		long deadline = runtimeState().getLong("padma.deadline", 0);
		if (deadline == 0 || runtimeState().getBoolean("padma.complete", false)
				|| runtimeState().getBoolean("padma.expired", false)) {
			return;
		}
		int[] messages = { 1400507, 1400508, 1400509, 1400510, 1400511, 1400512, 1400513, 1400514, 1400515 };
		int[] remainingMinutes = { 90, 60, 30, 15, 10, 5, 3, 2, 1 };
		for (int i = 0; i < messages.length; i++) {
			int message = messages[i];
			scheduleDeadline("warning_" + remainingMinutes[i], deadline - remainingMinutes[i] * 60_000L,
					() -> sendMsg(message));
		}
		scheduleDeadline("expire", deadline, this::expirePadmarashka);
	}

	private void expirePadmarashka() {
		runtimeState().put("padma.expired", true);
		sendMsg(1400524, 0, false, 25, 0);
		deleteNpc(218756);
	}

	private void cancelPadmarashkaTimer() {
		for (int remaining : new int[] { 90, 60, 30, 15, 10, 5, 3, 2, 1 }) {
			cancelDeadline("warning_" + remaining);
		}
		cancelDeadline("expire");
	}
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void sendMovie(Player player, int movie) {
		String key = "padma.movie." + movie;
        if (!runtimeState().getBoolean(key, false)) {
			runtimeState().put(key, true);
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
    }
}
