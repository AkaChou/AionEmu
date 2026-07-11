package com.aionemu.gameserver.ai2.poll;

/**
 * 预置的 AI 投票回答常量，提供肯定与否定两种共享实例。
 * Predefined AI poll answer constants providing shared positive and negative instances.
 *
 * @author ATracer
 */
public class AIAnswers {

	/** 肯定回答 / Positive answer */
	public static final AIAnswer POSITIVE = new SimpleAIAnswer(true);

	/** 否定回答 / Negative answer */
	public static final AIAnswer NEGATIVE = new SimpleAIAnswer(false);

}
