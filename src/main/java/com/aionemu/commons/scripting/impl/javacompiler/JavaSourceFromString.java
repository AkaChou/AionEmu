package com.aionemu.commons.scripting.impl.javacompiler;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * Java字符串源代码对象，用于处理内存中的Java源代码
 * Java String Source Object for handling Java source code in memory
 *
 * 该类继承自SimpleJavaFileObject，主要功能包括：
 * 1. 存储内存中的源代码字符串
 * 2. 提供源代码的字符序列访问
 * 3. 支持从字符串创建Java源文件对象
 *
 * This class extends SimpleJavaFileObject and provides:
 * 1. Storing source code string in memory
 * 2. Providing character sequence access to source code
 * 3. Supporting creation of Java source file object from string
 */
public class JavaSourceFromString extends SimpleJavaFileObject {
    /**
     * 源代码字符串
     * Source code string
     */
    private final String code;

    /**
     * 构造函数
     * Constructor
     * 
     * @param className 类名 (Class name)
     * @param code 源代码 (Source code)
     */
    public JavaSourceFromString(String className, String code) {
      super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.code = code;
   }

   public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return this.code;
   }
}
