package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Robot 边界模板（静态数据/XML）。
 * XML template. / XML template.
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
