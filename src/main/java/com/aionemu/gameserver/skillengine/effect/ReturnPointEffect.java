package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 回城卷轴效果：按物品模板中的地图与别名传送施法者。
 * Return-scroll effect: teleports the effector using the item template world id and alias.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReturnPointEffect")
public class ReturnPointEffect extends EffectTemplate {
	/**
	 * 副本中先离本，再按物品回城别名与世界 ID 传送。
	 * Leaves instance if needed, then teleports via the item return alias and world id.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected().isInInstance()) {
			InstanceService.onLeaveInstance((Player) effect.getEffector());
		}
		ItemTemplate itemTemplate = effect.getItemTemplate();
		int worldId = itemTemplate.getReturnWorldId();
		String pointAlias = itemTemplate.getReturnAlias();
		TeleportService2.useTeleportScroll((Player) effect.getEffector(), pointAlias, worldId);
	}

	/**
	 * 存在物品模板时标记本效果成功。
	 * Marks this effect successful when an item template is present.
	 */
	@Override
	public void calculate(Effect effect) {
		ItemTemplate itemTemplate = effect.getItemTemplate();
		if (itemTemplate != null) {
			effect.addSucessEffect(this);
		}
	}
}
