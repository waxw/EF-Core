package com.miyako.core.ksp.mvi

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

class MviActionProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    logger.warn("process DispatchAction")
    val ret = mutableListOf<KSAnnotated>()

    val dispatcherFunctions = resolver.getSymbolsWithAnnotation(DispatchAction::class.java.name)
      .filterIsInstance<KSFunctionDeclaration>().map {
        it.validate()
        it.simpleName.asString()
      }
    logger.warn("func: ${dispatcherFunctions.joinToString(", ")}")

    //返回无法处理的符号
    resolver.getSymbolsWithAnnotation(DelegateDispatch::class.java.name).forEach { symbol ->
      if (!symbol.validate())
        ret.add(symbol)
      else
        symbol.accept(DispatcherDelegateCreator(codeGenerator, logger), Unit)//处理符号
    }

    return ret
  }
}
