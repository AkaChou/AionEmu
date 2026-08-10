package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.player.PlayerMailboxState;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 对话窗口包：打开 NPC/对象对话框，并按页类型写入邮箱状态或城镇挑战任务城镇 ID。
 * Opens an NPC/object dialog window; for mail or town-challenge pages writes mailbox state or town id.
 */
public class SM_DIALOG_WINDOW extends AionServerPacket {
	private int targetObjectId;
	private int dialogID;
	private int questId = 0;

	/**
	 * @param targetObjectId 对话目标对象 ID / dialog target object id
	 * @param dlgID          对话框页 ID / dialog page id
	 */
	public SM_DIALOG_WINDOW(int targetObjectId, int dlgID) {
		this.targetObjectId = dlgID == DialogPage.NULL.id() ? 0 : targetObjectId;
		this.dialogID = dlgID;
	}

	/**
	 * @param targetObjectId 对话目标对象 ID / dialog target object id
	 * @param dlgID          对话框页 ID / dialog page id
	 * related quest id
	 */
	public SM_DIALOG_WINDOW(int targetObjectId, int dlgID, int questId) {
		this(targetObjectId, dlgID);
		this.questId = dlgID == DialogPage.NULL.id() ? 0 : questId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		writeD(targetObjectId);
		writeH(dialogID);
		writeD(questId);
		writeH(0);
		if (this.dialogID == DialogPage.MAIL.id()) {
			AionObject object = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);
			if (object != null && object instanceof Npc) {
				Npc znpc = (Npc) object;
				if (znpc.getNpcId() == 798100 || znpc.getNpcId() == 798101) {
					player.getMailbox().mailBoxState = PlayerMailboxState.EXPRESS;
					writeH(2);
				} else {
					player.getMailbox().mailBoxState = PlayerMailboxState.REGULAR;
				}
			} else {
				writeH(0);
			}
		} else if (this.dialogID == DialogPage.TOWN_CHALLENGE_TASK.id()) {
			AionObject object = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);
			if (object != null && object instanceof Npc) {
				Npc npc = (Npc) object;
				if (npc.getNpcId() == 205770 || npc.getNpcId() == 730677 || npc.getNpcId() == 730679) {
					int townId = 0;
					MapRegion region = npc.getPosition().getMapRegion();
					if (region == null) {
					} else {
						List<ZoneInstance> zones = region.getZones(npc);
						for (ZoneInstance zone : zones) {
							townId = zone.getTownId();
							if (townId > 0) {
								break;
							}
						}
						writeH(townId);
					}
				}
			}
		} else {
			writeH(0);
		}
	}
}
