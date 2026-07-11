package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESURRECT;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：复活已死亡的目标玩家。
 * GM command handler that resurrects a dead target player.
 *
 * @author Alcapwnd
 */
public class CmdResurrect extends AbstractGMHandler {

	/**
	 * 创建处理器并立即尝试复活目标。
	 * Creates the handler and immediately attempts resurrection.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 指令参数（当前未使用） / command parameters (currently unused)
	 */
	public CmdResurrect(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 若目标已死亡则激活复活并发送复活包。
	 * If the target is dead, activates resurrection and sends the resurrect packet.
	 */
	public void run() {
		Player t = target != null ? target : admin;
		if (!t.getLifeStats().isAlreadyDead()) {
			return;
		}
		t.setPlayerResActivate(true);
		PacketSendUtility.sendPacket(t, new SM_RESURRECT(admin));
	}
}
