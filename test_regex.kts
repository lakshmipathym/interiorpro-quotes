val v = "CableManager,LEDProfile,WallHanging Brackets"
val formattedV = v.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace(",", ", ").replace(Regex("\\s+"), " ").trim()
println(formattedV)
