package com.aionemu.gameserver.model.templates.windstreams;

/**
 * 风道路径模板（静态数据/XML）。
 * XML template.
 */

public class WindstreamPath {
	public int teleportId;
	public int distance;
	private final WindstreamRoute route;

	public WindstreamPath(WindstreamRoute route, int teleportId, int distance) {
		this.route = route;
		this.teleportId = teleportId;
		this.distance = distance;
	}

	public boolean accepts(int mapId, int distance, float x, float y, float z) {
		return route.getMapId() == mapId && route.contains(distance, x, y, z, 50);
	}
}
