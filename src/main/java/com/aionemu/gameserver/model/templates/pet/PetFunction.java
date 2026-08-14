package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 宠物函数模板（静态数据/XML）。
 * Pet function template (static data / XML).
 *
 * @author IlBuono
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "petfunction")
public class PetFunction {

	@XmlAttribute(name = "type")
	private PetFunctionType type;
	@XmlAttribute(name = "id")
	private int id;
	@XmlAttribute(name = "slots")
	private int slots;

	/** 获取宠物函数类型。 / Returns the pet function type. */
	public PetFunctionType getPetFunctionType() {
		return type;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回槽位 / Returns the slots*/
	public int getSlots() {
		return slots;
	}

	/** 创建空对象。 / Creates an empty object. */
	public static PetFunction CreateEmpty() {
		PetFunction result = new PetFunction();
		result.type = PetFunctionType.NONE;
		return result;
	}
}
