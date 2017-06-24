package com.ericsson.ema.tim.dml.order

/**
  * Created by eqinson on 2017/5/13.
  */
trait ChainableOrderings {

	class ChainableOrdering[T](outer: Ordering[T]) {
		def thenOrdering(next: Ordering[T]): Ordering[T] = new Ordering[T] {
			def compare(t1: T, t2: T): Int = {
				val first = outer.compare(t1, t2)
				if (first != 0) first else next.compare(t1, t2)
			}
		}
	}

	implicit def chainOrdering[T](o: Ordering[T]): ChainableOrdering[T] = new ChainableOrdering[T](o)
}
