package com.aionemu.gameserver.ai.siege;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_ARTIFACT_INFO3;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FORTRESS_INFO;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 攻城战相关 NPC AI：Siege Teleporter（@AIName "siege_teleporter"），继承 GeneralNpcAI2。
 * Siege-related NPC AI: Siege Teleporter (@AIName "siege_teleporter"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("siege_teleporter")
public class Siege_TeleporterAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDespawned() {
		siegeTeleport(false);
		artifactTeleport(false);
		super.handleDespawned();
	}
	
	@Override
	protected void handleSpawned() {
		siegeTeleport(true);
		artifactTeleport(true);
		super.handleSpawned();
	}
	
	private void siegeTeleport(final boolean status) {
		final int id = ((SiegeNpc) getOwner()).getSiegeId();
		GameFeatureServices.siegeService().getFortress(id).setCanTeleport(status);
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_FORTRESS_INFO(id, status));
			}
		});
	}
	
	private void artifactTeleport(final boolean status) {
        final int id = ((SiegeNpc) getOwner()).getSiegeId();
        GameFeatureServices.siegeService().getArtifact(id).setCanTeleport(status);
        getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, new SM_ABYSS_ARTIFACT_INFO3(id, status));
            }
        });
    }
}
