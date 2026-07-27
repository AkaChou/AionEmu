package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端快递邮件操作包：召唤或解散邮差。
 * Client packet for express mail actions: summon or dismiss the postman.
 */
public class CM_READ_EXPRESS_MAIL extends AionClientPacket {
	private int action;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_READ_EXPRESS_MAIL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.action = readC();
	}

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		boolean haveUnreadExpress = (player.getMailbox().haveUnreadByType(LetterType.EXPRESS)
				|| player.getMailbox().haveUnreadByType(LetterType.BLACKCLOUD));
		switch (this.action) {
		case 0:
			if (player.getPostman() != null) {
				player.getPostman().getController().onDelete();
				player.setPostman(null);
			}
			break;
		case 1:
			if (player.getPostman() != null) {
				// 快递邮差已经到达。 / An express courier has already arrived.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_POSTMAN_ALREADY_SUMMONED);
				return;
			} else if (player.isInPrison()) {
				// 此处无法呼叫邮差。 / You cannot call a courier here.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_POSTMAN_UNABLE_POSITION);
				return;
			} else if (player.isFlying()) {
				// 飞行中无法呼叫邮差。 / You cannot call a courier while flying.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_POSTMAN_UNABLE_IN_FLIGHT);
				return;
			} else if (player.getController().hasScheduledTask(TaskId.EXPRESS_MAIL_USE)) {
				// 请稍候再呼叫邮差。 / Please wait for a while before you call for the courier again.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_POSTMAN_UNABLE_IN_COOLTIME);
				return;
			} else if (haveUnreadExpress) {
				VisibleObjectSpawner.spawnPostman(player);
				Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
					}
				}, TimeUnit.SECONDS.toMillis(Math.max(0, CustomConfig.EXPRESS_MAIL_COOLDOWN_SECONDS)));
				player.getController().addTask(TaskId.EXPRESS_MAIL_USE, task);
			}
			break;
		}
	}
}
