package com.aionemu.gameserver.model.templates.windstreams;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.flypath.FlyPathType;

/**
 * 二维地点模板（静态数据/XML）。
 * 2D location template (static data/XML).
 *
 * @author LokiReborn
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Location2D")
public class Location2D {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "state")
	protected int state;

	@XmlAttribute(name = "fly_path")
	protected FlyPathType flyPath;

	public Location2D() {
	}

	public Location2D(int id, int state, FlyPathType flyPath) {
		this.id = id;
		this.state = state;
		this.flyPath = flyPath;
	}

	/**
	 * 返回地点 ID。
	 * Returns the location id.
	 *
	 * @return 地点 ID / location id
	 */
	public int getId() {
		return id;
	}

	/**
	 * 返回地点状态。
	 * Returns the location state.
	 *
	 * @return 状态 / state
	 */
	public int getState() {
		return state;
	}

	/** 设置状态。 / Sets the state. */
	public void setState(int state) {
		this.state = state;
	}

	/** 获取飞行路径类型。 / Returns the fly path type. */
	public FlyPathType getFlyPathType() {
		return flyPath;
	}
}
