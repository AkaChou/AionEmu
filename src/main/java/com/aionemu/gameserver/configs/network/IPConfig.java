package com.aionemu.gameserver.configs.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.aionemu.commons.network.IPRange;
import org.springframework.stereotype.Component;

/**
 * 加载游戏服对外地址，提供静态门面与 Spring Bean 两种访问方式。
 * Loads the game-server public address, exposing both a static facade and a Spring bean accessor.
 *
 * <p>对外地址由遗留 {@code Config.load()} 在 Spring 上下文刷新之后解析写入
 * {@link NetworkConfig#PUBLIC_ADDRESS}；本类刻意不注册 Spring 生命周期回调，避免在遗留配置装载前
 * 提前解析并把回环兜底地址固化下来。</p>
 * The public address is resolved by the legacy {@code Config.load()} flow after the Spring context
 * has refreshed, so this class deliberately registers no Spring lifecycle callback: resolving early
 * would freeze the loopback fallback before the legacy configuration was applied.
 *
 * @author Taran, SoulKeeper
 */
@Component
public class IPConfig {
	/**
	 * 默认对外地址字节。
	 * Default public address bytes.
	 */
	private static byte[] defaultAddress;

	/**
	 * 加载 IP 配置（支持启动覆盖项）。
	 * Loads IP configuration (supports boot overrides).
	 */
	public static void load() {
		String address = firstNonBlank(
			NetworkConfig.PUBLIC_ADDRESS,
			"127.0.0.1"
		);
		try {
			defaultAddress = InetAddress.getByName(address).getAddress();
		} catch (UnknownHostException e) {
			throw new Error("Can't resolve game server address: " + address, e);
		}
	}

	/**
	 * 返回首个非空白字符串。
	 * Returns the first non-blank string.
	 */
	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}

	/**
	 * 返回 IP 段映射列表。
	 * Returns the list of IP ranges.
	 *
	 * @return IP 段映射列表 / List of IP ranges
	 */
	public static List<IPRange> getRanges() {
		return List.of();
	}

	/**
	 * 返回默认对外地址。
	 * Returns the default public address.
	 *
	 * @return 默认地址字节 / Default address bytes
	 */
	public static byte[] getDefaultAddress() {
		return defaultAddress;
	}

	/**
	 * 实例方法：获取对外地址字节。
	 * Instance method: returns public address bytes.
	 *
	 * @return 默认地址字节 / Default address bytes
	 */
	public byte[] getPublicAddress() {
		return getDefaultAddress();
	}

	/**
	 * 实例方法：获取 IP 段列表。
	 * Instance method: returns IP ranges.
	 */
	public List<IPRange> ipRanges() {
		return getRanges();
	}
}
