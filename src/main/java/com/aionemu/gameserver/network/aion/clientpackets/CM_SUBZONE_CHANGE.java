package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 客户端子区域变更通知包；高权限玩家会输出当前经过的区域调试信息。
 * Client packet notifying a subzone change; dumps zone debug info for high-access players.
 */
public class CM_SUBZONE_CHANGE extends AionClientPacket {
	private int unk;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SUBZONE_CHANGE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		unk = readC();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		player.revalidateZones();
		if (player.getAccessLevel() >= 5) {
			List<ZoneInstance> zones = player.getPosition().getMapRegion().getZones(player);
			int foundZones = 0;
			for (ZoneInstance zone : zones) {
				if (zone.getZoneTemplate().getZoneType() == ZoneClassName.DUMMY
						|| zone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
					continue;
				}
				foundZones++;
				PacketSendUtility.sendMessage(player, "Passed zone: unk=" + unk + "; "
						+ zone.getZoneTemplate().getZoneType() + " " + zone.getAreaTemplate().getZoneName().name());
			}
			if (foundZones == 0) {
				PacketSendUtility.sendMessage(player, "Passed unknown zone, unk=" + unk);
				return;
			}
		}
	}
}
