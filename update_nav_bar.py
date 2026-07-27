import re

file_path = "/app/applet/app/src/main/java/com/example/MainActivity.kt"
with open(file_path, "r") as f:
    content = f.read()

old_nav = """    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "history",
                    onClick = {
                        if (currentRoute != "history") {
                            navController.navigate("history") {
                                popUpTo("history") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = "History") },
                    label = { Text("History", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "new_quote",
                    onClick = {
                        quotationViewModelProvider().startNewQuotation()
                        if (currentRoute != "new_quote") {
                            navController.navigate("new_quote") {
                                popUpTo("history") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Create") },
                    label = { Text("New Quote", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "clients",
                    onClick = {
                        if (currentRoute != "clients") {
                            navController.navigate("clients") {
                                popUpTo("history") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.People, contentDescription = "Clients") },
                    label = { Text("Clients", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                popUpTo("history") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) }
                )
            }
        }
    )"""

new_nav = """    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                val isHistory = currentRoute == "history"
                NavigationBarItem(
                    selected = isHistory,
                    onClick = {
                        if (!isHistory) {
                            navController.navigate("history") {
                                popUpTo("history") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(if(isHistory) Icons.Filled.Receipt else Icons.Outlined.Receipt, contentDescription = "History") },
                    label = { Text("History", fontWeight = if(isHistory) FontWeight.Bold else FontWeight.Normal) },
                    alwaysShowLabel = true
                )
                
                val isNewQuote = currentRoute == "new_quote"
                NavigationBarItem(
                    selected = isNewQuote,
                    onClick = {
                        quotationViewModelProvider().startNewQuotation()
                        if (!isNewQuote) {
                            navController.navigate("new_quote") {
                                popUpTo("history") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(if(isNewQuote) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline, contentDescription = "Create") },
                    label = { Text("New Quote", fontWeight = if(isNewQuote) FontWeight.Bold else FontWeight.Normal) },
                    alwaysShowLabel = true
                )
                
                val isClients = currentRoute == "clients"
                NavigationBarItem(
                    selected = isClients,
                    onClick = {
                        if (!isClients) {
                            navController.navigate("clients") {
                                popUpTo("history") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(if(isClients) Icons.Filled.People else Icons.Outlined.PeopleOutline, contentDescription = "Clients") },
                    label = { Text("Clients", fontWeight = if(isClients) FontWeight.Bold else FontWeight.Normal) },
                    alwaysShowLabel = true
                )
                
                val isSettings = currentRoute == "settings"
                NavigationBarItem(
                    selected = isSettings,
                    onClick = {
                        if (!isSettings) {
                            navController.navigate("settings") {
                                popUpTo("history") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(if(isSettings) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if(isSettings) FontWeight.Bold else FontWeight.Normal) },
                    alwaysShowLabel = true
                )
            }
        }
    )"""

content = content.replace(old_nav, new_nav)
with open(file_path, "w") as f:
    f.write(content)
