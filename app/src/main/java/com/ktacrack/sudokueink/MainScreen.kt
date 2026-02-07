package com.ktacrack.sudokueink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MainScreen(
    onDifficultySelected: (Difficulty) -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LanguageManager.loadLanguage(context)) }
    var isDarkTheme by remember { mutableStateOf(ThemeManager.loadDarkMode(context)) }

    // Aquesta key farà que tot es recompongui quan canviï l'idioma
    key(currentLanguage, isDarkTheme) {
        val strings = when (currentLanguage) {
            Language.CATALAN -> StringsCa
            Language.SPANISH -> StringsEs
            Language.ENGLISH -> StringsEn
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selector d'idioma i tema a dalt a la dreta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Botons d'idioma
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botó CA
                    Button(
                        onClick = {
                            currentLanguage = Language.CATALAN
                            LanguageManager.saveLanguage(context, Language.CATALAN)
                        },
                        modifier = Modifier.size(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == Language.CATALAN) Color.Black else Color.White,
                            contentColor = if (currentLanguage == Language.CATALAN) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("CA", fontSize = 28.sp)
                    }

                    // Botó ES
                    Button(
                        onClick = {
                            currentLanguage = Language.SPANISH
                            LanguageManager.saveLanguage(context, Language.SPANISH)
                        },
                        modifier = Modifier.size(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == Language.SPANISH) Color.Black else Color.White,
                            contentColor = if (currentLanguage == Language.SPANISH) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("ES", fontSize = 28.sp)
                    }

                    // Botó EN
                    Button(
                        onClick = {
                            currentLanguage = Language.ENGLISH
                            LanguageManager.saveLanguage(context, Language.ENGLISH)
                        },
                        modifier = Modifier.size(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == Language.ENGLISH) Color.Black else Color.White,
                            contentColor = if (currentLanguage == Language.ENGLISH) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("EN", fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botons de tema
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isDarkTheme = false
                            ThemeManager.saveDarkMode(context, false)
                            onThemeChange(false)
                        },
                        modifier = Modifier.size(width = 80.dp, height = 50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDarkTheme) Color.Black else Color.White,
                            contentColor = if (!isDarkTheme) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("☀️", fontSize = 32.sp)
                    }

                    Button(
                        onClick = {
                            isDarkTheme = true
                            ThemeManager.saveDarkMode(context, true)
                            onThemeChange(true)
                        },
                        modifier = Modifier.size(width = 80.dp, height = 50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color.Black else Color.White,
                            contentColor = if (isDarkTheme) Color.White else Color.Black
                        ),
                        border = BorderStroke(2.dp, Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🌙", fontSize = 32.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = { onDifficultySelected(Difficulty.EASY) },
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(strings.difficultyEasy, fontSize = 34.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onDifficultySelected(Difficulty.MEDIUM) },
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(strings.difficultyMedium, fontSize = 34.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onDifficultySelected(Difficulty.HARD) },
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text(strings.difficultyHard, fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStatisticsClick,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Text(strings.statistics, fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = strings.createdBy,
                fontSize = 20.sp,
                color = if (isDarkTheme) {
                    Color.LightGray  // Mode fosc: gris clar
                } else {
                    Color.Gray   // Mode clar: gris fosc
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
