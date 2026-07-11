package com.aionemu.gameserver.model.templates.item.actions;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品 Actions 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemActions")
public class ItemActions {
	@XmlElements({ @XmlElement(name = "skilllearn", type = SkillLearnAction.class),
			@XmlElement(name = "extract", type = ExtractAction.class),
			@XmlElement(name = "extractabyss", type = ExtractAbyssAction.class),
			@XmlElement(name = "extractexp", type = ExtractExpAction.class),
			@XmlElement(name = "idian", type = IdianAction.class),
			@XmlElement(name = "bonusexp", type = BonusAddExpAction.class),
			@XmlElement(name = "houselimit", type = HouseLimitAction.class),
			@XmlElement(name = "skilluse", type = SkillUseAction.class),
			@XmlElement(name = "enchant", type = EnchantItemAction.class),
			@XmlElement(name = "queststart", type = QuestStartAction.class),
			@XmlElement(name = "dye", type = DyeAction.class),
			@XmlElement(name = "craftlearn", type = CraftLearnAction.class),
			@XmlElement(name = "toypetspawn", type = ToyPetSpawnAction.class),
			@XmlElement(name = "disassemble", type = DisassemblyAction.class),
			@XmlElement(name = "titleadd", type = TitleAddAction.class),
			@XmlElement(name = "learnemotion", type = EmotionLearnAction.class),
			@XmlElement(name = "read", type = ReadAction.class),
			@XmlElement(name = "fireworkact", type = FireworksUseAction.class),
			@XmlElement(name = "instancetimeclear", type = InstanceTimeClear.class),
			@XmlElement(name = "expandinventory", type = ExpandInventoryAction.class),
			@XmlElement(name = "animation", type = AnimationAddAction.class),
			@XmlElement(name = "cosmetic", type = CosmeticItemAction.class),
			@XmlElement(name = "charge", type = ChargeAction.class),
			@XmlElement(name = "ride", type = RideAction.class),
			@XmlElement(name = "houseobject", type = SummonHouseObjectAction.class),
			@XmlElement(name = "housedeco", type = DecorateAction.class),
			@XmlElement(name = "assemble", type = AssemblyItemAction.class),
			@XmlElement(name = "adoptpet", type = AdoptPetAction.class),
			@XmlElement(name = "composition", type = CompositionAction.class),
			@XmlElement(name = "retuning", type = RetuningAction.class),
			@XmlElement(name = "wrapping", type = WrappingAction.class),
			@XmlElement(name = "f2p", type = F2pAction.class),
			@XmlElement(name = "tempering", type = TemperingAction.class),
			@XmlElement(name = "multireturn", type = MultiReturnAction.class),
			@XmlElement(name = "purifierexp", type = PurifierExpAction.class),
			@XmlElement(name = "unbinding", type = UnbindingAction.class),
			@XmlElement(name = "reductlevel", type = EquipedLevelAdjAction.class),
			@XmlElement(name = "unseal", type = UnSealAction.class),
			@XmlElement(name = "luna", type = LunaChestAction.class),
			@XmlElement(name = "enhance", type = EnhanceAction.class),
			@XmlElement(name = "enchant_stigma", type = EnchantStigmaAction.class),
			@XmlElement(name = "sweep", type = ShugoSweepAction.class),
			@XmlElement(name = "skill_skin", type = SkillAnimationAction.class), })
	protected List<AbstractItemAction> itemActions;

	/** 返回 item actions / Returns the item actions */
	public List<AbstractItemAction> getItemActions() {
		if (itemActions == null) {
			itemActions = new ArrayList<AbstractItemAction>();
		}
		return this.itemActions;
	}

	/** 返回 toy pet spawn actions / Returns the toy pet spawn actions */
	public List<ToyPetSpawnAction> getToyPetSpawnActions() {
		List<ToyPetSpawnAction> result = new ArrayList<ToyPetSpawnAction>();
		if (itemActions == null) {
			return result;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof ToyPetSpawnAction) {
				result.add((ToyPetSpawnAction) action);
			}
		}
		return result;
	}

	/** 返回强化动作 / Returns the enchant action*/
	public EnchantItemAction getEnchantAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if ((action instanceof EnchantItemAction)) {
				return (EnchantItemAction) action;
			}
		}
		return null;
	}

	/** 返回强化烙印之石动作 / Returns the enchant stigma action*/
	public EnchantStigmaAction getEnchantStigmaAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if ((action instanceof EnchantStigmaAction)) {
				return (EnchantStigmaAction) action;
			}
		}
		return null;
	}

	/** 获取房屋对象动作。 / Returns the house object action. */
	public SummonHouseObjectAction getHouseObjectAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if ((action instanceof SummonHouseObjectAction)) {
				return (SummonHouseObjectAction) action;
			}
		}
		return null;
	}

	/** 返回 craft learn action / Returns the craft learn action */
	public CraftLearnAction getCraftLearnAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if ((action instanceof CraftLearnAction)) {
				return (CraftLearnAction) action;
			}
		}
		return null;
	}

	/** 返回 decorate action / Returns the decorate action */
	public DecorateAction getDecorateAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if ((action instanceof DecorateAction)) {
				return (DecorateAction) action;
			}
		}
		return null;
	}

	/** 返回 dye action / Returns the dye action */
	public DyeAction getDyeAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof DyeAction) {
				return (DyeAction) action;
			}
		}
		return null;
	}

	/** 返回收养宠物动作 / Returns the adopt pet action*/
	public AdoptPetAction getAdoptPetAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof AdoptPetAction) {
				return (AdoptPetAction) action;
			}
		}
		return null;
	}

	/** 返回 tuning action / Returns the tuning action */
	public RetuningAction getTuningAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof RetuningAction) {
				return (RetuningAction) action;
			}
		}
		return null;
	}

	/** 返回 polish action / Returns the polish action */
	public IdianAction getPolishAction() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof IdianAction) {
				return (IdianAction) action;
			}
		}
		return null;
	}

	/** 返回 tempering / Returns the tempering */
	public TemperingAction getTempering() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof TemperingAction) {
				return (TemperingAction) action;
			}
		}
		return null;
	}

	/** 返回 unbinding / Returns the unbinding */
	public UnbindingAction getUnbinding() {
		if (itemActions == null) {
			return null;
		}
		for (AbstractItemAction action : itemActions) {
			if (action instanceof UnbindingAction) {
				return (UnbindingAction) action;
			}
		}
		return null;
	}
}
