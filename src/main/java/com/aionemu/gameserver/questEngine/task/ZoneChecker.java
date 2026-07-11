package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 区域目的地检查器：跟随生物进入指定区域即判定到达。
 * Zone destination checker: success when the follower is inside the given zone.
 */
final class ZoneChecker extends DestinationChecker {

	/** 目标区域名。 Target zone name. */
	private final ZoneName zoneName;

	/**
	 * 构造区域目的地检查器。
	 * Constructs a zone destination checker.
	 *
	 * Follower creature
	 * Target zone
	 */
	ZoneChecker(Creature follower, ZoneName zoneName) {
		this.follower = follower;
		this.zoneName = zoneName;
	}

	/**
	 * 判断跟随者是否在目标区域内。
	 * Returns whether the follower is inside the target zone.
	 *
	 * @return true 表示在区域内 / true if inside the zone
	 */
	@Override
	boolean check() {
		return follower.isInsideZone(zoneName);
	}
}

/**
 * 双区域目的地检查器：跟随生物进入任一指定区域即判定到达。
 * Dual-zone destination checker: success when the follower is inside either of the two zones.
 */
final class ZoneChecker2 extends DestinationChecker {

	/** 目标区域 1 与 2。 Target zones 1 and 2. */
	private final ZoneName zone1, zone2;

	/**
	 * 构造双区域目的地检查器。
	 * Constructs a dual-zone destination checker.
	 *
	 * Follower creature
	 * Zone 1
	 * Zone 2
	 */
	ZoneChecker2(Creature follower, ZoneName zone1, ZoneName zone2) {
		this.follower = follower;
		this.zone1 = zone1;
		this.zone2 = zone2;
	}

	/**
	 * 判断跟随者是否在任一目标区域内。
	 * Returns whether the follower is inside either target zone.
	 *
	 * @return true 表示在 zone1 或 zone2 内 / true if inside zone1 or zone2
	 */
	@Override
	boolean check() {
		return follower.isInsideZone(zone1) || follower.isInsideZone(zone2);
	}
}
