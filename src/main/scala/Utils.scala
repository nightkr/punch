package se.nullable

package object punch {
	class OptPartialFunction[A, B](val f: A => Option[B]) extends PartialFunction[A, B] {
		def apply(a: A): B = f(a).get
		def isDefinedAt(a: A): Boolean = !f(a).isEmpty
	}

	implicit class OptFunction[A, B](val f: A => Option[B]) extends AnyVal {
		def partial: PartialFunction[A, B] = new OptPartialFunction(f)
	}
}