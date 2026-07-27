import re
file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace Template Dropdown
template_old = """                // Project Preset (Optional)
                Column {
                    Text(
                        "Project Preset (Optional)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = currentTemplate?.name ?: "No Preset Selected (Custom)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedTemplate = true }
                        )
                        DropdownMenu(
                            expanded = expandedTemplate,
                            onDismissRequest = { expandedTemplate = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Preset (Custom from scratch)") },
                                onClick = {
                                    onSelectTemplate(null)
                                    expandedTemplate = false
                                }
                            )
                            templates.forEach { tmpl ->
                                DropdownMenuItem(
                                    text = { Text(tmpl.name) },
                                    onClick = {
                                        onSelectTemplate(tmpl)
                                        expandedTemplate = false
                                    }
                                )
                            }
                        }
                    }
                }"""

template_new = """                com.example.ui.components.PremiumDropdown(
                    value = currentTemplate?.name ?: "No Preset (Custom)",
                    onValueChange = { name -> 
                        if (name == "No Preset (Custom)") {
                            onSelectTemplate(null)
                        } else {
                            val tmpl = templates.find { it.name == name }
                            if (tmpl != null) onSelectTemplate(tmpl)
                        }
                    },
                    label = "Project Preset (Optional)",
                    options = listOf("No Preset (Custom)") + templates.map { it.name },
                    modifier = Modifier.fillMaxWidth()
                )"""

# Replace Product Type Dropdown
prod_type_old = """                // Product Type (Mandatory)
                Column {
                    Text(
                        "Product Type *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val displayVal = currentProjectType.ifEmpty { "Select Product Type" }
                        TextField(
                            value = displayVal,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedProjectType = true }
                        )
                        DropdownMenu(
                            expanded = expandedProjectType,
                            onDismissRequest = { expandedProjectType = false }
                        ) {
                            val list = if (projectTypes.isEmpty()) listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") else projectTypes
                            list.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        onSelectProjectType(type)
                                        expandedProjectType = false
                                    }
                                )
                            }
                        }
                    }
                }"""

prod_type_new = """                com.example.ui.components.PremiumDropdown(
                    value = currentProjectType.ifEmpty { "Select Product Type" },
                    onValueChange = onSelectProjectType,
                    label = "Product Type *",
                    options = if (projectTypes.isEmpty()) listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") else projectTypes,
                    modifier = Modifier.fillMaxWidth()
                )"""

# Replace Material Type Dropdown
mat_old = """                // Material Type (Mandatory)
                Column {
                    Text(
                        "Material Type *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val displayVal = currentMaterial.ifEmpty { "Select Material Type" }
                        TextField(
                            value = displayVal,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedMaterial = true }
                        )
                        DropdownMenu(
                            expanded = expandedMaterial,
                            onDismissRequest = { expandedMaterial = false }
                        ) {
                            val list = if (materials.isEmpty()) listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") else materials
                            list.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text(mat) },
                                    onClick = {
                                        onSelectMaterial(mat)
                                        expandedMaterial = false
                                    }
                                )
                            }
                        }
                    }
                }"""

mat_new = """                com.example.ui.components.PremiumDropdown(
                    value = currentMaterial.ifEmpty { "Select Material Type" },
                    onValueChange = onSelectMaterial,
                    label = "Material Type *",
                    options = if (materials.isEmpty()) listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") else materials,
                    modifier = Modifier.fillMaxWidth()
                )"""

content = content.replace(template_old, template_new)
content = content.replace(prod_type_old, prod_type_new)
content = content.replace(mat_old, mat_new)

with open(file_path, "w") as f:
    f.write(content)
