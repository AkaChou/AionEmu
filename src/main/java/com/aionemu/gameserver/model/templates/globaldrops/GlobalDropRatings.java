package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Ratings 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropRatings")
public class GlobalDropRatings {
	@XmlElement(name = "gd_rating")
	protected List<GlobalDropRating> gdRatings;

	/** 返回 global drop ratings / Returns the global drop ratings */
	public List<GlobalDropRating> getGlobalDropRatings() {
		if (gdRatings == null) {
			gdRatings = new ArrayList<GlobalDropRating>();
		}
		return this.gdRatings;
	}
}
