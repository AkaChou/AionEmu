package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * Absolute 属性 Owner 游戏对象。
 * Absolute Stat Owner game object.
 *
 * @author Rolandas
 */
public class AbsoluteStatOwner implements StatOwner {

	Player target;
	ModifiersTemplate template;
	boolean isActive = false;

	public AbsoluteStatOwner(Player player, int templateId) {
		this.target = player;
		setTemplate(templateId);
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置模板。 / Sets the template. */
	public void setTemplate(int templateId) {
		if (isActive) {
			cancel();
		}
		this.template = DataManager.ABSOLUTE_STATS_DATA.getTemplate(templateId);
	}

	/** 应用。 / Apply. */
	public void apply() {
		if (template == null) {
			return;
		}
		target.getGameStats().addEffect(this, template.getModifiers());
		isActive = true;
	}

	/** 取消。 / Whether cel. */
	public void cancel() {
		if (template == null) {
			return;
		}
		target.getGameStats().endEffect(this);
		isActive = false;
	}
}
