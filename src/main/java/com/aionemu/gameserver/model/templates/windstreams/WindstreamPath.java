package com.aionemu.gameserver.model.templates.windstreams;

import lombok.AllArgsConstructor;

/**
 * 风道路径：传送 ID 与距离。
 * Windstream path: teleport id and distance.
 */

@AllArgsConstructor
public class WindstreamPath {
	public int teleportId;
	public int distance;
}
