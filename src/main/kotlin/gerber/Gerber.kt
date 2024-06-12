package gerber

import java.math.BigDecimal

class Gerber {
    var fileUnit = MeasuringUnit.Millimeter
    val buildingBlocks = mutableListOf<GerberBuildingBlock>()
    var decimalDigits = 6
    var integerDigits = 6
    var plottingMode: PlottingMode? = null
    var currentPolarity: Polarity? = null
    var currentMirrorType: MirrorType? = null
    var currentRotation: Double? = null
    var currentScale: Double? = null

    inline operator fun <reified T : GerberBuildingBlock> T.unaryPlus() {
        buildingBlocks += this
    }

    val BigDecimal.gerberNumber get() =
        "${
            "%0${integerDigits}d".format(toBigInteger().intValueExact()).trimStart('0')
        }${
            "%.${decimalDigits}f".format(this).split(".").last()
        }".let { str ->
            if (str.all { it == '0' }) "0"
            else str
        }

    val Int.gerberNumber get() = toBigDecimal().gerberNumber
    val Double.gerberNumber get() = toBigDecimal().gerberNumber
    val Float.gerberNumber get() = toBigDecimal().gerberNumber

    override fun toString(): String {
        return (
                buildingBlocks + GerberCommand("M02*")
        ).joinToString("\n")
    }

    fun checksumRelevantString() =
        buildingBlocks.joinToString("").replace("\r", "").replace("\n", "")

    fun usingAperture(aperture: Int, block: Gerber.() -> Unit) {
        currentAperture = aperture
        block()
    }
}

inline fun gerber(block: Gerber.() -> Unit) = Gerber().apply(block)

val freeCharacter = "[^%*]"
val word = "($freeCharacter|$freeCharacter[*]\\n$freeCharacter)+[*]"
val wordRegex = Regex(word)
val wordCommand = word
val extendedCommand = "%$word+%"
val command = "($extendedCommand|$wordCommand)"
val commandRegex = Regex(command)
val name = "[._\$a-zA-Z][._\$a-zA-Z0-9]{0,126}"
val standardName = "\\.[._\$a-zA-Z][._\$a-zA-Z0-9]{0,125}"
val userDefinedName = "[_\$a-zA-Z][_.\$a-zA-Z0-9]{0,126}"
val nameRegex = Regex(name)

interface GerberBuildingBlock {
    override fun toString(): String
}

class GerberWord(val text: String) : GerberBuildingBlock {
    override fun toString() = text

    init {
        require(text matches wordRegex) {
            """"$text" is an invalid Gerber word"""
        }
    }
}

class GerberCommand(val text: String) : GerberBuildingBlock {
    override fun toString() = text

    init {
        require(text matches commandRegex) {
            """"$text" is an invalid Gerber command"""
        }
    }
}

class GerberStandardComment(val comment: String) : GerberBuildingBlock {
    override fun toString() = comment.lineSequence().joinToString(
        separator = "\n", prefix = "# "
    )
}

open class GerberString(val text: String) {
    override fun toString() = text
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("*", "\\*")
        .replace(",", "\\,")
}

class GerberField(text: String) : GerberString(text)

class GerberName(val name: String) {
    override fun toString() = name

    init {
        require(name matches nameRegex) {
            """"$name" is not a valid Gerber name"""
        }
    }
}

val String.gerberString get() = GerberString(this)
