import gerber.Polarity
import gerber.apertureDefinition
import gerber.apertureMacro
import gerber.apertureMacroDefinition
import gerber.circle
import gerber.circularPlotting
import gerber.comment
import gerber.currentAperture
import gerber.flash
import gerber.formatSpecification
import gerber.gerber
import gerber.linearPlotting
import gerber.move
import gerber.obround
import gerber.plot
import gerber.polarity
import gerber.polygon
import gerber.rectangle
import gerber.region
import gerber.unitMode

fun main() {
    gerber {
        comment("Test")
        unitMode()
        integerDigits = 3
        formatSpecification()
        val thermal80 = apertureMacroDefinition(name = "THERMAL80") {
            primitiveCode(7, listOf(0, 0, 0.8, 0.55, 0.125, 45))
        }
        val ap10 = circle(0.1)
        val ap11 = circle(0.6)
        val ap12 = rectangle(0.6, 0.6)
        val ap13 = rectangle(0.4, 1.0)
        val ap14 = rectangle(1.0, 0.4)
        val ap15 = obround(0.4, 1.0)
        val ap16 = polygon(1.0, 3)

        polarity(Polarity.Dark) {
            linearPlotting {
                usingAperture(ap10) {
                    move(0.0, 2.5)
                    plot(x = 0, y = 0)
                    plot(x = 2.5, y = 0.0)
                    move(x = 10, y = 10)
                    plot(x = 15)
                    plot(x = 20, y = 15)
                    move(x = 25)
                    plot(y = 10)
                }
                usingAperture(ap11) {
                    flash(10, 10)
                    flash(20, 10)
                    flash(25, 10)
                    flash(25, 15)
                    flash(20, 15)
                }
                usingAperture(ap12) {
                    flash(10, 15)
                }
                usingAperture(ap13) {
                    flash(30, 15)
                }
                usingAperture(ap14) {
                    flash(30.0, 12.5)
                }
                usingAperture(ap15) {
                    flash(30, 10)
                }
                usingAperture(ap10) {
                    move(37.5, 10.0)
                }
                circularPlotting(clockwise = false) {
                    plot(
                        x = 37.5,
                        y = 10.0,
                        xOffset = 2.5,
                        yOffset = 0.0
                    )
                }
                usingAperture(ap16) {
                    flash(34, 10)
                    flash(35, 9)
                }
                region {
                    move(5, 20)
                    linearPlotting {
                        plot(y = 37.5)
                        plot(x = 37.5)
                        plot(y = 20)
                        plot(x = 5)
                    }
                }
                polarity(Polarity.Clear) {
                    region {
                        move(10, 25)
                        linearPlotting { plot(y = 30) }
                        circularPlotting(clockwise = true) {
                            plot(
                                x = 12.5,
                                y = 32.5,
                                xOffset = 2.5,
                                yOffset = 0.0
                            )
                        }
                        linearPlotting { plot(x = 30) }
                        circularPlotting(clockwise = true) {
                            plot(
                                x = 30.0,
                                y = 25.0,
                                xOffset = 0.0,
                                yOffset = -3.75
                            )
                        }
                        linearPlotting {
                            plot(x = 10)
                        }
                    }
                }
                polarity(Polarity.Dark) {
                    usingAperture(ap10) {
                        move(15.0, 28.75)
                        plot(x = 20)
                    }
                    usingAperture(ap11) {
                        flash(x = 15.0, y = 28.75)
                        flash(x = 20)
                    }
                    usingAperture(thermal80) {
                        flash(x = 28.75, 28.75)
                    }
                }
            }
        }
    }.toString().let {
        println(it)
    }
}