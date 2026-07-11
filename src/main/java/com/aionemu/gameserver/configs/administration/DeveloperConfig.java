package com.aionemu.gameserver.configs.administration;

import com.aionemu.commons.configuration.Property;

/**
 * 开发调试相关配置（刷怪、道具属性、封包显示等）。
 * Developer debugging configuration (spawns, item stats, packet display, etc.).
 */
public class DeveloperConfig {

	/**
	 * 是否加载刷怪数据；为 false 时不加载任何刷怪。
	 * Whether spawn data is loaded; if false, no spawns are loaded.
	 */
	@Property(key = "gameserver.developer.spawn.enable", defaultValue = "true")
	public static boolean SPAWN_ENABLE;

	/**
	 * 是否检查刷怪是否落在已知区域之外。
	 * Whether to check spawns that lie outside any known zones.
	 */
	@Property(key = "gameserver.developer.spawn.check", defaultValue = "false")
	public static boolean SPAWN_CHECK;

	/**
	 * 为带随机词缀的物品附加指定属性加成 ID（0 表示不附加）。
	 * Stat bonus ID applied to items with random bonuses (0 = none).
	 */
	@Property(key = "gameserver.developer.itemstat.id", defaultValue = "0")
	public static int ITEM_STAT_ID;

	/**
	 * 是否在游戏服日志中打印收发的 CM/SM 封包。
	 * Whether to log sent/received CM/SM packets in the game server log.
	 */
	@Property(key = "gameserver.developer.showpackets.enable", defaultValue = "false")
	public static boolean SHOW_PACKETS;

	/**
	 * 是否在聊天窗口显示封包名称。
	 * Whether to display packet names in the chat window.
	 */
	@Property(key = "gameserver.developer.show.packetnames.inchat.enable", defaultValue = "false")
	public static boolean SHOW_PACKET_NAMES_INCHAT;

	/**
	 * 是否在聊天窗口显示封包十六进制字节。
	 * Whether to display packet hex bytes in the chat window.
	 */
	@Property(key = "gameserver.developer.show.packetbytes.inchat.enable", defaultValue = "false")
	public static boolean SHOW_PACKET_BYTES_INCHAT;

	/**
	 * 聊天窗口中显示的封包字节数上限（默认 200 个十六进制字节）。
	 * Max packet bytes shown in chat (default: 200 hex bytes).
	 */
	@Property(key = "gameserver.developer.show.packetbytes.inchat.total", defaultValue = "200")
	public static int TOTAL_PACKET_BYTES_INCHAT;

	/**
	 * 聊天窗口中显示的封包名称过滤器（* 表示全部；如 SM_MOVE, CM_CASTSPELL）。
	 * Packet name filter for chat display (* = all; e.g. SM_MOVE, CM_CASTSPELL).
	 */
	@Property(key = "gameserver.developer.filter.packets.inchat", defaultValue = "*")
	public static String FILTERED_PACKETS_INCHAT;

	/**
	 * 可在聊天窗口查看封包名称/字节的最低权限等级（建议 ≥ 3；默认 6）。
	 * Minimum access level to see packet names/bytes in chat (recommended ≥ 3; default 6).
	 */
	@Property(key = "gameserver.developer.show.packets.inchat.accesslevel", defaultValue = "6")
	public static int SHOW_PACKETS_INCHAT_ACCESSLEVEL;
}
