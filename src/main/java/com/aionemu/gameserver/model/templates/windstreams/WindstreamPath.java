package com.aionemu.gameserver.model.templates.windstreams;

/**
 * 风道路径模板（静态数据/XML）。
 * XML template.
 */

public class WindstreamPath {
	public int teleportId;
	public int distance;

	public WindstreamPath(int teleportId, int distance) {

		this.teleportId = teleportId;
		this.distance = distance;
	}
}
