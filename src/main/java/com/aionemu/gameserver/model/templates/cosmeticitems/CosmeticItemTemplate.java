package com.aionemu.gameserver.model.templates.cosmeticitems;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 外观物品模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CosmeticItemTemplate")
public class CosmeticItemTemplate {

	/**
	 * -- GETTER --
	 * 获取类型。 / Returns the type.
	 */
	@XmlAttribute(name = "type")
	private String type;
	/**
	 * -- GETTER --
	 * 获取外观名称。 / Returns the cosmetic name.
	 */
	@XmlAttribute(name = "cosmetic_name")
	private String cosmeticName;
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@XmlAttribute(name = "id")
	private int id;
	/**
	 * -- GETTER --
	 * 获取种族。 / Returns the race.
	 */
	@XmlAttribute(name = "race")
	private Race race;
	/**
	 * -- GETTER --
	 * 返回允许的性别 / Returns the gender permitted
	 */
	@XmlAttribute(name = "gender_permitted")
	private String genderPermitted;
	/**
	 * -- GETTER --
	 * 返回预设 / Returns the preset
	 */
	@XmlElement(name = "preset")
	private Preset preset;

	@Getter
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "Preset")
	public static class Preset {
		/**
		 * -- GETTER --
		 * 返回缩放 / Returns the scale
		 */
		@XmlElement(name = "scale")
		private float scale;
		/**
		 * -- GETTER --
		 * 返回发型类型 / Returns the hair type
		 */
		@XmlElement(name = "hair_type")
		private int hairType;
		/**
		 * -- GETTER --
		 * 返回脸型类型 / Returns the face type
		 */
		@XmlElement(name = "face_type")
		private int faceType;
		/**
		 * -- GETTER --
		 * 返回发色 / Returns the hair color
		 */
		@XmlElement(name = "hair_color")
		private int hairColor;
		/**
		 * -- GETTER --
		 * 返回唇色 / Returns the lip color
		 */
		@XmlElement(name = "lip_color")
		private int lipColor;
		/**
		 * -- GETTER --
		 * 返回瞳色 / Returns the eye color
		 */
		@XmlElement(name = "eye_color")
		private int eyeColor;
		/**
		 * -- GETTER --
		 * 返回肤色 / Returns the skin color
		 */
		@XmlElement(name = "skin_color")
		private int skinColor;

	}
}
