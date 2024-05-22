package com.miyako.core.ksp.mvi

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT

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
      logger.error("@DispatcherFunction must only one in class")
    } else {
      // 需要导入的包
      val importClassList = mutableListOf<ClassName>()
      val dispatcherAction = dispatchFunctions.first().annotations.first {
        DispatchAction::class.qualifiedName == it.annotationType.resolve().declaration.qualifiedName?.asString()
      }

      var parameterSpec: ClassName = UNIT
      var returnSpec: ClassName = UNIT
      dispatcherAction.arguments.forEach {
        val declaration = (it.value as? KSType)?.declaration
        logger.warn("dec: $declaration")
        when (it.name?.asString()) {
          DispatchAction::param.name -> {
            ClassName.bestGuess(declaration!!.qualifiedName!!.asString()).also {
              importClassList.add(it)
              parameterSpec = it
              logger.warn("param: $it")
            }
          }

          DispatchAction::returnType.name -> {
            ClassName.bestGuess(declaration!!.qualifiedName!!.asString()).also {
              importClassList.add(it)
              returnSpec = it
              logger.warn("return: $it")
            }
          }
        }
      }
      val actionFunctionList =
        classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(Action::class) }

      val ktFileBuilder = FileSpec.builder(packageName, "$className.kt")

      val classBuilder = TypeSpec.classBuilder(className)

      val clazz = ClassName(classDeclaration.packageName.asString(), classDeclaration.simpleName.asString())

      val interfaceSpec = ClassName("com.miyako.core.ksp.mvi", "IDispatcher")

      classBuilder.addSuperinterface(interfaceSpec.parameterizedBy(listOf(parameterSpec, returnSpec)))

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

      val paramName = "arg"

      val functionBuilder = FunSpec.builder("dispatch")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(paramName, parameterSpec)
        .returns(returnSpec)

      val codeBuilder = CodeBlock.builder()
      codeBuilder.beginControlFlow("return when ($paramName)")
      actionFunctionList.forEach {
        val function = it.simpleName.asString()
        val action = it.parameters.firstOrNull()?.type?.resolve()?.declaration?.let {
          ClassName.bestGuess(it.qualifiedName!!.asString()).also {
            importClassList.add(it)
          }
        }
        logger.warn("action: $function, ${it.parameters}")
        if (action != null) {
          codeBuilder.beginControlFlow("is %L -> ", action.simpleName)
          codeBuilder.add("target.%L(%L)", function, paramName)
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
        ktFileBuilder.addImport(it, "")
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
