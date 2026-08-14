package com.aionemu.gameserver.ai.instance.transidiumAnnex;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transidium Annex 副本 NPC AI：Hangar Controller（@AIName "hangar_controller"），继承 NpcAI2。
 * Transidium Annex instance NPC AI: Hangar Controller (@AIName "hangar_controller"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("hangar_controller")
public class Hangar_ControllerAI2 extends NpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 297310: // 战车机库 I 控制器 / Chariot Hangar I Controller
				    announceGAB1SubTankA();
				break;
				case 297311: // 战车机库 II 控制器 / Chariot Hangar II Controller
				    announceGAB1SubTankB();
				break;
				case 297312: // 伊格努斯引擎机库 I 控制器 / Ignus Engine Hangar I Controller
					announceGAB1SubTankC();
				break;
				case 297313: // 伊格努斯引擎机库 II 控制器 / Ignus Engine Hangar II Controller
					announceGAB1SubTankD();
				break;
			}
		}
	}
	
	private void announceGAB1SubTankA() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 战车机库 I 控制器遭受攻击。 / Chariot Hangar I Controller is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_TANK_A_ATTACKED);
				}
			}
		});
	}
	private void announceGAB1SubTankB() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 战车机库 II 控制器遭受攻击。 / Chariot Hangar II Controller is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_TANK_B_ATTACKED);
				}
			}
		});
	}
	private void announceGAB1SubTankC() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 伊格努斯引擎机库 I 控制器遭受攻击。 / Ignus Engine Hangar I Controller is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_TANK_C_ATTACKED);
				}
			}
		});
	}
	private void announceGAB1SubTankD() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 伊格努斯引擎机库 II 控制器遭受攻击。 / Ignus Engine Hangar II Controller is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_TANK_D_ATTACKED);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
