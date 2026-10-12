package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.item.WeaponType;

/**
 * 武器类型包装模型测试：验证 Comparable 契约、双持归一化及 TreeMap/TreeSet 兼容性。
 * WeaponTypeWrapper tests: verifies Comparable contract, dual-wield normalization, and tree collections.
 */
class WeaponTypeWrapperTest {

	/**
	 * 双持归一化测试：双手均持武器时归一为主副手 SWORD_1H。
	 * Dual-wield normalization test: weapons in both hands normalize to SWORD_1H.
	 */
	@Test
	void dualWieldNormalizesToSword1H() {
		WeaponTypeWrapper wrapper = new WeaponTypeWrapper(WeaponType.DAGGER_1H, WeaponType.DAGGER_1H);
		assertEquals(WeaponType.SWORD_1H, wrapper.mainHand());
		assertEquals(WeaponType.SWORD_1H, wrapper.offHand());
	}

	/**
	 * Comparable 契约测试：验证自反性与反对称性。
	 * Comparable contract test: verifies reflexivity and antisymmetry.
	 */
	@Test
	void compareToContractIsReflexiveAndAntisymmetric() {
		WeaponTypeWrapper dual = new WeaponTypeWrapper(WeaponType.DAGGER_1H, WeaponType.DAGGER_1H);
		WeaponTypeWrapper singleSword = new WeaponTypeWrapper(WeaponType.SWORD_1H, null);
		WeaponTypeWrapper singleDagger = new WeaponTypeWrapper(WeaponType.DAGGER_1H, null);

		// 自反性 / Reflexivity
		assertEquals(0, dual.compareTo(dual));
		assertEquals(0, singleSword.compareTo(singleSword));

		// 双持 vs 双持 / Dual vs Dual
		WeaponTypeWrapper anotherDual = new WeaponTypeWrapper(WeaponType.SWORD_1H, WeaponType.SWORD_1H);
		assertEquals(0, dual.compareTo(anotherDual));
		assertEquals(0, anotherDual.compareTo(dual));

		// 反对称性 / Antisymmetry
		assertTrue(dual.compareTo(singleSword) > 0);
		assertTrue(singleSword.compareTo(dual) < 0);

		// 单手比较 / Single hand comparison
		assertTrue(singleSword.compareTo(singleDagger) > 0);
		assertTrue(singleDagger.compareTo(singleSword) < 0);
	}

	/**
	 * 集合兼容性测试：在 TreeSet 和 TreeMap 中正确查表与去重。
	 * Collection compatibility test: verifies lookup and deduplication in TreeSet and TreeMap.
	 */
	@Test
	void worksCorrectlyInTreeSetAndTreeMap() {
		TreeSet<WeaponTypeWrapper> set = new TreeSet<>();
		WeaponTypeWrapper dual1 = new WeaponTypeWrapper(WeaponType.DAGGER_1H, WeaponType.DAGGER_1H);
		WeaponTypeWrapper dual2 = new WeaponTypeWrapper(WeaponType.SWORD_1H, WeaponType.SWORD_1H);
		WeaponTypeWrapper singleSword = new WeaponTypeWrapper(WeaponType.SWORD_1H, null);

		assertTrue(set.add(dual1));
		assertFalse(set.add(dual2)); // dual2 normalizes to SWORD_1H/SWORD_1H which equals dual1
		assertTrue(set.add(singleSword));
		assertEquals(2, set.size());
		assertTrue(set.contains(dual1));
		assertTrue(set.contains(singleSword));

		TreeMap<WeaponTypeWrapper, String> map = new TreeMap<>();
		map.put(dual1, "dual");
		map.put(singleSword, "single");
		assertEquals("dual", map.get(dual2));
		assertEquals("single", map.get(singleSword));
	}

	/**
	 * 离线相等性测试：无需运行时单例服务即可正常计算 equals 与 hashCode。
	 * Offline equality test: calculates equals and hashCode without runtime singleton services.
	 */
	@Test
	void equalsAndHashCodeDoNotThrowWithoutService() {
		WeaponTypeWrapper w1 = new WeaponTypeWrapper(WeaponType.SWORD_1H, null);
		WeaponTypeWrapper w2 = new WeaponTypeWrapper(WeaponType.SWORD_1H, null);
		WeaponTypeWrapper w3 = new WeaponTypeWrapper(WeaponType.DAGGER_1H, null);

		assertEquals(w1, w2);
		assertEquals(w1.hashCode(), w2.hashCode());
		assertNotEquals(w1, w3);
	}
}
