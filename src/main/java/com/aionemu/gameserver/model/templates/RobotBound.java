package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 机甲边界模板（静态数据/XML）。
 * Robot bound template (static data / XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RobotBound")
public class RobotBound extends BoundRadius {
	public RobotBound() {
	}

	public RobotBound(float front, float side, float upper) {
		super(front, side, upper);
	}
}
