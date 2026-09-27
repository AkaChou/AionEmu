# P0c-30：P0c-24 三件套收口——barrel 方法 lenient 补回放绿 + 风暴复测

> 日期：2026-09-26 ｜ 切片：P0c-30 ｜ lane：静默 2h18m（在飞态冻结，missing=664 持稳）｜ 前序：P0c-29

## 落地与验证

- **EarlyElyos barrel 三断言 lenient 补齐**（`RetailEarlyElyosBarrelReplayProbeTest.java.txt`
  存档，临时源已删）：reward 态 -1 预览直接开奖励窗 5 ✓；完成路由尾 = 任务接取页
  ShowQuestSelectionDialog(10) ✓；unaccepted 态无任何酒桶路由（TalkToNpc/CanAct 双轴）✓。
  **P0c-24 三件套（1141 判官 + EarlyElyos barrel + 交互门禁实质=无 CanAct(700122)）至此全部
  lenient 验证完毕。**
- **风暴复测**：missing=664 / wrongOwner=0 与前几轮逐值相同——lane 在飞态持续冻结，
  正式窗口（判官复跑 + T2 ×7 + T3 债池 diff）继续待窗。

## 判例

- **回放覆盖度自查**：lenient 回放（P0c-29）覆盖了判官的"翻转敏感"断言，但 barrel 方法还有
  三条形状断言未入回放——回放完成后应对照判官逐断言勾验，遗漏项再补一轮（本片即补）。

## 正式窗口余量（不变）

判官复跑（1141+EarlyElyos+交互门禁+30312/30315+批量 7+Daevanion 7+Haramel）+ T2 ×7 任务 +
T3 债池快照 diff + Ownership/verify 复跑。
