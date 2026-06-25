package io.github.serg987.sudokueinkhtr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.slider.SliderMMD
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val strings = rememberStrings()
    val scale = AdaptiveSizes.getScaleFactor()

    var currentLanguage by remember { mutableStateOf(LanguageManager.loadLanguage(context)) }
    var currentDifficulty by remember { mutableStateOf(SettingsManager.loadDifficulty(context)) }
    var isDarkTheme by remember { mutableStateOf(ThemeManager.loadDarkMode(context)) }
    var isPencil by remember { mutableStateOf(SettingsManager.loadIsPencil(context)) }
    var inkThickness by remember { mutableStateOf(SettingsManager.loadInkThickness(context)) }
    var htrModel by remember { mutableStateOf(SettingsManager.loadHtrModel(context)) }
    var isZenMode by remember { mutableStateOf(SettingsManager.loadZenMode(context)) }

    var isOnnxDownloaded by remember { mutableStateOf(ModelDownloadManager.isOnnxModelDownloaded(context)) }
    var isMlKitDownloaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ModelDownloadManager.isMlKitModelDownloaded { isDownloaded ->
            isMlKitDownloaded = isDownloaded
        }
    }

    var showOnnxSetupDialog by remember { mutableStateOf(false) }
    var showMlKitSetupDialog by remember { mutableStateOf(false) }

    val onnxDownloadProgress by ModelDownloadManager.onnxDownloadProgress.collectAsState()
    val mlKitDownloadState by ModelDownloadManager.mlKitDownloadState.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp)
    ) {
        Text(
            text = strings.settingsTitle,
            fontSize = (24 * scale).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = (12 * scale).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
        ) {
            // Difficulty Selection
            Text(strings.difficulty, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.selectableGroup().fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentDifficulty == Difficulty.EASY,
                        onClick = { 
                            currentDifficulty = Difficulty.EASY
                            SettingsManager.saveDifficulty(context, Difficulty.EASY) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentDifficulty == Difficulty.EASY,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.difficultyEasy, fontSize = (15 * scale).sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentDifficulty == Difficulty.MEDIUM,
                        onClick = { 
                            currentDifficulty = Difficulty.MEDIUM
                            SettingsManager.saveDifficulty(context, Difficulty.MEDIUM) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentDifficulty == Difficulty.MEDIUM,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.difficultyMedium, fontSize = (15 * scale).sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentDifficulty == Difficulty.HARD,
                        onClick = { 
                            currentDifficulty = Difficulty.HARD
                            SettingsManager.saveDifficulty(context, Difficulty.HARD) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentDifficulty == Difficulty.HARD,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.difficultyHard, fontSize = (15 * scale).sp)
                }
            }

            HorizontalDividerMMD()

            // Language Selection
            Text(strings.language, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.selectableGroup().fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentLanguage == Language.CATALAN,
                        onClick = { 
                            currentLanguage = Language.CATALAN
                            LanguageManager.saveLanguage(context, Language.CATALAN) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentLanguage == Language.CATALAN,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.catalan, fontSize = (15 * scale).sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentLanguage == Language.SPANISH,
                        onClick = { 
                            currentLanguage = Language.SPANISH
                            LanguageManager.saveLanguage(context, Language.SPANISH) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentLanguage == Language.SPANISH,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.spanish, fontSize = (15 * scale).sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = currentLanguage == Language.ENGLISH,
                        onClick = { 
                            currentLanguage = Language.ENGLISH
                            LanguageManager.saveLanguage(context, Language.ENGLISH) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = currentLanguage == Language.ENGLISH,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.english, fontSize = (15 * scale).sp)
                }
            }

            HorizontalDividerMMD()

            // Theme (Day/Night) - Disabled for now as dark mode is not working
            /*
            Row(
                modifier = Modifier.fillMaxWidth().height((36 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.darkTheme, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                SwitchMMD(
                    checked = isDarkTheme,
                    onCheckedChange = { 
                        isDarkTheme = it
                        ThemeManager.saveDarkMode(context, it)
                        onThemeChange(it)
                    },
                    modifier = Modifier.scale(0.75f)
                )
            }

            HorizontalDividerMMD()
            */

            // Zen Mode
            Row(
                modifier = Modifier.fillMaxWidth().height((36 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.zenMode, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                SwitchMMD(
                    checked = isZenMode,
                    onCheckedChange = { 
                        isZenMode = it
                        SettingsManager.saveZenMode(context, it)
                    },
                    modifier = Modifier.scale(0.75f)
                )
            }

            HorizontalDividerMMD()

            // IsPencil
            Row(
                modifier = Modifier.fillMaxWidth().height((36 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.isPencilSetting, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                SwitchMMD(
                    checked = isPencil,
                    onCheckedChange = { 
                        isPencil = it
                        SettingsManager.saveIsPencil(context, it)
                        // Also update AppConfig for backward compatibility until it's fully migrated
                    },
                    modifier = Modifier.scale(0.75f)
                )
            }

            HorizontalDividerMMD()

            // InkThickness
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth().height((40 * scale).dp)
            ) {
                Text("${strings.inkThicknessSetting}: ${inkThickness.toInt()}", fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width((16 * scale).dp))
                SliderMMD(
                    value = inkThickness,
                    onValueChange = { 
                        inkThickness = it
                        SettingsManager.saveInkThickness(context, it)
                        AppConfig.handwritingStrokeThickness = it // Update AppConfig
                    },
                    valueRange = 3f..20f,
                    steps = 16,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDividerMMD()

            // HTR Model
            Text(strings.htrModelSetting, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.selectableGroup().fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((32 * scale).dp).selectable(
                        selected = htrModel == HtrModel.TFLITE,
                        onClick = { 
                            htrModel = HtrModel.TFLITE
                            SettingsManager.saveHtrModel(context, HtrModel.TFLITE) 
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = htrModel == HtrModel.TFLITE,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.htrModelTfLite, fontSize = (15 * scale).sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((48 * scale).dp).selectable(
                        selected = htrModel == HtrModel.ONNX,
                        enabled = isOnnxDownloaded,
                        onClick = { 
                            if (isOnnxDownloaded) {
                                htrModel = HtrModel.ONNX
                                SettingsManager.saveHtrModel(context, HtrModel.ONNX) 
                            }
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = htrModel == HtrModel.ONNX,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.htrModelOnnx, fontSize = (15 * scale).sp, modifier = Modifier.weight(1f))
                    if (!isOnnxDownloaded) {
                        ButtonMMD(
                            onClick = { showOnnxSetupDialog = true },
                            modifier = Modifier.padding(end = (8 * scale).dp)
                        ) {
                            Text(strings.setup, fontSize = (15 * scale).sp)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth().height((48 * scale).dp).selectable(
                        selected = htrModel == HtrModel.MLKIT,
                        enabled = isMlKitDownloaded,
                        onClick = { 
                            if (isMlKitDownloaded) {
                                htrModel = HtrModel.MLKIT
                                SettingsManager.saveHtrModel(context, HtrModel.MLKIT) 
                            }
                        },
                        role = Role.RadioButton
                    )
                ) {
                    RadioButtonMMD(
                        selected = htrModel == HtrModel.MLKIT,
                        onClick = null,
                        modifier = Modifier.scale(0.75f)
                    )
                    Spacer(modifier = Modifier.width((8 * scale).dp))
                    Text(strings.htrModelMlKit, fontSize = (15 * scale).sp, modifier = Modifier.weight(1f))
                    if (!isMlKitDownloaded) {
                        ButtonMMD(
                            onClick = { showMlKitSetupDialog = true },
                            modifier = Modifier.padding(end = (8 * scale).dp)
                        ) {
                            Text(strings.setup, fontSize = (15 * scale).sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height((16 * scale).dp))
        }
    }

    if (showOnnxSetupDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (onnxDownloadProgress == null || onnxDownloadProgress == -1f) {
                    showOnnxSetupDialog = false 
                }
            },
            shape = RoundedCornerShape((16 * scale).dp),
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = {
                Text(
                    text = strings.onnxSetupTitle,
                    fontSize = (24 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val annotatedString = buildAnnotatedString {
                        val fullText = strings.onnxSetupDescription
                        val url = "https://huggingface.co/deepshah23/digit-blank-classifier-cnn"
                        val startIndex = fullText.indexOf(url)
                        
                        if (startIndex != -1) {
                            append(fullText.substring(0, startIndex))
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                                append(url)
                            }
                            pop()
                            append(fullText.substring(startIndex + url.length))
                        } else {
                            append(fullText)
                        }
                    }

                    ClickableText(
                        text = annotatedString,
                        style = androidx.compose.ui.text.TextStyle(fontSize = (18 * scale).sp, color = Color.Black),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )
                    
                    if (onnxDownloadProgress != null && onnxDownloadProgress != -1f && onnxDownloadProgress != 1f) {
                        Spacer(modifier = Modifier.height((16 * scale).dp))
                        Text(strings.downloading, fontSize = (16 * scale).sp, color = Color.Black)
                        Spacer(modifier = Modifier.height((8 * scale).dp))
                        LinearProgressIndicator(
                            progress = onnxDownloadProgress ?: 0f,
                            modifier = Modifier.fillMaxWidth().height((8 * scale).dp),
                            color = Color.Black,
                            trackColor = Color.LightGray
                        )
                    } else if (onnxDownloadProgress == -1f) {
                        Spacer(modifier = Modifier.height((16 * scale).dp))
                        Text(strings.error, fontSize = (16 * scale).sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                if (onnxDownloadProgress == null || onnxDownloadProgress == -1f) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = ModelDownloadManager.downloadOnnxModel(context)
                                if (success) {
                                    isOnnxDownloaded = true
                                    showOnnxSetupDialog = false
                                    ModelDownloadManager.resetStates()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.download, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = {
                if (onnxDownloadProgress == null || onnxDownloadProgress == -1f) {
                    Button(
                        onClick = { 
                            showOnnxSetupDialog = false 
                            ModelDownloadManager.resetStates()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.cancel, fontSize = (18 * scale).sp)
                    }
                }
            }
        )
    }

    if (showMlKitSetupDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (mlKitDownloadState != true) {
                    showMlKitSetupDialog = false 
                }
            },
            shape = RoundedCornerShape((16 * scale).dp),
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = {
                Text(
                    text = strings.mlKitSetupTitle,
                    fontSize = (24 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    val annotatedString = buildAnnotatedString {
                        val fullText = strings.mlKitSetupDescription
                        val mlKitTermsUrl = "https://developers.google.com/ml-kit/terms"
                        val modelUrl = "https://developers.google.com/ml-kit/vision/digital-ink-recognition"
                        
                        val firstLinkIndex = fullText.indexOf(mlKitTermsUrl)
                        val secondLinkIndex = fullText.indexOf(modelUrl)

                        var currentIndex = 0
                        
                        if (firstLinkIndex != -1 && firstLinkIndex < secondLinkIndex) {
                            append(fullText.substring(currentIndex, firstLinkIndex))
                            pushStringAnnotation(tag = "URL1", annotation = mlKitTermsUrl)
                            withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                                append(mlKitTermsUrl)
                            }
                            pop()
                            currentIndex = firstLinkIndex + mlKitTermsUrl.length
                        }
                        
                        if (secondLinkIndex != -1) {
                            append(fullText.substring(currentIndex, secondLinkIndex))
                            pushStringAnnotation(tag = "URL2", annotation = modelUrl)
                            withStyle(style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)) {
                                append(modelUrl)
                            }
                            pop()
                            currentIndex = secondLinkIndex + modelUrl.length
                        }
                        
                        append(fullText.substring(currentIndex))
                    }

                    ClickableText(
                        text = annotatedString,
                        style = androidx.compose.ui.text.TextStyle(fontSize = (18 * scale).sp, color = Color.Black),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL1", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                            annotatedString.getStringAnnotations(tag = "URL2", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )
                    
                    if (mlKitDownloadState == true) {
                        Spacer(modifier = Modifier.height((16 * scale).dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size((24 * scale).dp))
                            Spacer(modifier = Modifier.width((16 * scale).dp))
                            Text(strings.downloading, fontSize = (16 * scale).sp, color = Color.Black)
                        }
                    } else if (mlKitDownloadState == false) {
                        Spacer(modifier = Modifier.height((16 * scale).dp))
                        Text(strings.error, fontSize = (16 * scale).sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                if (mlKitDownloadState != true) {
                    Button(
                        onClick = {
                            ModelDownloadManager.downloadMlKitModel { success ->
                                if (success) {
                                    isMlKitDownloaded = true
                                    showMlKitSetupDialog = false
                                    ModelDownloadManager.resetStates()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.download, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = {
                if (mlKitDownloadState != true) {
                    Button(
                        onClick = { 
                            showMlKitSetupDialog = false 
                            ModelDownloadManager.resetStates()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.cancel, fontSize = (18 * scale).sp)
                    }
                }
            }
        )
    }
}
