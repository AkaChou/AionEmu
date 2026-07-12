package com.aionemu.gameserver.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 描述 ID 模型。
 * Description Id model.
 *
 * @author MrPoke
 */
@Getter
@Setter
public final class DescriptionId {

	private int value;

	public DescriptionId(int value) {
		this.value = value;
	}

}
