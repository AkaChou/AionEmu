package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.autogroup.AutoGroup;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 自动组队数据容器，分别按实例 ID 与 NPC ID 索引自动组队模板。
 * Auto-group data holder, dual-indexed by instance id and NPC id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "autoGroup" })
@XmlRootElement(name = "auto_groups")
public class AutoGroupData {

	@XmlElement(name = "auto_group")
	protected List<AutoGroup> autoGroup;
	@XmlTransient
	private IntObjectHashMap<AutoGroup> autoGroupByInstanceId = new IntObjectHashMap<AutoGroup>();
	@XmlTransient
	private IntObjectHashMap<AutoGroup> autoGroupByNpcId = new IntObjectHashMap<AutoGroup>();

	/**
	 * JAXB 反序列化完成后，按实例 ID 与 NPC ID 建立双索引并释放列表。
	 * After JAXB unmarshalling, dual-indexes by instance id and NPC id, then clears the list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (AutoGroup ag : autoGroup) {
			autoGroupByInstanceId.put(ag.getId(), ag);
			if (!ag.getNpcIds().isEmpty()) {
				for (int npcId : ag.getNpcIds()) {
					autoGroupByNpcId.put(npcId, ag);
				}
			}
		}
		autoGroup.clear();
		autoGroup = null;
	}

	/**
	 * 按实例 mask ID 获取自动组队模板。
	 * Returns the auto-group template for the given instance mask id.
	 *
	 * instance mask id
	 *
	 * @param maskId
	 * @return 模板，不存在则为 null / template or null
	 */
	public AutoGroup getTemplateByInstaceMaskId(int maskId) {
		return autoGroupByInstanceId.get(maskId);
	}

	/**
	 * 返回已加载的自动组队模板数量。
	 * Returns the number of loaded auto-group templates.
	 *
	 * template count
	 */
	public int size() {
		return autoGroupByInstanceId.size();
	}
}
