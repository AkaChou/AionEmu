package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.Range;

import com.aionemu.gameserver.model.templates.itemgroups.BonusItemGroup;
import com.aionemu.gameserver.model.templates.itemgroups.BossGroup;
import com.aionemu.gameserver.model.templates.itemgroups.CraftItemGroup;
import com.aionemu.gameserver.model.templates.itemgroups.CraftRecipeGroup;
import com.aionemu.gameserver.model.templates.itemgroups.EnchantGroup;
import com.aionemu.gameserver.model.templates.itemgroups.FeedGroups;
import com.aionemu.gameserver.model.templates.itemgroups.FoodGroup;
import com.aionemu.gameserver.model.templates.itemgroups.GatherGroup;
import com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry;
import com.aionemu.gameserver.model.templates.itemgroups.ManastoneGroup;
import com.aionemu.gameserver.model.templates.itemgroups.MedalGroup;
import com.aionemu.gameserver.model.templates.itemgroups.MedicineGroup;
import com.aionemu.gameserver.model.templates.itemgroups.OreGroup;
import com.aionemu.gameserver.model.templates.pet.FoodType;
import com.aionemu.gameserver.model.templates.rewards.CraftItem;
import com.aionemu.gameserver.model.templates.rewards.CraftRecipe;
import com.aionemu.gameserver.model.templates.rewards.CraftReward;
import com.aionemu.gameserver.model.templates.rewards.IdLevelReward;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * 物品分组奖励数据容器，持有制作 / 魔石 / 食物 / 矿石 / 采集 / 宠物饲料等奖励组。
 * Item-group reward data holder for craft, manastone, food, ore, gather and pet-food bonus groups.
 */
@XmlRootElement(name = "item_groups")
@XmlType(name = "", propOrder = { "craftMaterials", "craftShop", "craftBundles", "craftRecipes", "manastonesCommon",
		"manastonesRare", "manastonesLegend", "manastonesEpic", "medals", "foodCommon", "foodRare", "foodLegendary",
		"medicineCommon", "medicineRare", "medicineLegendary", "oresRare", "oresUnique", "oresLegendary", "oresEpic",
		"gatherCommon", "gatherRare", "gatherUnique", "gatherLegendary", "gatherEpic", "enchants", "boss", "feedFluids",
		"feedArmor", "feedThorns", "feedBones", "feedBalaurScales", "feedSouls", "feedExcludes", "stinkingJunk",
		"healthyFoodAll", "healthyFoodSpicy", "aetherPowderBiscuit", "aetherCrystalBiscuit", "aetherGemBiscuit",
		"poppySnack", "poppySnackTasty", "poppySnackNutritious", "infernalDiabolAp", "innocentMerekXp", "shugoCoin",
		"newYearPetFood", "highCraftStep" })
@XmlAccessorType(XmlAccessType.NONE)
public class ItemGroupsData {
	static int RECIPE_UPPER = 40;

	@XmlElement(name = "craft_materials")
	protected CraftItemGroup craftMaterials;

	@XmlElement(name = "craft_shop")
	protected CraftItemGroup craftShop;

	@XmlElement(name = "craft_bundles")
	protected CraftRecipeGroup craftBundles;

	@XmlElement(name = "craft_recipes")
	protected CraftRecipeGroup craftRecipes;

	@XmlElement(name = "manastones_common")
	protected ManastoneGroup manastonesCommon;

	@XmlElement(name = "manastones_rare")
	protected ManastoneGroup manastonesRare;

	@XmlElement(name = "manastones_legend")
	protected ManastoneGroup manastonesLegend;

	@XmlElement(name = "manastones_epic")
	protected ManastoneGroup manastonesEpic;

	@XmlElement(name = "medals")
	protected MedalGroup medals;

	@XmlElement(name = "food_common")
	protected FoodGroup foodCommon;

	@XmlElement(name = "food_rare")
	protected FoodGroup foodRare;

	@XmlElement(name = "food_legendary")
	protected FoodGroup foodLegendary;

	@XmlElement(name = "medicine_common")
	protected MedicineGroup medicineCommon;

