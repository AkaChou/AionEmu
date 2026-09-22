package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.Setter;
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
    /**
     * -- SETTER --
     *  设置 Spring 实例提供者。
     *  Sets the Spring instance provider.
     */
    @Setter
    private static volatile ObjectProvider<StaticDoorService> instanceProvider;

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
	public static StaticDoorService getInstance() {
		ObjectProvider<StaticDoorService> provider = instanceProvider;
		StaticDoorService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("StaticDoorService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

    /**
	 * 尝试打开指定静态门；管理员会收到门/钥匙 ID 提示。
	 * Attempts to open the given static door; admins receive door/key id hints.
	 * 玩家 / player
	 * door id
	 */
	public void openStaticDoor(final Player player, int doorId) {
		if (canBypassDoorKey(player)) {
			PacketSendUtility.sendMessage(player, "Door Id: " + doorId);
		}
		StaticDoor door = player.getPosition().getWorldMapInstance().getDoors().get(doorId);
		if (door == null) {
			log.warn(I18n.get("log.63610a6189ea", player.getWorldId(), doorId));
			return;
		}
		int keyId = door.getObjectTemplate().getKeyId();
		if (canBypassDoorKey(player)) {
			PacketSendUtility.sendMessage(player, "Key Id: " + keyId);
		}
		boolean opened = false;
		synchronized (door) {
			if (!door.isOpen() && checkStaticDoorKey(player, doorId, keyId)) {
				door.setOpen(true);
				opened = true;
			}
		}
		if (opened) {
			InstanceService.onOpenDoor(player, doorId);
		}
	}

	/**
	 * 校验玩家是否可开启该门（管理员、无钥匙门或消耗钥匙）。
	 * Checks whether the player may open the door (admin, keyless, or consume a key).
	 * 玩家 / player
	 * door id
	 * @param keyId 钥匙物品 ID；0 无需钥匙，1 禁止开启 / key item id; 0 none, 1 locked
	 * @return 允许开启返回 true / true if the door may be opened
	 */
	public boolean checkStaticDoorKey(Player player, int doorId, int keyId) {
		if (canBypassDoorKey(player)) {
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

	/**
	 * 判断管理员是否按配置拥有免钥匙开门权限。
	 * Checks whether the administrator is configured to bypass door keys.
	 * @param player 玩家 / player
	 * @return 允许免钥匙开门返回 true / true if key bypass is allowed
	 */
	private boolean canBypassDoorKey(Player player) {
		return player.getAccessLevel() >= AdminConfig.DOORS_OPEN;
	}
}
