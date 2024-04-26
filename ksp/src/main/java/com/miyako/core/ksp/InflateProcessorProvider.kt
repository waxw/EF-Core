package com.miyako.core.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class InflateProcessorProvider: SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    environment.logger.warn("create provider")
    return InflateProcessor(environment.logger)
    // return MiyakoFunctionProcessor(environment.logger)
  }
}
