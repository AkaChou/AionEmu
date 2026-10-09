package com.aionemu.gameserver.model.templates;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

import com.aionemu.gameserver.skillengine.effect.AbnormalState;

/**
 * 原版「重复异常状态递减/免疫」条目，逐位对应
 * &lt;原版根&gt;/Map/XML/repeated_abnormal_status_immune.xml。
 * Retail repeated-abnormal decay entry, mirroring
 * &lt;retail root&gt;/Map/XML/repeated_abnormal_status_immune.xml.
 * <p>语义 / Semantics：目标每次被同一状态命中后步数 +1（上限 5）；本次施加落在
 * {@code lastHit + holding_time1 × 原始时长 + holding_time2} 窗口内时，时长按
 * {@code time_value[step]%} 缩放、抵抗追加 {@code resist_value[step]}（千分制）。
 * Each successful hit advances the target's chain step (cap 5); while the next hit lands within
 * {@code lastHit + holding_time1 × base duration + holding_time2}, duration scales by
 * {@code time_value[step]%} and resistance gains {@code resist_value[step]} (per-mille).</p>
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "repeated_abnormal_status_immune_entry")
public class RepeatedAbnormalStatusImmuneTemplate {

	/** 状态名（映射 {@link AbnormalState}），未知名字 fail-fast。 / State name mapped to {@link AbnormalState}, unknown fails fast. */
	@XmlAttribute(name = "name", required = true)
	private String name;
	/** 窗口时长系数（乘以本次施加的原始时长）。 / Window factor applied to the base duration of the hit. */
	@XmlAttribute(name = "holding_time1", required = true)
	private int holdingTime1;
	/** 窗口固定附加时长（毫秒）。 / Fixed window extension in milliseconds. */
	@XmlAttribute(name = "holding_time2", required = true)
	private int holdingTime2;
	/** 逗号分隔的追加抵抗档位（千分制）。 / Comma-separated added-resist tiers (per-mille). */
	@XmlAttribute(name = "resist_value", required = true)
	private String resistValue;
	/** 逗号分隔的时长百分比档位。 / Comma-separated duration-percent tiers. */
	@XmlAttribute(name = "time_value", required = true)
	private String timeValue;

	/** 解析后的异常状态。 / Parsed abnormal state. */
	@XmlTransient
	private AbnormalState abnormalState;
	/** 解析后的追加抵抗档位。 / Parsed added-resist tiers. */
	@XmlTransient
	private int[] resistValues;
	/** 解析后的时长百分比档位。 / Parsed duration-percent tiers. */
	@XmlTransient
	private int[] timeValues;
	/** 数据表位置索引（由持有者赋值，作追踪数组下标）。 / Table position index, assigned by the holder. */
	@XmlTransient
	@Setter
	private int index = -1;

	/**
	 * 反序列化后解析状态名与档位数组，任何非法配置直接终止加载。
	 * Parses the state name and tier arrays after unmarshalling; any invalid configuration aborts loading.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		abnormalState = AbnormalState.getIdByName(name);
		if (abnormalState == null) {
			throw new IllegalStateException("Unknown repeated abnormal status: " + name);
		}
		resistValues = parseTierValues(resistValue, "resist_value");
		timeValues = parseTierValues(timeValue, "time_value");
		if (resistValues.length != timeValues.length) {
			throw new IllegalStateException("Mismatched tier length for repeated abnormal status " + name + ": resist_value="
					+ resistValues.length + ", time_value=" + timeValues.length);
		}
	}

	/**
	 * 返回指定链步数应追加的抵抗值（千分制）。
	 * Returns the added resist value (per-mille) for the given 1-based chain step.
	 * @param step 1 起的链步数 / 1-based chain step
	 * @return 追加抵抗值 / added resist value
	 */
	public int getResistValue(int step) {
		return resistValues[clampStep(step)];
	}

	/**
	 * 返回指定链步数的时长百分比。
	 * Returns the duration percent for the given 1-based chain step.
	 * @param step 1 起的链步数 / 1-based chain step
	 * @return 时长百分比（100 表示不变） / duration percent, 100 means unchanged
	 */
	public int getTimeValue(int step) {
		return timeValues[clampStep(step)];
	}

	/**
	 * 计算本次施加的命中窗口（毫秒）；窗口判定为闭区间。
	 * Computes the hit window in milliseconds; the boundary is inclusive.
	 * @param baseDurationMillis 本次施加的原始时长（毫秒） / base duration of the hit in milliseconds
	 * @return 命中窗口毫秒数 / hit window in milliseconds
	 */
	public long getWindowMillis(int baseDurationMillis) {
		return (long) holdingTime1 * baseDurationMillis + holdingTime2;
	}

	private int clampStep(int step) {
		return Math.min(Math.max(step, 1), resistValues.length) - 1;
	}

	private int[] parseTierValues(String raw, String fieldName) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException("Missing " + fieldName + " for repeated abnormal status " + name);
		}
		String[] parts = raw.split(",");
		int[] values = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				values[i] = Integer.parseInt(parts[i].trim());
			} catch (NumberFormatException e) {
				throw new IllegalStateException(
						"Invalid " + fieldName + " value for repeated abnormal status " + name + ": " + raw, e);
			}
		}
		return values;
	}
}
