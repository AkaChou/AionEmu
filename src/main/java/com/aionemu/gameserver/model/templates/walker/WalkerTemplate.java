package com.aionemu.gameserver.model.templates.walker;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.spawnengine.WalkerGroupType;

/**
 * 巡逻模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author KKnD
 */
@XmlRootElement(name = "walker_template")
@XmlAccessorType(XmlAccessType.FIELD)
public class WalkerTemplate {

	@XmlAttribute(name = "reversed")
	private Boolean isReversed = false;

	@XmlAttribute(name = "pool", required = true)
	private int pool = 1;

	@XmlAttribute(name = "route_id", required = true)
	private String routeId;

	@XmlAttribute(name = "formation")
	private WalkerGroupType formation = WalkerGroupType.POINT;

	@XmlAttribute(name = "rows")
	private String rowValues;

	@XmlAttribute(name = "offsetsx")
	private String offsetsxValues;
	
	@XmlAttribute(name = "offsetsy")
	private String offsetsyValues;

	@XmlElement(name = "routestep")
	private List<RouteStep> routeStepList;

	@XmlTransient
	private int[] rows;
	
	@XmlTransient
	private int[] offsetsx;
	
	@XmlTransient
	private int[] offsetsy;

	public WalkerTemplate() {
	}

	public WalkerTemplate(String routeId) {
		this.routeId = routeId;
	}

	void beforeMarshal(Marshaller marshaller) {
		if (isReversed == false) {
			isReversed = null;
		}
		if (formation == WalkerGroupType.POINT) {
			formation = null;
		}
	}

	void afterMarshal(Marshaller marshaller) {
		if (isReversed == null) {
			isReversed = false;
		}
		if (formation == null) {
			formation = WalkerGroupType.POINT;
		}
	}

	/**
	 * @param u
	 * @param parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (isReversed) {
			for (int i = routeStepList.size() - 2; i > 0; i--) {
				RouteStep step = routeStepList.get(i);
				routeStepList.add(new RouteStep(step.getX(), step.getY(), step.getZ(), step.getRestTime()));
			}
		}
		for (int i = 0; i < routeStepList.size() - 1; i++) {
			routeStepList.get(i).setNextStep(routeStepList.get(i + 1));
			routeStepList.get(i).setRouteStep(i + 1);
		}
		routeStepList.get(routeStepList.size() - 1).setRouteStep(routeStepList.size());
		routeStepList.get(routeStepList.size() - 1).setNextStep(routeStepList.get(0));

		if ((pool == 2) && (formation != WalkerGroupType.OFFSET)) {
			formation = WalkerGroupType.SQUARE;
			rows = new int[1];
			rows[0] = 2;
		} else if (formation == WalkerGroupType.SQUARE) {
			if (rowValues != null) {
				String[] values = rowValues.split(",");
				rows = new int[values.length];
				for (int i = 0; i < values.length; i++) {
					rows[i] = Integer.parseInt(values[i]);
				}
			} else {
				formation = WalkerGroupType.POINT;
			}
		}
		if (offsetsxValues != null) {
			String[] valuesX = offsetsxValues.split(",");
			String[] valuesY = offsetsyValues.split(",");
			offsetsx = new int[valuesX.length];
			offsetsy = new int[valuesY.length];
			for (int i = 0; i < valuesX.length; i++) {
				offsetsx[i] = Integer.parseInt(valuesX[i]);
				offsetsy[i] = Integer.parseInt(valuesY[i]);
			}
		}
		rowValues = null;
	}

	/** 返回 route steps / Returns the route steps */
	public List<RouteStep> getRouteSteps() {
		return routeStepList;
	}

	/** 返回 route step / Returns the route step */
	public RouteStep getRouteStep(int value) {
		return routeStepList.get(value - 1);
	}

	/** 返回 route id / Returns the route id */
	public String getRouteId() {
		return routeId;
	}

	/** 返回 pool / Returns the pool */
	public int getPool() {
		return pool;
	}

	/** 设置 pool / Sets the pool */
	public void setPool(int pool) {
		this.pool = pool;
	}

	/** 设置 route steps / Sets the route steps */
	public void setRouteSteps(ArrayList<RouteStep> newSteps) {
		routeStepList = newSteps;
	}

	/**
	 * @return Whether reversed / Whether reversed
	 */
	public boolean isReversed() {
		return isReversed;
	}

	/** 设置 is reversed / Sets the is reversed */
	public void setIsReversed(boolean value) {
		isReversed = value;
	}

	/** 获取类型。 / Returns the type. */
	public WalkerGroupType getType() {
		return formation;
	}

	/**
	 * @return the rows
	 */
	public int[] getRows() {
		return rows;
	}
	
	/** getoffsets X / getoffsets X */
	public int[] getoffsetsX() {
		return offsetsx;
	}
	
	/** getoffsets Y / getoffsets Y */
	public int[] getoffsetsY() {
		return offsetsy;
	}	
}
