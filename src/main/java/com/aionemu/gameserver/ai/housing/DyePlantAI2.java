package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemService;

/**
 * 房屋相关 NPC AI：Dye Plant（@AIName "dyeplant"），继承 ActionItemNpcAI2。
 * Housing-related NPC AI: Dye Plant (@AIName "dyeplant"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("dyeplant")
public class DyePlantAI2 extends ActionItemNpcAI2
{
    @Override
    protected void handleUseItemFinish(Player player) {
		// 随机发放一种染料/壁纸奖励，共 21 种。 / Grants a random paint/wallpaper reward, 21 variants total.
		switch (Rnd.get(1, 21)) {
		    case 1:
				ItemService.addItem(player, 171110041, 1); //紫色素色壁纸。 / Purple Plain Wallpaper.
			break;
			case 2:
				ItemService.addItem(player, 182006982, 2); //花朵精华。 / Flower's Essence.
			break;
			case 3:
				ItemService.addItem(player, 169120000, 1); //染料：红色。 / Paint: Red.
			break;
			case 4:
				ItemService.addItem(player, 169120001, 1); //染料：深红。 / Paint: Deep Red.
			break;
			case 5:
				ItemService.addItem(player, 169120002, 1); //染料：粉红。 / Paint: Pink.
			break;
			case 6:
				ItemService.addItem(player, 169120003, 1); //染料：蓝色。 / Paint: Blue.
			break;
			case 7:
				ItemService.addItem(player, 169120004, 1); //染料：紫色。 / Paint: Purple.
			break;
			case 8:
				ItemService.addItem(player, 169120005, 1); //染料：橙色。 / Paint: Orange.
			break;
			case 9:
				ItemService.addItem(player, 169120006, 1); //染料：芥末黄。 / Paint: Mustard.
			break;
			case 10:
				ItemService.addItem(player, 169120035, 1); //染料：淡粉。 / Paint: Pale Pink.
			break;
			case 11:
				ItemService.addItem(player, 169120036, 1); //染料：酒红。 / Paint: Wine.
			break;
			case 12:
				ItemService.addItem(player, 169120037, 1); //染料：猩红。 / Paint: Scarlet Red.
			break;
			case 13:
				ItemService.addItem(player, 169120038, 1); //染料：薄荷绿。 / Paint: Mint.
			break;
			case 14:
				ItemService.addItem(player, 169120039, 1); //染料：猩红。 / Paint: Scarlet Red.
			break;
			case 15:
				ItemService.addItem(player, 169120040, 1); //染料：绿色。 / Paint: Green.
			break;
			case 16:
				ItemService.addItem(player, 169120041, 1); //染料：天蓝。 / Paint: Sky Blue.
			break;
			case 17:
				ItemService.addItem(player, 169120042, 1); //染料：钢青。 / Paint: Steel Blue.
			break;
			case 18:
				ItemService.addItem(player, 169120043, 1); //染料：皇家蓝。 / Paint: Imperial Blue.
			break;
			case 19:
				ItemService.addItem(player, 169120044, 1); //染料：米色。 / Paint: Beige.
			break;
			case 20:
				ItemService.addItem(player, 169120045, 1); //染料：深棕。 / Paint: Espresso.
			break;
			case 21:
				ItemService.addItem(player, 169120046, 1); //染料：柠檬黄。 / Paint: Lemon.
			break;
		}
        AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
    }
}
