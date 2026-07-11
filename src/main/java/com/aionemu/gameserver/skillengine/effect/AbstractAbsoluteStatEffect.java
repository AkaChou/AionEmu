package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 绝对属性效果抽象基类：通过 statsetid 从数据表加载修饰器集合。
 * Abstract absolute-stat effect base: loads a modifiers set by statsetid from data.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractAbsoluteStatEffect")
public abstract class AbstractAbsoluteStatEffect extends BuffEffect {

	@XmlAttribute(name = "statsetid")
	private int statSetId;

	/**
	 * 按 statSetId 取得绝对属性修饰器模板。
	 * Returns the absolute-stat modifiers template for the configured statSetId.
	 *
	 * @return 修饰器模板 / modifiers template
	 */
	public ModifiersTemplate getModifiersSet() {
		return DataManager.ABSOLUTE_STATS_DATA.getTemplate(statSetId);
	}

	@Override
	public void startEffect(Effect effect) {
		ModifiersTemplate modifiers = getModifiersSet();
		if (modifiers != null && modifiers.getModifiers() != null) {
			effect.getEffected().getGameStats().addEffect(effect, modifiers.getModifiers());
		}
	}
}
