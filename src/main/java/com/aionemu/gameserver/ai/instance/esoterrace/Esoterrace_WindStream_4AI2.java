package com.aionemu.gameserver.ai.instance.esoterrace;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.windstreams.Location2D;
import com.aionemu.gameserver.model.templates.windstreams.WindstreamTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WINDSTREAM_ANNOUNCE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Esoterrace 副本 NPC AI：Esoterrace Wind Stream 4（@AIName "Esoterrace_WindStream_4"），继承 NpcAI2。
 * Esoterrace instance NPC AI: Esoterrace Wind Stream 4 (@AIName "Esoterrace_WindStream_4"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Esoterrace_WindStream_4")
public class Esoterrace_WindStream_4AI2 extends NpcAI2
{
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		Npc npc = getOwner();
		startWindStream(npc);
        windStreamAnnounce(getOwner(), 0);
    }
	
	private void startWindStream(final Npc npc) {
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                Npc npc2 = (Npc) spawn(300250000, 703059, 392.27563f, 543.89026f, 318.3265f, (byte) 18, 0, 1);
				windStreamAnnounce(npc2, 0);
				despawnNpc(703058);
                spawn(703059, 392.27563f, 543.89026f, 318.3265f, (byte) 18);
                PacketSendUtility.broadcastPacket(npc2, new SM_WINDSTREAM_ANNOUNCE(1, 300250000, 160, 0));
                if (npc2 != null) {
                    npc2.getController().onDelete();
                } if (npc != null) {
                    npc.getController().onDelete();
                }
            }
        }, 5000);
    }
	
    private void windStreamAnnounce(final Npc npc, final int state) {
        WindstreamTemplate template = DataManager.WINDSTREAM_DATA.getStreamTemplate(npc.getPosition().getMapId());
        for (Location2D wind: template.getLocations().getLocation()) {
            if (wind.getId() == 160) {
                wind.setState(state);
                break;
            }
        }
        npc.getPosition().getWorld().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, new SM_WINDSTREAM_ANNOUNCE(1, 300250000, 160, state));
            }
        });
    }
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
