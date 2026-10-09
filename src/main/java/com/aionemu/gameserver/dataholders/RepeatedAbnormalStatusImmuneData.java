package com.aionemu.gameserver.dataholders;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.RepeatedAbnormalStatusImmuneTemplate;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;

/**
 * 重复异常状态免疫数据容器，按异常状态与抵抗属性双重索引。
 * Repeated-abnormal-status immunity data holder indexing entries by abnormal state and resist stat.
 * <p>仅收录原版表内状态（PARALYZE / SLEEP / FEAR）；表外状态（含 STUN）在读取端
 * 自然查不到条目，不参与递减链。
 * Only states present in the retail table (PARALYZE / SLEEP / FEAR) are indexed; states absent
 * from it (STUN included) resolve to no entry and take no part in the decay chain.</p>
 */
@XmlRootElement(name = "repeated_abnormal_status_immune")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepeatedAbnormalStatusImmuneData {

	@XmlElement(name = "abnormal_status")
	private List<RepeatedAbnormalStatusImmuneTemplate> list;

	/**
	 * 运行期抵抗属性 ↔ 异常状态白名单，是两者间唯一映射源。
	 * The only runtime resist-stat ↔ abnormal-state mapping; entries without a mapping abort loading.
	 */
	private static final Map<AbnormalState, StatEnum> RESIST_STAT_BY_STATE = Map.of(
			AbnormalState.SLEEP, StatEnum.SLEEP_RESISTANCE,
			AbnormalState.PARALYZE, StatEnum.PARALYZE_RESISTANCE,
			AbnormalState.FEAR, StatEnum.FEAR_RESISTANCE);

	private EnumMap<AbnormalState, RepeatedAbnormalStatusImmuneTemplate> byState;
	private EnumMap<StatEnum, RepeatedAbnormalStatusImmuneTemplate> byResistStat;

	/**
	 * JAXB 反序列化完成后建立双索引并赋数据表位置，同时释放列表。
	 * After JAXB unmarshalling, builds both indexes, assigns table positions and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		byState = new EnumMap<>(AbnormalState.class);
		byResistStat = new EnumMap<>(StatEnum.class);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				RepeatedAbnormalStatusImmuneTemplate template = list.get(i);
				template.setIndex(i);
				StatEnum resistStat = RESIST_STAT_BY_STATE.get(template.getAbnormalState());
				if (resistStat == null) {
					throw new IllegalStateException("Repeated abnormal status " + template.getName()
							+ " has no runtime resist stat mapping");
				}
				byState.put(template.getAbnormalState(), template);
				byResistStat.put(resistStat, template);
			}
			list = null;
		}
	}

	/**
	 * 按异常状态返回条目。
	 * Returns the entry for the given abnormal state.
	 * @param state 异常状态 / abnormal state
	 * @return 条目，表外状态为 null / entry, or null for untracked states
	 */
	public RepeatedAbnormalStatusImmuneTemplate getTemplate(AbnormalState state) {
		return byState == null ? null : byState.get(state);
	}

	/**
	 * 按抵抗属性返回条目（读取端主入口）。
	 * Returns the entry for the given resist stat (the main lookup used by the resist phase).
	 * @param resistStat 抵抗属性 / resist stat
	 * @return 条目，表外状态为 null / entry, or null for untracked states
	 */
	public RepeatedAbnormalStatusImmuneTemplate getTemplate(StatEnum resistStat) {
		return byResistStat == null ? null : byResistStat.get(resistStat);
	}

	/**
	 * 返回已加载的条目数量。
	 * Returns the number of loaded entries.
	 * @return 已加载条目数量 / number of loaded entries
	 */
	public int size() {
		return byState == null ? 0 : byState.size();
	}
}
