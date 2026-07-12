package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.templates.tribe.Tribe;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 种族关系静态数据容器，按种族枚举索引敌友关系。
 * Tribe-relation static-data holder, indexing hostility and friendship by tribe class.
 */
@XmlRootElement(name = "tribe_relations")
@XmlAccessorType(XmlAccessType.FIELD)
public class TribeRelationsData {
	@XmlElement(name = "tribe", required = true)
	protected List<Tribe> tribeList;

	protected Map<TribeClass, Tribe> tribeNameMap = new LinkedHashMap<TribeClass, Tribe>();

	/**
	 * JAXB 反序列化完成后，将种族按名称索引并释放列表。
	 * After JAXB unmarshalling, indexes tribes by name and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (Tribe tribe : tribeList) {
			tribeNameMap.put(tribe.getName(), tribe);
		}
		tribeList = null;
	}

	/**
	 * 返回已加载的种族数量。
	 * Returns the number of loaded tribes.
	 *
	 * tribe count
	 */
	public int size() {
		return tribeNameMap.size();
	}

	/**
	 * 判断该种族是否配置了攻击性关系。
	 * Returns whether the tribe has any aggressive relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在攻击性关系则为 true / true if aggressive relations exist
	 */
	public boolean hasAggressiveRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? (Tribe) tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getAggressive().isEmpty() || (baseTribe != null && !baseTribe.getAggressive().isEmpty());
	}

	/**
	 * 判断该种族是否配置了敌对关系。
	 * Returns whether the tribe has any hostile relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在敌对关系则为 true / true if hostile relations exist
	 */
	public boolean hasHostileRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getHostile().isEmpty() || (baseTribe != null && !baseTribe.getHostile().isEmpty());
	}

	/**
	 * 判断该种族是否配置了支援关系。
	 * Returns whether the tribe has any support relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在支援关系则为 true / true if support relations exist
	 */
	public boolean hasSupportRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getSupport().isEmpty() || (baseTribe != null && !baseTribe.getSupport().isEmpty());
	}

	/**
	 * 判断该种族是否配置了友好关系。
	 * Returns whether the tribe has any friendly relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在友好关系则为 true / true if friendly relations exist
	 */
	public boolean hasFriendRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getFriendly().isEmpty() || (baseTribe != null && !baseTribe.getFriendly().isEmpty());
	}

	/**
	 * 判断该种族是否配置了无关系。
	 * Returns whether the tribe has any none relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在无关系配置则为 true / true if none relations exist
	 */
	public boolean hasNoneRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getNone().isEmpty() || (baseTribe != null && !baseTribe.getNone().isEmpty());
	}

	/**
	 * 判断该种族是否配置了中立关系。
	 * Returns whether the tribe has any neutral relations configured.
	 *
	 * tribe
	 *
	 * @param tribeName
	 * @return 存在中立关系则为 true / true if neutral relations exist
	 */
	public boolean hasNeutralRelations(TribeClass tribeName) {
		Tribe tribe = tribeNameMap.get(tribeName);
		if (tribe == null) {
			return false;
		}
		Tribe baseTribe = tribe.isBasic() ? tribeNameMap.get(tribe.getBase()) : null;
		return !tribe.getNeutral().isEmpty() || (baseTribe != null && !baseTribe.getNeutral().isEmpty());
	}

	/**
	 * 判断种族 1 对种族 2 是否为攻击性关系（友好优先排除）。
	 * Returns whether tribe1 is aggressive toward tribe2 (friendly takes priority).
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 攻击性关系则为 true / true if aggressive
	 */
	public boolean isAggressiveRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		if (isFriendlyRelation(tribeName1, tribeName2)) {
			return false;
		}
		return tribe1.getAggressive().contains(tribeName2)
				|| tribe2.isBasic() && tribe1.getAggressive().contains(tribe2.getBase());
	}

	/**
	 * 判断种族 1 对种族 2 是否为支援关系。
	 * Returns whether tribe1 supports tribe2.
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 支援关系则为 true / true if support
	 */
	public boolean isSupportRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		return tribe1.getSupport().contains(tribeName2)
				|| tribe2.isBasic() && tribe1.getAggressive().contains(tribe2.getBase());
	}

	/**
	 * 判断种族 1 对种族 2 是否为友好关系。
	 * Returns whether tribe1 is friendly toward tribe2.
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 友好关系则为 true / true if friendly
	 */
	public boolean isFriendlyRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		return tribe1.getFriendly().contains(tribeName2)
				|| tribe2.isBasic() && tribe1.getFriendly().contains(tribe2.getBase());
	}

	/**
	 * 判断种族 1 对种族 2 是否为中立关系。
	 * Returns whether tribe1 is neutral toward tribe2.
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 中立关系则为 true / true if neutral
	 */
	public boolean isNeutralRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		return tribe1.getNeutral().contains(tribeName2)
				|| tribe2.isBasic() && tribe1.getNeutral().contains(tribe2.getBase());
	}

	/**
	 * 判断种族 1 对种族 2 是否为无关系。
	 * Returns whether tribe1 has a none relation toward tribe2.
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 无关系则为 true / true if none
	 */
	public boolean isNoneRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		return tribe1.getNone().contains(tribeName2) || tribe2.isBasic() && tribe1.getNone().contains(tribe2.getBase());
	}

	/**
	 * 判断种族 1 对种族 2 是否为敌对关系。
	 * Returns whether tribe1 is hostile toward tribe2.
	 *
	 * source tribe
	 * target tribe
	 *
	 * @return 敌对关系则为 true / true if hostile
	 */
	public boolean isHostileRelation(TribeClass tribeName1, TribeClass tribeName2) {
		Tribe tribe1 = tribeNameMap.get(tribeName1);
		Tribe tribe2 = tribeNameMap.get(tribeName2);
		if (tribe1 == null || tribe2 == null) {
			return false;
		}
		return tribe1.getHostile().contains(tribeName2)
				|| tribe2.isBasic() && tribe1.getHostile().contains(tribe2.getBase());
	}

	/**
	 * 判断是否存在任意种族支援指定种族。
	 * Returns whether any tribe is configured as a supporter of the given tribe.
	 *
	 * target tribe
	 *
	 * @param tribeName
	 * @return 存在支援者则为 true / true if any supporter exists
	 */
	public boolean hasAnySupporter(TribeClass tribeName) {
		Tribe tribe1 = tribeNameMap.get(tribeName);
		if (tribe1 == null) {
			return false;
		}
		for (TribeClass tribe2 : tribeNameMap.keySet()) {
			if (isSupportRelation(tribe2, tribeName)) {
				return true;
			}
		}
		return false;
	}
}
