package com.aionemu.gameserver.model;

/**
 * 描述 ID 模型。
 * Description Id model.
 *
 * @author MrPoke
 */
public final class DescriptionId {

	private int value;

	public DescriptionId(int value) {
		this.value = value;
	}

	/** 返回值 / Returns the value*/
	public int getValue() {
		return value;
	}

	/** 设置值 / Sets the value*/
	public void setValue(int val) {
		this.value = val;
	}
}
