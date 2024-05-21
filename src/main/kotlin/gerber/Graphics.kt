package gerber

import lib.bigdecimal.BigD
import lib.bigdecimal.BigD.big
import lib.string.isInteger
import java.math.BigDecimal

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

fun Gerber.unitMode() = +GerberCommand("%MO$fileUnit*%")

fun Gerber.formatSpecification() =
    +GerberCommand("%FSLAX$integerDigits${decimalDigits}Y$integerDigits$decimalDigits*%").also {
        require(
            integerDigits in 1..6 &&
            decimalDigits == 6
        ) {
            "integer digits must be 1..6 and decimal digits must be 6"
        }
    }

interface MacroBodyBlock : GerberBuildingBlock

class MacroBodyPrimitiveCode(
    private val code: Int,
    private val expressions: List<String>
) : MacroBodyBlock {
    override fun toString() = "$code${
        if (expressions.isNotEmpty()) {
            ",${expressions.joinToString(",")}"
        } else ""
    }*"
}

class MacroBodyVariableDefinition(
    private val name: GerberName,
    private val expression: GerberWord
) : MacroBodyBlock {
    init {
        require(name.toString().let { it.isInteger && it.toIntOrNull()?.let { int -> int != 0 } ?: false }) {
            "Invalid variable name $name, must be integer > 0"
        }
    }

    override fun toString() = "\$$name=$expression*"
}

class MacroBody : GerberBuildingBlock {
    private val blocks = mutableListOf<MacroBodyBlock>()

    fun variable(name: String, expression: String) {
        blocks += MacroBodyVariableDefinition(
            GerberName(name), GerberWord(expression)
        )
    }

    fun primitiveCode(code: Int, expressions: List<Any> = listOf()) {
        blocks += MacroBodyPrimitiveCode(
            code, expressions.map { formatAny(it) }
        )
    }

    override fun toString() = blocks.joinToString("")
}

private var nextApertureId = 10

fun Gerber.simpleAperture(templateName: String, parameters: List<Any> = listOf()) =
    apertureDefinition(templateName = templateName, parameters = parameters)

inline fun <reified N : Number?> Gerber.simpleAperture(templateName: String, vararg parameters: N) =
    simpleAperture(templateName, parameters.toList().filterNotNull())

private fun formatAny(any: Any) = when (any) {
    is BigDecimal, is Double, is Float -> "%.3f".format(any)
    is Long, is Int, is Short, is Byte -> "%d".format(any)
    is String -> any
    is GerberWord -> any.toString()
    else -> error("Unsupported type ${any::class}")
}

fun Gerber.apertureDefinition(
    id: Int = nextApertureId++,
    templateName: String,
    parameters: List<Any> = listOf(),
) = require(id >= 10) {
    "Aperture identification must be >= 10"
}.run {
    +GerberCommand(
        "%ADD$id${GerberName(templateName)}${
            if (parameters.isNotEmpty()) {
                ",${formatAny(parameters.first())}${
                    parameters.drop(1).joinToString(
                        separator = "X",
                    ) { formatAny(it) }.let { 
                        if (it.isNotEmpty()) "X$it"
                        else ""
                    }
                }"
            } else ""
        }*%"
    )

    id
}

fun Gerber.apertureMacroDefinition(
    id: Int = nextApertureId++,
    name: String,
    parameters: List<Any> = listOf(),
    body: MacroBody.() -> Unit,
) = apertureDefinition(
    id, apertureMacro(name, body), parameters
)

fun Gerber.apertureMacro(name: String, body: MacroBody.() -> Unit): String {
    val finalName = GerberName(name)
    val macroBody = MacroBody().apply(body)
    +GerberCommand(
        "%AM$finalName*\n$macroBody%"
    )
    return finalName.toString()
}

fun Gerber.apertureBlock(
    id: Int = nextApertureId++,
    block: Gerber.() -> Unit
) {
    +GerberCommand("%ABD$id*%")
    block()
    +GerberCommand("%AB*%")
}

var Gerber.currentAperture: Int
    get() = error("currentAperture is setter-only")
    set (value) {
        require(value >= 10) {
            "Aperture number must be > 9"
        }
        +GerberCommand("D$value*")
    }


enum class PlottingMode {
    Linear, Circular
}

fun Gerber.linearPlotting(aperture: Int? = null, block: Gerber.() -> Unit) {
    aperture?.let { currentAperture = it }
    +GerberCommand("G01*")
    plottingMode = PlottingMode.Linear
    block()
}

