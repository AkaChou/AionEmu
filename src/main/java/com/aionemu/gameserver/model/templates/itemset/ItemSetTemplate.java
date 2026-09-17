package com.aionemu.gameserver.model.templates.itemset;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.StatOwner;
import lombok.Getter;

/**
 * 套装模板（静态数据/XML）。
 * Item set template (static data/XML).
 *
 * @author ATracer, modified by Antivirus
 */
@Getter
@XmlRootElement(name = "itemset")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemSetTemplate implements StatOwner {

	/**
	 * @return the itempart
	 */
	@XmlElement(required = true)
	protected List<ItemPart> itempart;
	/**
	 * @return the partbonus
	 */
	@XmlElement(required = true)
	protected List<PartBonus> partbonus;
	/**
	 * @return the fullbonus
	 */
	protected FullBonus fullbonus;
	/**
	 * @return the name
	 */
	@XmlAttribute
	protected String name;
	/**
	 * @return the id
	 */
	@XmlAttribute
	protected int id;

	/**
	 * @param u
	 * @param parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (fullbonus != null) {
			// 设置应用完整加成的物品数量 / Set number of items to apply the full bonus
			fullbonus.setNumberOfItems(itempart.size());
		}
	}
}
