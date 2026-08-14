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
 * Transidium Annex 副本 NPC AI：Advance Corridor Shield（@AIName "advance_corridor_shield"），继承 NpcAI2。
 * Transidium Annex instance NPC AI: Advance Corridor Shield (@AIName "advance_corridor_shield"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("advance_corridor_shield")
public class Advance_Corridor_ShieldAI2 extends NpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 297306: // 贝卢斯进阶走廊护盾 / Belus Advance Corridor Shield
				    announceBelusAdvance();
				break;
				case 297307: // 阿斯皮达进阶走廊护盾 / Aspida Advance Corridor Shield
				    announceAspidaAdvance();
				break;
				case 297308: // 阿塔纳托斯进阶走廊护盾 / Atanatos Advance Corridor Shield
					announceAtanatosAdvance();
				break;
				case 297309: // 迪西隆进阶走廊护盾 / Disillon Advance Corridor Shield
					announceDisillonAdvance();
				break;
			}
		}
	}
	
	private void announceBelusAdvance() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 贝卢斯进阶走廊护盾遭受攻击。 / The Belus Advance Corridor Shield is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_PORTAL_DEST_69_ATTACKED);
				}
			}
		});
	}
	private void announceAspidaAdvance() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 阿斯皮达进阶走廊护盾遭受攻击。 / The Aspida Advance Corridor Shield is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_PORTAL_DEST_70_ATTACKED);
				}
			}
		});
	}
	private void announceAtanatosAdvance() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 阿塔纳托斯进阶走廊护盾遭受攻击。 / The Atanatos Advance Corridor Shield is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_PORTAL_DEST_71_ATTACKED);
				}
			}
		});
	}
	private void announceDisillonAdvance() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 迪西隆进阶走廊护盾遭受攻击。 / The Disillon Advance Corridor Shield is under attack.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_PORTAL_DEST_72_ATTACKED);
				}
			}
		});
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
