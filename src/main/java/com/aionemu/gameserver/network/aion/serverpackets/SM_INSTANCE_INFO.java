package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownList;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.templates.InstanceCooltime;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向客户端同步玩家副本冷却与进入次数信息的服务端包。
 * Server packet synchronizing a player's instance cooldown and entrance-count info to the client.
 */
public class SM_INSTANCE_INFO extends AionServerPacket {
	private Player player;
	private boolean isAnswer;
	private int cooldownId;
	private int worldId;
	private TemporaryPlayerTeam<?> playerTeam;

	/**
	 * 构造全量副本冷却同步包（可附带队伍应答上下文）。
	 * Creates a full instance-cooldown sync packet (optionally with team-answer context).
	 *
	 * target player
	 * @param isAnswer 是否为队伍应答场景 / whether this is a team-answer context
	 * @param playerTeam 临时队伍（可为空） / temporary player team (may be null)
	 */
	public SM_INSTANCE_INFO(Player player, boolean isAnswer, TemporaryPlayerTeam<?> playerTeam) {
		this.player = player;
		this.isAnswer = isAnswer;
		this.playerTeam = playerTeam;
		this.worldId = 0;
		this.cooldownId = 0;
	}

	/**
	 * 构造单个副本冷却同步包。
	 * Creates a single-instance cooldown sync packet.
	 *
	 * target player
	 * instance world id
	 */
	public SM_INSTANCE_INFO(Player player, int instanceId) {
		this.player = player;
		this.isAnswer = false;
		this.playerTeam = null;
		this.worldId = instanceId;
		this.cooldownId = DataManager.INSTANCE_COOLTIME_DATA.getInstanceCooltimeByWorldId(instanceId) != null
				? DataManager.INSTANCE_COOLTIME_DATA.getInstanceCooltimeByWorldId(instanceId).getId()
				: 0;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		boolean hasTeam = playerTeam != null;
		writeC(!isAnswer ? 0x2 : hasTeam ? 0x1 : 0x0);
		writeC(cooldownId);
		writeD(0x00);
		writeH(0x01);
		if (cooldownId == 0) {
			writeD(player.getObjectId());
			writeH(DataManager.INSTANCE_COOLTIME_DATA.size());
			PortalCooldownList cooldownList = player.getPortalCooldownList();
			for (Map.Entry<Integer, InstanceCooltime> e : DataManager.INSTANCE_COOLTIME_DATA.getAllInstances().entrySet()) {
				writeD(e.getValue().getId());
				writeD(0x00);
				if (cooldownList.getPortalCooldown(e.getValue().getWorldId()) == 0) {
					writeD(0x00);
				} else {
					writeD((int) (cooldownList.getPortalCooldown(e.getValue().getWorldId())
							- System.currentTimeMillis()) / 1000);
				}
				writeD(DataManager.INSTANCE_COOLTIME_DATA.getInstanceEntranceCountByWorldId(e.getKey()));
				writeD(cooldownList.getPortalCooldownItem(e.getValue().getWorldId()) != null
						? cooldownList.getPortalCooldownItem(e.getValue().getWorldId()).getEntryCount() * -1
						: 0);
				writeD(0x00);
				writeD(0x00);
				writeD(0x01);
				writeC(0x01);
			}
			writeS(player.getName());
		} else {
			writeD(player.getObjectId());
			writeH(0x01);
			writeD(cooldownId);
			writeD(0x00);
			long time = player.getPortalCooldownList().getPortalCooldown(worldId);
			writeD((time == 0 ? 0 : ((int) (time - System.currentTimeMillis()) / 1000)));
			writeD(DataManager.INSTANCE_COOLTIME_DATA.getInstanceEntranceCountByWorldId(worldId));
			writeD(player.getPortalCooldownList().getPortalCooldownItem(worldId) != null
					? player.getPortalCooldownList().getPortalCooldownItem(worldId).getEntryCount() * -1
					: 0);
			writeD(0x00);
			writeD(0x00);
			writeD(0x01);
			writeC(0x01);
			writeS(player.getName());
		}
	}
}
