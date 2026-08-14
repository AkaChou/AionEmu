package com.aionemu.gameserver.model.templates.walker;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * RouteStep 模板（静态数据/XML）。
 * XML template.
 *
 * @author KKnD, Rolandas
 */
@XmlRootElement(name = "routestep")
@XmlAccessorType(XmlAccessType.FIELD)
public class RouteStep {

	/** 休息时间（毫秒）/ Rest time in milliseconds */
	@XmlAttribute(name = "rest_time", required = true)
	private Integer time = 0;

	/** Z 坐标 / Z coordinate */
	@XmlAttribute(name = "z", required = true)
	private float locZ;

	/** Y 坐标 / Y coordinate */
	@XmlAttribute(name = "y", required = true)
	private float locY;

	/** X 坐标 / X coordinate */
	@XmlAttribute(name = "x", required = true)
	private float locX;

	/** 路线步序号 / Route step index */
	@XmlAttribute(name = "step", required = true)
	private int routeStep;

	/** 下一步 / Next step */
	@XmlTransient
	private RouteStep nextStep;

	void beforeMarshal(Marshaller marshaller) {
		if (time == 0) {
			time = null;
		}
	}

	void afterMarshal(Marshaller marshaller) {
		if (time == null) {
			time = 0;
		}
	}

	public RouteStep() {
	}

	public RouteStep(float x, float y, float z, int restTime) {
		locX = x;
		locY = y;
		locZ = z;
		time = restTime;
	}

	/** 返回 X 坐标 / Returns the x */
	public float getX() {
		return locX;
	}

	/** 返回 Y 坐标 / Returns the y */
	public float getY() {
		return locY;
	}

	/** 返回 Z 坐标 / Returns the z */
	public float getZ() {
		return locZ;
	}

	/** 设置 Z 坐标 / Sets the z */
	public void setZ(float z) {
		locZ = z;
	}

	/** 返回休息时间 / Returns the rest time */
	public int getRestTime() {
		return time;
	}

	/** 返回下一步 / Returns the next step */
	public RouteStep getNextStep() {
		return nextStep;
	}

	/** 设置下一步 / Sets the next step */
	public void setNextStep(RouteStep nextStep) {
		this.nextStep = nextStep;
	}

	/** 返回路线步序号 / Returns the route step */
	public int getRouteStep() {
		return routeStep;
	}

	/** 设置路线步序号 / Sets the route step */
	public void setRouteStep(int routeStep) {
		this.routeStep = routeStep;
	}
}