fun Gerber.circularPlotting(
    clockwise: Boolean = true,
    block: Gerber.() -> Unit,
) {
    +GerberCommand(if (clockwise) "G02*" else "G03*")
    +GerberCommand("G75*")
    plottingMode = PlottingMode.Circular
    block()
}

fun Gerber.plot(
    x: Int? = null,
    y: Int? = null,
    xOffset: Int? = null,
    yOffset: Int? = null,
) = plot(x?.toDouble(), y?.toDouble(), xOffset?.toDouble(), yOffset?.toDouble())

fun Gerber.plot(
    x: Double? = null,
    y: Double? = null,
    xOffset: Double? = null,
    yOffset: Double? = null,
) {
    +GerberCommand(
        """${
            x?.let { "X${x.gerberNumber}" } ?: ""
        }${
            y?.let { "Y${y.gerberNumber}" } ?: ""
        }${
            if (plottingMode == PlottingMode.Circular) {
                xOffset?.let { "I$xOffset" } ?: error("x-offset required for circular")
            } else ""
        }${
            if (plottingMode == PlottingMode.Circular) {
                yOffset?.let { "J$yOffset" } ?: error("y-offset required for circular")
            } else ""
        }D01*"""
    )
}

fun Gerber.move(
    x: Int? = null,
    y: Int? = null,
) = move(x?.toDouble(), y?.toDouble())


fun Gerber.move(
    x: Double? = null,
    y: Double? = null,
) {
    +GerberCommand(
        """${
            x?.let { "X${x.gerberNumber}" } ?: ""
        }${
            y?.let { "Y${y.gerberNumber}" } ?: ""
        }D02*"""
    )
}

fun Gerber.flash(
    x: Int? = null,
    y: Int? = null,
) = flash(x?.toDouble(), y?.toDouble())

fun Gerber.flash(
    x: Double? = null,
    y: Double? = null,
) {
    +GerberCommand(
        """${
            x?.let { "X${x.gerberNumber}" } ?: ""
        }${
            y?.let { "Y${y.gerberNumber}" } ?: ""
        }D03*"""
    )
}

enum class Polarity(val polarityString: String) {
    Dark("D"), Clear("C")
}

fun Gerber.polarity(polarity: Polarity, block: Gerber.() -> Unit) {
    val lastPolarity = currentPolarity
    +GerberCommand("%LP${polarity.polarityString}*%")
    currentPolarity = polarity

    block()

    lastPolarity?.let { pol ->
        +GerberCommand("%LP$pol*%")
    }
    currentPolarity = lastPolarity
}

fun Gerber.region(block: Gerber.() -> Unit) {
    +GerberCommand("G36*")
    block()
    +GerberCommand("G37*")
}

enum class MirrorType(val mirrorString: String) {
    None("N"), X("X"),
    Y("Y"), XY("XY")
}

fun Gerber.mirror(mirrorType: MirrorType, block: Gerber.() -> Unit) {
    val lastMirrorType = currentMirrorType
    +GerberCommand("%LM${mirrorType.mirrorString}*%")
    currentMirrorType = mirrorType

    block()

    lastMirrorType?.let { mir ->
        +GerberCommand("%LM$mir*%")
    }
    currentMirrorType = lastMirrorType
}

/**
 * Rotates counterclockwise (4.9.4)
 */
fun Gerber.rotate(degrees: Double, block: Gerber.() -> Unit) {
    val lastRotation = currentRotation
    +GerberCommand("%LR${formatAny(degrees)}*%")
    currentRotation = degrees

    block()

    lastRotation?.let { rot ->
        +GerberCommand("%LR${formatAny(rot)}*%")
    }
    currentRotation = lastRotation
}

fun Gerber.scale(scaleBy: Double, block: Gerber.() -> Unit) {
    require(scaleBy > .0) {
        "Scale must be > 0"
    }
    val lastScale = currentScale
    +GerberCommand("%LS${formatAny(scaleBy)}*%")
    currentScale = scaleBy

    block()

    lastScale?.let { scale ->
        +GerberCommand("%LS${formatAny(scale)}*%")
    }
    currentScale = lastScale
}

fun Gerber.stepRepeat(
    x: Int = 1,
    y: Int = 1,
    xDistance: Double = .0,
    yDistance: Double = .0,
    block: Gerber.() -> Unit,
) {
    require(x > 0) { "x repeats must be > 0"}
    require(y > 0) { "y repeats must be > 0"}
    require(xDistance >= 0) { "x distance must be positive"}
    require(yDistance >= 0) { "y distance must be positive"}

    +GerberCommand("%SRX${x}Y${y}I${formatAny(xDistance)}J${formatAny(yDistance)}*%")
    block()
    +GerberCommand("%SR*%")
}

