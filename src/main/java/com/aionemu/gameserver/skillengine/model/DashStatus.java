package com.aionemu.gameserver.skillengine.model;

/**
 * 冲刺/位移状态：标识技能位移的网络与表现类型。
 * Dash/move status: identifies skill displacement for network and presentation.
 */
public enum DashStatus {

	/** 无位移 / No dash */
	NONE(0),
	/** 随机位置移动 / Random move location */
	RANDOMMOVELOC(1),
	/** 前冲 / Forward dash */
	DASH(2),
	/** 后撤 / Back dash */
	BACKDASH(3),
	/** 移到目标身后 / Move behind target */
	MOVEBEHIND(4),
	/** 骑乘位移 / Rider move location */
	RIDERMOVELOC(6);

	private int id;

	private DashStatus(int id) {
		this.id = id;
	}

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 *
	 */
	public int getId() {
		return id;
	}
}
