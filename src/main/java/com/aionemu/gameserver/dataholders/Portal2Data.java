package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalDialog;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalScroll;
import com.aionemu.gameserver.model.templates.portal.PortalUse;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 传送门（第二版）数据容器，索引使用、对话与卷轴三类传送配置。
 * Portal (v2) data holder indexing use, dialog and scroll portal configurations.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "portalUse", "portalDialog", "portalScroll" })
@XmlRootElement(name = "portal_templates2")
public class Portal2Data {

	@XmlElement(name = "portal_use")
	protected List<PortalUse> portalUse;

	@XmlElement(name = "portal_dialog")
	protected List<PortalDialog> portalDialog;

	@XmlElement(name = "portal_scroll")
	protected List<PortalScroll> portalScroll;

	@XmlTransient
	private IntObjectHashMap<PortalUse> portalUses = new IntObjectHashMap<PortalUse>();

	@XmlTransient
	private IntObjectHashMap<PortalDialog> portalDialogs = new IntObjectHashMap<PortalDialog>();

	@XmlTransient
	private Map<String, PortalScroll> portalScrolls = new HashMap<String, PortalScroll>();

	/**
	 * JAXB 反序列化完成后，将三类传送配置写入对应索引。
	 * After JAXB unmarshalling, indexes the three portal configuration types.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (portalUse != null) {
			for (PortalUse portal : portalUse) {
				portalUses.put(portal.getNpcId(), portal);
			}
		}
		if (portalDialog != null) {
			for (PortalDialog portal : portalDialog) {
				portalDialogs.put(portal.getNpcId(), portal);
			}
		}
		if (portalScroll != null) {
			for (PortalScroll portal : portalScroll) {
				portalScrolls.put(portal.getName(), portal);
			}
		}
	}

	/**
	 * 返回三类传送配置的合计数量。
	 * Returns the total count of all three portal configuration types.
	 *
	 * total configuration count
	 */
	public int size() {
		return portalScrolls.size() + portalDialogs.size() + portalUses.size();
	}

	/**
	 * 按 NPC、对话 ID 与阵营查找传送路径。
	 * Finds a portal path by NPC, dialog id and race.
	 *
	 * npc id
	 * dialog id
	 * 阵营 / race
	 * @return 传送路径，不匹配则为 null / portal path or null
	 */
	public PortalPath getPortalDialog(int npcId, int dialogId, Race race) {
		PortalDialog portal = portalDialogs.get(npcId);
		if (portal != null) {
			for (PortalPath path : portal.getPortalPath()) {
				if (path.getDialog() == dialogId
						&& (race.equals(path.getRace()) || path.getRace().equals(Race.PC_ALL))) {
					return path;
				}
			}
		}
		return null;
	}

	/**
	 * 判断指定 NPC 是否为传送门 NPC。
	 * Returns whether the given NPC is a portal NPC.
	 *
	 * npc id
	 *
	 * @param npcId @return 是否为传送门 / whether it is a portal NPC
	 */
	public boolean isPortalNpc(int npcId) {
		return portalUses.get(npcId) != null || portalDialogs.get(npcId) != null;
	}

	/**
	 * 按 NPC ID 获取使用型传送门配置。
	 * Returns the use-type portal configuration for the given NPC id.
	 *
	 * npc id
	 *
	 * @param npcId @return 使用型传送配置，不存在则为 null / use portal or null
	 */
	public PortalUse getPortalUse(int npcId) {
		return portalUses.get(npcId);
	}

	/**
	 * 按名称获取传送卷轴配置。
	 * Returns the portal scroll configuration for the given name.
	 *
	 * @param name 卷轴名称 / scroll name
	 * @return 传送卷轴，不存在则为 null / portal scroll or null
	 */
	public PortalScroll getPortalScroll(String name) {
		return portalScrolls.get(name);
	}

	/**
	 * 返回指定 NPC 的传送对话 ID；无配置时默认 1011。
	 * Returns the teleport dialog id for the NPC; defaults to 1011 when unset.
	 *
	 * npc id
	 * teleport dialog id
	 */
	public int getTeleportDialogId(int npcId) {
		PortalDialog portal = portalDialogs.get(npcId);
		return portal == null ? 1011 : portal.getTeleportDialogId();
	}
}
