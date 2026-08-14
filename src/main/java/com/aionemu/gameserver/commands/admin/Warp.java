package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.model.TeleportAnimation;

/**
 * 根据聊天位置链接传送的管理员命令（依赖 Geo）。
 * Admin command to warp from a chat location link (requires Geo).
 *
 * @author Source
 * @rework Kill3r
 */
public class Warp extends AdminCommand {

	/**
	 * 构造 warp 命令。
	 * Creates the warp command.
	 */
	public Warp() {
		super("warp");
	}

	/**
	 * 解析位置链接并传送；失败时尝试备用链接格式。
	 * Parses a location link and teleports; falls back to alternate link format.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 位置链接分词 / Location link tokens
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length < 5) {
			onFail(player, "");
			return;
		}

		if (!GeoDataConfig.GEO_ENABLE) {
			onFail(player, "");
			return;
		}

		// [pos:Location;1 120010000 1304.7 1423.1 0.0 0] <-- 使用此坐标格式 / uses this format of Location
		try {

			String LocS, first, last;
			float x, y, z;
			LocS = "";
			int mapL = 0;
			int layerI = -1;
			// int race;

			first = params[0];
			mapL = Integer.parseInt(params[1]);
			x = Float.parseFloat(params[2]);
			y = Float.parseFloat(params[3]);
			z = Float.parseFloat(params[4]);
			last = params[5];

			Pattern f = Pattern.compile("\\[pos:([^;]+);\\s*+(\\d{1})");
			Pattern l = Pattern.compile("(\\d)\\]");
			Matcher fm = f.matcher(first);
			Matcher lm = l.matcher(last);

			if (fm.find()) {
				LocS = fm.group(1);
				// race = Integer.parseInt(fm.group(2));
			}

			if (lm.find()) {
				layerI = Integer.parseInt(lm.group(1));
			}

			z = GameWorldServices.geoService().getZ(mapL, x, y);
			PacketSendUtility.sendMessage(player, "Map ID (" + mapL + ")\n" + "x: " + x + "y: " + y + "z: " + z + " L(" + layerI + ")");

			//if (mapL == 400010000) {
			//	PacketSendUtility.sendMessage(player, "Sorry you can't warp at abyss");
			//}
			//else {
			TeleportService2.teleportTo(player, mapL, x, y, z, player.getHeading(), TeleportAnimation.NO_ANIMATION);
			PacketSendUtility.sendMessage(player, "You have successfully warped to this location --- > " + LocS);
			//}

		}
		catch (NumberFormatException e) {

			// [pos:Location;120010000 1304.7 1423.1 0.0 0] <-- 使用此坐标格式 / uses this format of Location
			if (params.length < 5) {
				onFail(player, "");
				return;
			}

			String locS, first, last;
			float xF, yF, zF;
			locS = "";
			int mapL = 0;
			int layerI = -1;

			first = params[0];
			xF = Float.parseFloat(params[1]);
			yF = Float.parseFloat(params[2]);
			zF = Float.parseFloat(params[3]);
			last = params[4];

			Pattern f = Pattern.compile("\\[pos:([^;]+);\\s*+(\\d{9})");
			Pattern l = Pattern.compile("(\\d)\\]");
			Matcher fm = f.matcher(first);
			Matcher lm = l.matcher(last);

			if (fm.find()) {
				locS = fm.group(1);
				mapL = Integer.parseInt(fm.group(2));
			}
			if (lm.find()) {
				layerI = Integer.parseInt(lm.group(1));
			}

			zF = GameWorldServices.geoService().getZ(mapL, xF, yF);
			PacketSendUtility.sendMessage(player, "MapId (" + mapL + ")\n" + "x:" + xF + " y:" + yF + " z:" + zF + " l(" + layerI + ")");

			//if (mapL == 400010000) {
			//	PacketSendUtility.sendMessage(player, "Sorry you can't warp at abyss");
			//}
			//else {
			TeleportService2.teleportTo(player, mapL, xF, yF, zF, player.getHeading(), TeleportAnimation.NO_ANIMATION);
			PacketSendUtility.sendMessage(player, "You have successfully warp -> " + locS);
			//}
		}
	}

	/**
	 * 参数错误或 Geo 关闭时的提示。
	 * Hint when parameters are invalid or Geo is disabled.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		if (!GeoDataConfig.GEO_ENABLE) {
			PacketSendUtility.sendMessage(player, "You must turn on geo in config to use this command!");
			return;
		}
		PacketSendUtility.sendMessage(player, "syntax //warp <@link>");
	}
}
