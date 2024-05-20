package lib.bigdecimal

object BigD {
    inline val Iterable<Double>.big get() = map { it.toBigDecimal() }
}

object BigF {
    inline val Iterable<Float>.big get() = map { it.toBigDecimal() }
}

object BigI {
    inline val Iterable<Int>.big get() = map { it.toBigDecimal() }
}

object BigL {
    inline val Iterable<Long>.big get() = map { it.toBigDecimal() }
}