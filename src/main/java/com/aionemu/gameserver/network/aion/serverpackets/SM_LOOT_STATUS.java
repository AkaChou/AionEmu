package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collections;
import java.util.Set;

import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 掉落状态变更的服务端包（启用/禁用拾取、打开/关闭掉落列表）。
 * Server packet for loot status changes (enable/disable loot, open/close drop list).
 *
 * @author alexa026
 */
public class SM_LOOT_STATUS extends AionServerPacket {

	private final int targetObjectId;
	private final Status status;
	private final int lootEffectId;

	/**
	 * 构造指定目标的掉落状态包。
	 * Builds a loot-status packet for the given target.
	 *
	 * @param targetObjectId 掉落目标对象 ID / loot target object id
	 * loot status
	 */
	public SM_LOOT_STATUS(int targetObjectId, Status status) {
		this.targetObjectId = targetObjectId;
		this.status = status;
		this.lootEffectId = status == Status.LOOT_ENABLE ? getLootEffect(targetObjectId) : 0;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjectId);
		writeC(status.getId());
		writeD(lootEffectId);
	}

	/**
	 * 从当前掉落表中读取首个非零拾取特效 ID。
	 * Resolves the first non-zero loot effect id from the current drop map.
	 *
	 * @param targetObjectId 掉落目标对象 ID / loot target object id
	 * effect id, or 0 if none
	 */
	private static int getLootEffect(int targetObjectId) {
		Set<DropItem> items = DropRegistrationService.getInstance().getCurrentDropMap()
				.getOrDefault(targetObjectId, Collections.emptySet());
		synchronized (items) {
			return items.stream().mapToInt(DropItem::getLootEffectId).filter(id -> id != 0).findAny().orElse(0);
		}
	}

	/**
	 * 掉落状态枚举。
	 * Loot status enum.
	 */
	public enum Status {
		LOOT_ENABLE(0),
		LOOT_DISABLE(1),
		OPEN_DROP_LIST(2),
		CLOSE_DROP_LIST(3);

		private final int id;

		Status(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}
}
