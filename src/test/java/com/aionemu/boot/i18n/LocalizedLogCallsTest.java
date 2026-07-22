package com.aionemu.boot.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class LocalizedLogCallsTest {

    private static final Pattern DIRECT_VISIBLE_LOG = Pattern.compile(
        "(?s)\\b(?:log|logger|LOG|LOGGER)\\.(?:info|warn|error|fatal)\\s*\\(\\s*(?:\"|String\\.format\\s*\\()"
    );

    @Test
    void visibleLogStringLiteralsUseI18n() throws IOException {
        List<Path> violations = new ArrayList<>();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path).replaceAll("(?s)/\\*.*?\\*/|//[^\\r\\n]*", "");
                    if (DIRECT_VISIBLE_LOG.matcher(source).find()) {
                        violations.add(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(violations.isEmpty(), "Direct visible log strings must use I18n: " + violations);
    }

    @Test
    void caughtExceptionsArePassedToLogger() throws IOException {
        List<String> violations = new ArrayList<>();
        List<Path> sources;
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                files.getJavaFileObjectsFromPaths(sources));
            Trees trees = Trees.instance(task);
            for (var unit : task.parse()) {
                new TreePathScanner<Void, Void>() {
                    private final Deque<String> exceptionVariables = new ArrayDeque<>();

                    @Override
                    public Void visitMethod(MethodTree node, Void unused) {
                        List<String> parameters = node.getParameters().stream()
                            .filter(parameter -> parameter.getType().toString().matches(".*(?:Throwable|Exception|Error)"))
                            .map(parameter -> parameter.getName().toString())
                            .toList();
                        parameters.forEach(exceptionVariables::push);
                        scan(node.getBody(), unused);
                        parameters.forEach(ignored -> exceptionVariables.pop());
                        return null;
                    }

                    @Override
                    public Void visitCatch(CatchTree node, Void unused) {
                        exceptionVariables.push(node.getParameter().getName().toString());
                        scan(node.getBlock(), unused);
                        exceptionVariables.pop();
                        return null;
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        if (isLogCall(node)) {
                            for (String exception : exceptionVariables) {
                                if (references(node, exception) && !passesThrowable(node, exception)) {
                                    long position = trees.getSourcePositions().getStartPosition(unit, node);
                                    violations.add(unit.getSourceFile().getName() + ":" + unit.getLineMap().getLineNumber(position));
                                }
                            }
                        }
                        return super.visitMethodInvocation(node, unused);
                    }
                }.scan(unit, null);
            }
        }
        assertTrue(violations.isEmpty(), "Caught exceptions must be passed to the logger: " + violations);
    }

    private static boolean isLogCall(MethodInvocationTree node) {
        if (!(node.getMethodSelect() instanceof MemberSelectTree method)) {
            return false;
        }
        return Set.of("log", "logger", "LOG", "LOGGER").contains(method.getExpression().toString())
            && Set.of("trace", "debug", "info", "warn", "error", "fatal").contains(method.getIdentifier().toString());
    }

    private static boolean references(Tree tree, String name) {
        boolean[] found = {false};
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void unused) {
                found[0] |= node.getName().contentEquals(name);
                return super.visitIdentifier(node, unused);
            }
        }.scan(tree, null);
        return found[0];
    }

    private static boolean passesThrowable(MethodInvocationTree node, String name) {
        if (node.getArguments().size() < 2) {
            return false;
        }
        String lastArgument = node.getArguments().getLast().toString().replaceAll("\\s+", "");
        return lastArgument.equals(name) || lastArgument.equals(name + ".getCause()");
    }
}
