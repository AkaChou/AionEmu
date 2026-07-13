package com.aionemu.gameserver.model.items;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.GodstoneInfo;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GodStone extends ItemStone {

	private static final int PROBABILITY_SCALE = 1000;
	private static final int BREAK_DELAY = 600_000;

	private final ItemTemplate godstoneItem;
	private final GodstoneInfo godstoneInfo;
	private int activatedCount;
	private long cooldownExpireTime;
	private ActionObserver attackObserver;
	private boolean processingAttack;
	private boolean breaking;

	public GodStone(int itemObjId, int itemId, PersistentState persistentState) {
		this(itemObjId, itemId, 0, persistentState);
	}

	public GodStone(int itemObjId, int itemId, int activatedCount, PersistentState persistentState) {
		super(itemObjId, itemId, 0, persistentState);
		this.activatedCount = activatedCount;
		godstoneItem = DataManager.ITEM_DATA.getItemTemplate(itemId);
		godstoneInfo = godstoneItem == null ? null : godstoneItem.getGodstoneInfo();
		if (godstoneInfo == null) {
			log.warn(I18n.get("log.a69bb45a0c71", itemId));
			return;
		}
		validateProbability(godstoneInfo.getProbability());
		validateProbability(godstoneInfo.getProbabilityleft());
	}

	public void onEquip(Player player) {
		onUnEquip(player);
		Item weapon = player.getEquipment().getEquippedItemByObjId(getItemObjId());
		if (godstoneInfo == null || weapon == null) {
			return;
		}

		int probability = weapon.getItemTemplate().isTwoHandWeapon()
			? godstoneInfo.getProbability()
			: weapon.getEquipmentSlot() == ItemSlot.MAIN_HAND.getSlotIdMask()
				? godstoneInfo.getProbability()
				: godstoneInfo.getProbabilityleft();
		attackObserver = new ActionObserver(ObserverType.ATTACK) {
			@Override
			public void attack(Creature target) {
				handleAttack(player, target, weapon, probability);
			}
		};
		player.getObserveController().addObserver(attackObserver);
	}

	private synchronized void handleAttack(Player player, Creature target, Item weapon, int probability) {
		if (processingAttack) {
			return;
		}
		processingAttack = true;
		try {
			if (!tryActivate(target, probability)) {
				return;
			}
			Skill skill = GameEngineServices.skillEngine().getSkill(player, godstoneInfo.getSkillid(), godstoneInfo.getSkilllvl(), target,
				godstoneItem);
			if (skill == null) {
				return;
			}
			skill.setFirstTargetRangeCheck(false);
			if (skill.canUseSkill()) {
				Effect effect = new Effect(player, target, skill.getSkillTemplate(), 1, 0, godstoneItem);
				effect.initialize();
				effect.applyEffect();
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_PROC_EFFECT_OCCURRED(skill.getSkillTemplate().getNameId()));
				onActivated(player, weapon);
			}
		} finally {
			processingAttack = false;
		}
	}

	private boolean tryActivate(Creature target, int probability) {
		long now = System.currentTimeMillis();
		if (CustomConfig.GODSTONE_ACTIVATION_RATE <= 0 || now < cooldownExpireTime) {
			return false;
		}
		cooldownExpireTime = now + CustomConfig.GODSTONE_EVALUATION_COOLDOWN_MILLIS;
		int reduction = target.getGameStats().getStat(StatEnum.PROC_REDUCE_RATE, 0).getCurrent();
		return roll(adjustProbability(probability, reduction, CustomConfig.GODSTONE_ACTIVATION_RATE));
	}

	static int adjustProbability(int probability, int reduction, float rate) {
		int adjusted = probability - reduction;
		return adjusted > 0 ? Math.max(1, Math.round(adjusted * rate)) : adjusted;
	}

	static boolean roll(int probability) {
		return Rnd.get(PROBABILITY_SCALE) < probability;
	}

	private static void validateProbability(int probability) {
		if (probability < 0 || probability > PROBABILITY_SCALE) {
			throw new IllegalArgumentException("神石发动概率必须在 0-1000 之间: " + probability);
		}
	}

	private void onActivated(Player player, Item weapon) {
		if (breaking || godstoneInfo.getBreakprob() <= 0) {
			return;
		}
		activatedCount++;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
		if (shouldBreak(activatedCount, godstoneInfo.getBreakcount(), godstoneInfo.getBreakprob())) {
			breakGodstone(player, weapon);
		}
	}

	static boolean shouldBreak(int activatedCount, int breakCount, int breakProbability) {
		return activatedCount > breakCount && roll(breakProbability);
	}

	public int getActivatedCount() {
		return activatedCount;
	}

	private void breakGodstone(Player player, Item weapon) {
		breaking = true;
		DescriptionId weaponName = new DescriptionId(weapon.getNameId());
		DescriptionId godstoneName = new DescriptionId(godstoneItem.getNameId());
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402536, weaponName, godstoneName));
		PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402537, weaponName, godstoneName, 5), 300_000);
		PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402538, weaponName, godstoneName, 60), 540_000);
		PacketSendUtility.playerSendPacketTime(player, new SM_SYSTEM_MESSAGE(1402237, weaponName, godstoneName), BREAK_DELAY);
		GameThreadPoolServices.threadPoolManager().schedule(() -> removeGodstone(player, weapon), BREAK_DELAY);
	}

	private void removeGodstone(Player player, Item weapon) {
		onUnEquip(player);
		weapon.setGodStone(null);
		setPersistentState(PersistentState.DELETED);
		ItemPacketService.updateItemAfterInfoChange(player, weapon);
		DAOManager.getDAO(InventoryDAO.class).store(weapon, player);
		PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, weapon));
	}

	public void onUnEquip(Player player) {
		if (attackObserver != null) {
			player.getObserveController().removeObserver(attackObserver);
			attackObserver = null;
		}
	}
}
