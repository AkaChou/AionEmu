package com.aionemu.gameserver.model.templates.housing;

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import org.apache.commons.lang3.StringUtils;

/**
 * Building 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "parts" })
@XmlRootElement(name = "building")
public class Building {
	private Parts parts;

	@XmlAttribute(name = "default")
	protected boolean isDefault;

	@XmlAttribute(name = "parts_match")
	protected String partsMatch;

	@XmlAttribute
	protected String size;

	@XmlAttribute
	protected BuildingType type;

	@XmlAttribute(required = true)
	protected int id;

	/** 是否默认 / Whether default*/
	public boolean isDefault() {
		return isDefault;
	}

	@XmlTransient
	Map<PartType, Integer> partsByType = new HashMap<PartType, Integer>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (parts == null) {
			return;
		}
		if (parts.getDoor() != 0) {
			partsByType.put(PartType.DOOR, parts.getDoor());
		}
		if (parts.getFence() != null) {
			partsByType.put(PartType.FENCE, parts.getFence());
		}
		if (parts.getFrame() != null) {
			partsByType.put(PartType.FRAME, parts.getFrame());
		}
		if (parts.getGarden() != null) {
			partsByType.put(PartType.GARDEN, parts.getGarden());
		}
		if (parts.getInfloor() != 0) {
			partsByType.put(PartType.INFLOOR_ANY, parts.getInfloor());
		}
		if (parts.getInwall() != 0) {
			partsByType.put(PartType.INWALL_ANY, parts.getInwall());
		}
		if (parts.getOutwall() != null) {
			partsByType.put(PartType.OUTWALL, parts.getOutwall());
		}
		if (parts.getRoof() != null) {
			partsByType.put(PartType.ROOF, parts.getRoof());
		}
	}

	/** 返回 parts match tag / Returns the parts match tag */
	public String getPartsMatchTag() {
		if (StringUtils.isEmpty(partsMatch)) {
			return DataManager.HOUSE_BUILDING_DATA.getBuilding(id).getPartsMatchTag();
		}
		return partsMatch;
	}

	/** 返回大小 / Returns the size*/
	public String getSize() {
		if (StringUtils.isEmpty(size)) {
			return DataManager.HOUSE_BUILDING_DATA.getBuilding(id).getSize();
		}
		return size;
	}

	/** 获取类型。 / Returns the type. */
	public BuildingType getType() {
		if (type == null) {
			return DataManager.HOUSE_BUILDING_DATA.getBuilding(id).getType();
		}
		return type;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 default part id / Returns the default part id */
	public Integer getDefaultPartId(PartType partType) {
		return DataManager.HOUSE_BUILDING_DATA.getBuilding(id).partsByType.get(partType);
	}
}
