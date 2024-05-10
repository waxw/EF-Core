package com.miyako.core.ksp.mvi

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class MviActionProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    logger.warn("process DispatchAction")
    val generatedAnnotations = mutableListOf<KSAnnotated>()

    val annotatedFunctions = resolver.getSymbolsWithAnnotation(DispatchAction::class.java.name)
      .filterIsInstance<KSFunctionDeclaration>()
    logger.warn("size: ${annotatedFunctions.toList().size}")


    annotatedFunctions.forEach { function ->
      val sb = StringBuilder("function: ${function.qualifiedName?.asString()}")
      val parameters = function.parameters
      parameters.forEach {
        sb.append(", ").append("param: $it, ").append("class: ${it.type}")
      }
      logger.warn(sb.toString())
    }

    return generatedAnnotations
  }
}
