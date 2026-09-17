# 充能阶段：服务器用「半程折中」缩放蓄力窗口，导致第三阶段按前两阶段结算

- 记录时间：2026-09-17（用户追加反馈）
- 分支：`quest`（未提交）
- 状态：源码头 + 定向测试通过（`mvn -q -DskipTests compile` 成功；
  `mvn -Dtest=SkillCancellationTest,SkillConfigTest,StatFunctionsTest test` → 27/27，含本文件的边界断言）；
  **真机/客户端验证待做**
- 相关决策：魔增上限 `gameserver.magicboost.cap` **本轮不改**（保持 6500，见同目录 `2026-09-17-magicboost-cap-decision.zh-CN.md`）

## 现象（用户原话）

> 充能应该还是每个阶段的伤害不对，第三阶段有可能是前 2 个阶段的伤害

## 第一步排除：数据与阶段映射没有问题

- 同一充能组的三阶段是**三个独立技能 id**，伤害值不同（例：`EL_CursedBreath1` → 3751=261 / 3752=527 / 3753=1134）；
  `charge_skills.xml` 的 `first/second/third` 与 `<charge skill_id>` 顺序一致。
- 169 个充能组的阶段技能 id **全部**能在 `skill_templates_part_*.xml` 解析（离线核对，0 缺失）⇒ 不存在
  `getSkillTemplate(...) == null` 导致的"技能不结算"。
- 各阶段 `<charge time>` 与阶段模板 `duration` 并不同源（155 组里只有 8 组第三阶段相等）⇒ `time` 是蓄力条时长数据，
  不是动画时长。

## 根因：阶段窗口的速度缩放系数与客户端不同源

`Skill.endCast()` 的阶段选择（`Skill.java:1360-1374`）：

```java
int time = (int) (System.currentTimeMillis() - castStart);
time += 100;                       // 客户端松手包延迟的补偿
if (time < scaleChargeTime(minCharge)) return;
for (ChargeTemplate charge : chargeTemplate.getCharges()) {
    time -= scaleChargeTime(charge.getTime());
    skillId = charge.getSkillId();
    if (time < 0) break;
}
```

阶段窗口 = `charge.time × chargeTimeMultiplier`。该系数此前是
`1 - (1 - ratio) * 0.5`（**半程折中**，见 `cc013f49e` 的 `fix(skill): scale charge stages with speed`）：

- 物理充能：`ratio = 攻速（攻击间隔）current / base`，加速后 `ratio < 1`；
- 魔法充能：`ratio = duration / 模板 duration`（`SM_CASTSPELL` 下发的施法时长比例，含 30% 施法速度下限）。

**关键推理（由现象反推客户端行为）**：该半程折中的系数恒有 `multiplier ≥ ratio`，且加速时 `multiplier ≤ 1`。

- 若客户端蓄力条**不随速度缩放**，服务器的阶段只会比客户端**更早**（`multiplier ≤ 1`），玩家只会看到
  "低阶段打出高阶段伤害"，**不可能**出现"客户端已到第三阶段却按前两阶段结算"；
- 用户实际观察到的是服务器**落后**于客户端 ⇒ 客户端蓄力条确实随速度缩放，且缩放比例**强于**半程折中
  （自然解释：客户端就是按收到的（已经过速度修正的）施法时长/攻速比例缩放蓄力条，即 `f_client = ratio`）。

于是每次加速后服务器阶段窗口都比客户端长（例如 `ratio = 0.5` 时：客户端窗口 0.5t，服务器 0.75t），
客户端刚进入第三阶段时服务器还在第二甚至第一阶段 ⇒ "第三阶段有可能是前 2 个阶段的伤害"。

## 修复

`src/main/java/com/aionemu/gameserver/skillengine/model/Skill.java`

1. `calculateChargeTimeMultiplier()` 直接返回与客户端同源的比例：
   `Math.max(speedRatio, MIN_CHARGE_SPEED_RATIO)`（新增常量 `MIN_CHARGE_SPEED_RATIO = 0.3f` 兜底，
   防止速度修正为 0 时窗口塌缩；魔法分支本身已含施法速度 30% 下限）。
2. 阶段窗口、`min_charge` 与总时长（`this.duration = Σ scaleChargeTime(...)`，即自动满蓄力时间）
   三者同源缩放，客户端蓄力条与服务器结算点一致。
3. `bonus_type="NONE"` 仍返回 1（不缩放）；`isCastTimeFixed()` 的技能 `ratio == 1` 行为不变。

`src/test/java/com/aionemu/gameserver/skillengine/model/SkillCancellationTest.java`

- `chargedStageUsesTheSameSpeedMultiplierAsTheClient`：期望系数 `0.845 → 0.69`（690/1000）。
- 新增边界断言（窗口 1035 / 1035 / 4830ms）：800ms → 第一阶段；1500ms → 第二阶段；
  **2100ms（客户端刚进入第三阶段 2×1035ms）→ 必须第三阶段**（旧系数会判成第二阶段）；6000ms → 第三阶段。

## 已执行

```bash
mvn -q -DskipTests compile                                   # 成功
mvn -Dtest=SkillCancellationTest,SkillConfigTest,StatFunctionsTest test
# SkillCancellationTest 6/6、StatFunctionsTest 18/18、SkillConfigTest 3/3，共 27/27 通过
```

未执行的边界：全量测试套件、`clean package`、部署、重启、真机/客户端验收。

## 实机验收建议

1. 取一个充能技能，**在客户端蓄力条刚跳到第三阶段的瞬间立刻松手**：伤害应为第三阶段数值。
2. 分别在带速度 buff（攻速/施法速度）与不带时各测一次：客户端与服务器阶段应始终一致。
3. 若不松手：技能应在客户端蓄力条走满时自动结算第三阶段。
