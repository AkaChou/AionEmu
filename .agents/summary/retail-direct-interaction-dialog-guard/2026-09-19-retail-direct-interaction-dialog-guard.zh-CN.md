# 固定乘坐交互 load fail 修复记录

## 现象

- NPC `702685`（攻城炮 / Rentus Elyos Field Gun）点击乘坐时，客户端弹出：
  - `load fail!`
  - `IDYun_Siegeweapon_Li_01.html`
  - `(HtmlPageId 10)`
  - `(QuestId 0)`

## 根因

- `702685` 在 `npc-ai.xml` 中绑定 `IDYun_SiezeWeapon_Li_03`。
- 该模式的 `on_talked_by_user` 是直接动作：
  - `use_skill SKILLI_INDEX_0`
  - `teleport_target_alias LocationsIDYun_siezeweapon3`
  - `despawn_self`
- `RetailPatternAI2.handleTalkedByUser` 原先无条件调用 `super.handleDialogStart(player)`，进入 `TalkEventHandler` 默认分支并下发 `SM_DIALOG_WINDOW(objectId, 10)`。
- 客户端因此尝试加载不存在的默认 HTML 页第 10 页，出现 `load fail`；真端乘坐动作反而被错误前置的对话包干扰。

## 修复范围

静态扫描 compact AI pattern：

- `on_talked_by_user` 含 `use_skill`、`teleport_target`、`teleport_target_alias` 且无 `on_hyperlink_clicked` 的直接交互模式约 393 个，覆盖约 997 条 NPC 映射。
- 其中很多 NPC 模板带 `is_dialog="true"`，不能仅依赖模板 `is_dialog` 判断是否应打开 HTML，否则大量炮台、坦克、固定炮仍会错发第 10 页。
- 另有约 20 条 gauge 驱动交互映射，以及约 25 条空规则 `useitem` 映射（含 `702648` / `702649`）属于同类真实交互协议。

修复：

1. `RetailPatternAI2.handleTalkedByUser`
   - 直接交互模式（`use_skill` / `teleport_target` / `teleport_target_alias`）和 gauge 驱动模式不再下发默认 HTML 页。
   - 仅无直接动作、无 gauge 的模式继续走 `TalkEventHandler`。
2. `AI2Engine.selectNpcAi`
   - retail pattern 没有任何可执行规则时，不再覆盖 `useitem` fallback；恢复原生乘坐/宝箱等 `useitem` 交互协议。
3. 回归测试
   - `RetailPatternAI2Test#treatsSiegeWeaponTalkAsDirectInteraction`
   - `RetailPatternAI2Test#detectsGaugeDrivenTalkPatterns`
   - `AI2EngineRetailSelectionTest#keepsUseitemFallbackWhenRetailPatternHasNoRules`

## 验证

已执行：

```bash
mvn -Dtest=RetailPatternAI2Test,AI2EngineRetailSelectionTest test
```

结果：

- Tests run: `89`
- Failures: `0`
- Errors: `0`
- Skipped: `0`
- BUILD SUCCESS

未执行：

- 服务端部署与真实客户端复验；需要后续对 `702685` 及代表性炮台/坦克做客户端乘坐验收。
