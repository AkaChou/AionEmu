package com.aionemu.gameserver.model.templates.windstreams;

/**
 * 风道路径：传送 ID 与距离。
 * Windstream path: teleport id and distance.
 */

public class WindstreamPath {
	public int teleportId;
	public int distance;

	public WindstreamPath(int teleportId, int distance) {
		this.teleportId = teleportId;
		this.distance = distance;
	}
}
