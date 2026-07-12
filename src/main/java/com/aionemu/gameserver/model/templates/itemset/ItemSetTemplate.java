package com.aionemu.gameserver.model.templates.itemset;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.StatOwner;

/**
 * 物品 Set 模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer, modified by Antivirus
 */
@XmlRootElement(name = "itemset")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemSetTemplate implements StatOwner {

	@XmlElement(required = true)
	protected List<ItemPart> itempart;
	@XmlElement(required = true)
	protected List<PartBonus> partbonus;
	protected FullBonus fullbonus;
	@XmlAttribute
	protected String name;
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

	/**
	 * @return the itempart
	 */
	public List<ItemPart> getItempart() {
		return itempart;
	}

	/**
	 * @return the partbonus
	 */
	public List<PartBonus> getPartbonus() {
		return partbonus;
	}

	/**
	 * @return the fullbonus
	 */
	public FullBonus getFullbonus() {
		return fullbonus;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
