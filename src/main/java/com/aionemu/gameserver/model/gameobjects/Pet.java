package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.controllers.PetController;
import com.aionemu.gameserver.controllers.movement.MoveController;
import com.aionemu.gameserver.controllers.movement.PetMoveController;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 宠物游戏对象。
 * Pet game object.
 *
 * @author ATracer
 */
public class Pet extends VisibleObject {

	private final Player master;
	private MoveController moveController;
	private final PetTemplate petTemplate;

	/**
	 * 构造宠物。
	 * Constructs a pet.
	 *
	 * @param petTemplate 宠物模板 / pet template
	 * @param controller 宠物控制器 / pet controller
	 * @param commonData 宠物公共数据 / pet common data
	 * @param master 主人玩家 / master player
	 */
	public Pet(PetTemplate petTemplate, PetController controller, PetCommonData commonData, Player master) {
		super(commonData.getObjectId(), controller, null, commonData, new WorldPosition(master.getWorldId()));
		controller.setOwner(this);
		this.master = master;
		this.petTemplate = petTemplate;
		this.moveController = new PetMoveController();
	}

	/** 返回主人 / Returns the master. */
	public Player getMaster() {
		return master;
	}

	/** 返回宠物 ID / Returns the pet id */
	public int getPetId() {
		return objectTemplate.getTemplateId();
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return objectTemplate.getName();
	}

	/** 获取公共数据。 / Returns the common data. */
	public final PetCommonData getCommonData() {
		return (PetCommonData) objectTemplate;
	}

	/** 返回移动控制器 / Returns the move controller */
	public final MoveController getMoveController() {
		return moveController;
	}

	/** 获取宠物模板。 / Returns the pet template. */
	public final PetTemplate getPetTemplate() {
		return petTemplate;
	}
}
