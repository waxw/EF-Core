package com.miyako.core.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSEmptyVisitor

class InflateProcessor(
  private val logger: KSPLogger,
): SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    logger.warn("process InflateViewBinding")
    val generatedAnnotations = mutableListOf<KSAnnotated>()

    // 不能使用 InflateViewBinding::class.java.toString()
    val annotationName = "com.miyako.core.ksp.InflateViewBinding"
    logger.warn("name: [${InflateViewBinding::class.java.name}, $annotationName]")
    val annotatedFunctions = resolver.getSymbolsWithAnnotation(InflateViewBinding::class.java.name)
      .filterIsInstance<KSFunctionDeclaration>()
    logger.warn("size: ${annotatedFunctions.toList().size}")


    annotatedFunctions.forEach { function ->
      logger.warn("function: ${function.simpleName.asString()}")
      val parameters = function.parameters
      val returnType = function.returnType?.resolve()
      logger.warn("param: $parameters")
      logger.warn("return: $returnType")
      // function.typeParameters 可以拿到内联方法声明的泛型参数名，拿不到具体类型
      logger.warn("return: ${function.typeParameters}")

      function.typeParameters.forEach {
        it.bounds
        logger.warn("type bounds: ${it.isReified}, ${it.qualifiedName?.asString()}")
      }
      function.accept(object : KSEmptyVisitor<Unit, Unit>() {
        override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
          logger.warn("visit value: $valueParameter")
          return defaultHandler(valueParameter, data)
        }

        override fun defaultHandler(node: KSNode, data: Unit) = Unit
      }, Unit)

      if (returnType?.declaration is KSTypeParameter) {
        logger.warn("return is KSTypeParameter: ${returnType.declaration is KSTypeParameter}")
        logger.warn("${returnType.declaration.typeParameters}")
        returnType.declaration.typeParameters.forEach {
          val specificType = it.bounds.toList()[0].resolve().declaration.qualifiedName
          logger.warn("Specific type for VB: $specificType")
        }
      }
    }

    return generatedAnnotations
  }
}
