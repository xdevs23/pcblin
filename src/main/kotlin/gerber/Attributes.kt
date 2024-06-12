package gerber

import lib.cryptography.md5
import lib.string.isInteger
import java.math.BigDecimal
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlin.math.min

fun Gerber.fileAttribute(
    attributeName: String,
    vararg values: String?,
) {
    +GerberCommand(
        "%TF${GerberName(attributeName)}${
            values.filterNotNull().joinToString(",", prefix = ",")
                .trim(',')
        }*%"
    )
}

fun Gerber.apertureAttribute(
    attributeName: String,
    vararg values: String?,
) {
    +GerberCommand(
        "%TA${GerberName(attributeName)}${
            values.filterNotNull().joinToString(",", prefix = ",")
                .trim(',')
        }*%"
    )
}

fun Gerber.objectAttribute(
    attributeName: String,
    vararg values: String?,
) {
    +GerberCommand(
        "%TO${GerberName(attributeName)}${
            values.filterNotNull().joinToString(",", prefix = ",")
                .trim(',')
        }*%"
    )
}

fun Gerber.deleteAttribute(
    attributeName: String,
) {
    +GerberCommand(
        "%TD${GerberName(attributeName)}*%"
    )
}


fun Gerber.deleteAllAttributes() {
    +GerberCommand("%TD*%")
}

object StandardAttributes {
    object File {
        const val PART = ".Part"
        const val FILE_FUNCTION = ".FileFunction"
        const val FILE_POLARITY = ".FilePolarity"
        const val SAME_COORDINATES = ".SameCoordinates"
        const val CREATION_DATE = ".CreationDate"
        const val PROJECT_ID = ".ProjectId"
        const val MD5 = ".MD5"
        const val GENERATION_SOFTWARE = ".GenerationSoftware"
    }
    object Aperture {
        const val APERTURE_FUNCTION = ".AperFunction"
        const val DRILL_TOLERANCE = ".DrillTolerance"
        const val FLASH_TEXT = ".FlashText"
    }
    object GraphicalObject {
        const val CAD_NET_NAME = ".N"
        const val PIN_NUMBER = ".P"
        const val COMPONENT_REFERENCE_DESIGNATOR = ".C"
    }
}

enum class PartAttribute {
    Single, Array, FabricationPanel, Coupon, Other
}

fun Gerber.applyPartAttribute(attr: PartAttribute, other: String? = null) {
    require(attr != PartAttribute.Other || other != null) {
        "When attr is set to Other, the other String must be provided"
    }
    fileAttribute(StandardAttributes.File.PART, attr.name, other)

}

enum class CopperLayerPosition(val grbStr: String) {
    Top("Top"), Inner("Inr"), Bottom("Bot")
}

enum class CopperLayerType {
    Plane, Signal, Mixed, Hatched
}

object FileFunctions {
    object DataLayers {
        const val COPPER = "Copper"
        const val PLATED = "Plated"
        const val NON_PLATED = "NonPlated"
        const val PROFILE = "Profile"
        const val SOLDER_MASK = "Soldermask"
        const val LEGEND = "Legend"
        const val COMPONENT = "Component"
        const val PASTE = "Paste"
        const val GLUE = "Glue"
        const val CARBON_MASK = "Carbonmask"
        const val GOLD_MASK = "Goldmask"
        const val HEAT_SINK_MASK = "Heatsinkmask"
        const val PEELABLE_MASK = "Peelablemask"
        const val SILVER_MASK = "Silvermask"
        const val TIN_MASK = "Tinmask"
        const val DEPTH_ROUT = "Depthrout"
        const val VCUT = "Vcut"
        const val VIA_FILL = "Viafill"
        const val PADS = "Pads"
        const val OTHER = "Other"
    }
    object DrawingLayers {
        const val DRILL_MAP = "Drillmap"
        const val FABRICATION_DRAWING = "FabricationDrawing"
        const val VCUT_MAP = "Vcutmap"
        const val ASSEMBLY_DRAWING = "AssemblyDrawing"
        const val ARRAY_DRAWING = "ArrayDrawing"
        const val OTHER_DRAWING = "OtherDrawing"
    }
}

