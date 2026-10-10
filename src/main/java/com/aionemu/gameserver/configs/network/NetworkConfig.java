package com.aionemu.gameserver.configs.network;

import java.net.InetSocketAddress;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 游戏服务器网络相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Game server network related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver.network")
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
	public static int PACKET_PROCESSOR_MIN_THREADS = 4;

	/**
	 * 执行客户端数据包的最大线程数。
	 * Maximum threads used to execute Aion client packets.
	 */
	@Property(key = "gameserver.network.packet.processor.threads.max", defaultValue = "4")
	public static int PACKET_PROCESSOR_MAX_THREADS = 4;

	/**
	 * 判定可回收多余线程的阈值（min == max 时无效）。
	 * Threshold for killing extra threads (no effect when min threads == max threads).
	 */
	@Property(key = "gameserver.network.packet.processor.threshold.kill", defaultValue = "3")
	public static int PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 3;

	/**
	 * 判定应扩容额外线程的阈值（min == max 时无效）。
	 * Threshold for spawning extra threads (no effect when min threads == max threads).
	 */
	@Property(key = "gameserver.network.packet.processor.threshold.spawn", defaultValue = "50")
	public static int PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 50;

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

	private final ClientProperties client = new ClientProperties();
	private final LoginProperties login = new LoginProperties();
	private final NioProperties nio = new NioProperties();
	private final PacketProperties packet = new PacketProperties();
	private final DisplayProperties display = new DisplayProperties();
	private final FloodProperties flood = new FloodProperties();

	public String getAddress() {
		return PUBLIC_ADDRESS;
	}

	public void setAddress(String address) {
		PUBLIC_ADDRESS = address;
	}

	public ClientProperties getClient() {
		return client;
	}

	public LoginProperties getLogin() {
		return login;
	}

	public NioProperties getNio() {
		return nio;
	}

	public PacketProperties getPacket() {
		return packet;
	}

	public DisplayProperties getDisplay() {
		return display;
	}

	public FloodProperties getFlood() {
		return flood;
	}

	public static class ClientProperties {
		public int getPort() {
			return GAME_PORT;
		}

		public void setPort(int port) {
			GAME_PORT = port;
		}

		public String getHost() {
			return GAME_BIND_ADDRESS;
		}

		public void setHost(String host) {
			GAME_BIND_ADDRESS = host;
		}

		public int getMaxplayers() {
			return MAX_ONLINE_PLAYERS;
		}

		public void setMaxplayers(int maxplayers) {
			MAX_ONLINE_PLAYERS = maxplayers;
		}
	}

	public static class LoginProperties {
		public String getAddress() {
			return LOGIN_ADDRESS != null ? (LOGIN_ADDRESS.getHostString() + ":" + LOGIN_ADDRESS.getPort()) : null;
		}

		public void setAddress(String address) {
			LOGIN_ADDRESS = parseSocketAddress(address);
		}

		public int getGsid() {
			return GAMESERVER_ID;
		}

		public void setGsid(int gsid) {
			GAMESERVER_ID = gsid;
		}

		public String getPassword() {
			return LOGIN_PASSWORD;
		}

		public void setPassword(String password) {
			LOGIN_PASSWORD = password;
		}
	}

	public static class NioProperties {
		public int getThreads() {
			return NIO_READ_WRITE_THREADS;
		}

		public void setThreads(int threads) {
			NIO_READ_WRITE_THREADS = threads;
		}
	}

	public static class PacketProperties {
		private final ProcessorProperties processor = new ProcessorProperties();

		public ProcessorProperties getProcessor() {
			return processor;
		}

		public static class ProcessorProperties {
			private final ThreadsProperties threads = new ThreadsProperties();
			private final ThresholdProperties threshold = new ThresholdProperties();

			public ThreadsProperties getThreads() {
				return threads;
			}

			public ThresholdProperties getThreshold() {
				return threshold;
			}

			public static class ThreadsProperties {
				public int getMin() {
					return PACKET_PROCESSOR_MIN_THREADS;
				}

				public void setMin(int min) {
					PACKET_PROCESSOR_MIN_THREADS = min;
				}

				public int getMax() {
					return PACKET_PROCESSOR_MAX_THREADS;
				}

				public void setMax(int max) {
					PACKET_PROCESSOR_MAX_THREADS = max;
				}
			}

			public static class ThresholdProperties {
				public int getKill() {
					return PACKET_PROCESSOR_THREAD_KILL_THRESHOLD;
				}

				public void setKill(int kill) {
					PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = kill;
				}

				public int getSpawn() {
					return PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD;
				}

				public void setSpawn(int spawn) {
					PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = spawn;
				}
			}
		}
	}

	public static class DisplayProperties {
		public boolean isUnknownpackets() {
			return DISPLAY_UNKNOWNPACKETS;
		}

		public void setUnknownpackets(boolean unknownpackets) {
			DISPLAY_UNKNOWNPACKETS = unknownpackets;
		}

		public boolean isPackets() {
			return DISPLAY_PACKETS;
		}

		public void setPackets(boolean packets) {
			DISPLAY_PACKETS = packets;
		}
	}

	public static class FloodProperties {
		private final ShortProperties shortWindow = new ShortProperties();
		private final LongProperties longWindow = new LongProperties();

		public boolean isConnections() {
			return ENABLE_FLOOD_CONNECTIONS;
		}

		public void setConnections(boolean connections) {
			ENABLE_FLOOD_CONNECTIONS = connections;
		}

		public int getTick() {
			return Flood_Tick;
		}

		public void setTick(int tick) {
			Flood_Tick = tick;
		}

		public ShortProperties getShort() {
			return shortWindow;
		}

		public LongProperties getLong() {
			return longWindow;
		}

		public static class ShortProperties {
			public int getWarn() {
				return Flood_SWARN;
			}

			public void setWarn(int warn) {
				Flood_SWARN = warn;
			}

			public int getReject() {
				return Flood_SReject;
			}

			public void setReject(int reject) {
				Flood_SReject = reject;
			}

			public int getTick() {
				return Flood_STick;
			}

			public void setTick(int tick) {
				Flood_STick = tick;
			}
		}

		public static class LongProperties {
			public int getWarn() {
				return Flood_LWARN;
			}

			public void setWarn(int warn) {
				Flood_LWARN = warn;
			}

			public int getReject() {
				return Flood_LReject;
			}

			public void setReject(int reject) {
				Flood_LReject = reject;
			}

			public int getTick() {
				return Flood_LTick;
			}

			public void setTick(int tick) {
				Flood_LTick = tick;
			}
		}
	}

	private static InetSocketAddress parseSocketAddress(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String[] parts = value.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid socket address format, expected host:port, got: " + value);
		}
		int port = Integer.parseInt(parts[1].trim());
		String host = parts[0].trim();
		if ("*".equals(host)) {
			return new InetSocketAddress(port);
		}
		return new InetSocketAddress(host, port);
	}
}
