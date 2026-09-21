package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HEADING_UPDATE;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.services.mail.MailboxState;

/**
 * 关闭与 NPC 对话或邮箱界面的客户端包。
 * Client packet that closes an NPC dialog or mailbox UI.
 */
public class CM_CLOSE_DIALOG extends AionClientPacket {
	private int targetObjectId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_CLOSE_DIALOG(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		targetObjectId = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		player.clearNpcQuestDialogSelection();
		// 关闭对话同样结束当前交互，重发计数必须从零开始。
		// Closing a dialog also ends the interaction, so the resend counter restarts.
		player.clearDialogSelectRepeat();
		final VisibleObject obj = player.getKnownList().getObject(targetObjectId);
		final AionConnection client = getConnection();
		if (obj == null) {
			return;
		}
		if (obj instanceof Npc npc) {
			npc.getAi2().onCreatureEvent(AIEventType.DIALOG_FINISH, player);
			DialogService.onCloseDialog(npc, player);
			GameThreadPoolServices.threadPoolManager().schedule(() -> client.sendPacket(new SM_HEADING_UPDATE(targetObjectId, obj.getHeading())), 1200);
		}
		var mailbox = player.getMailbox();
		if (mailbox != null && mailbox.mailBoxState != MailboxState.CLOSED) {
			mailbox.mailBoxState = MailboxState.CLOSED;
		}
	}
}
