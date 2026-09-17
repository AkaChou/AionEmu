# 决策记录：`quest` 分支**不**移植 `gameserver.magicboost.cap`

- 记录时间：2026-09-17
- 结论：**保持 `quest` 分支现状 `gameserver.magicboost.cap = 6500`**；曾经同步过 history 的 3400，按用户指示已回退。
- 影响文件（现均与 `HEAD` 一致，无改动）：
  `src/main/resources/aion/config/main/skill.properties`、`src/main/java/com/aionemu/gameserver/configs/main/SkillConfig.java`、
  `src/test/java/com/aionemu/gameserver/configs/main/SkillConfigTest.java`

## 背景（留给后续排查，避免再被"顺手改回"）

- 两个分支该配置不一致：`origin/history` = `3400`（注释"真端普通模式为 3400"，来自提交 `01a44dc93`，其 squash 体含
  `fix(combat): align damage formulas with retail`）；`quest` = `6500`。
- 生效链路：`StatFunctions.capMagicBoostForDamage()` → `scaleMagicBoostDifference()` /
  `calculateMagicalSkillDamageFactor()`（`knowledge / 100 + min(魔增, cap) / 1000`），
  **魔增越高差异越大**（例：魔增 5000 时，6500 上限按 5000 计，3400 上限按 3400 计）。
- 两边 `StatFunctions` 的伤害公式本身一致（仅 instanceof 写法与注释差异）⇒ 该配置是历史上唯一与
  "伤害对齐真端"相关的差异项。
- 另有一处同类但**未移植**的差异：`gameserver.skill.root.break.on.dot` + `SkillConfig.ROOT_BREAK_ON_DOT` +
  `RootEffect` 的 DoT 解除束缚（`origin/history` 的 `af567123d`，同时移除了 `Rnd.get(0, 100) > resistchance`
  随机判定，属行为变更；本分支 `ObserverType` 无 `ATTACKED_OR_DOT`，移植需一并改造）。

## 若将来要重新评估

- 需要真机/客户端证据（同一技能、同一角色在 3400 与 6500 下的实测伤害），而不是仅凭分支差异就直接改数值；
- 运行期配置在部署目录 `aion/config/main/skill.properties`（`.gitignore` 的 `/aion`），改源码头不影响已部署实例。
