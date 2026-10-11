package com.aionemu.gameserver.spawnengine;

/**
 * 巡逻组成员相对基准点的偏移（矢状/冠状）。
 * Relative offset of a walker group member (sagittal/coronal).
 *
 * @param sagittalShift 左右（矢状）偏移。
 *                      Left/right (sagittal) shift.
 * @param coronalShift  前后（冠状）偏移。
 *                      Back/front (coronal) shift.
 * @author Rolandas
 */
public record WalkerGroupShift(float sagittalShift, float coronalShift) {

	/**
	 * 默认成员间距（米）。
	 * Default inter-member distance in meters.
	 */
	public static final float DISTANCE = 2;
}
