package com.aionemu.commons.scripting.impl.javacompiler;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 编译错误监听器，用于处理Java编译过程中的诊断信息
 * Compilation Error Listener for handling diagnostic messages during Java compilation
 *
 * 该类实现DiagnosticListener接口，主要功能包括：
 * 1. 接收编译器的诊断信息
 * 2. 格式化错误信息
 * 3. 通过日志系统输出编译错误
 *
 * This class implements DiagnosticListener interface and provides:
 * 1. Receiving compiler diagnostic messages
 * 2. Formatting error messages
 * 3. Outputting compilation errors through logging system
 */
public class ErrorListener implements DiagnosticListener<JavaFileObject> {
    /**
     * 日志记录器实例
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ErrorListener.class);

   public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
      StringBuilder sb = new StringBuilder();
      sb.append("Java Compiler ");
      sb.append(diagnostic.getKind());
      sb.append(": ");
      sb.append(diagnostic.getMessage(Locale.ENGLISH));
      if (diagnostic.getSource() != null) {
         sb.append("\n");
         sb.append("Source: ");
         sb.append(((JavaFileObject)diagnostic.getSource()).getName());
         sb.append("\n");
         sb.append("Line: ");
         sb.append(diagnostic.getLineNumber());
         sb.append("\n");
         sb.append("Column: ");
         sb.append(diagnostic.getColumnNumber());
      }

      log.error(sb.toString());
   }
}
