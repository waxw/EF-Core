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

    logger.warn("size: ${classDeclaration.getDeclaredFunctions().toList().size}")

    // 需要导入的包
    val importClassList = mutableListOf<ClassName>()
    val listDispatch = mutableListOf<Pair<KSType?, KSType?>>()

    val buildClass = mutableListOf<String>()

    classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(DispatchAction::class) }.forEach {
      var paramType: KSType? = null
      var returnType: KSType? = null
      var parameterSpec: ClassName = UNIT
      var returnSpec: ClassName = UNIT
      it.annotations.find {
        DispatchAction::class.qualifiedName == it.annotationType.resolve().declaration.qualifiedName?.asString()
      }?.arguments?.forEach {
        val declaration = (it.value as? KSType)?.declaration
        when (it.name?.asString()) {
          DispatchAction::param.name -> {
            paramType = (it.value as? KSType)
            ClassName.bestGuess(declaration!!.qualifiedName!!.asString()).also {
              importClassList.add(it)
              parameterSpec = it
              logger.warn("param: $it")
            }
          }

          DispatchAction::returnType.name -> {
            returnType = (it.value as? KSType)
            ClassName.bestGuess(declaration!!.qualifiedName!!.asString()).also {
              importClassList.add(it)
              returnSpec = it
              logger.warn("return: $it")
            }
          }
        }
      }
      listDispatch.add(paramType to returnType)
      val className =
        classDeclaration.simpleName.asString() + "_${parameterSpec.simpleName}${returnSpec.simpleName}Dispatcher"
      buildClass.add("${classDeclaration.qualifiedName?.asString()}_${parameterSpec.simpleName}${returnSpec.simpleName}Dispatcher")

      val actionFunctionList =
        classDeclaration.getDeclaredFunctions().filter { it.isAnnotationPresent(Action::class) }

      val ktFileBuilder = FileSpec.builder(packageName, "$className.kt")

      val classBuilder = TypeSpec.classBuilder(className)

      val clazz = ClassName(classDeclaration.packageName.asString(), classDeclaration.simpleName.asString())

      val interfaceSpec = ClassName("com.miyako.core.ksp.mvi", "IDelegateDispatcher")

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
      actionFunctionList
        .filter {
          if (it.parameters.size == 1 && it.returnType != null) {
            val actionP = it.parameters.first()
            val actionR = it.returnType
            val msg = "fun ${it.simpleName.asString()}" +
              "(${actionP.name?.asString()} ${actionP.type.resolve().declaration.qualifiedName?.asString()}): " +
              "${actionR?.resolve()?.declaration?.qualifiedName?.asString()}"
            logger.warn(msg)
            // 检查是否为目标类型或者及其子类
            paramType?.isAssignableFrom(actionP.type.resolve()) == true && returnType?.isAssignableFrom(actionR!!.resolve()) == true
          } else false
        }.forEach {
          val function = it.simpleName.asString()
          val action = it.parameters.firstOrNull()?.type?.resolve()?.declaration?.let {
            ClassName.bestGuess(it.qualifiedName!!.asString()).also {
              importClassList.add(it)
            }
          }
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
    val buildClassFile = "${classDeclaration.simpleName.asString()}_Build"
    val build = TypeSpec.objectBuilder(buildClassFile)
    buildClass.forEach {
      logger.warn("generate class: $it")
      val key = it.split("_")[1]
      build.addProperty(PropertySpec.builder(key, String::class, KModifier.PUBLIC).initializer("%S", it).build())

    }
    FileSpec.builder(packageName, ".kt")
      .addType(build.build())
      .build().let {
        codeGenerator.createNewFile(
          dependencies = Dependencies(
            true,
            classDeclaration.containingFile!!
          ),
          packageName = packageName,
          fileName = buildClassFile,
          extensionName = "kt"
        ).bufferedWriter().use { file ->
          it.writeTo(file)
        }
      }
  }
}
