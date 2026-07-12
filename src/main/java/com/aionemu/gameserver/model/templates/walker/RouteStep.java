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

	@XmlAttribute(name = "rest_time", required = true)
	private Integer time = 0;

	@XmlAttribute(name = "z", required = true)
	private float locZ;

	@XmlAttribute(name = "y", required = true)
	private float locY;

	@XmlAttribute(name = "x", required = true)
	private float locX;

	@XmlAttribute(name = "step", required = true)
	private int routeStep;

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

	/** 返回 x / Returns the x */
	public float getX() {
		return locX;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return locY;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return locZ;
	}

	/** 设置 z / Sets the z */
	public void setZ(float z) {
		locZ = z;
	}

	/** 返回 rest time / Returns the rest time */
	public int getRestTime() {
		return time;
	}

	/** 返回 next step / Returns the next step */
	public RouteStep getNextStep() {
		return nextStep;
	}

	/** 设置 next step / Sets the next step */
	public void setNextStep(RouteStep nextStep) {
		this.nextStep = nextStep;
	}

	/** 返回 route step / Returns the route step */
	public int getRouteStep() {
		return routeStep;
	}

	/** 设置 route step / Sets the route step */
	public void setRouteStep(int routeStep) {
		this.routeStep = routeStep;
	}
}
