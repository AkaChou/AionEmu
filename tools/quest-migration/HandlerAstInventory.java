import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

/**
 * 使用 JDK AST 提取任务 Handler、调用、控制路径和 receiver 证据。
 * Uses the JDK AST to extract quest handlers, calls, control paths, and receiver evidence.
 */
public final class HandlerAstInventory {

	private static final Pattern QUEST_CLASS = Pattern.compile("^_(\\d+).*");
	private static final Set<String> TASK_ACCESS_METHODS = Set.of(
		"addQuest", "canRepeat", "changeQuestStatus", "changeQuestStep", "delQuest", "finishQuest",
		"getAllFinishedQuests", "getAllQuestState", "getCompleteCount", "getCompleteTime", "getNextRepeatTime",
		"getNormalQuestListSize", "getNormalQuests", "getPersistentState", "getQuestState", "getQuestStateList",
		"getQuestVarById", "getQuestVars", "getQuests", "getReward", "getStatus", "hasQuest", "questTimerEnd",
		"questTimerStart", "removeQuest", "setCompleteCount", "setCompleteTime", "setNextRepeatTime",
		"setPersistentState", "setQuestVar", "setQuestVarById", "setReward", "setStatus", "startQuest",
		"updateCompleteTime", "updateQuestStatus");

	/** 禁止实例化静态 AST 工具。 / Prevents instantiation of the static AST tool. */
	private HandlerAstInventory() {
	}

