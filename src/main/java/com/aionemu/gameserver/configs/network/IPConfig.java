package com.aionemu.gameserver.configs.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.aionemu.commons.network.IPRange;
import lombok.Getter;

/**
 * 加载游戏服对外地址，作为静态门面供网络包使用。
 * <p>刻意**不**注册为 Spring Bean：对外地址由遗留 {@code Config.load()} 在 Spring 上下文刷新之后
 * 解析写入 {@link NetworkConfig#PUBLIC_ADDRESS}；在本类上挂 Bean 生命周期只会在遗留配置装载前
 * 提前解析，把回环兜底地址固化下来，而不会带来任何绑定能力。</p>
 * Loads the game-server public address as a static facade for the network packets.
 * <p>Deliberately not a Spring bean: the address is resolved by the legacy {@code Config.load()} flow
 * after the Spring context has refreshed, so a bean lifecycle here would only resolve early and freeze
 * the loopback fallback without adding binding capability.</p>
 * @author Taran, SoulKeeper
 */
public class IPConfig {
	/**
	 * 默认对外地址字节。
	 * Default public address bytes.
     * -- GETTER --
     *  返回默认对外地址。
     *  Returns the default public address.
     */
	@Getter
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
	 * @return IP 段映射列表 / List of IP ranges
	 */
	public static List<IPRange> getRanges() {
		return List.of();
	}

}