fun Gerber.fileFunctionCopper(
    layerNumber: Int,
    layerPosition: CopperLayerPosition,
    layerType: CopperLayerType? = null,
) {
    require(layerNumber > 0) { "Layer number must be > 0" }
    require(layerPosition == CopperLayerPosition.Top || layerNumber != 1) {
        "L1 should be the top layer"
    }
    require(layerPosition != CopperLayerPosition.Top || layerNumber == 1) {
        "L>1 can't be the top layer"
    }

    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.COPPER,
        "L$layerNumber",
        layerPosition.grbStr,
        layerType?.name
    )
}

enum class DrillRoutData {
    PTH, Blind, Buried
}

enum class PlatingLabel {
    Drill, Rout, Mixed
}

fun Gerber.fileFunctionPlated(
    fromLayer: Int,
    toLayer: Int,
    drillRoutData: DrillRoutData,
    platingLabel: PlatingLabel? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.PLATED,
        fromLayer.toString(),
        toLayer.toString(),
        drillRoutData.name,
        platingLabel?.name
    )

fun Gerber.fileFunctionNonPlated(
    fromLayer: Int,
    toLayer: Int,
    drillRoutData: DrillRoutData,
    platingLabel: PlatingLabel? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.NON_PLATED,
        fromLayer.toString(),
        toLayer.toString(),
        drillRoutData.name,
        platingLabel?.name
    )

fun Gerber.fileFunctionProfile(
    edgePlated: Boolean
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.PROFILE,
        if (edgePlated) "P" else "NP"
    )

enum class OuterSide(val grbStr: String) {
    Top("Top"), Bottom("Bot")
}

fun Gerber.fileFunctionSolderMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.SOLDER_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionLegend(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.LEGEND,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionComponent(
    layerNumber: Int,
    position: OuterSide,
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.COMPONENT,
        "L$layerNumber",
        position.grbStr,
    )

fun Gerber.fileFunctionPaste(position: OuterSide) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.PASTE,
        position.grbStr
    )

fun Gerber.fileFunctionGlue(position: OuterSide) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.GLUE,
        position.grbStr
    )

fun Gerber.fileFunctionCarbonMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.CARBON_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionGoldMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.GOLD_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionHeatSinkMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.HEAT_SINK_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionPeelableMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.PEELABLE_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionSilverMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.SILVER_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionTinMask(
    position: OuterSide,
    index: Int? = null
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.TIN_MASK,
        position.grbStr,
        index?.toString()
    )

fun Gerber.fileFunctionDepthrout(position: OuterSide) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.DEPTH_ROUT,
        position.grbStr
    )

fun Gerber.fileFunctionVCut(
    position: OuterSide? = null,
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.VCUT,
        position?.grbStr,
    )

fun Gerber.fileFunctionViaFill() =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.VIA_FILL
    )

fun Gerber.fileFunctionPads(
    position: OuterSide
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.PADS,
        position.grbStr
    )

fun Gerber.fileFunctionOther(other: String) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DataLayers.OTHER,
        other
    )

fun Gerber.fileFunctionDrillMap() =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.DRILL_MAP
    )

fun Gerber.fileFunctionFabricationDrawing() =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.FABRICATION_DRAWING
    )

fun Gerber.fileFunctionVCutMap() =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.VCUT_MAP
    )

fun Gerber.fileFunctionAssemblyDrawing(
    position: OuterSide
) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.ASSEMBLY_DRAWING,
        position.grbStr
    )

fun Gerber.fileFunctionArrayDrawing() =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.ARRAY_DRAWING
    )

fun Gerber.fileFunctionOtherDrawing(other: String) =
    fileAttribute(
        StandardAttributes.File.FILE_FUNCTION,
        FileFunctions.DrawingLayers.OTHER_DRAWING,
        other
    )

enum class FilePolarity {
    Positive, Negative
}

fun Gerber.filePolarity(polarity: FilePolarity) =
    fileAttribute(
        StandardAttributes.File.FILE_POLARITY,
        polarity.name
    )

fun Gerber.sameCoordinates() =
    fileAttribute(
        StandardAttributes.File.SAME_COORDINATES
    )

fun Gerber.creationDate() =
    fileAttribute(
        StandardAttributes.File.CREATION_DATE,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Clock.systemUTC().instant())
    )

