package com.miyako.core.ksp.mvi

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class DispatcherDelegateCreator(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) :
  KSVisitorVoid() {

  @OptIn(KspExperimental::class)
  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val packageName = classDeclaration.containingFile?.packageName!!.asString()
    val className = classDeclaration.simpleName.asString() + "Dispatcher"

    logger.warn("size: ${classDeclaration.getDeclaredFunctions().toList().size}")
    val dispatchFunctions =
      classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(DispatchAction::class) }.toList()
    if (dispatchFunctions.size != 1) {
      logger.warn("@DispatcherFunction must only one in class")
    } else {
      val function = dispatchFunctions.first()
      logger.warn("func: $packageName.$className.${function.qualifiedName?.asString() ?: "none"}")
      val actionFunctionList =
        classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(Action::class) }

      val importClassList = mutableListOf<Pair<String, String>>()
      val parameterSpecList = function.parameters.map {
        val paramName = it.name?.asString() ?: "any"
        val paramType = it.type.resolve().declaration.let {
          val pair = it.packageName.asString() to it.simpleName.asString()
          logger.warn("pkg: ${it.qualifiedName?.asString()}")
          importClassList.add(it.qualifiedName!!.asString() to it.simpleName.asString())
          ClassName(pair.first, pair.second)
        }
        ParameterSpec.builder(paramName, paramType).build()
      }
      val ktFileBuilder = FileSpec.builder(packageName, "$className.kt")

      val classBuilder = TypeSpec.classBuilder(className)

      val clazz = ClassName(classDeclaration.packageName.asString(), classDeclaration.simpleName.asString())

      // 构造方法字段
      val constructor = FunSpec.constructorBuilder()
        .addParameter(
          ParameterSpec.builder(
            "target",
            clazz
          ).build()
        )

      classBuilder.primaryConstructor(constructor.build())
        .addProperty(
          PropertySpec.builder("target", clazz)
            .addModifiers(KModifier.PRIVATE).initializer("target").build()
        )

      val functionBuilder = FunSpec.builder("dispatchFunction")
        .addParameters(parameterSpecList)

      val codeBuilder = CodeBlock.builder()
      codeBuilder.beginControlFlow("when (${parameterSpecList.first().name})")
      actionFunctionList.forEach {
        logger.warn("action: ${it.simpleName}, ${it.parameters}")
        val function = it.simpleName.asString()
        val action = it.parameters.firstOrNull()?.type?.resolve()?.declaration?.let {
          importClassList.add(it.qualifiedName!!.asString() to it.simpleName.asString())
          ClassName(it.packageName.asString(), it.simpleName.asString())
        }
        if (action != null) {
          codeBuilder.beginControlFlow("is %L -> ", action.simpleName)
          codeBuilder.add("target.%L(%L)", function, parameterSpecList.map { it.name }.joinToString())
          codeBuilder.endControlFlow()
        }
      }
      codeBuilder.beginControlFlow("else ->").endControlFlow()
      codeBuilder.endControlFlow()

      classBuilder.addFunction(
        functionBuilder.addCode(
          codeBuilder.build()
        ).build()
      )
      importClassList.forEach {
        ktFileBuilder.addImport(ClassName.bestGuess(it.first), "")
      }
      ktFileBuilder
        .addType(classBuilder.build())
        .build().let {
          codeGenerator.createNewFile(
            dependencies = Dependencies(
              true,
              classDeclaration.containingFile!!
            ),
            packageName = packageName,
            fileName = className,
            extensionName = "kt"
          ).bufferedWriter().use { file ->
            it.writeTo(file)
          }
        }
    }
  }
}
