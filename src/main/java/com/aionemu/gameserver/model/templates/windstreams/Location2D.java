package com.aionemu.gameserver.model.templates.windstreams;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.flypath.FlyPathType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 二维地点模板（静态数据/XML）。
 * 2D location template (static data/XML).
 *
 * @author LokiReborn
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Location2D")
@NoArgsConstructor
@AllArgsConstructor
public class Location2D {
	/**
	 * 返回地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	@XmlAttribute(name = "id")
	protected int id;

	/**
	 * 返回地点状态。
	 * Returns the location state.
	 *
	 * @return 状态 / state
	 */
	@XmlAttribute(name = "state")
	protected int state;

	@XmlAttribute(name = "fly_path")
	protected FlyPathType flyPath;

	/** 获取飞行路径类型。 / Returns the fly path type. */
	public FlyPathType getFlyPathType() {
		return flyPath;
	}
}
