package gerber

/**
 * Creates a rectangle according to 4.4.3
 */
fun Gerber.rectangle(
    width: Double,
    height: Double = width,
    holeDiameter: Double? = null,
) =
    simpleAperture("R", width, height, holeDiameter).also {
        require(width > .0 && height > .0) {
            "Width and height of a rectangle must be > 0"
        }
    }

/**
 * Creates a circle according to 4.4.2
 * Circles can have zero size, see 4.3.2
 */
fun Gerber.circle(diameter: Double, holeDiameter: Double? = 0.0) =
    simpleAperture("C", diameter, holeDiameter)

/**
 * Creates an obround according to 4.4.4
 */
fun Gerber.obround(
    width: Double,
    height: Double = width,
    holeDiameter: Double? = null,
) =
    simpleAperture("O", width, height, holeDiameter).also {
        require(width > .0 && height > .0) {
            "Width and height of an obround must be > 0"
        }
    }

fun Gerber.polygon(
    outerDiameter: Double,
    vertexCount: Int,
    rotation: Double = .0,
    holeDiameter: Double? = null,
) =
    simpleAperture("P", outerDiameter, vertexCount, rotation, holeDiameter).also {
        require(outerDiameter > .0) {
            "Circle diameter of polygon must be > 0"
        }
        require(vertexCount >= 3) {
            "A polygon must have at least 3 vertices"
        }
        require(vertexCount <= 12) {
            "A polygon can at most be a dodecagon"
        }
        require(holeDiameter == null || outerDiameter > holeDiameter) {
            "Hole diameter must be smaller than the outer diameter"
        }
    }
