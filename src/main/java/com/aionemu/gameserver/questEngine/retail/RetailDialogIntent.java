package com.aionemu.gameserver.questEngine.retail;

/**
 * 真端任务对话中的非状态迁移意图。
 * Non-transition dialog intents in retail quest conversations.
 */
public enum RetailDialogIntent {
	/** 客户端本地页面导航；服务端只确认收到，不修改任务状态。 / Client-local page navigation; the server acknowledges without mutating quest state. */
	LOCAL_PAGE_NAVIGATION,
	/** 客户端本地关闭对话；服务端不把它当成续接动作。 / Client-local dialog close; never treated as a continuation action. */
	LOCAL_CLOSE
}
