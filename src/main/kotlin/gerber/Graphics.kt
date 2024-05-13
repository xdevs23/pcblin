package gerber

import lib.string.isInteger
import java.math.BigDecimal
import kotlin.math.exp

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

interface MacroBodyBlock : GerberBuildingBlock

class MacroBodyPrimitiveCode(
    private val code: GerberWord,
    private val expressions: List<GerberWord>
) : MacroBodyBlock {
    override fun toString() = "$code${
        if (expressions.isNotEmpty()) {
            expressions.joinToString()
        } else ""
    }"
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

    fun primitiveCode(code: String, expressions: List<String> = listOf()) {
        blocks += MacroBodyPrimitiveCode(
            GerberWord(code), expressions.map { GerberWord(it) }
        )
    }

    override fun toString() = blocks.joinToString("")
}

private var nextApertureId = 0

fun Gerber.apertureDefinition(
    id: Int = nextApertureId++,
    templateName: String? = null,
    template: MacroBody.() -> Unit = {},
    parameters: List<BigDecimal> = listOf()
) = require(id >= 10) {
    "Aperture identification must be >= 10"
}.run {
    val finalTemplateName = GerberName(
        templateName ?: apertureMacro(GerberName("tpl$id").toString(), template)
    )
    +GerberCommand(
        "%AD$id$finalTemplateName${
            if (parameters.isNotEmpty()) {
                ",${parameters.first().gerberNumber}${
                    parameters.joinToString(
                        separator = "X",
                    ) { it.gerberNumber }
                }"
            } else ""
        }"
    )

    finalTemplateName.toString()
}

fun Gerber.apertureMacro(name: String, body: MacroBody.() -> Unit): String {
    val finalName = GerberName(name)
    val macroBody = MacroBody().apply(body)
    +GerberCommand(
        "%AM$finalName*$macroBody%"
    )
    return finalName.toString()
}