package com.aionemu.gameserver.network;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_MACBAN_CONTROL;

/**
 * MAC 地址封禁管理器，维护内存封禁表并同步至登录服。
 * MAC ban manager maintaining an in-memory ban table and syncing to the login server.
 * @author KID
 */
@Slf4j
public class BannedMacManager {
	private static volatile ObjectProvider<BannedMacManager> instanceProvider;
	private static volatile BannedMacManager resolvedInstance;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static BannedMacManager getInstance() {
		BannedMacManager resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<BannedMacManager> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("BannedMacManager 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<BannedMacManager> instanceProvider) {
		BannedMacManager.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/** 按 MAC 索引的封禁表 / Ban table keyed by MAC */
	private final Map<String, BannedMacEntry> bannedList = new ConcurrentHashMap<>();

	/**
	 * 封禁指定 MAC，并通知登录服。
	 * Bans the given MAC and notifies the login server.
	 * target MAC
	 * @param newTime 截止时间戳（毫秒） / end timestamp in ms
	 * details
	 */
	public final void banAddress(String address, long newTime, String details) {
		BannedMacEntry entry;
		if (bannedList.containsKey(address)) {
			if (bannedList.get(address).isActiveTill(newTime)) {
				return;
			} else {
				entry = bannedList.get(address);
				entry.updateTime(newTime);
			}
		} else {
			entry = new BannedMacEntry(address, newTime);
		}
		entry.setDetails(details);

		bannedList.put(address, entry);

		log.info(I18n.get("log.595215b863dc", address, entry.getTime().toString(), details));
		LoginServer.getInstance().sendPacket(new SM_MACBAN_CONTROL((byte) 1, address, newTime, details));
	}

	/**
	 * 解除指定 MAC 的封禁，并通知登录服。
	 * Unbans the given MAC and notifies the login server.
	 * @param address target MAC
	 * @param details details
	 * @return 是否成功解除 / true if unbanned
	 */
	public final boolean unbanAddress(String address, String details) {
		if (bannedList.containsKey(address)) {
			bannedList.remove(address);
			log.info(I18n.get("log.e5131f1cc020", address, details));
			LoginServer.getInstance().sendPacket(new SM_MACBAN_CONTROL((byte) 0, address, 0, details));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断指定 MAC 当前是否被封禁。
	 * Whether the given MAC is currently banned.
	 * target MAC
	 * @return 是否封禁中 / true if banned
	 */
	public final boolean isBanned(String address) {
		if (bannedList.containsKey(address)) {
			return this.bannedList.get(address).isActive();
		} else {
			return false;
		}
	}

	/**
	 * 从数据库加载一条封禁记录到内存。
	 * Loads one ban record from DB into memory.
	 * target MAC
	 * @param time 截止时间戳（毫秒） / end timestamp in ms
	 * details
	 */
	public final void dbLoad(String address, long time, String details) {
		this.bannedList.put(address, new BannedMacEntry(address, new Timestamp(time), details));
	}

	/**
	 * 服务结束时输出当前封禁数量。
	 * Logs the current ban count on shutdown.
	 */
	public void onEnd() {
		log.info(I18n.get("log.d9df101cc7b2", this.bannedList.size()));
	}
}
