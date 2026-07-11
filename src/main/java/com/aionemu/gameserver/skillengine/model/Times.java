package com.aionemu.gameserver.skillengine.model;

import java.util.HashMap;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.item.WeaponType;

/**
 * 武器相关动作时间表：按武器类型解析逗号分隔的时间序列。
 * Weapon-related motion times: parses comma-separated times per weapon type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Times")
public class Times {

	@XmlAttribute(required = true)
	protected String times;

	@XmlTransient
	private HashMap<WeaponTypeWrapper, Integer> timeForWeaponType = new HashMap<WeaponTypeWrapper, Integer>();

	/**
	 * 获取原始时间字符串。
	 * Gets raw times string.
	 *
	 * @return 逗号分隔时间 / comma-separated times
	 */
	public String getTimes() {
		return times;
	}

	/**
	 * 设置原始时间字符串。
	 * Sets raw times string.
	 *
	 * @param times 逗号分隔时间 / comma-separated times
	 */
	public void setTimes(String times) {
		this.times = times;
	}

	/**
	 * 按武器包装获取动作时间。
	 * Gets motion time for a weapon wrapper.
	 *
	 * weapon wrapper
	 * time
	 */
	public int getTimeForWeapon(WeaponTypeWrapper weapon) {
		return timeForWeaponType.get(weapon);
	}

	/**
	 * 反序列化后解析各武器类型时间。
	 * Parses per-weapon times after unmarshalling.
	 *
	 * unmarshaller
	 * parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		String[] tokens = times.split(",");
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.BOOK_2H, null), Integer.parseInt(tokens[0]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.BOW, null), Integer.parseInt(tokens[1]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.DAGGER_1H, null), Integer.parseInt(tokens[2]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.MACE_1H, null), Integer.parseInt(tokens[3]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.ORB_2H, null), Integer.parseInt(tokens[4]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.POLEARM_2H, null), Integer.parseInt(tokens[5]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.STAFF_2H, null), Integer.parseInt(tokens[6]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.SWORD_1H, null), Integer.parseInt(tokens[7]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.SWORD_2H, null), Integer.parseInt(tokens[8]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.SWORD_1H, WeaponType.SWORD_1H),
				Integer.parseInt(tokens[9]));
		// 4.3
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.GUN_1H, null), Integer.parseInt(tokens[10]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.CANNON_2H, null), Integer.parseInt(tokens[11]));
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.HARP_2H, null), Integer.parseInt(tokens[12]));
		// 4.5
		timeForWeaponType.put(new WeaponTypeWrapper(WeaponType.KEYBLADE_2H, null), Integer.parseInt(tokens[13]));
	}
}
