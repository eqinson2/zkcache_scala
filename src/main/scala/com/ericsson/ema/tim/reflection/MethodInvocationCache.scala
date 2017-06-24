package com.ericsson.ema.tim.reflection

import java.beans.Introspector
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import com.ericsson.ema.tim.exception.DmlNoSuchFieldException
import com.ericsson.ema.tim.reflection.AccessType.AccessType

/**
  * Created by eqinson on 2017/5/5.
  */
class MethodInvocationCache {
	private[this] val getterStore = new ConcurrentHashMap[MethodInvocationKey, Method]
	private[this] val setterStore = new ConcurrentHashMap[MethodInvocationKey, Method]
	private[this] val lock = new ReentrantLock

	def cleanup(): Unit = {
		getterStore.clear()
		setterStore.clear()
	}

	private[this] def lookup(clz: Class[_], property: String, accessType: AccessType): Method = {
		val beanInfo = Introspector.getBeanInfo(clz)
		beanInfo.getPropertyDescriptors.toList.filter(property == _.getName).map(
			p => if (accessType eq AccessType.GET) p.getReadMethod else p.getWriteMethod)
		match {
			case h :: _ => h
			case _      => throw DmlNoSuchFieldException(property)
		}
	}

	def get(clz: Class[_], field: String, accessType: AccessType.AccessType): Method = {
		val key = new MethodInvocationKey(clz, field)
		val store = if (accessType == AccessType.GET) getterStore else setterStore
		Option(store.get(key)) match {
			case Some(cached) => cached
			case None         =>
				lock.lock()
				try {
					Option(store.get(key)) match {
						case Some(cached) => cached
						case None         =>
							val cached = lookup(clz, field, accessType)
							store.put(key, cached)
							cached
					}
				}
				finally {
					lock.unlock()
				}
		}
	}

	private[this] class MethodInvocationKey(val lookupClass: Class[_], val methodName: String) {
		require(Option(lookupClass).isDefined && Option(methodName).isDefined)

		private[this] val hashcode: Int = 31 * lookupClass.hashCode + methodName.hashCode

		override def equals(o: Any): Boolean = {
			o match {
				case that: MethodInvocationKey => (this eq that) ||
					(lookupClass == that.lookupClass) && (methodName == that.methodName)
				case _                         => false
			}
		}

		override def hashCode(): Int = this.hashcode
	}

}

object AccessType extends Enumeration {
	type AccessType = Value
	val GET, SET = Value
}