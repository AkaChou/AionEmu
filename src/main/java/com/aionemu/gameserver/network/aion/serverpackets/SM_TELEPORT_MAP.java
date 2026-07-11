package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * 打开传送 NPC 传送地图/目的地列表的服务端包。
 * destination list. / destination list.
 * <p>
 * 可按配置禁用部分 NPC 的目的地展示。
 * Destination listing for certain NPCs can be disabled via config.
 *
 * @author alexa026 , orz
 */
@Slf4j
public class SM_TELEPORT_MAP extends AionServerPacket {

	private int targetObjectId;
	private Player player;
	private TeleporterTemplate teleport;
	public Npc npc;
	private static final List<Integer> disableTeleportNpcs = new ArrayList<Integer>();

	/**
	 * @param player         请求传送的玩家 / player requesting teleport
	 * teleporter NPC object id
	 * teleporter template
	 */
	public SM_TELEPORT_MAP(Player player, int targetObjectId, TeleporterTemplate teleport) {
		this.player = player;
		this.targetObjectId = targetObjectId;
		this.npc = (Npc) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);
		this.teleport = teleport;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		for (String s : CustomConfig.DISABLE_TELEPORTER_NPCS.split(",")) {
			disableTeleportNpcs.add(Integer.parseInt(s));
		}

		if (teleport != null && teleport.getTeleportId() != 0) {
			writeD(targetObjectId);
			writeH(teleport.getTeleportId());
			if (disableTeleportNpcs.contains(npc.getNpcId())) {
				for (Integer npcId : disableTeleportNpcs) {
					if (npc.getNpcId() == npcId) {
						writeH(teleport.getTeleLocIdData().getTelelocations().size());
						for (TeleportLocation locationid : teleport.getTeleLocIdData().getTelelocations()) {
							writeD(locationid.getLocId());
						}
					} else {
						continue;
					}
				}
			} else {
				writeH(0);
			}
		} else {
			PacketSendUtility.sendMessage(player, "Missing info at npc_teleporter.xml with npcid: " + npc.getNpcId());
			log.info(I18n.get("log.4903a85ac6d7", npc.getNpcId()));
		}
		disableTeleportNpcs.clear();
	}
}