	/** 扫描 Java 源码并逐行输出确定性 JSON。 / Scans Java sources and emits deterministic JSON lines. */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: HandlerAstInventory <project-root> <java-source-root>");
			System.exit(2);
		}
		Path projectRoot = Path.of(args[0]).toAbsolutePath().normalize();
		Path sourceRoot = Path.of(args[1]).toAbsolutePath().normalize();
		if (!Files.isDirectory(sourceRoot)) {
			throw new IllegalArgumentException("Java source root does not exist: " + sourceRoot);
		}

		List<Path> files;
		try (var stream = Files.walk(sourceRoot)) {
			files = stream.filter(path -> path.toString().endsWith(".java")).sorted().toList();
		}
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("A full JDK is required");
		}

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		List<FileSummary> summaries = new ArrayList<>();
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
			Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(files);
			JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics,
				List.of("-proc:none", "-Xlint:none"), null, units);
			Trees trees = Trees.instance(task);
			SourcePositions positions = trees.getSourcePositions();
			for (CompilationUnitTree unit : task.parse()) {
				Path path = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
				FileSummary summary = new FileSummary(relative(projectRoot, path),
					unit.getPackageName() == null ? "" : unit.getPackageName().toString());
				new InventoryScanner(unit, positions, summary).scan(unit, null);
				summary.finish();
				if (summary.isRelevant()) {
					summaries.add(summary);
				}
			}
		}

		List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
			.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
			.toList();
		if (!errors.isEmpty()) {
			for (Diagnostic<? extends JavaFileObject> error : errors) {
				System.err.printf("%s:%d: %s%n",
					error.getSource() == null ? "<unknown>" : error.getSource().getName(),
					error.getLineNumber(), error.getMessage(null));
			}
			System.exit(2);
		}

		summaries.sort(Comparator.comparing(summary -> summary.path));
		PrintWriter writer = new PrintWriter(System.out, false, StandardCharsets.UTF_8);
		for (FileSummary summary : summaries) {
			writer.println(summary.toJson());
		}
		writer.flush();
	}

	/** 返回相对项目根的稳定路径。 / Returns a stable path relative to the project root. */
	private static String relative(Path root, Path path) {
		try {
			return root.relativize(path).toString().replace('\\', '/');
		} catch (IllegalArgumentException e) {
			return path.toString().replace('\\', '/');
		}
	}

	/** 遍历单个编译单元并收集任务迁移所需的 AST 事实。 / Walks one compilation unit and collects migration AST facts. */
	private static final class InventoryScanner extends TreePathScanner<Void, Void> {
		private final CompilationUnitTree unit;
		private final SourcePositions positions;
		private final FileSummary summary;
		private final Deque<String> classes = new ArrayDeque<>();
		private final Deque<String> methods = new ArrayDeque<>();
		private final Deque<String> controlPath = new ArrayDeque<>();
		private final Map<String, Integer> integerConstants = new TreeMap<>();
		private final Map<String, List<Integer>> integerCollections = new TreeMap<>();
		private final Map<String, List<Integer>> loopIntegerValues = new TreeMap<>();
		private final Map<String, String> questValueAliases = new TreeMap<>();
		private final Map<String, String> routingAliases = new TreeMap<>();
		private final Map<String, Set<String>> variableTypes = new TreeMap<>();

		/** 创建绑定到单个编译单元的扫描器。 / Creates a scanner bound to one compilation unit. */
		private InventoryScanner(CompilationUnitTree unit, SourcePositions positions, FileSummary summary) {
			this.unit = unit;
			this.positions = positions;
			this.summary = summary;
		}

		/** 记录类、父类和 Handler 候选关系。 / Records classes, superclasses, and handler candidacy. */
		@Override
		public Void visitClass(ClassTree node, Void unused) {
			String name = node.getSimpleName().toString();
			classes.push(name);
			summary.classes.add(name);
			if (node.getExtendsClause() != null) {
				String superclass = normalize(node.getExtendsClause().toString());
				summary.superclasses.add(superclass);
				if (superclass.endsWith("QuestHandler")) {
					summary.handlerCandidate = true;
				}
			}
			if (node.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
				summary.publicClass = true;
			}
			super.visitClass(node, unused);
			classes.pop();
			return null;
		}

		/** 记录方法及显式 override。 / Records methods and explicit overrides. */
		@Override
		public Void visitMethod(MethodTree node, Void unused) {
			String method = node.getReturnType() == null ? "<init>" : node.getName().toString();
			methods.push(method);
			summary.methods.add(method);
			for (AnnotationTree annotation : node.getModifiers().getAnnotations()) {
				if (annotation.getAnnotationType().toString().endsWith("Override")) {
					summary.overrides.add(method);
				}
			}
			super.visitMethod(node, unused);
			methods.pop();
			return null;
		}

		/** 记录变量类型、整数常量和受限别名。 / Records variable types, integer constants, and bounded aliases. */
		@Override
		public Void visitVariable(VariableTree node, Void unused) {
			if (node.getType() != null) {
				variableTypes.computeIfAbsent(node.getName().toString(), key -> new TreeSet<>())
					.add(normalize(node.getType().toString()));
			}
			Integer value = integerValue(node.getInitializer());
			if (value != null) {
				integerConstants.put(node.getName().toString(), value);
			}
			List<Integer> collection = integerValues(node.getInitializer());
			if (!collection.isEmpty()) {
				integerCollections.put(node.getName().toString(), collection);
			}
			rememberRoutingAlias(node.getName().toString(), node.getInitializer());
			rememberQuestValueAlias(node.getName().toString(), node.getInitializer());
			return super.visitVariable(node, unused);
		}

		/** 记录调用签名、参数、receiver、控制路径和结果消费。 / Records call signatures, arguments, receivers, control paths, and result use. */
		@Override
		public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
			String select = normalize(node.getMethodSelect().toString());
			String method = methodName(node.getMethodSelect());
			List<String> arguments = node.getArguments().stream()
				.map(argument -> expandQuestValueAliases(normalize(argument.toString()))).toList();
			List<Integer> argumentValues = new ArrayList<>();
			List<List<Integer>> argumentValueSets = new ArrayList<>();
			for (Tree argument : node.getArguments()) {
				Integer value = integerValue(argument);
				if (value == null && argument.getKind() == Tree.Kind.IDENTIFIER) {
					value = integerConstants.get(argument.toString());
				}
				if (value == null && argument instanceof MethodInvocationTree invocation
					&& "getQuestId".equals(methodName(invocation.getMethodSelect()))) {
					value = resolvedQuestId();
				}
				argumentValues.add(value);
				List<Integer> candidates = argument.getKind() == Tree.Kind.IDENTIFIER
					? loopIntegerValues.getOrDefault(argument.toString(), List.of()) : List.of();
				argumentValueSets.add(candidates.isEmpty() && value != null ? List.of(value) : candidates);
			}
			List<String> path = new ArrayList<>(controlPath);
			java.util.Collections.reverse(path);
			ResultConsumption resultConsumption = resultConsumption(node);
			summary.calls.add(new Call(method, select, methods.isEmpty() ? "" : methods.peek(), line(node), arguments,
				argumentValues, argumentValueSets, receiverTypes(node.getMethodSelect()), path, resultConsumption.usage(),
				resultConsumption.context(), resultConsumption.trueOutcome(), resultConsumption.falseOutcome(),
				positions.getStartPosition(unit, node)));
			if (TASK_ACCESS_METHODS.contains(method)) {
				summary.hasTaskAccess = true;
			}
			if ("super".equals(select) && !node.getArguments().isEmpty()) {
				Integer questId = integerValue(node.getArguments().get(0));
				if (questId == null && node.getArguments().get(0).getKind() == Tree.Kind.IDENTIFIER) {
					questId = integerConstants.get(node.getArguments().get(0).toString());
				}
				if (questId != null) {
					summary.questIds.add(questId);
				}
			}
			return super.visitMethodInvocation(node, unused);
		}

		/**
		 * 解析调用结果的首个语句级消费者，避免按源码文本猜测 helper 的失败分支。
		 * Resolves the first statement-level consumer of a call result so helper failure branches are not inferred from source text.
		 */
		private ResultConsumption resultConsumption(MethodInvocationTree node) {
			TreePath path = getCurrentPath().getParentPath();
			while (path != null) {
				Tree parent = path.getLeaf();
				if (parent instanceof ExpressionStatementTree statement) {
					return new ResultConsumption("IGNORED", normalize(statement.getExpression().toString()), "NONE", "NONE");
				}
				if (parent instanceof ReturnTree statement) {
					return new ResultConsumption("RETURNED", normalize(statement.getExpression().toString()), "NONE", "NONE");
				}
				if (parent instanceof IfTree statement && contains(statement.getCondition(), node)) {
					return new ResultConsumption("CONDITION", normalize(statement.getCondition().toString()),
						branchOutcome(statement.getThenStatement()), branchOutcome(statement.getElseStatement()));
				}
				if (parent instanceof WhileLoopTree statement && contains(statement.getCondition(), node)) {
					return new ResultConsumption("CONDITION", normalize(statement.getCondition().toString()), "LOOP_BODY", "LOOP_EXIT");
				}
				if (parent instanceof DoWhileLoopTree statement && contains(statement.getCondition(), node)) {
					return new ResultConsumption("CONDITION", normalize(statement.getCondition().toString()), "LOOP_BODY", "LOOP_EXIT");
				}
				if (parent instanceof ForLoopTree statement && statement.getCondition() != null && contains(statement.getCondition(), node)) {
					return new ResultConsumption("CONDITION", normalize(statement.getCondition().toString()), "LOOP_BODY", "LOOP_EXIT");
				}
				if (parent instanceof VariableTree variable && contains(variable.getInitializer(), node)) {
					return new ResultConsumption("VARIABLE_INITIALIZER", normalize(variable.toString()), "NONE", "NONE");
				}
				if (parent instanceof AssignmentTree assignment && contains(assignment.getExpression(), node)) {
					return new ResultConsumption("ASSIGNED", normalize(assignment.toString()), "NONE", "NONE");
				}
				if (parent instanceof MethodInvocationTree outer && outer != node) {
					return new ResultConsumption("ARGUMENT", normalize(outer.toString()), "NONE", "NONE");
				}
				if (parent instanceof MethodTree || parent instanceof LambdaExpressionTree || parent instanceof ClassTree) {
					break;
				}
				path = path.getParentPath();
			}
			return new ResultConsumption("OTHER", normalize(node.toString()), "NONE", "NONE");
		}

		/**
		 * 对条件调用的直接分支做结构分类，不推断复杂块中的隐式控制流。
		 * Classifies direct branches of a condition call without inferring control flow inside complex blocks.
		 */
		private String branchOutcome(Tree branch) {
			if (branch == null) {
				return "FALLTHROUGH";
			}
			Tree statement = branch;
			if (statement instanceof BlockTree block) {
				if (block.getStatements().size() != 1) {
					return "COMPLEX";
				}
				statement = block.getStatements().get(0);
			}
			if (!(statement instanceof ReturnTree returned)) {
				return "COMPLEX";
			}
			ExpressionTree expression = returned.getExpression();
			if (expression instanceof LiteralTree literal && literal.getValue() instanceof Boolean value) {
				return value ? "RETURN_TRUE" : "RETURN_FALSE";
			}
			return "RETURN_VALUE";
		}

		/**
		 * 判断父表达式的源码范围是否包含目标调用。
		 * Tests whether a parent expression's source range contains the target invocation.
		 */
		private boolean contains(Tree parent, Tree child) {
			if (parent == null) {
				return false;
			}
			long parentStart = positions.getStartPosition(unit, parent);
			long parentEnd = positions.getEndPosition(unit, parent);
			long childStart = positions.getStartPosition(unit, child);
			long childEnd = positions.getEndPosition(unit, child);
			return parentStart >= 0 && childStart >= parentStart && childEnd <= parentEnd;
		}

		/** 收集整数文字以支持引用盘点。 / Collects integer literals for reference inventory. */
		@Override
		public Void visitLiteral(LiteralTree node, Void unused) {
			if (node.getValue() instanceof Number number) {
				summary.integerLiterals.add(number.longValue());
			}
			return super.visitLiteral(node, unused);
		}

		/**
		 * 记录对话 Handler 中具有精确控制路径的布尔返回值，供编译器证明无协议端点的事件消费语义。
		 * Records boolean returns with exact control paths so the compiler can prove event consumption without a protocol endpoint.
		 */
		@Override
		public Void visitReturn(ReturnTree node, Void unused) {
			if (inDialogEvent() && node.getExpression() instanceof LiteralTree literal
				&& literal.getValue() instanceof Boolean value) {
				List<String> path = new ArrayList<>(controlPath);
				java.util.Collections.reverse(path);
				summary.booleanReturns.add(new BooleanReturn(value, line(node), path,
					positions.getStartPosition(unit, node)));
			}
			return super.visitReturn(node, unused);
		}

		/** 记录赋值控制形状并更新可证明别名。 / Records assignment shape and updates proven aliases. */
		@Override
		public Void visitAssignment(AssignmentTree node, Void unused) {
			summary.increment("ASSIGNMENT");
			String variable = normalize(node.getVariable().toString());
			if (variable.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
				Integer value = integerValue(node.getExpression());
				if (value != null) {
					integerConstants.put(variable, value);
				}
				rememberRoutingAlias(variable, node.getExpression());
				rememberQuestValueAlias(variable, node.getExpression());
			}
			return super.visitAssignment(node, unused);
		}

		/** 展开 if 真/假分支为精确控制路径。 / Expands true and false branches into exact control paths. */
		@Override
		public Void visitIf(IfTree node, Void unused) {
			summary.increment("IF");
			String condition = expandQuestValueAliases(expandRoutingAliases(normalize(node.getCondition().toString())));
			if (inDialogEvent() && condition.contains("getDialog")) {
				summary.dialogBranches.add(new DialogBranch("CONDITION", condition, line(node)));
			}
			scan(node.getCondition(), unused);
			controlPath.push("IF_TRUE:" + condition);
			scan(node.getThenStatement(), unused);
			controlPath.pop();
			if (node.getElseStatement() != null) {
				controlPath.push("IF_FALSE:" + condition);
				scan(node.getElseStatement(), unused);
				controlPath.pop();
			}
			return null;
		}

		/** 展开 switch 标签并保留 fall-through 标签集合。 / Expands switch labels while preserving fall-through label sets. */
		@Override
		public Void visitSwitch(SwitchTree node, Void unused) {
			summary.increment("SWITCH");
			String expression = expandQuestValueAliases(expandRoutingAliases(normalize(node.getExpression().toString())));
			scan(node.getExpression(), unused);
			List<String> pendingLabels = new ArrayList<>();
			for (CaseTree caseTree : node.getCases()) {
				List<String> labels = caseTree.getExpressions().stream().map(value -> normalize(value.toString())).toList();
				if (inDialogEvent() && expression.contains("getDialog")) {
					for (String label : labels) {
						summary.dialogBranches.add(new DialogBranch("SWITCH_CASE", label, line(caseTree)));
					}
				}
				pendingLabels.addAll(labels.isEmpty() ? List.of("default") : labels);
				String branch = String.join("|", pendingLabels);
				controlPath.push("SWITCH:" + expression + "=" + branch);
				if (caseTree.getStatements() != null) {
					scan(caseTree.getStatements(), unused);
				}
				if (caseTree.getBody() != null) {
					scan(caseTree.getBody(), unused);
				}
				controlPath.pop();
				if (caseTree.getBody() != null || caseTree.getStatements() != null && !caseTree.getStatements().isEmpty()) {
					pendingLabels.clear();
				}
			}
			return null;
		}

		/** 记录对话枚举引用。 / Records dialog enum references. */
		@Override
		public Void visitMemberSelect(MemberSelectTree node, Void unused) {
			if (inDialogEvent() && "QuestDialog".equals(normalize(node.getExpression().toString()))) {
				summary.dialogBranches.add(new DialogBranch("ENUM_REFERENCE", node.getIdentifier().toString(), line(node)));
			}
			return super.visitMemberSelect(node, unused);
		}

		/** 统计传统 for 控制结构。 / Counts traditional for control structures. */
		@Override
		public Void visitForLoop(ForLoopTree node, Void unused) {
			summary.increment("FOR");
			return super.visitForLoop(node, unused);
		}

		/** 展开静态整数集合上的增强 for，并传播循环变量类型。 / Expands enhanced loops over static integer sets and propagates loop-variable types. */
		@Override
		public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
			summary.increment("ENHANCED_FOR");
			summary.recordControlMethod("ENHANCED_FOR", methods.isEmpty() ? "" : methods.peek());
			String variable = node.getVariable().getName().toString();
			List<Integer> previous = loopIntegerValues.get(variable);
			Set<String> previousTypes = variableTypes.containsKey(variable)
				? new TreeSet<>(variableTypes.get(variable)) : null;
			if (node.getVariable().getType() != null) {
				variableTypes.computeIfAbsent(variable, key -> new TreeSet<>())
					.add(normalize(node.getVariable().getType().toString()));
			}
			List<Integer> values = integerValues(node.getExpression());
			scan(node.getExpression(), unused);
			if (!values.isEmpty()) {
				loopIntegerValues.put(variable, values);
			}
			scan(node.getStatement(), unused);
			if (previous == null) {
				loopIntegerValues.remove(variable);
			} else {
				loopIntegerValues.put(variable, previous);
			}
			if (previousTypes == null) {
				variableTypes.remove(variable);
			} else {
				variableTypes.put(variable, previousTypes);
			}
			return null;
		}

		/** 统计 while 并记录所在方法。 / Counts while loops and records their enclosing methods. */
		@Override
		public Void visitWhileLoop(WhileLoopTree node, Void unused) {
			summary.increment("WHILE");
			summary.recordControlMethod("WHILE", methods.isEmpty() ? "" : methods.peek());
			return super.visitWhileLoop(node, unused);
		}

		/** 统计 do-while 控制结构。 / Counts do-while control structures. */
		@Override
		public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
			summary.increment("DO_WHILE");
			return super.visitDoWhileLoop(node, unused);
		}

		/** 统计 try 控制结构。 / Counts try control structures. */
		@Override
		public Void visitTry(TryTree node, Void unused) {
			summary.increment("TRY");
			return super.visitTry(node, unused);
		}

		/** 统计 lambda 控制结构。 / Counts lambda control structures. */
		@Override
		public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
			summary.increment("LAMBDA");
			return super.visitLambdaExpression(node, unused);
		}

		/** 统计同步控制结构。 / Counts synchronized control structures. */
		@Override
		public Void visitSynchronized(SynchronizedTree node, Void unused) {
			summary.increment("SYNCHRONIZED");
			return super.visitSynchronized(node, unused);
		}

		/** 返回 AST 节点的源码行号。 / Returns the source line of an AST node. */
		private long line(Tree node) {
			long position = positions.getStartPosition(unit, node);
			return position < 0 || unit.getLineMap() == null ? -1 : unit.getLineMap().getLineNumber(position);
		}

		/** 返回调用根 receiver 的已知静态类型。 / Returns known static types of a call's root receiver. */
		private List<String> receiverTypes(Tree select) {
			if (select instanceof MemberSelectTree member) {
				return List.copyOf(rootReceiverTypes(member.getExpression()));
			}
			return List.of();
		}

		/** 递归解析表达式根 receiver 的类型。 / Recursively resolves types of an expression's root receiver. */
		private Set<String> rootReceiverTypes(Tree expression) {
			if (expression.getKind() == Tree.Kind.IDENTIFIER) {
				return variableTypes.getOrDefault(expression.toString(), Set.of());
			}
			if (expression instanceof MemberSelectTree member) {
				return rootReceiverTypes(member.getExpression());
			}
			if (expression instanceof MethodInvocationTree invocation
				&& invocation.getMethodSelect() instanceof MemberSelectTree member) {
				return rootReceiverTypes(member.getExpression());
			}
			return Set.of();
		}

		/** 解析带可选正负号的整数字面量。 / Resolves integer literals with an optional unary sign. */
		private Integer integerValue(Tree tree) {
			if (tree instanceof LiteralTree literal && literal.getValue() instanceof Number number) {
				return number.intValue();
			}
			if (tree instanceof UnaryTree unary && unary.getExpression() instanceof LiteralTree literal
				&& literal.getValue() instanceof Number number) {
				return switch (unary.getKind()) {
					case UNARY_MINUS -> -number.intValue();
					case UNARY_PLUS -> number.intValue();
					default -> null;
				};
			}
			return null;
		}

		/**
		 * 解析静态整数数组或当前增强循环的有限取值。
		 * Resolves a static integer array or the finite values of the current enhanced loop.
		 */
		private List<Integer> integerValues(Tree tree) {
			if (tree == null) {
				return List.of();
			}
			if (tree.getKind() == Tree.Kind.IDENTIFIER) {
				return integerCollections.getOrDefault(tree.toString(), List.of());
			}
			if (!(tree instanceof NewArrayTree array) || array.getInitializers() == null) {
				return List.of();
			}
			List<Integer> result = new ArrayList<>();
			for (ExpressionTree initializer : array.getInitializers()) {
				Integer value = integerValue(initializer);
				if (value == null && initializer.getKind() == Tree.Kind.IDENTIFIER) {
					value = integerConstants.get(initializer.toString());
				}
				if (value == null) {
					return List.of();
				}
				result.add(value);
			}
			return List.copyOf(result);
		}

		/**
		 * 记录仅参与事件路由的局部别名，避免扩展任意业务表达式。
		 * Records local aliases used only for event routing without expanding arbitrary business expressions.
		 */
		private void rememberRoutingAlias(String variable, Tree expression) {
			if (expression == null) {
				return;
			}
			String value = expandRoutingAliases(normalize(expression.toString()));
			if (value.contains("getDialog()") || value.contains("getDialogId()")
				|| value.contains("getTargetId()") || value.contains("getNpcId()")) {
				routingAliases.put(variable, value);
			}
		}

		/**
		 * 记录直接任务变量读取的局部别名，用于调用参数和条件的机械展开。
		 * Records direct quest-variable read aliases for mechanical expansion in call arguments and conditions.
		 */
		private void rememberQuestValueAlias(String variable, Tree expression) {
			if (expression == null) {
				return;
			}
			String value = expandQuestValueAliases(normalize(expression.toString()));
			if (value.matches(".*\\.getQuestVarById\\(\\d+\\).*")) {
				questValueAliases.put(variable, value);
			}
		}

		/**
		 * 将已证明的路由局部变量替换为其访问器表达式。
		 * Replaces proven routing locals with their accessor expressions.
		 */
		private String expandRoutingAliases(String value) {
			String result = value;
			for (int iteration = 0; iteration < 8; iteration++) {
				String previous = result;
				for (Map.Entry<String, String> alias : routingAliases.entrySet()) {
					result = result.replaceAll("\\b" + Pattern.quote(alias.getKey()) + "\\b",
						Matcher.quoteReplacement("(" + alias.getValue() + ")"));
				}
				if (result.equals(previous)) {
					break;
				}
			}
			return result;
		}

		/**
		 * 展开已证明的任务变量局部别名。
		 * Expands proven local aliases of quest-variable reads.
		 */
		private String expandQuestValueAliases(String value) {
			String result = value;
			for (int iteration = 0; iteration < 8; iteration++) {
				String previous = result;
				for (Map.Entry<String, String> alias : questValueAliases.entrySet()) {
					result = result.replaceAll("\\b" + Pattern.quote(alias.getKey()) + "\\b",
						Matcher.quoteReplacement("(" + alias.getValue() + ")"));
				}
				if (result.equals(previous)) {
					break;
				}
			}
			return result;
		}

		/** 返回当前类唯一可证明的任务 ID。 / Returns the uniquely proven quest id of the current class. */
		private Integer resolvedQuestId() {
			Integer constant = integerConstants.get("questId");
			if (constant != null) {
				return constant;
			}
			for (String name : classes) {
				Matcher matcher = QUEST_CLASS.matcher(name);
				if (matcher.matches()) {
					return Integer.parseInt(matcher.group(1));
				}
			}
			return null;
		}

		/** 判断扫描器当前是否位于对话事件方法。 / Tests whether the scanner is currently inside the dialog event method. */
		private boolean inDialogEvent() {
			return !methods.isEmpty() && "onDialogEvent".equals(methods.peek());
		}
	}

	/** 保存单个 Java 文件的规范化 AST 摘要。 / Holds the normalized AST summary for one Java file. */
	private static final class FileSummary {
		private final String path;
		private final String packageName;
		private final Set<String> classes = new TreeSet<>();
		private final Set<String> superclasses = new TreeSet<>();
		private final Set<Integer> questIds = new TreeSet<>();
		private final Set<String> methods = new TreeSet<>();
		private final Set<String> overrides = new TreeSet<>();
		private final Set<Long> integerLiterals = new TreeSet<>();
		private final List<Call> calls = new ArrayList<>();
		private final List<DialogBranch> dialogBranches = new ArrayList<>();
		private final List<BooleanReturn> booleanReturns = new ArrayList<>();
		private final Map<String, Integer> controls = new TreeMap<>();
		private final Map<String, Set<String>> controlMethods = new TreeMap<>();
		private boolean publicClass;
		private boolean handlerCandidate;
		private boolean hasTaskAccess;

		/** 创建文件摘要。 / Creates a file summary. */
		private FileSummary(String path, String packageName) {
			this.path = path;
			this.packageName = packageName;
		}

		/** 递增控制结构计数。 / Increments a control-structure count. */
		private void increment(String kind) {
			controls.merge(kind, 1, Integer::sum);
		}

		/** 记录控制结构出现的方法。 / Records methods containing a control structure. */
		private void recordControlMethod(String kind, String method) {
			controlMethods.computeIfAbsent(kind, key -> new TreeSet<>()).add(method);
		}

		/** 完成候选识别和稳定排序。 / Finalizes candidate recognition and stable ordering. */
		private void finish() {
			handlerCandidate = handlerCandidate && path.contains("/gameserver/quest/handlers/");
			if (handlerCandidate && questIds.isEmpty()) {
				for (String name : classes) {
					Matcher matcher = QUEST_CLASS.matcher(name);
					if (matcher.matches()) {
						questIds.add(Integer.parseInt(matcher.group(1)));
					}
				}
			}
			calls.sort(Comparator.comparing((Call call) -> call.enclosingMethod)
				.thenComparingLong(call -> call.line)
				.thenComparing(call -> call.method)
				.thenComparing(call -> call.select));
			dialogBranches.sort(Comparator.comparingLong((DialogBranch branch) -> branch.line)
				.thenComparing(branch -> branch.kind)
				.thenComparing(branch -> branch.value));
			booleanReturns.sort(Comparator.comparingLong((BooleanReturn returned) -> returned.sourcePosition)
				.thenComparing(returned -> returned.controlPath.toString()));
		}

		/** 判断文件是否属于任务迁移审计面。 / Tests whether the file belongs to the quest-migration audit surface. */
		private boolean isRelevant() {
			return handlerCandidate || hasTaskAccess
				|| path.endsWith("/questEngine/QuestEngine.java")
				|| path.endsWith("/questEngine/handlers/QuestHandler.java")
				|| path.endsWith("/questEngine/model/QuestState.java")
				|| path.endsWith("/services/QuestService.java")
				|| calls.stream().anyMatch(call -> call.receiverTypes.stream().anyMatch(type ->
					type.matches("(?:^|.*[.$])QuestState(?:List)?(?:<.*)?"))
					|| call.select.contains(".getQuestState") || call.select.startsWith("getQuestStateList()."));
		}

		/** 将摘要序列化为确定性 JSON。 / Serializes the summary as deterministic JSON. */
		private String toJson() {
			StringBuilder json = new StringBuilder(2048);
			json.append('{');
			field(json, "path", path).append(',');
			field(json, "package", packageName).append(',');
			array(json, "classes", classes).append(',');
			array(json, "superclasses", superclasses).append(',');
			numberArray(json, "quest_ids", questIds).append(',');
			json.append("\"dynamic_quest_id\":").append(handlerCandidate && questIds.size() != 1).append(',');
			json.append("\"handler_candidate\":").append(handlerCandidate).append(',');
			json.append("\"public_class\":").append(publicClass).append(',');
			array(json, "methods", methods).append(',');
			array(json, "overrides", overrides).append(',');
			numberArray(json, "integer_literals", integerLiterals).append(',');
			json.append("\"controls\":");
			map(json, controls);
			json.append(",\"control_methods\":");
			stringSetMap(json, controlMethods);
			json.append(",\"calls\":[");
			for (int i = 0; i < calls.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				calls.get(i).appendJson(json);
			}
			json.append("],\"dialog_branches\":[");
			for (int i = 0; i < dialogBranches.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				dialogBranches.get(i).appendJson(json);
			}
			json.append("],\"boolean_returns\":[");
			for (int i = 0; i < booleanReturns.size(); i++) {
				if (i > 0) {
					json.append(',');
				}
				booleanReturns.get(i).appendJson(json);
			}
			json.append("]}");
			return json.toString();
		}
	}

	/** 带精确控制路径的布尔返回语句。 / Boolean return statement with its exact control path. */
	private static final class BooleanReturn {
		private final boolean value;
		private final long line;
		private final List<String> controlPath;
		private final long sourcePosition;

		/** 创建布尔返回记录。 / Creates a boolean-return record. */
		private BooleanReturn(boolean value, long line, List<String> controlPath, long sourcePosition) {
			this.value = value;
			this.line = line;
			this.controlPath = controlPath;
			this.sourcePosition = sourcePosition;
		}

		/** 追加确定性 JSON。 / Appends deterministic JSON. */
		private void appendJson(StringBuilder json) {
			json.append('{');
			json.append("\"value\":").append(value).append(',');
			json.append("\"line\":").append(line).append(',');
			array(json, "control_path", controlPath).append(',');
			json.append("\"source_position\":").append(sourcePosition).append('}');
		}
	}

	/** 保存对话控制分支证据。 / Holds dialog control-branch evidence. */
	private static final class DialogBranch {
		private final String kind;
		private final String value;
		private final long line;

		/** 创建对话分支记录。 / Creates a dialog-branch record. */
		private DialogBranch(String kind, String value, long line) {
			this.kind = kind;
			this.value = value;
			this.line = line;
		}

		/** 追加确定性 JSON。 / Appends deterministic JSON. */
		private void appendJson(StringBuilder json) {
			json.append('{');
			field(json, "kind", kind).append(',');
			field(json, "value", value).append(',');
			json.append("\"line\":").append(line).append('}');
		}
	}

	/** 保存规范化调用及其控制流和 receiver 证据。 / Holds a normalized call with control-flow and receiver evidence. */
	private static final class Call {
		private final String method;
		private final String select;
		private final String enclosingMethod;
		private final long line;
		private final List<String> arguments;
		private final List<Integer> argumentValues;
		private final List<List<Integer>> argumentValueSets;
		private final List<String> receiverTypes;
		private final List<String> controlPath;
		private final String resultUsage;
		private final String resultContext;
		private final String conditionTrueOutcome;
		private final String conditionFalseOutcome;
		private final long sourcePosition;

		/** 创建规范化调用记录。 / Creates a normalized call record. */
		private Call(String method, String select, String enclosingMethod, long line, List<String> arguments,
			List<Integer> argumentValues, List<List<Integer>> argumentValueSets, List<String> receiverTypes,
			List<String> controlPath, String resultUsage,
			String resultContext, String conditionTrueOutcome, String conditionFalseOutcome, long sourcePosition) {
			this.method = method;
			this.select = select;
			this.enclosingMethod = enclosingMethod;
			this.line = line;
			this.arguments = arguments;
			this.argumentValues = argumentValues;
			this.argumentValueSets = argumentValueSets;
			this.receiverTypes = receiverTypes;
			this.controlPath = controlPath;
			this.resultUsage = resultUsage;
			this.resultContext = resultContext;
			this.conditionTrueOutcome = conditionTrueOutcome;
			this.conditionFalseOutcome = conditionFalseOutcome;
			this.sourcePosition = sourcePosition;
		}

		/** 追加确定性 JSON。 / Appends deterministic JSON. */
		private void appendJson(StringBuilder json) {
			json.append('{');
			field(json, "method", method).append(',');
			field(json, "select", select).append(',');
			field(json, "enclosing_method", enclosingMethod).append(',');
			json.append("\"line\":").append(line).append(',');
			array(json, "arguments", arguments).append(',');
			nullableNumberArray(json, "argument_values", argumentValues).append(',');
			nestedNumberArray(json, "argument_value_sets", argumentValueSets).append(',');
			array(json, "receiver_types", receiverTypes).append(',');
			array(json, "control_path", controlPath).append(',');
			field(json, "result_usage", resultUsage).append(',');
			field(json, "result_context", resultContext).append(',');
			field(json, "condition_true_outcome", conditionTrueOutcome).append(',');
			field(json, "condition_false_outcome", conditionFalseOutcome).append(',');
			json.append("\"source_position\":").append(sourcePosition);
			json.append('}');
		}
	}

	/** 保存调用结果的消费分类和规范化上下文。 / Holds a call-result consumption category and normalized context. */
	private record ResultConsumption(String usage, String context, String trueOutcome, String falseOutcome) {
	}

	/** 提取方法选择表达式中的方法名。 / Extracts a method name from a method-select expression. */
	private static String methodName(Tree select) {
		return select instanceof MemberSelectTree member ? member.getIdentifier().toString() : select.toString();
	}

	/** 规范化源码表达式中的空白。 / Normalizes whitespace in a source expression. */
	private static String normalize(String value) {
		return value.replaceAll("\\s+", " ").trim();
	}

	/** 追加字符串 JSON 字段。 / Appends a string JSON field. */
	private static StringBuilder field(StringBuilder json, String name, String value) {
		string(json, name).append(':');
		return string(json, value);
	}

	/** 追加字符串数组 JSON 字段。 / Appends a string-array JSON field. */
	private static StringBuilder array(StringBuilder json, String name, Iterable<String> values) {
		string(json, name).append(':').append('[');
		boolean first = true;
		for (String value : values) {
			if (!first) {
				json.append(',');
			}
			string(json, value);
			first = false;
		}
		return json.append(']');
	}

	/** 追加数字数组 JSON 字段。 / Appends a numeric-array JSON field. */
	private static StringBuilder numberArray(StringBuilder json, String name, Iterable<? extends Number> values) {
		string(json, name).append(':').append('[');
		boolean first = true;
		for (Number value : values) {
			if (!first) {
				json.append(',');
			}
			json.append(value);
			first = false;
		}
		return json.append(']');
	}

	/** 追加可空数字数组 JSON 字段。 / Appends a nullable numeric-array JSON field. */
	private static StringBuilder nullableNumberArray(StringBuilder json, String name, Iterable<? extends Number> values) {
		string(json, name).append(':').append('[');
		boolean first = true;
		for (Number value : values) {
			if (!first) {
				json.append(',');
			}
			json.append(value == null ? "null" : value);
			first = false;
		}
		return json.append(']');
	}

	/** 追加嵌套数字数组 JSON 字段。 / Appends a nested numeric-array JSON field. */
	private static StringBuilder nestedNumberArray(StringBuilder json, String name,
		Iterable<? extends Iterable<? extends Number>> values) {
		string(json, name).append(':').append('[');
		boolean first = true;
		for (Iterable<? extends Number> value : values) {
			if (!first) {
				json.append(',');
			}
			json.append('[');
			boolean nestedFirst = true;
			for (Number number : value) {
				if (!nestedFirst) {
					json.append(',');
				}
				json.append(number);
				nestedFirst = false;
			}
			json.append(']');
			first = false;
		}
		return json.append(']');
	}

	/** 追加字符串到整数的 JSON 映射。 / Appends a string-to-integer JSON map. */
	private static void map(StringBuilder json, Map<String, Integer> values) {
		json.append('{');
		boolean first = true;
		for (Map.Entry<String, Integer> entry : values.entrySet()) {
			if (!first) {
				json.append(',');
			}
			string(json, entry.getKey()).append(':').append(entry.getValue());
			first = false;
		}
		json.append('}');
	}

	/** 追加字符串到字符串集合的 JSON 映射。 / Appends a string-to-string-set JSON map. */
	private static void stringSetMap(StringBuilder json, Map<String, Set<String>> values) {
		json.append('{');
		boolean first = true;
		for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
			if (!first) {
				json.append(',');
			}
			array(json, entry.getKey(), entry.getValue());
			first = false;
		}
		json.append('}');
	}

	/** 追加经过完整转义的 JSON 字符串。 / Appends a fully escaped JSON string. */
	private static StringBuilder string(StringBuilder json, String value) {
		json.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> json.append("\\\"");
				case '\\' -> json.append("\\\\");
				case '\b' -> json.append("\\b");
				case '\f' -> json.append("\\f");
				case '\n' -> json.append("\\n");
				case '\r' -> json.append("\\r");
				case '\t' -> json.append("\\t");
				default -> {
					if (c < 0x20) {
						json.append(String.format("\\u%04x", (int) c));
					} else {
						json.append(c);
					}
				}
			}
		}
		return json.append('"');
	}
}
