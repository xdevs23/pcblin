package gerber

// Cartesian coordinates where, viewed from the front,
// the X axis points to the right → while the Y axis points upwards ↑
//
//   ^
//   | Front of PCB
//   |
//   |
// Y |
//   +---------------->
//    X

// Coordinates are shared across layers

// Positive rotation is counterclockwise

enum class MeasuringUnit(private val unit: String) {
    Millimeter("MM"),
    @Deprecated("Use metric. Imperial is historic and will be deprecated at a future date.")
    Inch("IN");

    override fun toString() = unit
}

fun Gerber.unitMode() = +GerberCommand("%MO$fileUnit%")

fun Gerber.formatSpecification(integerDigits: Int, decimalDigits: Int) =
    +GerberCommand("%FSLAX$integerDigits${decimalDigits}Y$integerDigits$decimalDigits").also {
        require(
            integerDigits in 1..6 &&
            decimalDigits == 6
        ) {
            "integer digits must be 1..6 and decimal digits must be 6"
        }
    }