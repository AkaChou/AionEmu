package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET.PacketElementType;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.PacketSendUtility;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.List;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 原始数据包发送指令；从 {@code data/packets/} 下的文本文件读取十六进制并下发自定义包。
 * Admin command that sends raw custom packets loaded as hex from {@code data/packets/} text files.
 *
 * @author Luno
 * @author Aquanox
 */
@Slf4j
public class Raw extends AdminCommand {

	private static final File ROOT = new File("data/packets/");


	public Raw() {
		super("raw");
	}

	/**
	 * 读取指定名称的数据包文本文件并发送给管理员客户端。
	 * Loads the named packet text file and sends it to the admin client.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * @param params 单一参数：不含扩展名的文件名 / single arg: file name without extension
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 1) {
			PacketSendUtility.sendMessage(admin, "Usage: //raw [name]");
			return;
		}
	
		File file = new File(ROOT, params[0] + ".txt");
	
		if (!file.exists() || !file.canRead()) {
			PacketSendUtility.sendMessage(admin, "Wrong file selected.");
			return;
		}
	
		try {
			// 使用 JDK 8的 Files 类读取文件 | Using JDK 8's Files class to read the file
			List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	
			SM_CUSTOM_PACKET packet = null;
			PacketSendUtility.sendMessage(admin, "lines "+lines.size());
			boolean init = false;
			for (int r = 0 ; r< lines.size(); r++){
				String row = lines.get(r);
				String[] tokens = row.substring(0, 48).trim().split(" ");
				int len = tokens.length;
				
				for (int i = 0; i < len; i++) {
					if (!init) {
						if (i == 1){
						packet = new SM_CUSTOM_PACKET(Integer.decode("0x"+tokens[i]+tokens[i-1]));
						init = true;
						}
					}
					else if ( r > 0 || i > 4){
						packet.addElement(PacketElementType.C, "0x" + tokens[i]);
					}
				}
			}
			if (packet != null){
				PacketSendUtility.sendMessage(admin, "Packet send..");
				PacketSendUtility.sendPacket(admin, packet);
			}
		}
		catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "An error has occurred.");
			log.warn(I18n.get("log.c97d2cc30727", e));
		}
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Usage: //raw [name]");
	}
}