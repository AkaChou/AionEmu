package com.aionemu.gameserver.services.siegeservice;

/**
 * 神器突击实现，调度神器攻城的龙族突击。
 * Artifact assault implementation scheduling Balaur assaults on artifacts.
 */
public class ArtifactAssault extends Assault<ArtifactSiege> {

	public ArtifactAssault(ArtifactSiege siege) {
		super(siege);
	}

	/**
	 * 调度突击。
	 * Schedules the assault.
	 *
	 * @param delay 延迟毫秒 / delay
	 */
	public void scheduleAssault(int delay) {
	}

	/**
	 * 突击结束回调。
	 * Callback when assault finishes.
	 *
	 * @param captured 是否占领成功 / whether captured
	 */
	public void onAssaultFinish(boolean captured) {
	}
}