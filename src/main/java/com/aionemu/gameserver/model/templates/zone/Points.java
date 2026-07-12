package com.aionemu.gameserver.model.templates.zone;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 点模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Points")
public class Points {

	@XmlElement(required = true)
	protected List<Point2D> point;
	@XmlAttribute(name = "top")
	protected float top;
	@XmlAttribute(name = "bottom")
	protected float bottom;

	public Points() {
	}

	public Points(float bottom, float top) {
		this.bottom = bottom;
		this.top = top;
	}

	/** 获取点。 / Returns the point. */
	public List<Point2D> getPoint() {
		if (point == null) {
			point = new ArrayList<Point2D>();
		}
		return this.point;
	}

	/**
	 * @return the top
	 */
	public float getTop() {
		return top;
	}

	/**
	 * @return the bottom
	 */
	public float getBottom() {
		return bottom;
	}
}
