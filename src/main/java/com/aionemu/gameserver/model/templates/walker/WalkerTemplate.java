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
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 巡逻模板（静态数据/XML）。
 * XML template.
 * @author KKnD
 */
@Getter
@Setter
@XmlRootElement(name = "walker_template")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
public class WalkerTemplate {

	/** 是否反转路线 / Whether the route is reversed */
	@XmlAttribute(name = "reversed")
	private Boolean isReversed = false;

	/** 巡逻池人数 / Walker pool size */
	@XmlAttribute(name = "pool", required = true)
	private int pool = 1;

	/** 路线 ID / Route id */
	@XmlAttribute(name = "route_id", required = true)
	private String routeId;

	/** 队形 / Formation
	 * -- SETTER --
	 * 设置队形类型 / Sets the formation type
	 */
	@XmlAttribute(name = "formation")
	private WalkerGroupType formation = WalkerGroupType.POINT;

	/** 行配置字符串（逗号分隔）/ Row values as comma-separated string */
	@XmlAttribute(name = "rows")
	private String rowValues;

	/** X 偏移量字符串（逗号分隔）/ X offsets as comma-separated string */
	@XmlAttribute(name = "offsetsx")
	private String offsetsxValues;

	/** Y 偏移量字符串（逗号分隔）/ Y offsets as comma-separated string */
	@XmlAttribute(name = "offsetsy")
	private String offsetsyValues;

	/** 路线步骤列表 / Route step list */
	@XmlElement(name = "routestep")
	private List<RouteStep> routeStepList;

	/** 解析后的行配置 / Parsed row values
     * -- SETTER --
     * 设置行列分布 / Sets row distribution
     */
	@XmlTransient
	private int[] rows;

	/** 解析后的 X 偏移量 / Parsed X offsets */
	@XmlTransient
	private int[] offsetsx;

	/** 解析后的 Y 偏移量 / Parsed Y offsets */
	@XmlTransient
	private int[] offsetsy;

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
	 * 反序列化后处理：反转路线、链接步骤、解析队形与偏移配置。
	 * Post-unmarshal handling: reverse the route, link steps, and parse formation and offset config.
	 * @param u JAXB 反序列化器 / JAXB unmarshaller
	 * @param parent 父对象 / Parent object
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

	/** 返回路线步骤列表 / Returns the route steps */
	public List<RouteStep> getRouteSteps() {
		return routeStepList;
	}

	/** 返回指定序号的路线步 / Returns the route step */
	public RouteStep getRouteStep(int value) {
		return routeStepList.get(value - 1);
	}

	/** 设置路线步骤列表 / Sets the route steps */
	public void setRouteSteps(ArrayList<RouteStep> newSteps) {
		routeStepList = newSteps;
	}

	/**
	 * 是否反转路线。
	 * Whether the route is reversed.
	 * @return 反转时为 {@code true} / {@code true} if reversed
	 */
	public boolean isReversed() {
		return isReversed;
	}

	/** 设置是否反转 / Sets whether reversed */
	public void setIsReversed(boolean value) {
		isReversed = value;
	}

	/** 获取队形类型 / Returns the formation type. */
	public WalkerGroupType getType() {
		return formation;
	}

	/**
	 * 获取队形定义的人数容量。
	 * Returns the member capacity defined by the formation.
	 * @return 队形人数，非方阵或偏移队形时返回 1 / formation member capacity, or 1 if not square/offset
	 */
	public int getFormationSize() {
		if (formation == WalkerGroupType.OFFSET && offsetsx != null && offsetsx.length > 0) {
			return offsetsx.length;
		}
		if (formation == WalkerGroupType.SQUARE && rows != null && rows.length > 0) {
			int sum = 0;
			for (int row : rows) {
				sum += row;
			}
			return sum;
		}
		return 1;
	}

	/** 获取 X 偏移量 / Gets the X offsets */
	public int[] getoffsetsX() {
		return offsetsx;
	}

    /** 获取 Y 偏移量 / Gets the Y offsets */
	public int[] getoffsetsY() {
		return offsetsy;
	}

	/** 设置 X 与 Y 偏移量 / Sets X and Y offsets */
	public void setOffsets(int[] offsetsX, int[] offsetsY) {
		this.offsetsx = offsetsX;
		this.offsetsy = offsetsY;
	}
}
