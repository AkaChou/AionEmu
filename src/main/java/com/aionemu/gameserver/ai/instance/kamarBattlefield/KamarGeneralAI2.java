package com.aionemu.gameserver.ai.instance.kamarBattlefield;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kamar Battlefield 副本 NPC AI：Kamar General（@AIName "kamar_general"），继承 AggressiveNpcAI2。
 * Kamar Battlefield instance NPC AI: Kamar General (@AIName "kamar_general"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("kamar_general")
public class KamarGeneralAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 233321: //General Varga.
				case 233322: //General Varga.
				case 233323: //General Varga.
					announceIDKamarDrakanGeneral();
				break;
				case 233327: //Acting Commander Cripsin.
				case 233329: //Acting Commander Cripsin.
				    announceIDKamarLightGeneral();
				break;
				case 233328: //Acting Commander Tepes.
				case 233330: //Acting Commander Tepes.
				    announceIDKamarDarkGeneral();
				break;
			}
		}
	}
	
	private void announceIDKamarLightGeneral() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 代理指挥官克里斯平遭受攻击。 / Acting Commander Crispin is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDKamar_LightGeneral_Hit);
				}
			}
		});
	}
	private void announceIDKamarDarkGeneral() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 代理指挥官特佩斯遭受攻击。 / Acting Commander Tepes is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDKamar_DarkGeneral_Hit);
				}
			}
		});
	}
	private void announceIDKamarDrakanGeneral() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 瓦尔加指挥官遭受攻击。 / Commander Varga is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDKamar_DrakanGeneral_Hit);
				}
			}
		});
	}
}
