package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import lombok.Getter;

/**
 * 物品分组奖励数据容器，持有制作 / 魔石 / 食物 / 矿石 / 采集 / 宠物饲料等奖励组。
 * Item-group reward data holder for craft, manastone, food, ore, gather and pet-food bonus groups.
 */
@Getter
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

	/** 制作材料组 / craft 材料 group */
	@XmlElement(name = "craft_materials")
	protected CraftItemGroup craftMaterials;

	/** 制作商店组 / craft shop group */
	@XmlElement(name = "craft_shop")
	protected CraftItemGroup craftShop;

	/** 制作捆包组 / craft 捆包 group */
	@XmlElement(name = "craft_bundles")
	protected CraftRecipeGroup craftBundles;

	/** 制作配方组 / craft recipes group */
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

	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftMaterialsBySkill = new LinkedHashMap<>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftShopBySkill = new LinkedHashMap<>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftBundlesBySkill = new LinkedHashMap<>();
	Map<Integer, Map<Range<Integer>, List<CraftReward>>> craftRecipesBySkill = new LinkedHashMap<>();

	/** 全部制作加成组 / all craft bonus groups */
	BonusItemGroup[] craftGroups;
	/** 全部魔石加成组 / all manastone bonus groups */
	BonusItemGroup[] manastoneGroups;
	/** 勋章加成组 / medal bonus groups */
	BonusItemGroup[] medalGroups;
	/** 全部食物加成组 / all food bonus groups */
	BonusItemGroup[] foodGroups;
	/** 全部药品加成组 / all medicine bonus groups */
	BonusItemGroup[] medicineGroups;
	/** 全部矿石加成组 / all ore bonus groups */
	BonusItemGroup[] oreGroups;
	/** 全部采集加成组 / all gather bonus groups */
	BonusItemGroup[] gatherGroups;
	/** 强化加成组 / enchant bonus groups */
	BonusItemGroup[] enchantGroups;
	/** 首领加成组 / boss bonus groups */
	BonusItemGroup[] bossGroups;
	Map<FoodType, Set<Integer>> petFood = new HashMap<>();

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
		int lowerBound, upperBound;
		if (reward instanceof CraftRecipe recipe) {
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
			ranges = new LinkedHashMap<>();
			dataHolder.put(reward.getSkill(), ranges);
		}
		List<CraftReward> items;
		if (ranges.containsKey(range)) {
			items = ranges.get(range);
		} else {
			items = new ArrayList<>();
			ranges.put(range, items);
		}
		items.add(reward);
	}





























	// 矿石。 / Ores.








	// 可采集。 / Gatherable.










	/** 强化物品 / enchant items */
	public Collection<IdLevelReward> getEnchants() {
		return enchants.getItems();
	}




	/**
	 * 判断物品是否属于指定宠物饲料类型（排除黑名单与臭食）。
	 * Returns whether the item is food of the given pet food type (excluding blacklist and stinky items).
	 * @param itemId 物品 ID / item id
	 * @param foodType 宠物饲料类型 / pet food type
	 * @return 若物品匹配该饲料类型则为 true / true if the item matches the food type
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
		for (FoodType miscType : List.of(FoodType.ARMOR, FoodType.BALAUR_SCALES, FoodType.BONES, FoodType.FLUIDS,
			FoodType.SOULS, FoodType.THORNS)) {
			if (petFood.get(miscType).contains(itemId)) {
				return true;
			}
		}
		return false;
	}

	private List<ItemRaceEntry> getPetFood(FoodType foodType) {
        return switch (foodType) {
            case AETHER_CRYSTAL_BISCUIT -> aetherCrystalBiscuit.getItems();
            case AETHER_GEM_BISCUIT -> aetherGemBiscuit.getItems();
            case AETHER_POWDER_BISCUIT -> aetherPowderBiscuit.getItems();
            case ARMOR -> feedArmor.getItems();
            case BALAUR_SCALES -> feedBalaurScales.getItems();
            case BONES -> feedBones.getItems();
            case FLUIDS -> feedFluids.getItems();
            case SOULS -> feedSouls.getItems();
            case THORNS -> feedThorns.getItems();
            case HIGH_CRAFT_STEP -> highCraftStep.getItems();
            case HEALTHY_FOOD_ALL -> healthyFoodAll.getItems();
            case HEALTHY_FOOD_SPICY -> healthyFoodSpicy.getItems();
            case POPPY_SNACK -> poppySnack.getItems();
            case POPPY_SNACK_TASTY -> poppySnackTasty.getItems();
            case POPPY_SNACK_NUTRITIOUS -> poppySnackNutritious.getItems();
            case INFERNAL_DIABOL_AP -> infernalDiabolAp.getItems();
            case INNOCENT_MEREK_XP -> innocentMerekXp.getItems();
            case SHUGO_COIN -> shugoCoin.getItems();
            case NEW_YEAR_PET_FOOD -> newYearPetFood.getItems();
            case STINKY -> stinkingJunk.getItems();
            case EXCLUDES -> feedExcludes.getItems();
            default -> null;
        };
    }

	/**
	 * 返回全部奖励组条目的总数（不含宠物饲料）。
	 * Returns the total count of bonus group entries (excluding pet food).
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
	 * @return 宠物饲料数量 / pet-food count
	 */
	public int petFoodSize() {
		return petFoodCount;
	}
}
