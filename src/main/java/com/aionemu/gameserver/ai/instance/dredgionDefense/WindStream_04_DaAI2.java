package com.aionemu.gameserver.ai.instance.dredgionDefense;

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
 * Dredgion Defense 副本 NPC AI：Wind Stream 04 Da（@AIName "WindStream_04_Da"），继承 NpcAI2。
 * Dredgion Defense instance NPC AI: Wind Stream 04 Da (@AIName "WindStream_04_Da"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("WindStream_04_Da")
public class WindStream_04_DaAI2 extends NpcAI2
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
                Npc npc2 = (Npc) spawn(302300000, 220970, 1223.5588f, 1358.0000f, 208.47737f, (byte) 0, 0, 1);
				windStreamAnnounce(npc2, 1);
				despawnNpc(220977);
                spawn(220970, 1223.5588f, 1358.0000f, 208.47737f, (byte) 0, 1283);
                PacketSendUtility.broadcastPacket(npc2, new SM_WINDSTREAM_ANNOUNCE(1, 302300000, 366, 1));
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
            if (wind.getId() == 366) {
                wind.setState(state);
                break;
            }
        }
        npc.getPosition().getWorld().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, new SM_WINDSTREAM_ANNOUNCE(1, 302300000, 366, state));
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
