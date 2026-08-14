package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rentus Base 副本 NPC AI：Sensory Area（@AIName "sensory_area"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Sensory Area (@AIName "sensory_area"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("sensory_area")
public class SensoryAreaAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 10) {
				if (startedEvent.compareAndSet(false, true)) {
					switch (player.getWorldId()) {
		                case 300280000: // Rentus Base 基地 / Rentus Base
						    // 萨斯塔从头顶飞过。 / Xasta flies past overhead.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_Spawn_01, 9000);
							// 用防空炮攻击头顶飞行的萨斯塔。 / Use the anti-aircraft gun to attack Xasta flying overhead.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_Spawn_02, 10000);
							// 萨斯塔受伤从空中坠落！ / Xasta falls from the sky, wounded!
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_SUCCEED_01, 120000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    @Override
								public void run() {
								    spawn(217309, 445.6442f, 439.13187f, 168.64172f, (byte) 40);
								}
							}, 10000);
							AI2Actions.deleteOwner(SensoryAreaAI2.this);
				        break;
					} switch (player.getWorldId()) {
		                case 300620000: // 被占领的 Rentus Base 4.8 / [Occupied] Rentus Base 4.8
						    // 萨斯塔从头顶飞过。 / Xasta flies past overhead.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_Spawn_01, 9000);
							// 用防空炮攻击头顶飞行的萨斯塔。 / Use the anti-aircraft gun to attack Xasta flying overhead.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_Spawn_02, 10000);
							// 萨斯塔受伤从空中坠落！ / Xasta falls from the sky, wounded!
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDYun_Rasta_SUCCEED_01, 120000);
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							    @Override
								public void run() {
								    spawn(236296, 445.6442f, 439.13187f, 168.64172f, (byte) 40);
								}
							}, 10000);
							AI2Actions.deleteOwner(SensoryAreaAI2.this);
				        break;
					}
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
