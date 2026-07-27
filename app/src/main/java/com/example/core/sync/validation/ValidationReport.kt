package com.example.core.sync.validation

data class ValidationReport(
    val resultType: ValidationResultType,
    val structure: PackageStructure,
    val errors: List<ValidationError>,
    val isValid: Boolean,
    val isCompatible: Boolean
)
