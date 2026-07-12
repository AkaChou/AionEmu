package com.aionemu.gameserver.model.templates.housing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.item.ItemQuality;

/**
 * 房屋 Part 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "house_part")
public class HousePart {

	@XmlAttribute(name = "building_tags", required = true)
	private List<String> buildingTags;

	@XmlAttribute(required = true)
	protected PartType type;

	@XmlAttribute(required = true)
	protected ItemQuality quality;

	@XmlAttribute
	protected String name;

	@XmlAttribute(required = true)
	protected int id;

	@XmlTransient
	protected Set<String> tagsSet = new HashSet<String>(1);

	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (buildingTags == null) {
			return;
		}
		for (String tag : buildingTags) {
			tagsSet.add(tag);
		}
		buildingTags.clear();
		buildingTags = null;
	}

	/** 获取类型。 / Returns the type. */
	public PartType getType() {
		return type;
	}

	/** 返回 quality / Returns the quality */
	public ItemQuality getQuality() {
		return quality;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 tags / Returns the tags */
	public Set<String> getTags() {
		return tagsSet;
	}

	/** 是否建筑 / Whether for building */
	public boolean isForBuilding(Building building) {
		return tagsSet.contains(building.getPartsMatchTag());
	}
}
