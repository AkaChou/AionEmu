package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 静态门服务，处理开门请求与钥匙校验。
 * Static door service that handles open requests and key checks.
 */
@Slf4j
public class StaticDoorService {
	private static volatile ObjectProvider<StaticDoorService> instanceProvider;

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static StaticDoorService getInstance() {
		ObjectProvider<StaticDoorService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<StaticDoorService> provider) {
		instanceProvider = provider;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final StaticDoorService instance = new StaticDoorService();
	}

	/**
	 * 尝试打开指定静态门；管理员会收到门/钥匙 ID 提示。
	 * Attempts to open the given static door; admins receive door/key id hints.
	 *
	 * 玩家 / player
	 * door id
	 */
	public void openStaticDoor(final Player player, int doorId) {
		if (player.getAccessLevel() >= 3) {
			PacketSendUtility.sendMessage(player, "Door Id: " + doorId);
		}
		StaticDoor door = player.getPosition().getWorldMapInstance().getDoors().get(doorId);
		if (door == null) {
			log.warn(I18n.get("log.63610a6189ea", player.getWorldId(), doorId));
			return;
		}
		int keyId = door.getObjectTemplate().getKeyId();
		if (player.getAccessLevel() >= 3) {
			PacketSendUtility.sendMessage(player, "Key Id: " + keyId);
		}
		if (checkStaticDoorKey(player, doorId, keyId)) {
			door.setOpen(true);
		}
		InstanceService.onOpenDoor(player, doorId);
	}

	/**
	 * 校验玩家是否可开启该门（管理员、无钥匙门或消耗钥匙）。
	 * Checks whether the player may open the door (admin, keyless, or consume a key).
	 *
	 * 玩家 / player
	 * door id
	 * @param keyId 钥匙物品 ID；0 无需钥匙，1 禁止开启 / key item id; 0 none, 1 locked
	 * @return 允许开启返回 true / true if the door may be opened
	 */
	public boolean checkStaticDoorKey(Player player, int doorId, int keyId) {
		if (player.getAccessLevel() >= AdminConfig.DOORS_OPEN) {
			return true;
		}
		if (keyId == 0) {
			return true;
		}
		if (keyId == 1) {
			return false;
		}
		if (!player.getInventory().decreaseByItemId(keyId, 1)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1300723, player.getObjectId(), 2));
			return false;
		}
		return true;
	}
}
