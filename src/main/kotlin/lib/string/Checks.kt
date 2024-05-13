package lib.string

val String.isInteger get() = all { it in '0'..'9' }
