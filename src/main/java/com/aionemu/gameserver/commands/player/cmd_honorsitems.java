package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：荣誉/声望装备信息展示与兑换发放。
 * Player command: shows honor/reputation gear info and exchanges rewards.
 *
 * @author Maestross
 */
public class cmd_honorsitems extends PlayerCommand {

	public cmd_honorsitems() {
		super("honoritems");
	}

	/**
	 * 按子命令展示分类信息或发放对应荣誉装备。
	 * Shows category info or grants honor gear by sub-command.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 命令参数 / command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length < 1) {
			PacketSendUtility.sendMessage(player, "Syntax: .honoritems <plate | leather | cloth | chain | weapons>");
			PacketSendUtility.sendMessage(player, "Syntax: .honoritems <pprices | lprices | cprices | ccprices | wprices>");
			return;
		}

		if (params[0].equalsIgnoreCase("plate")) {
			plate(player);
		}
		if (params[0].equalsIgnoreCase("leather")) {
			leather(player);
		}
		if (params[0].equalsIgnoreCase("cloth")) {
			cloth(player);
		}
		if (params[0].equalsIgnoreCase("chain")) {
			chain(player);
		}
		if (params[0].equalsIgnoreCase("weapons")) {
			weapons(player);
		}
		if (params[0].equalsIgnoreCase("pprices")) {
			plateInfo(player);
		}
		if (params[0].equalsIgnoreCase("lprices")) {
			leatherInfo(player);
		}
		if (params[0].equalsIgnoreCase("cprices")) {
			clothInfo(player);
		}
		if (params[0].equalsIgnoreCase("ccprices")) {
			chainInfo(player);
		}
		if (params[0].equalsIgnoreCase("wprices")) {
			weaponsInfo(player);
		}
		if (params[0].equalsIgnoreCase("1")) {// 板甲上衣 / Plate Breast
			case1(player);
		}
		if (params[0].equalsIgnoreCase("2")) {// 板甲手套 / Plate Hands
			case2(player);
		}
		if (params[0].equalsIgnoreCase("3")) {// 板甲鞋子 / Plate Shoes
			case3(player);
		}
		if (params[0].equalsIgnoreCase("4")) {// 板甲下装 / Plate Pants
			case4(player);
		}
		if (params[0].equalsIgnoreCase("5")) {// 板甲护肩 / Plate Shoulders
			case5(player);
		}
		if (params[0].equalsIgnoreCase("6")) {// 皮甲上衣 / Leather Breast
			case6(player);
		}
		if (params[0].equalsIgnoreCase("7")) {// 皮甲手套 / Leather Hands
			case7(player);
		}
		if (params[0].equalsIgnoreCase("8")) {// 皮甲鞋子 / Leather Shoes
			case8(player);
		}
		if (params[0].equalsIgnoreCase("9")) {// 皮甲下装 / Leather Pants
			case9(player);
		}
		if (params[0].equalsIgnoreCase("10")) {// 皮甲护肩 / Leather Shoulders
			case10(player);
		}
		if (params[0].equalsIgnoreCase("11")) {// 布甲上衣 / Cloth Breast
			case11(player);
		}
		if (params[0].equalsIgnoreCase("12")) {// 布甲手套 / Cloth Hands
			case12(player);
		}
		if (params[0].equalsIgnoreCase("13")) {// 布甲鞋子 / Cloth Shoes
			case13(player);
		}
		if (params[0].equalsIgnoreCase("14")) {// 布甲下装 / Cloth Pants
			case14(player);
		}
		if (params[0].equalsIgnoreCase("15")) {// 布甲护肩 / Cloth Shoulders
			case15(player);
		}
		if (params[0].equalsIgnoreCase("16")) {// 锁甲上衣 / Chain Breast
			case16(player);
		}
		if (params[0].equalsIgnoreCase("17")) {// 锁甲手套 / Chain Hands
			case17(player);
		}
		if (params[0].equalsIgnoreCase("18")) {// 锁甲鞋子 / Chain Shoes
			case18(player);
		}
		if (params[0].equalsIgnoreCase("19")) {// 锁甲下装 / Chain Pants
			case19(player);
		}
		if (params[0].equalsIgnoreCase("20")) {// 锁甲护肩 / Chain Shoulders
			case20(player);
		}
		if (params[0].equalsIgnoreCase("21")) {// 武器：剑 / Weapon Sword
			case21(player);
		}
		if (params[0].equalsIgnoreCase("22")) {// 武器：双手剑 / Weapon Greatsword
			case22(player);
		}
		if (params[0].equalsIgnoreCase("23")) {// 武器：长弓 / Weapon Longbow
			case23(player);
		}
		if (params[0].equalsIgnoreCase("24")) {// 武器：匕首 / Weapon Dagger
			case24(player);
		}
		if (params[0].equalsIgnoreCase("25")) {// 武器：宝珠 / Weapon Orb
			case25(player);
		}
		if (params[0].equalsIgnoreCase("26")) {// 武器：魔法书 / Weapon Tome
			case26(player);
		}
		if (params[0].equalsIgnoreCase("27")) {// 武器：法杖 / Weapon Staff
			case27(player);
		}
		if (params[0].equalsIgnoreCase("28")) {// 武器：锤 / Weapon Mace
			case28(player);
		}
		if (params[0].equalsIgnoreCase("29")) {// 武器：盾牌 / Weapon Shield
			case29(player);
		}
		if (params[0].equalsIgnoreCase("30")) {// 武器：枪 / Weapon Spear
			case30(player);
		}
	}

	private void plateInfo(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Armor Plate Prices");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110601342] AP: 4329504 Medals: 105");// 板甲上衣 / Plate Breast
		PacketSendUtility.sendMessage(player, "[item: 111601305] AP: 2164752 Medals: 52");// 板甲手套 / Plate Hands
		PacketSendUtility.sendMessage(player, "[item: 114601291] AP: 2164752 Medals: 52");// 板甲鞋子 / Plate Shoes
		PacketSendUtility.sendMessage(player, "[item: 113601294] AP: 3247344 Medals: 78");// 板甲下装 / Plate Pants
		PacketSendUtility.sendMessage(player, "[item: 112601285] AP: 2164752 Medals: 52");// 板甲护肩 / Plate Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
	}

	private void leatherInfo(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Leather Armor Prices");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110301393] AP: 4329504 Medals: 105");// 皮甲上衣 / Leather Breast
		PacketSendUtility.sendMessage(player, "[item: 111301334] AP: 2164752 Medals: 52");// 皮甲手套 / Leather Hands
		PacketSendUtility.sendMessage(player, "[item: 114301393] AP: 2164752 Medals: 52");// 皮甲鞋子 / Leather Shoes
		PacketSendUtility.sendMessage(player, "[item: 113301358] AP: 3247344 Medals: 78");// 皮甲下装 / Leather Pants
		PacketSendUtility.sendMessage(player, "[item: 112301277] AP: 2164752 Medals: 52");// 皮甲护肩 / Leather Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
	}

	private void clothInfo(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Cloth Armor prices");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110101485] AP: 4329504 Medals: 105");// 布甲上衣 / Cloth Breast
		PacketSendUtility.sendMessage(player, "[item: 111101339] AP: 2164752 Medals: 52");// 布甲手套 / Cloth Hands
		PacketSendUtility.sendMessage(player, "[item: 114101387] AP: 2164752 Medals: 52");// 布甲鞋子 / Cloth Shoes
		PacketSendUtility.sendMessage(player, "[item: 113101356] AP: 3247344 Medals: 78");// 布甲下装 / Cloth Pants
		PacketSendUtility.sendMessage(player, "[item: 112101296] AP: 2164752 Medals: 52");// 布甲护肩 / Cloth Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
	}

	private void chainInfo(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Chain Armor Prices");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110501368] AP: 4329504 Medals: 105");// 锁甲上衣 / Chain Breast
		PacketSendUtility.sendMessage(player, "[item: 111501326] AP: 2164752 Medals: 52");// 锁甲手套 / Chain Hands
		PacketSendUtility.sendMessage(player, "[item: 114501349] AP: 2164752 Medals: 52");// 锁甲鞋子 / Chain Shoes
		PacketSendUtility.sendMessage(player, "[item: 113501341] AP: 3247344 Medals: 78");// 锁甲下装 / Chain Pants
		PacketSendUtility.sendMessage(player, "[item: 112501266] AP: 2164752 Medals: 52");// 锁甲护肩 / Chain Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
	}

	private void weaponsInfo(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Weapon Pprices");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 100001412] AP: 6494256 Medals: 156");// 武器：剑 / Weapon Sword
		PacketSendUtility.sendMessage(player, "[item: 100901105] AP: 6494256 Medals: 156");// 武器：双手剑 / Weapon Greatsword
		PacketSendUtility.sendMessage(player, "[item: 101701134] AP: 6494256 Medals: 156");// 武器：长弓 / Weapon Longbow
		PacketSendUtility.sendMessage(player, "[item: 100201251] AP: 6494256 Medals: 156");// 武器：匕首 / Weapon Dagger
		PacketSendUtility.sendMessage(player, "[item: 100501097] AP: 6494256 Medals: 156");// 武器：宝珠 / Weapon Orb
		PacketSendUtility.sendMessage(player, "[item: 100601153] AP: 6494256 Medals: 156");// 武器：魔法书 / Weapon Tome
		PacketSendUtility.sendMessage(player, "[item: 101501123] AP: 6494256 Medals: 156");// 武器：法杖 / Weapon Staff
		PacketSendUtility.sendMessage(player, "[item: 100101089] AP: 6494256 Medals: 156");// 武器：锤 / Weapon Mace
		PacketSendUtility.sendMessage(player, "[item: 115001462] AP: 4329504 Medals: 105");// 武器：盾牌 / Weapon Shield
		PacketSendUtility.sendMessage(player, "[item: 101301042] AP: 6494256 Medals: 156");// 武器：枪 / Weapon Spear
		PacketSendUtility.sendMessage(player, "----------------");
	}

	private void plate(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Plates");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110601342] (1)");// 板甲上衣 / Plate Breast
		PacketSendUtility.sendMessage(player, "[item: 111601305] (2)");// 板甲手套 / Plate Hands
		PacketSendUtility.sendMessage(player, "[item: 114601291] (3)");// 板甲鞋子 / Plate Shoes
		PacketSendUtility.sendMessage(player, "[item: 113601294] (4)");// 板甲下装 / Plate Pants
		PacketSendUtility.sendMessage(player, "[item: 112601285] (5)");// 板甲护肩 / Plate Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendYellowMessageOnCenter(player, "Use now .honoritems and the corresponding ID number (Examplel: .honoritems 1");
	}

	private void leather(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Leather");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110301393] (6)");// 皮甲上衣 / Leather Breast
		PacketSendUtility.sendMessage(player, "[item: 111301334] (7)");// 皮甲手套 / Leather Hands
		PacketSendUtility.sendMessage(player, "[item: 114301393] (8)");// 皮甲鞋子 / Leather Shoes
		PacketSendUtility.sendMessage(player, "[item: 113301358] (9)");// 皮甲下装 / Leather Pants
		PacketSendUtility.sendMessage(player, "[item: 112301277] (10)");// 皮甲护肩 / Leather Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendYellowMessageOnCenter(player, "Use now .honoritems and the corresponding ID number (Examplel: .honoritems 6");
	}

	private void cloth(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Cloth");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110101485] (11)");// 布甲上衣 / Cloth Breast
		PacketSendUtility.sendMessage(player, "[item: 111101339] (12)");// 布甲手套 / Cloth Hands
		PacketSendUtility.sendMessage(player, "[item: 114101387] (13)");// 布甲鞋子 / Cloth Shoes
		PacketSendUtility.sendMessage(player, "[item: 113101356] (14)");// 布甲下装 / Cloth Pants
		PacketSendUtility.sendMessage(player, "[item: 112101296] (15)");// 布甲护肩 / Cloth Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendYellowMessageOnCenter(player, "Use now .honoritems and the corresponding ID number (Examplel: .honoritems 11");
	}

	private void chain(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Chain");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 110501368] (16)");// 锁甲上衣 / Chain Breast
		PacketSendUtility.sendMessage(player, "[item: 111501326] (17)");// 锁甲手套 / Chain Hands
		PacketSendUtility.sendMessage(player, "[item: 114501349] (18)");// 锁甲鞋子 / Chain Shoes
		PacketSendUtility.sendMessage(player, "[item: 113501341] (19)");// 锁甲下装 / Chain Pants
		PacketSendUtility.sendMessage(player, "[item: 112501266] (20)");// 锁甲护肩 / Chain Shoulders
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendYellowMessageOnCenter(player, "Use now .honoritems and the corresponding ID number (Examplel: .honoritems 16");
	}

	private void weapons(Player player) {
		PacketSendUtility.sendYellowMessageOnCenter(player, "Weapons");
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendMessage(player, "[item: 100001412] (21)");// 武器：剑 / Weapon Sword
		PacketSendUtility.sendMessage(player, "[item: 100901105] (22)");// 武器：双手剑 / Weapon Greatsword
		PacketSendUtility.sendMessage(player, "[item: 101701134] (23)");// 武器：长弓 / Weapon Longbow
		PacketSendUtility.sendMessage(player, "[item: 100201251] (24)");// 武器：匕首 / Weapon Dagger
		PacketSendUtility.sendMessage(player, "[item: 100501097] (25)");// 武器：宝珠 / Weapon Orb
		PacketSendUtility.sendMessage(player, "[item: 100601153] (26)");// 武器：魔法书 / Weapon Tome
		PacketSendUtility.sendMessage(player, "[item: 101501123] (27)");// 武器：法杖 / Weapon Staff
		PacketSendUtility.sendMessage(player, "[item: 100101089] (28)");// 武器：锤 / Weapon Mace
		PacketSendUtility.sendMessage(player, "[item: 115001462] (29)");// 武器：盾牌 / Weapon Shield
		PacketSendUtility.sendMessage(player, "[item: 101301042] (30)");// 武器：枪 / Weapon Spear
		PacketSendUtility.sendMessage(player, "----------------");
		PacketSendUtility.sendYellowMessageOnCenter(player, "Use now .honoritems and the corresponding ID number (Examplel: .honoritems 21");
	}

	private void case1(Player player) {// 板甲上衣 / Plate Breast
		Storage bag = player.getInventory();
		int count = 105;
		int ap = 4329504;
		int id = 110601342;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case2(Player player) {// 板甲手套 / Plate Hands
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 111601305;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case3(Player player) {// 板甲鞋子 / Plate Shoes
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 114601291;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case4(Player player) {// 板甲下装 / Plate Pants
		Storage bag = player.getInventory();
		int count = 78;
		int ap = 3247344;
		int id = 113601294;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case5(Player player) {// 板甲护肩 / Plate Shoulders
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 112601285;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case6(Player player) {// 皮甲上衣 / Leather Breast
		Storage bag = player.getInventory();
		int count = 105;
		int ap = 4329504;
		int id = 110301393;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case7(Player player) {// 皮甲手套 / Leather Hands
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 111301334;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case8(Player player) {// 皮甲鞋子 / Leather Shoes
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 114301393;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case9(Player player) {// 皮甲下装 / Leather Pants
		Storage bag = player.getInventory();
		int count = 78;
		int ap = 3247344;
		int id = 113301358;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case10(Player player) {// 皮甲护肩 / Leather Shoulders
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 112301277;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case11(Player player) {// 布甲上衣 / Cloth Breast
		Storage bag = player.getInventory();
		int count = 105;
		int ap = 4329504;
		int id = 110101485;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case12(Player player) {// 布甲手套 / Cloth Hands
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 111101339;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case13(Player player) {// 布甲鞋子 / Cloth Shoes
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 114101387;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case14(Player player) {// 布甲下装 / Cloth Pants
		Storage bag = player.getInventory();
		int count = 78;
		int ap = 3247344;
		int id = 113101356;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case15(Player player) {// 布甲护肩 / Cloth Shoulders
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 112101296;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case16(Player player) {// 锁甲上衣 / Chain Breast
		Storage bag = player.getInventory();
		int count = 105;
		int ap = 4329504;
		int id = 110501368;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case17(Player player) {// 锁甲手套 / Chain Hands
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 111501326;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case18(Player player) {// 锁甲鞋子 / Chain Shoes
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 114501349;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case19(Player player) {// 锁甲下装 / Chain Pants
		Storage bag = player.getInventory();
		int count = 78;
		int ap = 3247344;
		int id = 113501341;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case20(Player player) {// 锁甲护肩 / Chain Shoulders
		Storage bag = player.getInventory();
		int count = 52;
		int ap = 2164752;
		int id = 112501266;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case21(Player player) {// 武器：剑 / Weapon Sword
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100001412;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case22(Player player) {// 武器：双手剑 / Weapon Greatsword
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100901105;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case23(Player player) {// 武器：长弓 / Weapon Longbow
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 101701134;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case24(Player player) {// 武器：匕首 / Weapon Dagger
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100201251;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case25(Player player) {// 武器：宝珠 / Weapon Orb
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100501097;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case26(Player player) {// 武器：魔法书 / Weapon Tome
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100601153;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case27(Player player) {// 武器：法杖 / Weapon Staff
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 101501123;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case28(Player player) {// 武器：锤 / Weapon Mace
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 100101089;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case29(Player player) {// 武器：盾牌 / Weapon Shield
		Storage bag = player.getInventory();
		int count = 105;
		int ap = 4329504;
		int id = 115001462;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	private void case30(Player player) {// 武器：枪 / Weapon Spear
		Storage bag = player.getInventory();
		int count = 156;
		int ap = 6494256;
		int id = 101301042;
		long itemsInBag = bag.getItemCountByItemId(186000223);
		if (player.getAbyssRank().getAp() < ap) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough AP, you only have: " + ap);
			return;
		}
		if (itemsInBag < count) {
			PacketSendUtility.sendYellowMessageOnCenter(player, "You do not have enough medals, you have only: " + count);
			return;
		}
		AbyssPointsService.addAp(player, -ap);
		Item item = bag.getFirstItemByItemId(186000223);
		bag.decreaseByObjectId(item.getObjectId(), count);
		ItemService.addItem(player, id, 1);
		PacketSendUtility.sendMessage(player, "You have successfully received your item!");
	}

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param message 失败提示消息 / failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: .honoritems <plate | leather | cloth | chain | weapons>");
	}
}