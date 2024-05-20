package gerber

fun Gerber.rectangle(width: Double, height: Double = width) =
    simpleAperture("R", width, height)

fun Gerber.circle(diameter: Double) =
    simpleAperture("C", diameter)

fun Gerber.obround(width: Double, height: Double = width) =
    simpleAperture("O", width, height)

fun Gerber.polygon(circleDiameter: Double, vertexCount: Int) =
    simpleAperture("P", circleDiameter, vertexCount)
