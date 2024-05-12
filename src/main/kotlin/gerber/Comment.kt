package gerber

fun Gerber.comment(text: String) =
    +GerberCommand("G04 ${text.gerberString}")

