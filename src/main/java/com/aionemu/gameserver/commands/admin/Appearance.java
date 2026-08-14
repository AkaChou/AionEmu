package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员外观修改命令：调整目标玩家体型、发型、脸型等外观，或重置。
 * Admin appearance command: adjusts target player size/hair/face/etc., or resets appearance.
 *
 * @author Divinity
 */
public class Appearance extends AdminCommand {

	/**
	 * 注册 {@code //appearance} 命令。
	 * Registers the {@code //appearance} command.
	 */
	public Appearance() {
		super("appearance");
	}

	/**
	 * 执行外观修改：按属性名写入外观数据并刷新客户端。
	 * Executes appearance change: writes the attribute and refreshes the client.
	 *
	 * @param params 参数：属性名与值，或 reset / attribute name and value, or reset
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			onFail(admin, null);
			return;
		}

		VisibleObject target = admin.getTarget();
		Player player;

		if (target == null)
			player = admin;
		else
			player = (Player) target;

		if (params[0].equals("reset")) {
			PlayerAppearance savedPlayerAppearance = player.getSavedPlayerAppearance();

			if (savedPlayerAppearance == null) {
				PacketSendUtility.sendMessage(admin, "The target has already the normal appearance.");
				return;
			}

			// 用已保存的玩家外观编辑当前玩家外观。 / Edit the current player's appearance with the saved player's appearance
			player.setPlayerAppearance(savedPlayerAppearance);

			// 见第 44 行 / See line 44
			player.setSavedPlayerAppearance(null);

			// 警告玩家 / Warn the player
			PacketSendUtility.sendMessage(player, "An admin has resetted your appearance.");

			// 发送更新包 / Send update packets
			TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(),
				player.getZ(), player.getHeading());

			return;
		}

		if (params.length < 2) {
			onFail(player, null);
			return;
		}

		// 获取当前玩家外观 / Get the current player's appearance
		PlayerAppearance playerAppearance = player.getPlayerAppearance();

		// 保存干净的玩家外观 / Save a clean player's appearance
		if (player.getSavedPlayerAppearance() == null)
			player.setSavedPlayerAppearance((PlayerAppearance) playerAppearance.clone());

		if (params[0].equals("size")) // Edit player's size. Min: 0, Max: 50 (prevent bug)
		{
			float height;

			try {
				height = Float.parseFloat(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (height < 0 || height > 50) {
				PacketSendUtility.sendMessage(admin, "Size: Min value : 0 - Max value : 50");
				return;
			}

			// 编辑身高 / Edit the height
			playerAppearance.setHeight(height);
		}
		else if (params[0].equals("voice")) // Min: 0, Max: 3
		{
			int voice;

			try {
				voice = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (voice < 0 || voice > 3) {
				PacketSendUtility.sendMessage(admin, "Voice: Min value : 0 - Max value : 3");
				return;
			}

			// 编辑声音 / Edit the voice
			playerAppearance.setVoice(voice);
		}
		else if (params[0].equals("hair")) // Min: 1, Max: 43
		{
			int hair;

			try {
				hair = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (hair < 1 || hair > 43) {
				PacketSendUtility.sendMessage(admin, "Hair: Min value : 1 - Max value : 43");
				return;
			}

			// 编辑头发 / Edit the hair
			playerAppearance.setHair(hair);
		}
		else if (params[0].equals("face")) // Min: 1, Max: 24
		{
			int face;

			try {
				face = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (face < 1 || face > 24) {
				PacketSendUtility.sendMessage(admin, "Face: Min value : 1 - Max value : 24");
				return;
			}

			// 编辑面部 / Edit the face
			playerAppearance.setFace(face);
		}
		else if (params[0].equals("deco")) // Min: 1, Max: 18
		{
			int deco;

			try {
				deco = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (deco < 1 || deco > 18) {
				PacketSendUtility.sendMessage(admin, "Deco: Min value : 1 - Max value : 18");
				return;
			}

			// 编辑装饰 / Edit the deco
			playerAppearance.setDeco(deco);
		}
		else if (params[0].equals("head_size")) // Min: 0, Max: 100
		{
			int head;

			try {
				head = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (head < 0 || head > 100) {
				PacketSendUtility.sendMessage(admin, "Head Size: Min value : 0 - Max value : 100");
				return;
			}

			// 编辑头部 / Edit the head
			playerAppearance.setHeadSize(head + 200);
		}
		else if (params[0].equals("tattoo")) // Min: 1, Max: 13
		{
			int tattoo;

			try {
				tattoo = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "The value must be a number !");
				onFail(player, e.getMessage());
				return;
			}

			if (tattoo < 1 || tattoo > 13) {
				PacketSendUtility.sendMessage(admin, "Tattoo: Min value : 1 - Max value : 13");
				return;
			}

			// 编辑纹身 / Edit the tattoo
			playerAppearance.setTattoo(tattoo);
		}
		else {
			onFail(player, null);
			return;
		}

		// 用我们的修改编辑当前玩家外观 / Edit the current player's appearance with our modifications
		player.setPlayerAppearance(playerAppearance);

		// 警告玩家 / Warn the player
		PacketSendUtility.sendMessage(player, "An admin has changed your appearance.");

		// 发送更新包 / Send update packets
		TeleportService2.teleportTo(player, player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(),
			player.getZ(), player.getHeading());
	}

	/**
	 * 参数错误时输出 {@code //appearance} 用法。
	 * Prints {@code //appearance} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntax = "Syntax: //appearance <size | voice | hair | face | deco | head_size | tattoo | reset (to reset the appearance)> <value>";
		PacketSendUtility.sendMessage(player, syntax);
	}
}