	@XmlElement(name = "medicine_rare")
	protected MedicineGroup medicineRare;

	@XmlElement(name = "medicine_legendary")
	protected MedicineGroup medicineLegendary;

	// 矿石 / Ores
	@XmlElement(name = "ores_rare")
	protected OreGroup oresRare;
	@XmlElement(name = "ores_unique")
	protected OreGroup oresUnique;
	@XmlElement(name = "ores_legendary")
	protected OreGroup oresLegendary;
	@XmlElement(name = "ores_epic")
	protected OreGroup oresEpic;

	// 可采集 / Gatherable
	@XmlElement(name = "gather_common")
	protected GatherGroup gatherCommon;
	@XmlElement(name = "gather_rare")
	protected GatherGroup gatherRare;
	@XmlElement(name = "gather_unique")
	protected GatherGroup gatherUnique;
	@XmlElement(name = "gather_legendary")
	protected GatherGroup gatherLegendary;
	@XmlElement(name = "gather_epic")
	protected GatherGroup gatherEpic;

	@XmlElement(name = "enchants")
	protected EnchantGroup enchants;

	@XmlElement(name = "boss")
	protected BossGroup boss;

	@XmlElement(name = "feed_fluid")
	protected FeedGroups.FeedFluidGroup feedFluids;

	@XmlElement(name = "feed_armor")
	protected FeedGroups.FeedArmorGroup feedArmor;

	@XmlElement(name = "feed_thorn")
	protected FeedGroups.FeedThornGroup feedThorns;

	@XmlElement(name = "feed_bone")
	protected FeedGroups.FeedBoneGroup feedBones;

	@XmlElement(name = "feed_balaur_material")
	protected FeedGroups.FeedBalaurGroup feedBalaurScales;

	@XmlElement(name = "feed_soul")
	protected FeedGroups.FeedSoulGroup feedSouls;

	@XmlElement(name = "feed_exclude")
	protected FeedGroups.FeedExcludeGroup feedExcludes;

	@XmlElement(name = "stinking_junk")
	protected FeedGroups.StinkingJunkGroup stinkingJunk;

	@XmlElement(name = "feed_healthy_all")
	protected FeedGroups.HealthyFoodAllGroup healthyFoodAll;

	@XmlElement(name = "feed_healthy_spicy")
	protected FeedGroups.HealthyFoodSpicyGroup healthyFoodSpicy;

	@XmlElement(name = "feed_powder_biscuit")
	protected FeedGroups.AetherPowderBiscuitGroup aetherPowderBiscuit;

	@XmlElement(name = "feed_crystal_biscuit")
	protected FeedGroups.AetherCrystalBiscuitGroup aetherCrystalBiscuit;

	@XmlElement(name = "feed_gem_biscuit")
	protected FeedGroups.AetherGemBiscuitGroup aetherGemBiscuit;

	@XmlElement(name = "poppy_snack")
	protected FeedGroups.PoppySnackGroup poppySnack;

	@XmlElement(name = "tasty_poppy_snack")
	protected FeedGroups.PoppySnackTastyGroup poppySnackTasty;

	@XmlElement(name = "nutritious_poppy_snack")
	protected FeedGroups.PoppySnackNutritiousGroup poppySnackNutritious;

	@XmlElement(name = "infernal_diabol_ap")
	protected FeedGroups.InfernalDiabolApGroup infernalDiabolAp;

	@XmlElement(name = "innocent_merek_xp")
	protected FeedGroups.InnocentMerekXpGroup innocentMerekXp;

	@XmlElement(name = "shugo_coin")
	protected FeedGroups.ShugoCoinGroup shugoCoin;

	@XmlElement(name = "new_year_pet_food")
	protected FeedGroups.NewYearPetFoodGroup newYearPetFood;

