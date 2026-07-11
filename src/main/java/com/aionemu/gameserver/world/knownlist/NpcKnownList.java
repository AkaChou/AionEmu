package com.aionemu.gameserver.world.knownlist;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.world.MapRegion;

/**
 * NPC 使用的已知列表：区域不活跃时清空，活跃时再刷新。
 * NPC known list: clears when the region is inactive, refreshes while active.
 *
 * @author ATracer
 */
public class NpcKnownList extends CreatureAwareKnownList {

	/**
	 * 创建 NPC 已知列表。
	 * Creates an NPC known list.
	 *
	 * @param owner 列表所有者 / list owner
	 */
	public NpcKnownList(VisibleObject owner) {
		super(owner);
	}

	/**
	 * {@inheritDoc}
	 *
	 * 仅当所属地图区域处于活跃状态时执行更新；否则清空列表。
	 * Updates only while the active map region is live; otherwise clears the list.
	 */
	@Override
	public void doUpdate() {
		MapRegion activeRegion = owner.getActiveRegion();
		if (activeRegion != null && activeRegion.isMapRegionActive()) {
			super.doUpdate();
		} else {
			clear();
		}
	}
}