fun Gerber.generationSoftware() =
    fileAttribute(
        StandardAttributes.File.GENERATION_SOFTWARE,
        "superboringdev",
        "pcblin",
        "rolling"
    )

fun Gerber.projectId(id: String, guid: String, revision: String) =
    fileAttribute(
        StandardAttributes.File.PROJECT_ID,
        id, guid, revision
    )

fun Gerber.md5() =
    fileAttribute(
        StandardAttributes.File.MD5,
        checksumRelevantString().md5
    )

enum class ApertureFunction {
    ViaDrill, BackDrill, ComponentDrill, MechanicalDrill,
    CastellatedDrill, OtherDrill,

    ComponentPad, SMDPad, BGAPad, ConnectorPad, HeatsinkPad,
    ViaPad, TestPad, CastellatedPad, FiducialPad,
    ThermalReliefPad, WasherPad, AntiPad, OtherPad,
    Conductor, EtchedComponent, NonConductor,
    CopperBalancing, Border, OtherCopper, ComponentMain,
    ComponentOutline, ComponentPin,

    Profile, NonMaterial, Material, Other
}

fun Gerber.apertureFunction(function: ApertureFunction, vararg arguments: String) =
    apertureAttribute(
        StandardAttributes.Aperture.APERTURE_FUNCTION,
        function.name,
        *arguments
    )

fun Gerber.drillTolerance(plusTolerance: Double, minusTolerance: Double) =
    apertureAttribute(
        StandardAttributes.Aperture.DRILL_TOLERANCE,
        plusTolerance.formatted,
        minusTolerance.formatted
    )

enum class FlashTextRepresentation(val grbStr: String) {
    Barcode("B"), Characters("C")
}

enum class FlashTextReadability(val grbStr: String) {
    Readable("R"), MirroredLeftRight("M")
}

fun Gerber.flashText(
    text: String,
    representation: FlashTextRepresentation = FlashTextRepresentation.Characters,
    readability: FlashTextReadability = FlashTextReadability.Readable,
    font: String? = null,
    size: String? = null,
    comment: String? = null
) =
    apertureAttribute(
        StandardAttributes.Aperture.FLASH_TEXT,
        text, representation.grbStr, readability.grbStr,
        font, size, comment
    )

fun Gerber.net(netNames: List<String>) =
    objectAttribute(
        StandardAttributes.GraphicalObject.CAD_NET_NAME,
        *netNames.toTypedArray()
    )

fun Gerber.pin(referenceDescriptor: String, number: String, function: String) =
    objectAttribute(
        StandardAttributes.GraphicalObject.PIN_NUMBER,
        referenceDescriptor, number, function
    )

fun Gerber.componentReferenceDescriptor(referenceDescriptor: String) =
    objectAttribute(
        StandardAttributes.GraphicalObject.COMPONENT_REFERENCE_DESIGNATOR,
        referenceDescriptor
    )

fun Gerber.componentCharacteristics(characteristic: String, vararg args: Any) =
    objectAttribute(
        ".C$characteristic", *args
    )

fun Gerber.cRotationAngle(angle: Double) = componentCharacteristics("Rot", angle)
fun Gerber.cManufacturer(manufacturer: String) = componentCharacteristics("Mfr", manufacturer)
fun Gerber.cManufacturerPartNumber(number: String) = componentCharacteristics("MPN", number)
fun Gerber.cValue(value: String) = componentCharacteristics("Val", value)
fun Gerber.cFootprint(footprint: String) = componentCharacteristics("Ftp", footprint)
fun Gerber.cPackageName(name: String) = componentCharacteristics("PgN", name)
fun Gerber.cPackageDescription(description: String) = componentCharacteristics("PgD", description)
fun Gerber.cHeight(decimal: Double) = componentCharacteristics("Hgt", decimal)
fun Gerber.cLibraryName(name: String) = componentCharacteristics("LbN", name)
fun Gerber.cSupplier(supplierName: String, supplierPartName: String) =
    componentCharacteristics("Sup", supplierName, supplierPartName)
fun Gerber.cSupplier(supplierName: String, supplierPartName: String, supplierName2: String, supplierPartName2: String) =
    componentCharacteristics(
        "Sup", supplierName, supplierPartName, supplierName2, supplierPartName2
    )

