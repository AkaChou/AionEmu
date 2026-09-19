package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.mail.MailboxState;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 对话窗口包：打开 NPC/对象对话框，并按页类型写入邮箱状态或城镇挑战任务城镇 ID。
 * Opens an NPC/object dialog window; for mail or town-challenge pages writes mailbox state or town id.
 */
@Slf4j
public class SM_DIALOG_WINDOW extends AionServerPacket {
	/** 任务追踪日志出口，路由到 logback 的 quest logger。 / Quest trace sink for logback quest logger. */
	private static final Logger QUEST_TRACE_LOG = LoggerFactory.getLogger("quest");

	private final int targetObjectId;
	private final int dialogID;
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
		if (LoggingConfig.LOG_QUEST_TRACE || (player != null && player.isQuestTraceEnabled())) {
			QUEST_TRACE_LOG.info(I18n.get("log.quest_trace.dialog_window",
				player != null ? player.getName() : "unknown",
				targetObjectId,
				questId,
				dialogID));
		}
		writeD(targetObjectId);
		writeH(dialogID);
		writeD(questId);
		writeH(0);
		if (this.dialogID == DialogPage.MAIL.id()) {
			AionObject object = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);
			if (object != null && object instanceof Npc znpc) {
				if (znpc.getNpcId() == 798100 || znpc.getNpcId() == 798101) {
					player.getMailbox().mailBoxState = MailboxState.EXPRESS;
					writeH(2);
				} else {
					player.getMailbox().mailBoxState = MailboxState.REGULAR;
				}
			} else {
				writeH(0);
			}
		} else if (this.dialogID == DialogPage.TOWN_CHALLENGE_TASK.id()) {
			AionObject object = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);
			if (object != null && object instanceof Npc npc) {
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
