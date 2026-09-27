package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本服配方索引：{@code (skillid, productid)} → recipe id 集合。
 * <p>
 * 真端 CombineTask 表只给配方符号名（{@code recipe_name}，如 {@code r_ws_q5000}），而服务端运行时按
 * recipe id 工作（{@code learn-recipe}/{@code forget-recipe}）；本索引桥接两者，等价于
 * {@link RetailItemNameIndex} 在物品侧的作用。键为技能 id + 产物物品 id，因为真端表已经同时给出
 * {@code combineskill}/{@code product}，不需要再解析符号名。
 * <p>
 * Index from {@code (skillid, productid)} to recipe template ids; the recipe-side bridge the retail
 * CombineTask table needs (it only carries a symbolic recipe name).
 */
public final class RetailRecipeIndex {

	private static final Pattern TEMPLATE = Pattern.compile("<recipe_template\\b[^>]*>");
	private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)=\"([^\"]*)\"");

	private final Map<Key, Set<Integer>> byPair;

	private RetailRecipeIndex(Map<Key, Set<Integer>> byPair) {
		this.byPair = Map.copyOf(byPair);
	}

	/** 命中该 (技能, 产物) 的全部配方 id（升序）。 / All recipe ids for the pair, ascending. */
	public Set<Integer> resolveAll(int skillId, int productId) {
		Set<Integer> ids = byPair.get(new Key(skillId, productId));
		return ids == null ? Set.of() : ids;
	}

	/** 唯一命中时返回配方 id。 / The recipe id when the pair resolves uniquely. */
	public Optional<Integer> resolveUnique(int skillId, int productId) {
		Set<Integer> ids = resolveAll(skillId, productId);
		return ids.size() == 1 ? Optional.of(ids.iterator().next()) : Optional.empty();
	}

	public int size() {
		return byPair.size();
	}

	/**
	 * 从若干配方模板文件流构建索引；键重复（同技能同产物多配方）时保留全部 id 供调用方判歧义。
	 * Builds the index from recipe template streams; duplicate pairs keep every id so callers can detect
	 * ambiguity instead of silently binding the first recipe.
	 */
	public static RetailRecipeIndex build(Collection<InputStream> templates) throws IOException {
		Map<Key, Set<Integer>> byPair = new HashMap<>();
		try {
			for (InputStream input : templates) {
				String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				Matcher template = TEMPLATE.matcher(text);
				while (template.find()) {
					Map<String, String> fields = attributes(template.group());
					Integer recipeId = positiveInt(fields.get("id"));
					Integer skillId = positiveInt(fields.get("skillid"));
					Integer productId = positiveInt(fields.get("productid"));
					if (recipeId == null || skillId == null || productId == null) {
						continue;
					}
					byPair.computeIfAbsent(new Key(skillId, productId), key -> new TreeSet<>()).add(recipeId);
				}
			}
		} finally {
			for (InputStream input : templates) {
				input.close();
			}
		}
		return new RetailRecipeIndex(byPair);
	}

	private static Map<String, String> attributes(String tag) {
		Map<String, String> fields = new HashMap<>();
		Matcher matcher = ATTRIBUTE.matcher(tag);
		while (matcher.find()) {
			fields.putIfAbsent(matcher.group(1), matcher.group(2));
		}
		return fields;
	}

	private static Integer positiveInt(String raw) {
		if (raw == null || raw.isBlank() || !raw.chars().allMatch(Character::isDigit)) {
			return null;
		}
		int value = Integer.parseInt(raw);
		return value > 0 ? value : null;
	}

	/** 索引键：技能 id + 产物物品 id。 / Index key: craft skill id plus product item id. */
	private record Key(int skillId, int productId) {
	}
}
