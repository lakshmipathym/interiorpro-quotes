package com.example.domain.models

enum class UnitType(val label: String) {
    SQ_FT("Sq.Ft"),
    CU_FT("Cu.Ft"),
    SQ_M("Sq.M"),
    CU_M("Cu.M"),
    METER("Meter"),
    R_FT("R.Ft"),
    LUMPSUM("Lumpsum"),
    NOS("Nos");
    
    companion object {
        fun fromString(unit: String): UnitType {
            val uLower = unit.trim().lowercase()
            return when {
                uLower == "sq_m" || uLower.contains("sq.m") || uLower.contains("sqm") || uLower.contains("square meter") || uLower.contains("square mtr") -> SQ_M
                uLower == "cu_m" || uLower.contains("cu.m") || uLower.contains("cum") || uLower.contains("cubic meter") || uLower.contains("cubic mtr") -> CU_M
                uLower == "sq_ft" || uLower.contains("sq.ft") || uLower.contains("sqft") || uLower == "sft" || uLower.contains("square feet") || uLower.contains("sq") -> SQ_FT
                uLower == "cu_ft" || uLower.contains("cu.ft") || uLower.contains("cuft") || uLower.contains("cubic feet") || uLower.contains("cft") -> CU_FT
                uLower == "meter" || uLower.contains("meter") || uLower.contains("mtr") || uLower == "r.m" || uLower == "rm" -> METER
                uLower == "r_ft" || uLower == "running feet" || uLower.contains("run") || uLower.contains("rft") || uLower.contains("r.ft") || uLower == "r.f" -> R_FT
                uLower == "lumpsum" || uLower.contains("lump sum") || uLower == "l.s" || uLower == "ls" -> LUMPSUM
                else -> NOS
            }
        }
    }
}
