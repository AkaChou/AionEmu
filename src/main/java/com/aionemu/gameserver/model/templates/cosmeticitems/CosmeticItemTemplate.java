package com.aionemu.gameserver.model.templates.cosmeticitems;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 外观物品模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CosmeticItemTemplate")
public class CosmeticItemTemplate {

	@XmlAttribute(name = "type")
	private String type;
	@XmlAttribute(name = "cosmetic_name")
	private String cosmeticName;
	@XmlAttribute(name = "id")
	private int id;
	@XmlAttribute(name = "race")
	private Race race;
	@XmlAttribute(name = "gender_permitted")
	private String genderPermitted;
	@XmlElement(name = "preset")
	private Preset preset;

	/** 获取类型。 / Returns the type. */
	public String getType() {
		return type;
	}

	/** 获取外观名称。 / Returns the cosmetic name. */
	public String getCosmeticName() {
		return cosmeticName;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 返回允许的性别 / Returns the gender permitted */
	public String getGenderPermitted() {
		return genderPermitted;
	}

	/** 返回预设 / Returns the preset */
	public Preset getPreset() {
		return preset;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "Preset")
	public static class Preset {
		@XmlElement(name = "scale")
		private float scale;
		@XmlElement(name = "hair_type")
		private int hairType;
		@XmlElement(name = "face_type")
		private int faceType;
		@XmlElement(name = "hair_color")
		private int hairColor;
		@XmlElement(name = "lip_color")
		private int lipColor;
		@XmlElement(name = "eye_color")
		private int eyeColor;
		@XmlElement(name = "skin_color")
		private int skinColor;

		/** 返回缩放 / Returns the scale */
		public float getScale() {
			return scale;
		}

		/** 返回发型类型 / Returns the hair type */
		public int getHairType() {
			return hairType;
		}

		/** 返回脸型类型 / Returns the face type */
		public int getFaceType() {
			return faceType;
		}

		/** 返回发色 / Returns the hair color */
		public int getHairColor() {
			return hairColor;
		}

		/** 返回唇色 / Returns the lip color */
		public int getLipColor() {
			return lipColor;
		}

		/** 返回瞳色 / Returns the eye color */
		public int getEyeColor() {
			return eyeColor;
		}

		/** 返回肤色 / Returns the skin color */
		public int getSkinColor() {
			return skinColor;
		}
	}
}
