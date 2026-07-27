fun main() {
    val dimStr = "1'2\""
    val clean = dimStr.replace("\"", "").lowercase().trim()
    val numeric = clean.replace("ft", "").replace("feet", "").replace("in", "").replace("inch", "").replace("'", "").trim()
    println(numeric)
}
