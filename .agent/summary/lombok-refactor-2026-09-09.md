# Model 层 Lombok 化改造总结（2026-09-09）

## 第三轮：构造器 Lombok 化（全仓）

- 范围：`src/main/java/com/aionemu` 全部 + `src/test`（5621 个文件）。
- 转换：**432 个文件、446 个构造器**替换为类级 `@NoArgsConstructor`（~140）/ `@RequiredArgsConstructor`（~70）/ `@AllArgsConstructor`（~236）；46 个非 public 构造器通过 `access = AccessLevel.XXX` 保持原可见性。
- 附带收益：字段解析补上**无修饰符（包级私有）字段**后，访问器模式又多转换 29 个文件、248 个方法（如 `Effect` 107 个、`SkillTemplate` 58 个）。
- 转换标准：构造器体仅由直接赋值语句组成；赋值集合恰好等于全部非 static 字段（→AllArgs）或全部"未初始化 final 非 static"字段（→RequiredArgs）且按字段声明顺序；无 @NonNull 字段；类上无 @Data/@Value/@Builder（这些注解会随构造器消失自行生成构造器，签名/可见性不可控）；enum/interface/record 排除。
- 关键实证（Maven 探针 + javap 验证）：**显式构造器注解与类中保留的其他显式构造器可共存**（`Mixed(int)` 与 Lombok 生成的 `Mixed(int,String)` 同时存在），不存在 setter 那样的同名跳过规则；同时实证了 standalone javac `-proc:full` 下 Lombok 构造器处理器不生效（须以 Maven annotationProcessorPaths 为准）。
- 验证：`mvn compile` 与 `mvn test-compile` 通过（0 错误）；幂等复扫 0 剩余；空白标记文件与本轮改动零交集。
- 改造前快照：`/tmp/lombok_ctor_backup_20260909.tar.gz`（416 文件，系统自动清理）。
- 明细：`.agent/summary/lombok/lombok_ctors_report.json`（构造器）与 `.agent/summary/lombok/lombok_refactor_report.json`（访问器增量）。

## 第一轮：model 树访问器

### 第一轮：model 树
- 范围：`gameserver/model`、`loginserver/model`、`chatserver/model`，共 1050 个 Java 文件。
- 转换：**528 个文件、2725 个方法**替换为 Lombok 字段级 `@Getter` / `@Setter`。

## 第二轮：model 之外（controllers/services/network/ai2/world/commons/boot/test）
- 范围：全仓剩余 4429 个 Java 文件（排除三棵 model 树）。
- 转换：**134 个文件、311 个方法**。该区域业务逻辑密集，`annotated`（665）、`non-trivial body`（647）、`static`（214）、`synchronized`（118）等跳过占绝大多数，符合预期。
- 重点抽查：`EffectTemplate`（@XmlTransient 注解方法保留）、`AbstractAI`（@Override/final 方法保留、javadoc 迁移正确）、`AttackResult`（`setDamage` 参数类型不匹配 + 同名重载冲突双重防护均生效）。

- 验证（两轮合计）：`mvn compile` 与 `mvn test-compile` 均通过（0 错误）。
- 空白检查：`git diff --check` 标记的 14 个文件与本改造的 662 个文件零交集（均为并行改动遗留）。
- 逐方法明细：`.agent/summary/lombok/lombok_refactor_report.json`；转换器：`.agent/summary/lombok/lombok_refactor.py`（幂等，重跑 0 转换）。
- 改造前快照曾存放于 `.agent/lombok_backup/`、`.agent/lombok_backup2/`，编译验证通过后已按用户要求删除（约 43MB）。

## 转换标准（严格满足才转换）

1. 方法体恰好一条语句（`return [this.]field;` / `[this.]field = param;`）。
2. public、非 static、非 final、非 synchronized、无任何注解。
3. 方法名与返回/参数类型和字段完全一致，且与 Lombok 生成签名一致（boolean → `isX()`，其余 → `getX()`）。
4. getter 必须无参数；setter 恰好一个同类型参数。
5. 类中不得存在同名且同参数个数的其他方法（Lombok 冲突规则：存在时 Lombok 拒绝生成）。
6. 文件仅含一个类型声明；排除 record、@interface、枚举常量类体、文本块、带参 Lombok 注解。
7. 被删方法的 javadoc 迁移到字段上（字段已有文档时除外），不丢文档。

## 跳过的主要类别（保留手写代码的原因）

- non-trivial body 1224：方法体含业务逻辑。
- name mismatch 497：手写方法名与 Lombok 生成名不一致（如 `getType()` 返回 `chatType` 字段）。
- annotated 448：带 @Override / JAXB 等注解的方法。
- final method 177 / static 49 / synchronized 23：Lombok 无法复现的修饰符。
- 类型不匹配（如 `Integer` vs `int`）约 30：签名与字段类型不完全一致。

## 关键教训（本次修复的两个 bug）

1. **带参 "getter" 不是访问器**：`isCanTeleport(Player)` 这类方法名以 is/get 开头但带参数，误删会破坏子类 @Override。必须先校验参数列表为空。
2. **Lombok 同名+同参数个数冲突**：类中已存在同名同参数个数方法时（哪怕签名不同），Lombok 不生成方法。`Creature.setState(int)` 与 `setState(CreatureState)` 重载场景下，删除前者后 Lombok 拒绝重新生成，导致 198 处调用编译失败。转换前必须扫描全类方法签名表。

## 后续可选

- 其余包（controllers、services、network、commons 等）约 900 个文件含手写访问器，可用同一脚本按批推进。
- 平凡构造器（仅赋值）可评估 `@RequiredArgsConstructor` / `@AllArgsConstructor`；平凡 `equals/hashCode/toString` 可评估 `@EqualsAndHashCode` / `@ToString`（需逐个排除实体关系与循环引用）。
- `@Data` 仅适合所有字段都可安全参与生成行为的纯数据类，本次未批量使用。