	@XmlElement(name = "high_craft_step")
	protected FeedGroups.HighCraftStepGroup highCraftStep;

	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftMaterialsBySkill = new LinkedHashMap<Integer, Map<Range<Integer>, List<CraftReward>>>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftShopBySkill = new LinkedHashMap<Integer, Map<Range<Integer>, List<CraftReward>>>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftBundlesBySkill = new LinkedHashMap<Integer, Map<Range<Integer>, List<CraftReward>>>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftRecipesBySkill = new LinkedHashMap<Integer, Map<Range<Integer>, List<CraftReward>>>();

	BonusItemGroup[] craftGroups;
	BonusItemGroup[] manastoneGroups;
	BonusItemGroup[] medalGroups;
	BonusItemGroup[] foodGroups;
	BonusItemGroup[] medicineGroups;
	BonusItemGroup[] oreGroups;
	BonusItemGroup[] gatherGroups;
	BonusItemGroup[] enchantGroups;
	BonusItemGroup[] bossGroups;
	Map<FoodType, Set<Integer>> petFood = new HashMap<FoodType, Set<Integer>>();

	private int count = 0;
	private int petFoodCount = 0;

	/**
	 * JAXB 反序列化完成后，将制作奖励按技能与等级区间建索引，并缓存宠物饲料 ID。
	 * After JAXB unmarshalling, indexes craft rewards by skill/level range and caches pet-food item ids.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (CraftItem item : craftMaterials.getItems()) {
			MapCraftReward(craftMaterialsBySkill, item);
		}
		count += craftMaterials.getItems().size();
		craftMaterials.getItems().clear();
		craftMaterials.setDataHolder(craftMaterialsBySkill);
		for (CraftItem item : craftShop.getItems()) {
			MapCraftReward(craftShopBySkill, item);
		}
		count += craftShop.getItems().size();
		craftShop.getItems().clear();
		craftShop.setDataHolder(craftShopBySkill);
		for (CraftRecipe recipe : craftBundles.getItems()) {
			MapCraftReward(craftBundlesBySkill, recipe);
		}
		count += craftBundles.getItems().size();
		craftBundles.getItems().clear();
		craftBundles.setDataHolder(craftBundlesBySkill);
		for (CraftRecipe recipe : craftRecipes.getItems()) {
			MapCraftReward(craftRecipesBySkill, recipe);
		}
		count += craftRecipes.getItems().size();
		craftRecipes.getItems().clear();
		craftRecipes.setDataHolder(craftRecipesBySkill);
		craftGroups = new BonusItemGroup[] { craftMaterials, craftShop, craftBundles, craftRecipes };
		manastoneGroups = new BonusItemGroup[] { manastonesCommon, manastonesRare, manastonesLegend, manastonesEpic };
		medalGroups = new BonusItemGroup[] { medals };
		foodGroups = new BonusItemGroup[] { foodCommon, foodRare, foodLegendary };
		medicineGroups = new BonusItemGroup[] { medicineCommon, medicineRare, medicineLegendary };
		oreGroups = new BonusItemGroup[] { oresRare, oresUnique, oresLegendary, oresEpic };
		gatherGroups = new BonusItemGroup[] { gatherCommon, gatherRare, gatherUnique, gatherLegendary, gatherEpic };
		enchantGroups = new BonusItemGroup[] { enchants };
		bossGroups = new BonusItemGroup[] { boss };
		for (FoodType foodType : FoodType.values()) {
			List<ItemRaceEntry> food = getPetFood(foodType);
			if (food != null) {
				Set<Integer> itemIds = new HashSet<>();
				for (ItemRaceEntry item : food) {
					itemIds.add(item.getId());
				}
				petFood.put(foodType, itemIds);
				if (foodType != FoodType.EXCLUDES && foodType != FoodType.STINKY) {
					petFoodCount += itemIds.size();
				}
				food.clear();
			}
		}
	}

	void MapCraftReward(Map<Integer, Map<Range<Integer>, List<CraftReward>>> dataHolder, CraftReward reward) {
		int lowerBound = 0, upperBound = 0;
		if (reward instanceof CraftRecipe) {
			CraftRecipe recipe = (CraftRecipe) reward;
			lowerBound = recipe.getLevel();
			upperBound = lowerBound + RECIPE_UPPER;
			if (upperBound / 100 != lowerBound / 100) {
				upperBound = lowerBound / 100 + 99;
			}
		} else {
			CraftItem item = (CraftItem) reward;
			lowerBound = item.getMinLevel();
			upperBound = item.getMaxLevel();
		}
		Range<Integer> range = Range.of(lowerBound, upperBound);
		Map<Range<Integer>, List<CraftReward>> ranges;
		if (dataHolder.containsKey(reward.getSkill())) {
			ranges = dataHolder.get(reward.getSkill());
		} else {
			ranges = new LinkedHashMap<Range<Integer>, List<CraftReward>>();
			dataHolder.put(reward.getSkill(), ranges);
		}
		List<CraftReward> items;
		if (ranges.containsKey(range)) {
			items = ranges.get(range);
		} else {
			items = new ArrayList<CraftReward>();
			ranges.put(range, items);
		}
		items.add(reward);
	}

	/**
	 * 按技能 ID 获取制作材料奖励。
	 * Returns craft material rewards for the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 制作材料奖励集合 / craft material rewards
	 */
	public Collection<CraftReward> getCraftMaterials(int skillId) {
		if (craftMaterialsBySkill.containsKey(skillId)) {
			return Collections.emptyList();
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (List<CraftReward> items : craftMaterialsBySkill.get(skillId).values()) {
			result.addAll(items);
		}
		return result;
	}

	/**
	 * 返回制作材料组掉落概率。
	 * Returns the craft materials group chance.
	 *
	 * drop chance
	 */
	public float getCraftMaterialsChance() {
		return craftMaterials.getChance();
	}

	/**
	 * 按技能 ID 获取制作商店物品奖励。
	 * Returns craft shop item rewards for the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 制作商店物品奖励集合 / craft shop item rewards
	 */
	public Collection<CraftReward> getCraftShopItems(int skillId) {
		if (craftShopBySkill.containsKey(skillId)) {
			return Collections.emptyList();
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (List<CraftReward> items : craftShopBySkill.get(skillId).values()) {
			result.addAll(items);
		}
		return result;
	}

	/**
	 * 返回制作商店物品组掉落概率。
	 * Returns the craft shop items group chance.
	 *
	 * drop chance
	 */
	public float getCraftShopItemsChance() {
		return craftShop.getChance();
	}

	/**
	 * 按技能 ID 获取制作礼包奖励。
	 * Returns craft bundle rewards for the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 制作礼包奖励集合 / craft bundle rewards
	 */
	public Collection<CraftReward> getCraftBundles(int skillId) {
		if (craftBundlesBySkill.containsKey(skillId)) {
			return Collections.emptyList();
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (List<CraftReward> items : craftBundlesBySkill.get(skillId).values()) {
			result.addAll(items);
		}
		return result;
	}

	/**
	 * 返回制作礼包组掉落概率。
	 * Returns the craft bundles group chance.
	 *
	 * drop chance
	 */
	public float getCraftBundlesChance() {
		return craftBundles.getChance();
	}

	/**
	 * 按技能 ID 获取制作配方奖励。
	 * Returns craft recipe rewards for the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 制作配方奖励集合 / craft recipe rewards
	 */
	public Collection<CraftReward> getCraftRecipes(int skillId) {
		if (craftRecipesBySkill.containsKey(skillId)) {
			return Collections.emptyList();
		}
		List<CraftReward> result = new ArrayList<CraftReward>();
		for (List<CraftReward> items : craftRecipesBySkill.get(skillId).values()) {
			result.addAll(items);
		}
		return result;
	}

	/**
	 * 返回制作配方组掉落概率。
	 * Returns the craft recipes group chance.
	 *
	 * drop chance
	 */
	public float getCraftRecipesChance() {
		return craftRecipes.getChance();
	}

	/** 普通魔石 / common 魔石 */
	public Collection<ItemRaceEntry> getManastonesCommon() {
		return manastonesCommon.getItems();
	}

	/** 普通魔石概率 / common 魔石 chance */
	public float getManastonesCommonChance() {
		return manastonesCommon.getChance();
	}

	/** 稀有魔石 / rare 魔石 */
	public Collection<ItemRaceEntry> getManastonesRare() {
		return manastonesRare.getItems();
	}

	/** 稀有魔石概率 / rare 魔石 chance */
	public float getManastonesRareChance() {
		return manastonesRare.getChance();
	}

	/** 传颂魔石 / legend 魔石 */
	public Collection<ItemRaceEntry> getManastonesLegend() {
		return manastonesLegend.getItems();
	}

	/** 传颂魔石概率 / legend 魔石 chance */
	public float getManastonesLegendChance() {
		return manastonesLegend.getChance();
	}

	/** 史诗魔石 / epic 魔石 */
	public Collection<ItemRaceEntry> getManastonesEpic() {
		return manastonesEpic.getItems();
	}

	/** 史诗魔石概率 / epic 魔石 chance */
	public float getManastonesEpicChance() {
		return manastonesEpic.getChance();
	}

	/** 普通食物 / common food */
	public Collection<IdLevelReward> getFoodCommon() {
		return foodCommon.getItems();
	}

	/** 普通食物概率 / common food chance */
	public float getFoodCommonChance() {
		return foodCommon.getChance();
	}

	/** 稀有食物 / rare food */
	public Collection<IdLevelReward> getFoodRare() {
		return foodRare.getItems();
	}

	/** 稀有食物概率 / rare food chance */
	public float getFoodRareChance() {
		return foodRare.getChance();
	}

	/** 传说食物 / legendary food */
	public Collection<IdLevelReward> getFoodLegendary() {
		return foodLegendary.getItems();
	}

	/** 传说食物概率 / legendary food chance */
	public float getFoodLegendaryChance() {
		return foodLegendary.getChance();
	}

	/** 普通药品 / common medicine */
	public Collection<IdLevelReward> getMedicineCommon() {
		return medicineCommon.getItems();
	}

	/** 普通药品概率 / common medicine chance */
	public float getMedicineCommonChance() {
		return medicineCommon.getChance();
	}

	/** 稀有药品 / rare medicine */
	public Collection<IdLevelReward> getMedicineRare() {
		return medicineRare.getItems();
	}

	/** 稀有药品概率 / rare medicine chance */
	public float getMedicineRareChance() {
		return medicineRare.getChance();
	}

	/** 传说药品 / legendary medicine */
	public Collection<IdLevelReward> getMedicineLegendary() {
		return medicineLegendary.getItems();
	}

	/** 传说药品概率 / legendary medicine chance */
	public float getMedicineLegendaryChance() {
		return medicineLegendary.getChance();
	}

	// 矿石。 / Ores.
	/** 稀有矿石 / rare ores */
	public Collection<ItemRaceEntry> getOresRare() {
		return oresRare.getItems();
	}

	/** 稀有矿石概率 / rare ores chance */
	public float getOresRareChance() {
		return oresRare.getChance();
	}

	/** 唯一矿石 / unique ores */
	public Collection<ItemRaceEntry> getOresUnique() {
		return oresUnique.getItems();
	}

	/** 唯一矿石概率 / unique ores chance */
	public float getOresUniqueChance() {
		return oresUnique.getChance();
	}

	/** 传说矿石 / legendary ores */
	public Collection<ItemRaceEntry> getOresLegendary() {
		return oresLegendary.getItems();
	}

	/** 传说矿石概率 / legendary ores chance */
	public float getOresLegendaryChance() {
		return oresLegendary.getChance();
	}

	/** 史诗矿石 / epic ores */
	public Collection<ItemRaceEntry> getOresEpic() {
		return oresEpic.getItems();
	}

	/** 史诗矿石概率 / epic ores chance */
	public float getOresEpicChance() {
		return oresEpic.getChance();
	}

	// 可采集。 / Gatherable.
	/** 普通采集物 / common 采集物 */
	public Collection<ItemRaceEntry> getGatherCommon() {
		return gatherCommon.getItems();
	}

	/** 普通采集物概率 / common 采集物 chance */
	public float getGatherCommonChance() {
		return gatherCommon.getChance();
	}

	/** 稀有采集物 / rare 采集物 */
	public Collection<ItemRaceEntry> getGatherRare() {
		return gatherRare.getItems();
	}

	/** 稀有采集物概率 / rare 采集物 chance */
	public float getGatherRareChance() {
		return gatherRare.getChance();
	}

	/** 唯一采集物 / 唯一采集物 */
	public Collection<ItemRaceEntry> getGatherUnique() {
		return gatherUnique.getItems();
	}

	/** 唯一采集物概率 / 唯一采集物 chance */
	public float getGatherUniqueChance() {
		return gatherUnique.getChance();
	}

	/** 传说采集物 / legendary 采集物 */
	public Collection<ItemRaceEntry> getGatherLegendary() {
		return gatherLegendary.getItems();
	}

	/** 传说采集物概率 / legendary 采集物 chance */
	public float getGatherLegendaryChance() {
		return gatherLegendary.getChance();
	}

	/** 史诗采集物 / epic 采集物 */
	public Collection<ItemRaceEntry> getGatherEpic() {
		return gatherEpic.getItems();
	}

	/** 史诗采集物概率 / epic 采集物 chance */
	public float getGatherEpicChance() {
		return gatherEpic.getChance();
	}

	/** 强化物品 / enchant items */
	public Collection<IdLevelReward> getEnchants() {
		return enchants.getItems();
	}

	/** 强化物品概率 / enchant items chance */
	public float getEnchantsChance() {
		return enchants.getChance();
	}

	/** 首领掉落物品 / boss drop items */
	public Collection<ItemRaceEntry> getBoss() {
		return boss.getItems();
	}

	/** 首领掉落概率 / boss drop chance */
	public float getBossChance() {
		return boss.getChance();
	}

	/** 制作材料组 / craft 材料 group */
	public CraftItemGroup getCraftMaterials() {
		return craftMaterials;
	}

	/** 制作商店组 / craft shop group */
	public CraftItemGroup getCraftShop() {
		return craftShop;
	}

	/** 制作捆包组 / craft 捆包 group */
	public CraftRecipeGroup getCraftBundles() {
		return craftBundles;
	}

	/** 制作配方组 / craft recipes group */
	public CraftRecipeGroup getCraftRecipes() {
		return craftRecipes;
	}

	/** 全部制作加成组 / all craft bonus groups */
	public BonusItemGroup[] getCraftGroups() {
		return craftGroups;
	}

	/** 全部魔石加成组 / all manastone bonus groups */
	public BonusItemGroup[] getManastoneGroups() {
		return manastoneGroups;
	}

	/** 勋章加成组 / medal bonus groups */
	public BonusItemGroup[] getMedalGroups() {
		return medalGroups;
	}

	/** 全部食物加成组 / all food bonus groups */
	public BonusItemGroup[] getFoodGroups() {
		return foodGroups;
	}

	/** 全部药品加成组 / all medicine bonus groups */
	public BonusItemGroup[] getMedicineGroups() {
		return medicineGroups;
	}

	/** 全部矿石加成组 / all ore bonus groups */
	public BonusItemGroup[] getOreGroups() {
		return oreGroups;
	}

	/** 全部采集加成组 / all gather bonus groups */
	public BonusItemGroup[] getGatherGroups() {
		return gatherGroups;
	}

	/** 强化加成组 / enchant bonus groups */
	public BonusItemGroup[] getEnchantGroups() {
		return enchantGroups;
	}

	/** 首领加成组 / boss bonus groups */
	public BonusItemGroup[] getBossGroups() {
		return bossGroups;
	}

	/**
	 * 判断物品是否属于指定宠物饲料类型（排除黑名单与臭食）。
	 * Returns whether the item is food of the given pet food type (excluding blacklist and stinky items).
	 *
	 * item id
	 * food type
	 *
	 * @return 若 the item matches the food type 则为 true / true if the item matches the food type
	 */
	public boolean isFood(int itemId, FoodType foodType) {
		Set<Integer> food = petFood.get(FoodType.EXCLUDES);
		if (food.contains(itemId)) {
			return false;
		}
		food = petFood.get(FoodType.STINKY);
		if (food.contains(itemId)) {
			return false;
		}
		if (foodType != FoodType.MISCELLANEOUS) {
			food = petFood.get(foodType);
			return food.contains(itemId);
		}
		food = petFood.get(FoodType.ARMOR);
		if (food.contains(itemId)) {
			return true;
		}
		food = petFood.get(FoodType.BALAUR_SCALES);
		if (food.contains(itemId)) {
			return true;
		}
		food = petFood.get(FoodType.BONES);
		if (food.contains(itemId)) {
			return true;
		}
		food = petFood.get(FoodType.FLUIDS);
		if (food.contains(itemId)) {
			return true;
		}
		food = petFood.get(FoodType.SOULS);
		if (food.contains(itemId)) {
			return true;
		}
		food = petFood.get(FoodType.THORNS);
		if (food.contains(itemId)) {
			return true;
		}
		return false;
	}

	private List<ItemRaceEntry> getPetFood(FoodType foodType) {
		switch (foodType) {
		case AETHER_CRYSTAL_BISCUIT:
			return aetherCrystalBiscuit.getItems();
		case AETHER_GEM_BISCUIT:
			return aetherGemBiscuit.getItems();
		case AETHER_POWDER_BISCUIT:
			return aetherPowderBiscuit.getItems();
		case ARMOR:
			return feedArmor.getItems();
		case BALAUR_SCALES:
			return feedBalaurScales.getItems();
		case BONES:
			return feedBones.getItems();
		case FLUIDS:
			return feedFluids.getItems();
		case SOULS:
			return feedSouls.getItems();
		case THORNS:
			return feedThorns.getItems();
		case HIGH_CRAFT_STEP:
			return highCraftStep.getItems();
		case HEALTHY_FOOD_ALL:
			return healthyFoodAll.getItems();
		case HEALTHY_FOOD_SPICY:
			return healthyFoodSpicy.getItems();
		case POPPY_SNACK:
			return poppySnack.getItems();
		case POPPY_SNACK_TASTY:
			return poppySnackTasty.getItems();
		case POPPY_SNACK_NUTRITIOUS:
			return poppySnackNutritious.getItems();
		case INFERNAL_DIABOL_AP:
			return infernalDiabolAp.getItems();
		case INNOCENT_MEREK_XP:
			return innocentMerekXp.getItems();
		case SHUGO_COIN:
			return shugoCoin.getItems();
		case NEW_YEAR_PET_FOOD:
			return newYearPetFood.getItems();
		case STINKY:
			return stinkingJunk.getItems();
		case EXCLUDES:
			return feedExcludes.getItems();
		}
		return null;
	}

	/**
	 * 返回全部奖励组条目的总数（不含宠物饲料）。
	 * Returns the total count of bonus group entries (excluding pet food).
	 *
	 * @return 奖励条目总数 / total bonus entry count
	 */
	public int bonusSize() {
		return count + manastonesCommon.getItems().size() + manastonesRare.getItems().size()
				+ manastonesLegend.getItems().size() + manastonesEpic.getItems().size() + foodCommon.getItems().size()
				+ foodRare.getItems().size() + foodLegendary.getItems().size() + medicineCommon.getItems().size()
				+ medicineRare.getItems().size() + medicineLegendary.getItems().size() + oresRare.getItems().size()
				+ oresUnique.getItems().size() + oresLegendary.getItems().size() + oresEpic.getItems().size()
				+ gatherCommon.getItems().size() + gatherRare.getItems().size() + gatherUnique.getItems().size()
				+ gatherLegendary.getItems().size() + gatherEpic.getItems().size() + enchants.getItems().size()
				+ boss.getItems().size();
	}

	/**
	 * 返回已缓存的宠物饲料物品数量（不含排除与臭食）。
	 * Returns the cached pet-food item count (excluding excludes and stinky).
	 *
	 * @return 宠物饲料数量 / pet-food count
	 */
	public int petFoodSize() {
		return petFoodCount;
	}
}
