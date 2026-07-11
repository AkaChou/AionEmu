package com.aionemu.gameserver.spawnengine;

import lombok.Getter;

/**
 * 巡逻组成员相对基准点的偏移（矢状/冠状）。
 * Relative offset of a walker group member (sagittal/coronal).
 *
 * @author Rolandas
 */
@Getter
public class WalkerGroupShift {

	/**
	 * 左右（矢状）偏移。
	 * Left/right (sagittal) shift.
	 */
	private float sagittalShift;

	/**
	 * 前后（冠状）偏移。
	 * dorsoventral) shift. / dorsoventral) shift.
	 */
	private float coronalShift;

	/**
	 * 默认成员间距（米）。
	 * Default inter-member distance in meters.
	 */
	public static final float DISTANCE = 2;

	/**
	 * 以左右与前后偏移构造。
	 * Builds a shift from left/right and back/front offsets.
	 *
	 * left-right offset
	 * back-front offset
	 */
	public WalkerGroupShift(float leftRight, float backFront) {
		sagittalShift = leftRight;
		coronalShift = backFront;
	}

}
