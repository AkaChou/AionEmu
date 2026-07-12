package com.aionemu.gameserver.network;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_MACBAN_CONTROL;

/**
 * MAC 地址封禁管理器，维护内存封禁表并同步至登录服。
 * MAC ban manager maintaining an in-memory ban table and syncing to the login server.
 *
 * @author KID
 */
@Slf4j
public class BannedMacManager {
	private static BannedMacManager manager = new BannedMacManager();

	/**
	 * 获取管理器单例。
	 * Returns the manager singleton.
	 *
	 * @return 管理器实例 / manager instance
	 */
	public static BannedMacManager getInstance() {
		return manager;
	}

	/** 按 MAC 索引的封禁表 / Ban table keyed by MAC */
	private Map<String, BannedMacEntry> bannedList = new ConcurrentHashMap<>();

	/**
	 * 封禁指定 MAC，并通知登录服。
	 * Bans the given MAC and notifies the login server.
	 *
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
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_MACBAN_CONTROL((byte) 1, address, newTime, details));
	}

	/**
	 * 解除指定 MAC 的封禁，并通知登录服。
	 * Unbans the given MAC and notifies the login server.
	 *
	 * target MAC
	 * details
	 *
	 * @return 是否成功解除 / true if unbanned
	 */
	public final boolean unbanAddress(String address, String details) {
		if (bannedList.containsKey(address)) {
			bannedList.remove(address);
			log.info(I18n.get("log.e5131f1cc020", address, details));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_MACBAN_CONTROL((byte) 0, address, 0, details));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断指定 MAC 当前是否被封禁。
	 * Whether the given MAC is currently banned.
	 *
	 * target MAC
	 *
	 * @param address
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
	 *
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
