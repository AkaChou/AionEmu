package com.aionemu.gameserver.model.assemblednpc;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 组装 NPC，用于 assemblednpc 相关逻辑。
 * Assembled Npc for assemblednpc logic.
 *
 * @author xTz
 */
public class AssembledNpc {

	@Getter
	private List<AssembledNpcPart> assembledParts = new ArrayList<>();
	private long spawnTime = System.currentTimeMillis();
	@Getter
	private int routeId;
	@Getter
	private int mapId;

	public AssembledNpc(int routeId, int mapId, int liveTime, List<AssembledNpcPart> assembledParts) {
		this.assembledParts = new ArrayList<>(assembledParts);
		this.routeId = routeId;
		this.mapId = mapId;
	}

	/** 返回时间映射 / Returns the time on map */
	public long getTimeOnMap() {
		return System.currentTimeMillis() - spawnTime;
	}
}
