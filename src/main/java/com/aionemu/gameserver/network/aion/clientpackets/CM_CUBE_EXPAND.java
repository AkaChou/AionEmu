package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.CubeExpandService;

/**
 * 请求扩展背包（背包）格子的客户端包。
 * Client packet requesting cube (inventory) expansion.
 *
 * @author Ranastic (Encom)
 */
public class CM_CUBE_EXPAND extends AionClientPacket {
	int type;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_CUBE_EXPAND(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		type = readC();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		if (type == 0) { // 基纳 / Kinah
			if (activePlayer.getNpcExpands() < 15) {
				if (activePlayer.getNpcExpands() == 0) {
					if (activePlayer.getInventory().tryDecreaseKinah(1000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 1) {
					if (activePlayer.getInventory().tryDecreaseKinah(10000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 2) {
					if (activePlayer.getInventory().tryDecreaseKinah(50000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 3) {
					if (activePlayer.getInventory().tryDecreaseKinah(150000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 4) {
					if (activePlayer.getInventory().tryDecreaseKinah(300000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 5) {
					if (activePlayer.getInventory().tryDecreaseKinah(3000000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 6) {
					if (activePlayer.getInventory().tryDecreaseKinah(6000000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 7) {
					if (activePlayer.getInventory().tryDecreaseKinah(12000000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 8) {
					if (activePlayer.getInventory().tryDecreaseKinah(24000000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				} else if (activePlayer.getNpcExpands() == 9) {
					if (activePlayer.getInventory().tryDecreaseKinah(48000000)) {
						CubeExpandService.expand(activePlayer, true);
					}
				}
			}
		}
		// 背包扩展币。 / Cube Expansion Coin.
		else if (type == 1) {
			if (activePlayer.getNpcExpands() < 10) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			} else if (activePlayer.getNpcExpands() < 11) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			} else if (activePlayer.getNpcExpands() < 12) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			} else if (activePlayer.getNpcExpands() < 13) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			} else if (activePlayer.getNpcExpands() < 14) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			} else if (activePlayer.getNpcExpands() < 15) {
				if (activePlayer.getInventory().decreaseByItemId(186000419, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000440, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000444, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				} else if (activePlayer.getInventory().decreaseByItemId(186000445, 5)) { // 背包扩展币。 / Cube Expansion Coin.
					CubeExpandService.expand(activePlayer, true);
				}
			}
		}
	}
}