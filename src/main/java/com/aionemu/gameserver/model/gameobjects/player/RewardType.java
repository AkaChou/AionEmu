package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 奖励类型枚举。
 * Reward Type enumeration.
 */

public enum RewardType {
	/** 欧比斯点数玩家。 / Ap Player. */
	AP_PLAYER {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.AP_BOOST, 100).getCurrent() / 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_AP_PLAYER) {
				if (player.getLevel() >= 25 && player.getLevel() <= 40) {
					return (long) (reward * 2 * player.getRates().getApPlayerGainRate() * statRate);
				} else if (player.getLevel() >= 41 && player.getLevel() <= 55) {
					return (long) (reward * 3 * player.getRates().getApPlayerGainRate() * statRate);
				} else if (player.getLevel() >= 56 && player.getLevel() <= 65) {
					return (long) (reward * 4 * player.getRates().getApPlayerGainRate() * statRate);
				} else if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 5 * player.getRates().getApPlayerGainRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 6 * player.getRates().getApPlayerGainRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getApPlayerGainRate() * statRate);
		}
	},
	AP_NPC {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.AP_BOOST, 100).getCurrent() / 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_AP_NPC) {
				if (player.getLevel() >= 25 && player.getLevel() <= 40) {
					return (long) (reward * 2 * player.getRates().getApNpcRate() * statRate);
				} else if (player.getLevel() >= 41 && player.getLevel() <= 55) {
					return (long) (reward * 3 * player.getRates().getApNpcRate() * statRate);
				} else if (player.getLevel() >= 56 && player.getLevel() <= 65) {
					return (long) (reward * 4 * player.getRates().getApNpcRate() * statRate);
				} else if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 5 * player.getRates().getApNpcRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 6 * player.getRates().getApNpcRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getApNpcRate() * statRate);
		}
	},
	GP_PLAYER {

		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			return (long) (reward * player.getRates().getGpPlayerGainRate());
		}
	},
	HUNTING {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_HUNTING_XP_RATE, 100).getCurrent() / 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_HUNTING) {
				if (player.getLevel() >= 1 && player.getLevel() <= 65) {
					return (long) (reward * 5 * player.getRates().getXpRate() * statRate);
				} else if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 6 * player.getRates().getXpRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 7 * player.getRates().getXpRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getXpRate() * statRate);
		}
	},
	GROUP_HUNTING {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_GROUP_HUNTING_XP_RATE, 100).getCurrent()
					/ 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_GROUP_HUNTING) {
				if (player.getLevel() >= 1 && player.getLevel() <= 65) {
					return (long) (reward * 5 * player.getRates().getGroupXpRate() * statRate);
				} else if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 6 * player.getRates().getGroupXpRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 7 * player.getRates().getGroupXpRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getGroupXpRate() * statRate);
		}
	},
	MONSTER_BOOK {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_BOOK_XP_RATE, 100).getCurrent() / 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_BOOK) {
				if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 6 * player.getRates().getBookXpRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 7 * player.getRates().getQuestXpRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getBookXpRate() * statRate);
		}
	},
	PVP_KILL {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			return (reward);
		}
	},
	/** Already-resolved XP amount; keeps generic XP effects without applying a source rate. */
	EXACT {
		@Override
		public long calcReward(Player player, long reward) {
			return reward;
		}
	},
	QUEST {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_QUEST_XP_RATE, 100).getCurrent() / 100f;
			if (CustomConfig.ENABLE_EXP_PROGRESSIVE_QUEST) {
				if (player.getLevel() >= 1 && player.getLevel() <= 65) {
					return (long) (reward * 5 * player.getRates().getQuestXpRate() * statRate);
				} else if (player.getLevel() >= 66 && player.getLevel() <= 75) {
					return (long) (reward * 6 * player.getRates().getQuestXpRate() * statRate);
				} else if (player.getLevel() >= 76 && player.getLevel() <= 83) {
					return (long) (reward * 7 * player.getRates().getQuestXpRate() * statRate);
				}
			}
			return (long) (reward * player.getRates().getQuestXpRate() * statRate);
		}
	},
	CRAFTING {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_CRAFTING_XP_RATE, 100).getCurrent() / 100f;
			return (long) (reward * player.getRates().getCraftingXPRate() * statRate);
		}
	},
	GATHERING {
		/** 计算奖励。 / Calc reward. */
		@Override
		public long calcReward(Player player, long reward) {
			float statRate = player.getGameStats().getStat(StatEnum.BOOST_GATHERING_XP_RATE, 100).getCurrent() / 100f;
			return (long) (reward * player.getRates().getGatheringXPRate() * statRate);
		}
	};

	/** 计算奖励。 / Calc reward. */
	public abstract long calcReward(Player player, long reward);
}
