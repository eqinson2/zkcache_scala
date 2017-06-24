package com.ericsson.ema.tim.pojo

import java.io.Serializable
import javassist._

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * Created by eqinson on 2017/5/8.
  */
object PojoGenUtil {
	private[this] val LOGGER = LoggerFactory.getLogger(PojoGenUtil.getClass)

	def generatePojo(className: String, properties: mutable.Map[String, Class[_]]): Class[_] = {
		val cc = makeClass(className)
		cc.addInterface(resolveCtClass(classOf[Serializable]))
		cc.addInterface(resolveCtClass(classOf[Cloneable]))
		properties.foreach(kv => {
			Try {
				val field = new CtField(resolveCtClass(kv._2), kv._1, cc)
				field.setModifiers(Modifier.PRIVATE)
				cc.addField(field)
				cc.addMethod(generatePlainGetter(cc, kv._1, kv._2))
				cc.addMethod(generateSetter(cc, kv._1, kv._2))
			} match {
				case Success(_)  =>
				case Failure(ex) => LOGGER.error("PojoGenUtil.generatePojo error: " + ex.getMessage)
					throw new RuntimeException(ex)
			}
		})
		cc.addMethod(generateToString(cc))
		cc.addMethod(generateClone(cc, className))
		cc.toClass(new Loader)
	}

	def generateListPojo(className: String, properties: Map[String, Class[_]]): Class[_] = {
		val cc = makeClass(className)
		cc.addInterface(resolveCtClass(classOf[Serializable]))
		properties.foreach(kv => {
			Try {
				val field = new CtField(resolveCtClass(kv._2), kv._1, cc)
				field.setModifiers(Modifier.PRIVATE)
				cc.addField(field)
				cc.addMethod(generateListGetter(cc, kv._1, kv._2))
			} match {
				case Success(_)  =>
				case Failure(ex) => LOGGER.error("PojoGenUtil.generateListPojo error: " + ex.getMessage)
					throw new RuntimeException(ex)
			}
		})

		//make sure share same classloeader with generatePojo
		cc.toClass(Thread.currentThread.getContextClassLoader)
	}

	private[this] def generatePlainGetter(declaringClass: CtClass, fieldName: String, fieldClass: Class[_]): CtMethod = {
		val getterName = "get" + fieldName.substring(0, 1).toUpperCase + fieldName.substring(1)
		val sb = String.format("public %s %s() { return this.%s; }", fieldClass.getName, getterName, fieldName)
		LOGGER.debug("generatePlainGetter:{}", sb)
		CtMethod.make(sb, declaringClass)
	}

	private[this] def generateListGetter(declaringClass: CtClass, fieldName: String, fieldClass: Class[_]): CtMethod = {
		val getterName = "get" + fieldName.substring(0, 1).toUpperCase + fieldName.substring(1)
		val sb = String.format("public %s %s() { if (%s == null) { %s = new java.util.ArrayList(); } return this.%s; }", fieldClass.getName, getterName, fieldName, fieldName, fieldName)
		LOGGER.debug("generateListGetter:{}", sb)
		CtMethod.make(sb, declaringClass)
	}

	private[this] def generateSetter(declaringClass: CtClass, fieldName: String, fieldClass: Class[_]): CtMethod = {
		val setterName = "set" + fieldName.substring(0, 1).toUpperCase + fieldName.substring(1)
		val sb = String.format("public void %s(%s %s) { this.%s = %s; }", setterName, fieldClass.getName, fieldName, fieldName, fieldName)
		LOGGER.debug("generateSetter:{}", sb)
		CtMethod.make(sb, declaringClass)
	}

	private[this] def generateToString(declaringClass: CtClass): CtMethod = {
		val toStringBody = declaringClass.getDeclaredFields.toList.map("\"{\"+String.valueOf(" + _.getName + ")+\"}\"").foldLeft("return \"\"")(_ + " + " + _) + ";"
		val sb = String.format("public String toString() { %s }", toStringBody)
		LOGGER.debug("generateToString:{}", sb)
		CtMethod.make(sb, declaringClass)
	}

	private def generateClone(declaringClass: CtClass, className: String) = {
		val cloneBody = String.format("return (%s) super.clone();", className)
		val sb = String.format("public %s clone() { %s }", className, cloneBody)
		LOGGER.debug("generateToString:{}", sb)
		CtMethod.make(sb, declaringClass)
	}

	private[this] def resolveCtClass(clazz: Class[_]) = {
		ClassPool.getDefault.get(clazz.getName)
	}

	private[this] def makeClass(className: String) = {
		val pool = ClassPool.getDefault
		if (Option(pool.getOrNull(className)).isEmpty)
			pool.makeClass(className)
		else {
			val ccOld = pool.get(className)
			ccOld.defrost()
			pool.makeClass(className)
		}
	}
}
