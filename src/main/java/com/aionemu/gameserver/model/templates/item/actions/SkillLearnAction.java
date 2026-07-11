package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 技能 Learn 动作模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillLearnAction")
public class SkillLearnAction extends AbstractItemAction {

	@XmlAttribute
	protected int skillid;
	@XmlAttribute
	protected int level;
	@XmlAttribute(name = "class")
	protected PlayerClass playerClass;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		// 1. 检查玩家等级 / 1. check player level
		if (player.getCommonData().getLevel() < level) {
			return false;
		}

		PlayerClass pc = player.getCommonData().getPlayerClass();
		if (!validateClass(pc)) {
			return false;
		}

		// 4. 检查玩家种族与 Race.PC_ALL / 4. check player race and Race.PC_ALL
		Race race = parentItem.getItemTemplate().getRace();
		if (player.getRace() != race && race != Race.PC_ALL) {
			return false;
		}
		// 5. 检查该技能是否已学习 / 5. check whether this skill is already learned
		if (player.getSkillList().isSkillPresent(skillid)) {
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		// 物品动画与消息 / item animation and message
		ItemTemplate itemTemplate = parentItem.getItemTemplate();
		// PacketSendUtility.sendPacket(player,
		// SM_SYSTEM_MESSAGE.USE_ITEM(itemTemplate.getDescription()));
		player.getController().cancelUseItem();
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				parentItem.getObjectId(), itemTemplate.getTemplateId()), true);

		// 添加技能 / add skill
		SkillLearnService.learnSkillBook(player, skillid);

		// 从背包移除书本（假定不可堆叠） / remove book from inventory (assuming its not stackable)
		Item item = player.getInventory().getItemByObjId(parentItem.getObjectId());
		player.getInventory().delete(item);
	}

	private boolean validateClass(PlayerClass pc) {
		boolean result = false;
		// 2. 检查当前职业是否为二转，且书本是否针对起始职业。 / 2. check if current class is second class and book is for starting class
		if (!pc.isStartingClass() && PlayerClass.getStartingClassFor(pc).ordinal() == playerClass.ordinal()) {
			result = true;
		}
		// 3. 检查玩家职业与 SkillClass.ALL / 3. check player class and SkillClass.ALL
		if (pc == playerClass || playerClass == PlayerClass.ALL) {
			result = true;
		}
		return result;
	}
}
