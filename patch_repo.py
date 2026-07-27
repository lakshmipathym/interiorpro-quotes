with open('app/src/main/java/com/example/data/MasterRepository.kt', 'r') as f:
    content = f.read()

target = """    suspend fun isMasterUsed(masterType: String, name: String): Boolean {
        val trimmedName = name.trim()
        return try {
            when (masterType) {"""

replacement = """    suspend fun isMasterUsed(masterType: String, name: String): Boolean {
        val trimmedName = name.trim()
        val normalizedType = when(masterType) {
            "PROJECT_CATEGORY" -> "CATEGORY"
            "MATERIAL_TYPE" -> "MATERIAL"
            else -> masterType
        }
        return try {
            when (normalizedType) {"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/data/MasterRepository.kt', 'w') as f:
    f.write(content)
