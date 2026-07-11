package com.aionemu.gameserver.ai.instance.transidiumAnnex;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * Transidium Annex 副本 NPC AI：Ahserion Flight Barrier（@AIName "ahserion_flight_barrier"），继承 NpcAI2。
 * Transidium Annex instance NPC AI: Ahserion Flight Barrier (@AIName "ahserion_flight_barrier"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("ahserion_flight_barrier")
public class Ahserion_Flight_BarrierAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startShieldVulnerable();
	}
	
   /**
	* 「Ahserion」周围不可破坏屏障已移除；屏障关闭后可开始攻击。
	 * Indestructible barrier around "Ahserion" is removed. Once the barriers are off you can start attacking "Ahserion".
	 * You can say that after 25min.
	*/
	private void startShieldVulnerable() {
		final Npc GAb1SubCenterBarricadeDa65Ah = getPosition().getWorldMapInstance().getNpc(277230); //Ahserion Flight Barrier.
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GAb1SubCenterBarricadeDa65Ah.setTarget(getOwner());
				GAb1SubCenterBarricadeDa65Ah.setNpcType(NpcType.ATTACKABLE);
				WorldMapInstance instance = getPosition().getWorldMapInstance();
				for (Player player: instance.getPlayersInside()) {
					if (MathUtil.isIn3dRange(player, GAb1SubCenterBarricadeDa65Ah, 20)) {
						player.getEffectController().updatePlayerEffectIcons();
						player.clearKnownlist();
						player.updateKnownlist();
					}
				}
				// 特兰西迪姆附楼效果削弱了阿塞里昂飞行屏障。 / The effect of the Transidium Annex has weakened the Ahserion's Flight Barrier.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_11, 0);
			}
		}, 1500000); //...25 Minutes.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
