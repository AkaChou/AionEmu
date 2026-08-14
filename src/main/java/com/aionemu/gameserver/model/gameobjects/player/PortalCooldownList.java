package com.aionemu.gameserver.model.gameobjects.player;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门冷却列表。
 * Portal Cooldown List game object.
 */

public class PortalCooldownList {
	private Player owner;
	private Map<Integer, PortalCooldownItem> portalCooldowns;

	PortalCooldownList(Player owner) {
		this.owner = owner;
	}

	/**
	 * @param worldId 传送门是否禁用所查的世界 ID / Whether portal use disabled
	 */
	public boolean isPortalUseDisabled(int worldId) {
		if (portalCooldowns == null || !portalCooldowns.containsKey(worldId)) {
			return false;
		}
		PortalCooldownItem coolDown = portalCooldowns.get(worldId);
		if (coolDown == null) {
			return false;
		}
		if (DataManager.INSTANCE_COOLTIME_DATA.getInstanceEntranceCountByWorldId(worldId) == 0 || coolDown
				.getEntryCount() < DataManager.INSTANCE_COOLTIME_DATA.getInstanceEntranceCountByWorldId(worldId)) {
			return false;
		}
		if (coolDown.getCooldown() < System.currentTimeMillis()) {
			portalCooldowns.remove(worldId);
			return false;
		}
		return true;
	}

	/** 获取传送门冷却。 / Returns the portal cooldown. */
	public long getPortalCooldown(int worldId) {
		if (portalCooldowns == null || !portalCooldowns.containsKey(worldId)) {
			return 0;
		}
		return portalCooldowns.get(worldId).getCooldown();
	}

	/** 获取条目计数。 / Returns the entry count. */
	public long getEntryCount(int worldId) {
		if (portalCooldowns == null || !portalCooldowns.containsKey(worldId)) {
			return 0;
		}
		return portalCooldowns.get(worldId).getEntryCount();
	}

	/** 获取传送门冷却物品。 / Returns the portal cooldown item. */
	public PortalCooldownItem getPortalCooldownItem(int worldId) {
		if (portalCooldowns == null || !portalCooldowns.containsKey(worldId)) {
			return null;
		}
		return portalCooldowns.get(worldId);
	}

	/** 返回 portal cool downs / Returns the portal cool downs */
	public Map<Integer, PortalCooldownItem> getPortalCoolDowns() {
		return portalCooldowns;
	}

	/** 设置 portal cool downs / Sets the portal cool downs */
	public void setPortalCoolDowns(Map<Integer, PortalCooldownItem> portalCoolDowns) {
		this.portalCooldowns = portalCoolDowns;
	}

	/** 添加传送门冷却。 / Adds portal cooldown. */
	public void addPortalCooldown(int worldId, int entryCount, long useDelay) {
		if (portalCooldowns == null) {
			portalCooldowns = new HashMap<Integer, PortalCooldownItem>();
		}
		portalCooldowns.put(worldId, new PortalCooldownItem(worldId, entryCount, useDelay));
		if (owner.isInTeam()) {
			owner.getCurrentTeam().sendPacket(new SM_INSTANCE_INFO(owner, worldId));
		} else {
			PacketSendUtility.sendPacket(owner, new SM_INSTANCE_INFO(owner, worldId));
		}
	}

	/** 移除 portal cool down / Removes portal cool down */
	public void removePortalCoolDown(int worldId) {
		if (portalCooldowns != null) {
			portalCooldowns.remove(worldId);
		}
		if (owner.isInTeam()) {
			owner.getCurrentTeam().sendPacket(new SM_INSTANCE_INFO(owner, worldId));
		} else {
			PacketSendUtility.sendPacket(owner, new SM_INSTANCE_INFO(owner, worldId));
			// 你现在可进入 %0 区域。 / You can enter %0 area now.
			PacketSendUtility.sendPacket(owner, new SM_SYSTEM_MESSAGE(1400031, worldId));
		}
	}

	/** 添加条目。 / Adds entry. */
	public void addEntry(int worldId) {
		int floor = owner.getFloor();
		if (floor != 0) {
			return;
		}
		if (portalCooldowns != null && portalCooldowns.containsKey(worldId)) {
			portalCooldowns.get(worldId).setEntryCount(portalCooldowns.get(worldId).getEntryCount() + 1);
		}
		if (owner.isInTeam()) {
			owner.getCurrentTeam().sendPacket(new SM_INSTANCE_INFO(owner, worldId));
		} else {
			PacketSendUtility.sendPacket(owner, new SM_INSTANCE_INFO(owner, worldId));
		}
	}

	/** 减少进入次数 / Reduce Entry */
	public void reduceEntry(int worldId) {
		if (portalCooldowns != null && portalCooldowns.containsKey(worldId)) {
			portalCooldowns.get(worldId).setEntryCount(portalCooldowns.get(worldId).getEntryCount() - 1);
		}
		if (portalCooldowns.get(worldId).getEntryCount() == 0) {
			removePortalCoolDown(worldId);
			return;
		}
		if (owner.isInTeam()) {
			owner.getCurrentTeam().sendPacket(new SM_INSTANCE_INFO(owner, worldId));
		} else {
			PacketSendUtility.sendPacket(owner, new SM_INSTANCE_INFO(owner, worldId));
		}
	}

	/** 是否冷却 / Whether cooldowns*/
	public boolean hasCooldowns() {
		return portalCooldowns != null && portalCooldowns.size() > 0;
	}

	/** 大小 / size. */
	public int size() {
		return portalCooldowns != null ? portalCooldowns.size() : 0;
	}
}
