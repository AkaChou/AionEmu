package com.aionemu.gameserver.model.templates.staticdoor;

import java.util.EnumSet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.geoEngine.bounding.BoundingBox;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;

/**
 * 静态门模板（静态数据/XML）。
 * XML template.
 *
 * @author Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StaticDoor")
public class StaticDoorTemplate extends VisibleObjectTemplate {

	@XmlAttribute
	protected DoorType type = DoorType.DOOR;
	@XmlAttribute
	protected Float x;
	@XmlAttribute
	protected Float y;
	@XmlAttribute
	protected Float z;
	@XmlAttribute(name = "doorid")
	protected int doorId;
	@XmlAttribute(name = "keyid")
	protected int keyId;
	@XmlAttribute(name = "state")
	protected String statesHex;
	@XmlAttribute(name = "mesh")
	private String meshFile;
	@XmlElement(name = "box")
	private StaticDoorBounds box;

	@XmlTransient
	EnumSet<StaticDoorState> states = EnumSet.noneOf(StaticDoorState.class);

	/** 返回 X 坐标 / Returns the x */
	public Float getX() {
		return x;
	}

	/** 返回 Y 坐标 / Returns the y */
	public Float getY() {
		return y;
	}

	/** 返回 Z 坐标 / Returns the z */
	public Float getZ() {
		return z;
	}

	/**
	 * 返回门 ID。
	 * Returns the door id.
	 *
	 * @return 门 ID / the door id
	 */
	public int getDoorId() {
		return doorId;
	}

	/**
	 * 返回钥匙物品 ID。
	 * Returns the key item id.
	 *
	 * @return 钥匙物品 ID / the key item id
	 */
	public int getKeyId() {
		return keyId;
	}

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return 300001;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return "door";
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return 0;
	}

	/** 返回初始状态 / Returns the initial states */
	public EnumSet<StaticDoorState> getInitialStates() {
		if (statesHex != null) {
			int radix = 16;
			if (statesHex.startsWith("0x")) {
				statesHex = statesHex.replace("0x", "");
			} else
				radix = 10;
			try {
				StaticDoorState.setStates(Integer.parseInt(statesHex, radix), states);
			} catch (NumberFormatException ex) {
			} finally {
				statesHex = null;
			}
		}
		return states;
	}

	/** 返回网格文件 / Returns the mesh file */
	public String getMeshFile() {
		return meshFile;
	}

	/** 返回包围盒 / Returns the bounding box */
	public BoundingBox getBoundingBox() {
		if (box == null) {
			return null;
		}
		return box.getBoundingBox();
	}

	/** 返回门类型 / Returns the door type*/
	public DoorType getDoorType() {
		return type;
	}
}
