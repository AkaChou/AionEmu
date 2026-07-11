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
 * Transidium Annex 副本 NPC AI：Bulwark Shield（@AIName "bulwark_shield"），继承 NpcAI2。
 * Transidium Annex instance NPC AI: Bulwark Shield (@AIName "bulwark_shield"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("bulwark_shield")
public class Bulwark_ShieldAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startShieldVulnerable();
	}
	
   /**
	* 「Ahserion」周围墙壁变为可破坏，可开始摧毁与其之间的墙。
	 * Walls around "Ahserion" become vulnerable. You can start to destroy walls between you and "Ahserion". You can say that after 30min the real battle begins.
	*/
	private void startShieldVulnerable() {
		final Npc GAb1SubNamedBarricadeDa65Ah = getPosition().getWorldMapInstance().getNpc(277231); //Bulwark Shield.
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GAb1SubNamedBarricadeDa65Ah.setTarget(getOwner());
				GAb1SubNamedBarricadeDa65Ah.setNpcType(NpcType.ATTACKABLE);
				WorldMapInstance instance = getPosition().getWorldMapInstance();
				for (Player player: instance.getPlayersInside()) {
					if (MathUtil.isIn3dRange(player, GAb1SubNamedBarricadeDa65Ah, 20)) {
						player.getEffectController().updatePlayerEffectIcons();
						player.clearKnownlist();
						player.updateKnownlist();
					}
				}
				// 特兰西迪姆附楼效果削弱了壁垒护盾。 / The effect of the Transidium Annex has weakened the Bulwark Shield.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_12, 0);
			}
		}, 1800000); //...30 Minutes.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
