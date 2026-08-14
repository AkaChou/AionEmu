package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET.PacketElementType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 开发用自定义发包命令（{@code //fsc}）：按格式串向客户端发送自定义包。
 * Development command to craft and send custom client packets ({@code //fsc}).
 * <p>
 * 参数：包 ID（十进制或 0x 十六进制）、格式串（d/h/c/f/e/q/s）、以及对应数据列表。
 * Params: packet id (decimal or 0x hex), format string (d/h/c/f/e/q/s), then data values.
 * </p>
 * 示例 / Example: {@code //fsc 0xD8 cdds 8 50 80 someText}
 *
 * @author Luno
 */
public class Fsc extends AdminCommand {

	/**
	 * 注册命令名为 {@code fsc}。
	 * Registers the command name {@code fsc}.
	 */
	public Fsc() {
		super("fsc");
	}

	/**
	 * 按包 ID 与格式串组装并发送自定义包。
	 * Builds and sends a custom packet from id, format string and values.
	 *
	 * @param params 包 ID、格式串、数据值 / packet id, format string, data values
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length < 3) {
			PacketSendUtility.sendMessage(player, "Incorrent number of params in //fsc command");
			return;
		}

		int id = Integer.decode(params[0]);
		String format = "";

		if (params.length > 1)
			format = params[1];

		SM_CUSTOM_PACKET packet = new SM_CUSTOM_PACKET(id);

		int i = 0;
		for (char c : format.toCharArray()) {
			packet.addElement(PacketElementType.getByCode(c), params[i + 2]);
			i++;
		}
		PacketSendUtility.sendPacket(player, packet);
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Incorrent number of params in //fsc command");
	}
}
