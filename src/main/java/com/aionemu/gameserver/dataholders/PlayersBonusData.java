package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.bonus_service.PlayersBonusServiceAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 玩家服务加成数据容器，按 buffId 索引 PlayersBonusServiceAttr。
 * Player service bonus data holder, indexing PlayersBonusServiceAttr by buff id.
 *
 * @author Ranastic (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "playersServiceBonusattr" })
@XmlRootElement(name = "players_service_bonusattrs")
public class PlayersBonusData {
	@XmlElement(name = "players_service_bonusattr")
	protected List<PlayersBonusServiceAttr> playersServiceBonusattr;

	@XmlTransient
	private IntObjectHashMap<PlayersBonusServiceAttr> templates = new IntObjectHashMap<PlayersBonusServiceAttr>();

	/**
	 * JAXB 反序列化完成后，将加成模板写入 buffId 索引并释放列表。
	 * After JAXB unmarshalling, indexes bonus templates by buff id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PlayersBonusServiceAttr template : playersServiceBonusattr) {
			templates.put(template.getBuffId(), template);
		}
		playersServiceBonusattr.clear();
		playersServiceBonusattr = null;
	}

	/**
	 * 返回已加载的加成模板数量。
	 * Returns the number of loaded bonus templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按增益 ID 获取玩家服务加成属性。
	 * Returns the player service bonus attribute for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId
	 * @return 加成属性，不存在则为 null / bonus attribute or null
	 */
	public PlayersBonusServiceAttr getInstanceBonusattr(int buffId) {
		return templates.get(buffId);
	}
}
