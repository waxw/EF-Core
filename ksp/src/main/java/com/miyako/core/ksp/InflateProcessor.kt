package com.miyako.core.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class InflateProcessor(
  private val logger: KSPLogger,
): SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    logger.info("process")
    val generatedAnnotations = mutableListOf<KSAnnotated>()

    val annotatedFunctions = resolver.getSymbolsWithAnnotation(InflateViewBinding::class.java.toString())
      .filterIsInstance<KSFunctionDeclaration>()

    annotatedFunctions.forEach { function ->
      val parameters = function.parameters
      val returnType = function.returnType?.resolve()?.declaration?.qualifiedName

      // 检查参数中的泛型类型
      val genericType = parameters.firstOrNull {
        it.type.resolve().declaration.qualifiedName != null
      }
      // ?.type?.resolve()?.typeArguments?.firstOrNull()?.resolve()?.declaration?.qualifiedName?.asString()

      // // 生成替换的调用形式
      // val methodName = "process" // 方法名
      // val args = parameters.joinToString(", ") { it.name.asString() } // 参数列表
      // val generatedCode = "$genericType.$methodName($args)" // 生成代码
      // println("Generated code: $generatedCode")
      logger.info("type: $genericType")
      // println("type: $genericType")
    }

    return generatedAnnotations
  }
}
