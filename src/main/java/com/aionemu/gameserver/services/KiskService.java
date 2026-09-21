package com.aionemu.gameserver.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEVEL_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 基斯克（复活石）服务，管理绑定、离线保留与销毁时的成员清理。
 * Kisk (resurrection stone) service managing binds, offline retention, and member cleanup on removal.
 */
public class KiskService {
	private static volatile ObjectProvider<KiskService> instanceProvider;
	/** 已绑定但离线的玩家 → 基斯克。 / Bound-but-offline players to their kisk. */
	private final ConcurrentMap<Integer, Kisk> boundButOfflinePlayer = new ConcurrentHashMap<>();
	/** 基斯克拥有者 → 基斯克。 / Kisk owners to their kisk. */
	private final ConcurrentMap<Integer, Kisk> ownerPlayer = new ConcurrentHashMap<>();

	/**
	 * 移除基斯克并清理所有绑定成员的状态。
	 * Removes a kisk and clears bind state for all members.
	 *
	 * kisk
	 */
	public void removeKisk(Kisk kisk) {
		for (int memberId : kisk.getCurrentMemberIds()) {
			boundButOfflinePlayer.remove(memberId);
		}
		for (Map.Entry<Integer, Kisk> entry : ownerPlayer.entrySet()) {
			if (entry.getValue().equals(kisk)) {
				ownerPlayer.remove(entry.getKey(), kisk);
				break;
			}
		}
		for (Player member : kisk.getCurrentMemberList()) {
			member.setKisk(null);
			PacketSendUtility.sendPacket(member, new SM_BIND_POINT_INFO(0, 0f, 0f, 0f, member));
			if (member.getLifeStats().isAlreadyDead()) {
				member.getController().sendDie();
			}
		}
	}

	/**
	 * 玩家绑定到基斯克。
	 * Binds a player to a kisk.
	 *
	 * kisk
	 * 玩家 / player
	 */
	public void onBind(Kisk kisk, Player player) {
		if (player.getKisk() != null) {
			player.getKisk().removePlayer(player);
		}
		kisk.addPlayer(player);
		TeleportService2.sendSetBindPoint(player);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_BINDSTONE_REGISTER);
		PacketSendUtility.broadcastPacket(player,
				new SM_LEVEL_UPDATE(player.getObjectId(), 2, player.getCommonData().getLevel()), true);
	}

	/**
	 * 玩家登录时恢复离线前的基斯克绑定。
	 * Restores offline kisk binding when a player logs in.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		Kisk kisk = this.boundButOfflinePlayer.get(player.getObjectId());
		if (kisk != null) {
			kisk.addPlayer(player);
			this.boundButOfflinePlayer.remove(player.getObjectId());
		}
	}

	/**
	 * 玩家登出时暂存基斯克绑定关系。
	 * Stashes the kisk binding when a player logs out.
	 *
	 * @param player 玩家 / player
	 */
	public void onLogout(Player player) {
		Kisk kisk = player.getKisk();
		if (kisk != null) {
			this.boundButOfflinePlayer.put(player.getObjectId(), kisk);
		}
	}

	/**
	 * 注册基斯克拥有者映射。
	 * Registers the kisk-to-owner mapping.
	 *
	 * kisk
	 * @param objOwnerId 拥有者对象 ID / owner object id
	 */
	public void regKisk(Kisk kisk, Integer objOwnerId) {
		ownerPlayer.put(objOwnerId, kisk);
	}

	/**
	 * 判断指定玩家是否已拥有基斯克。
	 * Returns whether the given owner already has a kisk.
	 *
	 * @param objOwnerId 拥有者对象 ID / owner object id
	 * whether owned
	 */
	public boolean haveKisk(Integer objOwnerId) {
		return ownerPlayer.containsKey(objOwnerId);
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static KiskService getInstance() {
		ObjectProvider<KiskService> provider = instanceProvider;
		KiskService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("KiskService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<KiskService> provider) {
		instanceProvider = provider;
	}
}
