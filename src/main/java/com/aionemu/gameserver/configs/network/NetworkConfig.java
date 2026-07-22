package com.aionemu.gameserver.configs.network;

import java.net.InetSocketAddress;

import com.aionemu.commons.configuration.Property;

/**
 * 游戏服务器网络相关配置。
 * Game server network related configuration.
 */
public class NetworkConfig {

	/**
	 * 向客户端公布的游戏服务器地址。
	 * Game-server address advertised to clients.
	 */
	@Property(key = "gameserver.network.address", defaultValue = "127.0.0.1")
	public static String PUBLIC_ADDRESS;

	/**
	 * 游戏服务器端口。
	 * Game server port.
	 */
	@Property(key = "gameserver.network.client.port", defaultValue = "7777")
	public static int GAME_PORT;

	/**
	 * 游戏服务器绑定 IP。
	 * Game server bind IP.
	 */
	@Property(key = "gameserver.network.client.host", defaultValue = "*")
	public static String GAME_BIND_ADDRESS;

	/**
	 * 允许的最大在线玩家数。
	 * Max allowed online players.
	 */
	@Property(key = "gameserver.network.client.maxplayers", defaultValue = "100")
	public static int MAX_ONLINE_PLAYERS;

	/**
	 * 登录服务器地址。
	 * LoginServer address.
	 */
	@Property(key = "gameserver.network.login.address", defaultValue = "localhost:9014")
	public static InetSocketAddress LOGIN_ADDRESS;

	/**
	 * 聊天服务器地址。
	 * ChatServer address.
	 */
	@Property(key = "chatserver.network.gameserver.address", defaultValue = "localhost:9021")
	public static InetSocketAddress CHAT_ADDRESS;

	/**
	 * 向游戏客户端公布的聊天服务器地址。
	 * Chat-server address advertised to game clients.
	 */
	@Property(key = "chatserver.network.public.address", defaultValue = "localhost:10241")
	public static InetSocketAddress PUBLIC_CHAT_ADDRESS;

	/**
	 * 本游戏服在聊天服认证用的密码。
	 * Password for this GameServer ID for authentication at ChatServer.
	 */
	@Property(key = "chatserver.network.gameserver.password", defaultValue = "")
	public static String CHAT_PASSWORD;

	/**
	 * 本游戏服向登录服申请的游戏服 ID。
	 * GameServer id that this GameServer will request at LoginServer.
	 */
	@Property(key = "gameserver.network.login.gsid", defaultValue = "0")
	public static int GAMESERVER_ID;

	/**
	 * 本游戏服在登录服认证用的密码。
	 * Password for this GameServer ID for authentication at LoginServer.
	 */
	@Property(key = "gameserver.network.login.password", defaultValue = "")
	public static String LOGIN_PASSWORD;

	/**
	 * 专用于 IO 读写的线程数；始终另有 1 个 acceptor 线程。
	 * 小于 1 时由 acceptor 兼管读写；大于 0 时为指定数量的读写线程 + 1 个 acceptor。
	 * Number of threads dedicated to IO read & write. There is always 1 acceptor thread.
	 * If value is &lt; 1 the acceptor also handles read & write; if &gt; 0 there will be that many
	 * read/write threads plus 1 acceptor.
	 */
	@Property(key = "gameserver.network.nio.threads", defaultValue = "1")
	public static int NIO_READ_WRITE_THREADS;

	/**
	 * 执行客户端数据包的最小线程数。
	 * Minimum threads used to execute Aion client packets.
	 */
	@Property(key = "gameserver.network.packet.processor.threads.min", defaultValue = "4")
	public static int PACKET_PROCESSOR_MIN_THREADS;

	/**
	 * 执行客户端数据包的最大线程数。
	 * Maximum threads used to execute Aion client packets.
	 */
	@Property(key = "gameserver.network.packet.processor.threads.max", defaultValue = "4")
	public static int PACKET_PROCESSOR_MAX_THREADS;

	/**
	 * 是否记录未知数据包。
	 * Whether unknown packets should be logged.
	 */
	@Property(key = "gameserver.network.display.unknownpackets", defaultValue = "false")
	public static boolean DISPLAY_UNKNOWNPACKETS;

	/**
	 * 是否打印数据包日志。
	 * Whether to display packet logs.
	 */
	@Property(key = "gameserver.network.display.packets", defaultValue = "false")
	public static boolean DISPLAY_PACKETS;

	/**
	 * 是否启用连接洪水防护。
	 * Whether flood connection protection is enabled.
	 */
	@Property(key = "gameserver.network.flood.connections", defaultValue = "false")
	public static boolean ENABLE_FLOOD_CONNECTIONS;

	/**
	 * 洪水检测主周期（毫秒）。
	 * Flood detection main tick interval in milliseconds.
	 */
	@Property(key = "gameserver.network.flood.tick", defaultValue = "1000")
	public static int Flood_Tick;

	/**
	 * 短窗口洪水警告阈值。
	 * Short-window flood warning threshold.
	 */
	@Property(key = "gameserver.network.flood.short.warn", defaultValue = "10")
	public static int Flood_SWARN;

	/**
	 * 短窗口洪水拒绝阈值。
	 * Short-window flood reject threshold.
	 */
	@Property(key = "gameserver.network.flood.short.reject", defaultValue = "20")
	public static int Flood_SReject;

	/**
	 * 短窗口洪水统计周期。
	 * Short-window flood statistics tick.
	 */
	@Property(key = "gameserver.network.flood.short.tick", defaultValue = "10")
	public static int Flood_STick;

	/**
	 * 长窗口洪水警告阈值。
	 * Long-window flood warning threshold.
	 */
	@Property(key = "gameserver.network.flood.long.warn", defaultValue = "30")
	public static int Flood_LWARN;

	/**
	 * 长窗口洪水拒绝阈值。
	 * Long-window flood reject threshold.
	 */
	@Property(key = "gameserver.network.flood.long.reject", defaultValue = "60")
	public static int Flood_LReject;

	/**
	 * 长窗口洪水统计周期。
	 * Long-window flood statistics tick.
	 */
	@Property(key = "gameserver.network.flood.long.tick", defaultValue = "60")
	public static int Flood_LTick;
}
