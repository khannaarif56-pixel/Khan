package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun AiVisionScreen(viewModel: JarvisViewModel) {
    val pickedBitmap by viewModel.selectedImageBitmap.collectAsStateWithLifecycle()
    val analysisState by viewModel.visionAnalysisState.collectAsStateWithLifecycle()
    val genState by viewModel.generatedImageState.collectAsStateWithLifecycle()
    val history by viewModel.visionHistory.collectAsStateWithLifecycle()

    var activeMode by remember { mutableStateOf("Analysis") }
    var visionQuestion by remember { mutableStateOf("Describe objects, text, and scene details.") }
    var imagePrompt by remember { mutableStateOf("") }

    // Play-Policy compliant zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.selectImageUri(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = activeMode == "Analysis",
                    onClick = { activeMode = "Analysis" },
                    label = { Text("Image Understanding & Q&A") }
                )
                FilterChip(
                    selected = activeMode == "Generation",
                    onClick = { activeMode = "Generation" },
                    label = { Text("AI Image Generation") }
                )
            }
        }

        if (activeMode == "Analysis") {
            item {
                HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    HudSectionTitle(title = "Multimodal Vision Scanner", icon = Icons.Filled.ImageSearch)
                    Spacer(modifier = Modifier.height(10.dp))

                    GoldOutlinedButton(
                        text = "Pick Image (Android Photo Picker)",
                        icon = Icons.Filled.PhotoLibrary,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_vision_image_button")
                    )

                    if (pickedBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            bitmap = pickedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected image for AI analysis",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = visionQuestion,
                        onValueChange = { visionQuestion = it },
                        label = { Text("Ask JARVIS about this image") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vision_question_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GoldPrimaryButton(
                        text = "Analyze Image",
                        icon = Icons.Filled.ImageSearch,
                        enabled = pickedBitmap != null && analysisState !is AsyncUiState.Loading,
                        onClick = { viewModel.analyzeSelectedImage(visionQuestion) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_image_button")
                    )

                    when (val st = analysisState) {
                        is AsyncUiState.Loading -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = SolarGold,
                                    strokeWidth = 2.dp
                                )
                                Text("Analyzing visual scene with Gemini...", color = RadiantChampagne)
                            }
                        }
                        is AsyncUiState.Success -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(st.data, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                        }
                        is AsyncUiState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }
                }
            }
        } else {
            item {
                HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    HudSectionTitle(
                        title = "AI Image Studio (Gemini Nano Banana 2.1)",
                        icon = Icons.Filled.AutoAwesome
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text("Describe the image to generate...") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_gen_prompt_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GoldPrimaryButton(
                        text = "Generate AI Image",
                        icon = Icons.Filled.AutoAwesome,
                        enabled = imagePrompt.isNotBlank() && genState !is AsyncUiState.Loading,
                        onClick = { viewModel.generateAiImage(imagePrompt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_image_button")
                    )

                    when (val st = genState) {
                        is AsyncUiState.Loading -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(color = SolarGold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Synthesizing high-resolution image (30–60s)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RadiantChampagne
                                )
                            }
                        }
                        is AsyncUiState.Success -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            Image(
                                bitmap = st.data.asImageBitmap(),
                                contentDescription = "AI Generated Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GoldOutlinedButton(
                                text = "Save to Pictures/Jarvis",
                                icon = Icons.Filled.Download,
                                onClick = { viewModel.saveGeneratedImageToGallery(st.data) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_generated_image_button")
                            )
                        }
                        is AsyncUiState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Generation Error: ${st.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        item {
            HudSectionTitle(title = "Vision & Image History", trailingText = "${history.size} RECORDS")
        }

        if (history.isEmpty()) {
            item {
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No image analyses or generated artworks recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            items(history, key = { it.id.ifBlank { it.hashCode().toString() } }) { entry ->
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    StatusPill(
                        label = entry.mode.uppercase(),
                        tint = if (entry.mode == "generation") SolarGold else HoloCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(entry.prompt, style = MaterialTheme.typography.titleMedium, color = RadiantChampagne)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.resultSummary.take(260),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AiToolsScreen(
    viewModel: JarvisViewModel,
    onOpenVisionCenter: () -> Unit
) {
    val loadedDoc by viewModel.loadedDocument.collectAsStateWithLifecycle()
    val outputState by viewModel.toolsOutputState.collectAsStateWithLifecycle()

    val tools = listOf(
        "PDF / Doc Reader",
        "Translator",
        "Math Solver",
        "Study Tutor",
        "Summarizer",
        "Coding Assistant"
    )
    var selectedTool by remember { mutableStateOf(tools.first()) }
    var toolInput by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("Hindi") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openDocumentUri(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tools) { t ->
                    FilterChip(
                        selected = selectedTool == t,
                        onClick = { selectedTool = t },
                        label = { Text(t) }
                    )
                }
            }
        }

        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = selectedTool, icon = Icons.Filled.Description)
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTool == "PDF / Doc Reader") {
                    GoldOutlinedButton(
                        text = "Open PDF / TXT / MD / Code File",
                        icon = Icons.Filled.Description,
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf("application/pdf", "text/*", "application/json")
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_document_button")
                    )
                    if (loadedDoc != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        StatusPill(
                            label = "${loadedDoc!!.fileName} • ${loadedDoc!!.pageCount} pg",
                            tint = HoloCyan
                        )
                        loadedDoc!!.previewBitmap?.let { bmp ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page 1 Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = loadedDoc!!.extractedText.take(350),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (selectedTool == "Translator") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Hindi", "English", "Urdu", "Spanish", "French", "Arabic")) { lang ->
                            FilterChip(
                                selected = targetLanguage == lang,
                                onClick = { targetLanguage = lang },
                                label = { Text("To $lang") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = toolInput,
                    onValueChange = { toolInput = it },
                    label = {
                        Text(
                            when (selectedTool) {
                                "PDF / Doc Reader" -> "Optional question about the opened document..."
                                "Translator" -> "Enter text to translate into $targetLanguage..."
                                "Math Solver" -> "Enter equation or math problem..."
                                "Study Tutor" -> "Enter subject or concept to learn..."
                                "Summarizer" -> "Paste long article or notes to summarize..."
                                else -> "Describe code or technical concept..."
                            }
                        )
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_tool_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldPrimaryButton(
                        text = "Run $selectedTool",
                        enabled = (toolInput.isNotBlank() || (selectedTool == "PDF / Doc Reader" && loadedDoc != null)) &&
                            outputState !is AsyncUiState.Loading,
                        onClick = { viewModel.runAiTool(selectedTool, toolInput, targetLanguage) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_ai_tool_button")
                    )
                    GoldOutlinedButton(
                        text = "Vision",
                        onClick = onOpenVisionCenter,
                        modifier = Modifier.weight(0.45f)
                    )
                }

                when (val st = outputState) {
                    is AsyncUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SolarGold, strokeWidth = 2.dp)
                            Text("Processing with JARVIS AI...", color = RadiantChampagne)
                        }
                    }
                    is AsyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(st.data, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                    }
                    is AsyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }
    }
}
