import re

with open('app/src/main/java/com/example/ui/customer/CustomersScreen.kt', 'r') as f:
    content = f.read()

# Fix the end of the dialog
target_end = """                    Spacer(modifier = Modifier.height(24.dp))

    }
    }


    // --- DUPLICATE WARNING MODAL ---"""
replacement_end = """                    Spacer(modifier = Modifier.height(24.dp))
            }
    }
    }


    // --- DUPLICATE WARNING MODAL ---"""
content = content.replace(target_end, replacement_end)

# Fix the rows
pattern = r"(\s+Row\(\s+modifier = Modifier\.fillMaxWidth\(\),\s+horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\s+\)\s+\{\s+com\.example\.ui\.components\.PremiumOutlinedTextField\([\s\S]*?\}\s+)"
matches = list(re.finditer(pattern, content))
# We should replace the 3 rows. Let's just find the exact rows instead.

# Row 1: Site Address & Country
row1_target = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.PremiumOutlinedTextField(
                            value = siteAddress,
                            onValueChange = {siteAddress = it},
                            label = "Site Address (Optional)",
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        com.example.ui.components.PremiumOutlinedTextField(
                            value = country,
                            onValueChange = {country = it},
                            label = "Country",
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }"""
row1_repl = """                    com.example.ui.components.PremiumOutlinedTextField(
                        value = siteAddress,
                        onValueChange = {siteAddress = it},
                        label = "Site Address (Optional)",
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = country,
                        onValueChange = {country = it},
                        label = "Country",
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )"""

# Row 2: City & District
row2_target = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.PremiumOutlinedTextField(
    value = city,
    onValueChange = {city = it},
    label = "City",
    singleLine = true,
    modifier = Modifier.weight(1f)
)
                        com.example.ui.components.PremiumOutlinedTextField(
    value = district,
    onValueChange = {district = it},
    label = "District",
    singleLine = true,
    modifier = Modifier.weight(1f)
)
                    }"""
row2_repl = """                    com.example.ui.components.PremiumOutlinedTextField(
                        value = city,
                        onValueChange = {city = it},
                        label = "City",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = district,
                        onValueChange = {district = it},
                        label = "District",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )"""

# Row 3: State & PIN
row3_target = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.PremiumOutlinedTextField(
    value = state,
    onValueChange = {state = it},
    label = "State",
    singleLine = true,
    modifier = Modifier.weight(1f)
)
                        com.example.ui.components.PremiumOutlinedTextField(
    value = pincode,
    onValueChange = {pincode = it},
    label = "Pincode",
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    singleLine = true,
    modifier = Modifier.weight(1f)
)
                    }"""
row3_repl = """                    com.example.ui.components.PremiumOutlinedTextField(
                        value = state,
                        onValueChange = {state = it},
                        label = "State",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = pincode,
                        onValueChange = {pincode = it},
                        label = "Pincode",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )"""

content = content.replace(row1_target, row1_repl)
content = content.replace(row2_target, row2_repl)
content = content.replace(row3_target, row3_repl)

with open('app/src/main/java/com/example/ui/customer/CustomersScreen.kt', 'w') as f:
    f.write(content)
print("Done")
