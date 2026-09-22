package com.aionemu.gameserver.spawnengine;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 巡逻组成员相对基准点的偏移（矢状/冠状）。
 * Relative offset of a walker group member (sagittal/coronal).
 * @author Rolandas
 */
@Getter
@AllArgsConstructor
public class WalkerGroupShift {

	/**
	 * 左右（矢状）偏移。
	 * Left/right (sagittal) shift.
	 */
	private final float sagittalShift;

	/**
	 * 前后（冠状）偏移。
	 * Back/front (coronal) shift.
	 */
	private final float coronalShift;

	/**
	 * 默认成员间距（米）。
	 * Default inter-member distance in meters.
	 */
	public static final float DISTANCE = 2;
}
