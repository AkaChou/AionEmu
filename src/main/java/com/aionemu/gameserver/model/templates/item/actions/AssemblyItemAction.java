package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.AssemblyItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Assembly 物品动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssemblyItemAction")
public class AssemblyItemAction extends AbstractItemAction {
	@XmlAttribute
	private int item;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		AssemblyItem assemblyItem = getAssemblyItem();
		if (assemblyItem == null) {
			return false;
		}
		for (Integer itemId : assemblyItem.getParts()) {
			if (getAssemblyCount(getAvailablePartsCount(player, parentItem, itemId), assemblyItem.getPartsNum()) < 1) {
				return false;
			}
		}
		return true;
	}

	/** 移除物品。 / Removes items. */
	public static void removeItems(Player player, int itemId, long itemCount) {
		if (!player.getInventory().decreaseByItemId(itemId, itemCount)) {
		}
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				parentItem.getObjectId(), parentItem.getItemId(), 3000, 0, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
						.STR_ITEM_CANCELED(new DescriptionId(parentItem.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				player.getController().cancelTask(TaskId.ITEM_USE);
				AssemblyItem assemblyItem = getAssemblyItem();
				long assemblyCount = getAvailableAssemblyCount(player, parentItem, assemblyItem);
				if (assemblyCount < 1) {
					return;
				}
				long requiredPartsCount = getRequiredPartsCount(assemblyItem.getPartsNum(), assemblyCount);
				for (Integer itemId : assemblyItem.getParts()) {
					if (!decreaseParts(player, parentItem, itemId, requiredPartsCount)) {
						return;
					}
				}
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, 1, 0), true);
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401122));

				long normalCount = assemblyCount;
				long procCount = 0;
				if (assemblyItem.getProcAssembly() != 0) {
					normalCount = 0;
					for (long i = 0; i < assemblyCount; i++) {
						if (Rnd.get(1, 100) < 15) {
							procCount++;
						} else {
							normalCount++;
						}
					}
				}
				if (normalCount > 0) {
					ItemService.addItem(player, assemblyItem.getId(), normalCount);
				}
				if (procCount > 0) {
					ItemService.addItem(player, assemblyItem.getProcAssembly(), procCount);
				}
			}
		}, 3000));
	}

	private long getAvailableAssemblyCount(Player player, Item parentItem, AssemblyItem assemblyItem) {
		long assemblyCount = Long.MAX_VALUE;
		for (Integer itemId : assemblyItem.getParts()) {
			long availableCount = getAvailablePartsCount(player, parentItem, itemId);
			assemblyCount = Math.min(assemblyCount, getAssemblyCount(availableCount, assemblyItem.getPartsNum()));
		}
		return assemblyCount == Long.MAX_VALUE ? 0 : assemblyCount;
	}

	private long getAvailablePartsCount(Player player, Item parentItem, int itemId) {
		return parentItem.getItemId() == itemId ? parentItem.getItemCount() : player.getInventory().getItemCountByItemId(itemId);
	}

	private boolean decreaseParts(Player player, Item parentItem, int itemId, long count) {
		if (parentItem.getItemId() == itemId) {
			return player.getInventory().decreaseByObjectId(parentItem.getObjectId(), count);
		}
		return player.getInventory().decreaseByItemId(itemId, count);
	}

	static long getAssemblyCount(long availableCount, int partsNum) {
		if (availableCount < 1) {
			return 0;
		}
		return availableCount / getEffectivePartsNum(partsNum);
	}

	static long getRequiredPartsCount(int partsNum, long assemblyCount) {
		if (assemblyCount < 1) {
			return 0;
		}
		return getEffectivePartsNum(partsNum) * assemblyCount;
	}

	private static int getEffectivePartsNum(int partsNum) {
		return partsNum > 0 ? partsNum : 1;
	}

	/** 返回 assembly item / Returns the assembly item */
	public AssemblyItem getAssemblyItem() {
		return DataManager.ASSEMBLY_ITEM_DATA.getAssemblyItem(item);
	}
}
