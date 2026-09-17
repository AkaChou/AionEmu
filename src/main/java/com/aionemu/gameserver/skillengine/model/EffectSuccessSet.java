package com.aionemu.gameserver.skillengine.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.FearEffect;

/**
 * 运行时效果的成功模板集合。
 * Succeeded-template set of a runtime effect.
 *
 * <p>该类型只服务 {@link Effect}：维护通过命中/抗性判定的效果模板，提供位置、效果 ID 与恐惧效果查询，
 * 并保留原始集合视图。命中判定、生命周期与对外 API 仍由 {@link Effect} 负责。
 * This type only serves {@link Effect}: it owns the templates that passed hit/resistance resolution,
 * provides position, effect-id and fear-effect queries, and keeps the backing collection view.
 * Hit resolution, lifecycle and the public API stay on {@link Effect}.</p>
 */
final class EffectSuccessSet implements Iterable<EffectTemplate> {

	private final List<EffectTemplate> effects = new ArrayList<>();

	/**
	 * 添加成功效果，重复添加保持原列表语义。
	 * Adds a successful effect while preserving the original duplicate behavior.
	 *
	 * @param effect 效果模板 / effect template
	 */
	void add(EffectTemplate effect) {
		if (!effects.contains(effect)) {
			effects.add(effect);
		}
	}

	/**
	 * 替换全部成功效果。
	 * Replaces all successful effects.
	 *
	 * @param templates 新模板集合 / new template collection
	 */
	void replaceWith(Collection<? extends EffectTemplate> templates) {
		effects.clear();
		effects.addAll(templates);
	}

	/**
	 * 清空成功效果。
	 * Clears successful effects.
	 */
	void clear() {
		effects.clear();
	}

	/**
	 * 是否为空。
	 * Whether this set is empty.
	 *
	 * @return 为空则为 true / true if empty
	 */
	boolean isEmpty() {
		return effects.isEmpty();
	}

	/**
	 * 返回成功效果数量。
	 * Returns the successful effect count.
	 *
	 * @return 数量 / count
	 */
	int size() {
		return effects.size();
	}

	/**
	 * 按索引获取成功效果。
	 * Returns the successful effect at the given index.
	 *
	 * @param index 索引 / index
	 * @return 效果模板 / effect template
	 */
	EffectTemplate get(int index) {
		return effects.get(index);
	}

	/**
	 * 返回原始列表视图。
	 * Returns the backing list view.
	 *
	 * @return 列表视图 / list view
	 */
	List<EffectTemplate> asList() {
		return effects;
	}

	/**
	 * 返回原始集合视图。
	 * Returns the backing collection view.
	 *
	 * @return 集合视图 / collection view
	 */
	Collection<EffectTemplate> asCollection() {
		return effects;
	}

	/**
	 * 是否包含指定位置的模板。
	 * Whether the set contains a template at the given position.
	 *
	 * @param position 位置 / position
	 * @return 包含则为 true / true if present
	 */
	boolean containsPosition(int position) {
		for (EffectTemplate effect : effects) {
			if (effect.getPosition() == position) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否包含指定效果 ID。
	 * Whether the set contains the given effect id.
	 *
	 * @param effectId 效果 ID / effect id
	 * @return 包含则为 true / true if present
	 */
	boolean containsEffectId(int effectId) {
		for (EffectTemplate effect : effects) {
			if (effect.getEffectid() == effectId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否包含恐惧效果。
	 * Whether the set contains a fear effect.
	 *
	 * @return 包含则为 true / true if present
	 */
	boolean containsFearEffect() {
		for (EffectTemplate effect : effects) {
			if (effect instanceof FearEffect) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<EffectTemplate> iterator() {
		return effects.iterator();
	}
}
