package com.example.petcare.ui.screens.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.data.model.WeightLog
import com.example.petcare.ui.components.DateTextField
import com.example.petcare.util.EventDateUtils
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightTrackerScreen(
    petId: String,
    onBack: () -> Unit
) {
    val viewModel: WeightTrackerViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(petId) {
        viewModel.load(petId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
                Text(
                    "Weight Tracker",
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        WeightSummaryCard(
                            petName = state.petName,
                            latest = latestWeightLog(state.logs),
                            unit = state.preferredUnit,
                            onUnitSelected = viewModel::setPreferredUnit
                        )
                    }

                    item {
                        WeightLogForm(
                            state = state,
                            onWeightChange = viewModel::setWeightInput,
                            onDateSelected = viewModel::setDateInput,
                            onSave = viewModel::save,
                            onCancel = viewModel::cancelEditing
                        )
                    }

                    if (state.error != null) {
                        item {
                            Text(
                                state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    item {
                        WeightGraphCard(
                            state = state,
                            onRangeSelected = viewModel::setSelectedRange,
                            onLogSelected = viewModel::selectLog,
                            onEdit = viewModel::startEditing,
                            onDelete = viewModel::delete
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WeightSummaryCard(
    petName: String,
    latest: WeightLog?,
    unit: String,
    onUnitSelected: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(petName.ifBlank { "Pet" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = latest?.let { "${formatWeight(it.weight, unit)} logged ${displayDate(it.loggedAt)}" }
                    ?: "Start logging this pet's weight",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightUnitChip(selected = unit == "kg", text = "kg", onClick = { onUnitSelected("kg") })
                WeightUnitChip(selected = unit == "lb", text = "lb", onClick = { onUnitSelected("lb") })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WeightUnitChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun WeightLogForm(
    state: WeightTrackerUiState,
    onWeightChange: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (state.editingLogId == null) "New Weight Log" else "Edit Weight Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = state.weightInput,
                onValueChange = onWeightChange,
                label = { Text("Weight (${state.preferredUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DateTextField(
                name = "Logged Date *",
                value = state.dateInput,
                onDateSelected = onDateSelected,
                allowFutureDates = false,
                minDateInclusive = state.petBirthDateIso
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.editingLogId != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("Cancel") }
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (state.editingLogId == null) "Save Log" else "Save Changes", color = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WeightGraphCard(
    state: WeightTrackerUiState,
    onRangeSelected: (WeightGraphRange) -> Unit,
    onLogSelected: (WeightLog?) -> Unit,
    onEdit: (WeightLog) -> Unit,
    onDelete: (WeightLog) -> Unit
) {
    val chartLogs = logsForGraph(state.logs, state.selectedRange)
    val selectedLog = chartLogs.firstOrNull { it.id == state.selectedLogId }
    val window = graphWindowFor(state.selectedRange, logs = state.logs)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weight History",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WeightGraphRange.entries.forEach { range ->
                        FilterChip(
                            selected = state.selectedRange == range,
                            onClick = { onRangeSelected(range) },
                            label = { Text(range.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            WeightLineGraph(
                logs = chartLogs,
                selectedLogId = state.selectedLogId,
                unit = state.preferredUnit,
                window = window,
                onLogSelected = onLogSelected
            )

            if (chartLogs.isEmpty()) {
                Text(
                    if (state.logs.isEmpty()) "No weight logs yet" else "No logs in this range",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    window.axisLabels.forEach { label ->
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            selectedLog?.let { log ->
                SelectedWeightLogCard(
                    log = log,
                    unit = state.preferredUnit,
                    onEdit = { onEdit(log) },
                    onDelete = { onDelete(log) }
                )
            }
        }
    }
}

@Composable
private fun WeightLineGraph(
    logs: List<WeightLog>,
    selectedLogId: String?,
    unit: String,
    window: WeightGraphWindow,
    onLogSelected: (WeightLog?) -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val pointColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .border(1.dp, gridColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (logs.isEmpty()) {
            Text(
                "Add logs to see a trend",
                color = labelColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(logs, selectedLogId, unit, window) {
                    detectTapGestures { offset ->
                        onLogSelected(findNearestLog(offset, size, logs, unit, window))
                    }
                }
        ) {
            val canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            val points = chartOffsets(canvasSize, logs, unit, window)
            val chartLeft = 44f
            val chartTop = 12f
            val chartRight = size.width - 12f
            val chartBottom = size.height - 34f

            repeat(4) { index ->
                val y = chartTop + ((chartBottom - chartTop) * index / 3f)
                drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1f)
            }

            val (minWeight, maxWeight) = weightBounds(logs, unit)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                repeat(4) { index ->
                    val value = maxWeight - ((maxWeight - minWeight) * index / 3f)
                    val y = chartTop + ((chartBottom - chartTop) * index / 3f) + 8f
                    drawText(String.format(Locale.US, "%.1f", value), chartLeft - 8f, y, paint)
                }
            }

            if (points.size == 1) {
                drawCircle(
                    color = if (logs.first().id == selectedLogId) selectedColor else pointColor,
                    radius = if (logs.first().id == selectedLogId) 8f else 6f,
                    center = points.first()
                )
            } else {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                points.forEachIndexed { index, point ->
                    val selected = logs[index].id == selectedLogId
                    drawCircle(androidx.compose.ui.graphics.Color.White, radius = if (selected) 10f else 8f, center = point)
                    drawCircle(
                        color = if (selected) selectedColor else pointColor,
                        radius = if (selected) 7f else 5f,
                        center = point
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedWeightLogCard(
    log: WeightLog,
    unit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatWeight(log.weight, unit), fontWeight = FontWeight.Bold)
            Text(displayDate(log.loggedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (log.id.startsWith("local_weight_")) {
                Text("Pending sync", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun findNearestLog(
    offset: Offset,
    size: IntSize,
    logs: List<WeightLog>,
    unit: String,
    window: WeightGraphWindow
): WeightLog? {
    val points = chartOffsets(size, logs, unit, window)
    val index = points.indices.minByOrNull { i ->
        val point = points[i]
        abs(point.x - offset.x) + abs(point.y - offset.y)
    } ?: return null
    val point = points[index]
    return if (abs(point.x - offset.x) <= 44f && abs(point.y - offset.y) <= 44f) logs[index] else null
}

private fun chartOffsets(
    size: IntSize,
    logs: List<WeightLog>,
    unit: String,
    window: WeightGraphWindow
): List<Offset> {
    if (logs.isEmpty()) return emptyList()
    val chartLeft = 44f
    val chartTop = 12f
    val chartRight = size.width - 12f
    val chartBottom = size.height - 34f
    val days = (window.end.toEpochDay() - window.start.toEpochDay()).coerceAtLeast(1)
    val (minWeight, maxWeight) = weightBounds(logs, unit)
    val weightSpan = (maxWeight - minWeight).takeIf { it > 0.0 } ?: 1.0

    return logs.map { log ->
        val date = parseWeightLogDate(log.loggedAt) ?: window.end
        val x = chartLeft + ((date.toEpochDay() - window.start.toEpochDay()).coerceIn(0, days).toFloat() / days) *
            (chartRight - chartLeft)
        val displayWeight = displayWeightValue(log.weight, unit)
        val y = chartBottom - (((displayWeight - minWeight) / weightSpan).toFloat() * (chartBottom - chartTop))
        Offset(x, y)
    }
}

private fun weightBounds(logs: List<WeightLog>, unit: String): Pair<Double, Double> {
    val values = logs.map { displayWeightValue(it.weight, unit) }
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 1.0
    if (min == max) {
        val padding = (min * 0.1).coerceAtLeast(1.0)
        return (min - padding).coerceAtLeast(0.0) to (max + padding)
    }
    val padding = ((max - min) * 0.15).coerceAtLeast(0.5)
    return (min - padding).coerceAtLeast(0.0) to (max + padding)
}

private fun displayWeightValue(weightKg: Double, unit: String): Double =
    if (unit == "lb") weightKg * 2.20462 else weightKg

private fun formatWeight(weightKg: Double, unit: String): String =
    if (unit == "lb") {
        "${String.format(Locale.US, "%.1f", weightKg * 2.20462)} lb"
    } else {
        "${String.format(Locale.US, "%.1f", weightKg)} kg"
    }

private fun displayDate(raw: String): String =
    EventDateUtils.splitToAppDateTime(raw).first.ifBlank { raw.take(10) }
