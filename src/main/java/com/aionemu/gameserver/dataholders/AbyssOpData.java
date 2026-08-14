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

import com.aionemu.gameserver.model.templates.abyss_op.AbyssOp;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 欧比斯行动数据容器，按 ID 索引全部欧比斯行动模板。
 * Abyss operation data holder, indexing all abyss operation templates by id.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "abyss_ops")
@XmlAccessorType(XmlAccessType.FIELD)
public class AbyssOpData {
	@XmlElement(name = "abyss_op")
	private List<AbyssOp> aolist;

	@XmlTransient
	private IntObjectHashMap<AbyssOp> opData = new IntObjectHashMap<AbyssOp>();

	@XmlTransient
	private Map<Integer, AbyssOp> opDataMap = new HashMap<Integer, AbyssOp>(1);

	/**
	 * JAXB 反序列化完成后，将列表写入双索引映射。
	 * After JAXB unmarshalling, populates both index maps from the list.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (AbyssOp abyssOp : aolist) {
			opData.put(abyssOp.getId(), abyssOp);
			opDataMap.put(abyssOp.getId(), abyssOp);
		}
	}

	/**
	 * 返回已加载的欧比斯行动数量。
	 * Returns the number of loaded abyss operations.
	 *
	 * @return 已加载的欧比斯行动数量 / Returns the number of loaded abyss operations.
	 */
	public int size() {
		return opData.size();
	}

	/**
	 * 按 ID 获取欧比斯行动模板。
	 * Returns the abyss operation template for the given id.
	 *
	 * @param id 行动 ID / operation id
	 * @return 模板，不存在则为 null / template or null
	 */
	public AbyssOp getAbyssOpId(int id) {
		return opData.get(id);
	}

	/**
	 * 返回全部欧比斯行动映射。
	 * Returns the full abyss operation map.
	 *
	 * @return ID 到模板的映射 / map of id to template
	 */
	public Map<Integer, AbyssOp> getAll() {
		return opDataMap;
	}
}
