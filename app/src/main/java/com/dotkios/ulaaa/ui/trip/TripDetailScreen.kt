package com.dotkios.ulaaa.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.data.model.ChecklistItem
import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.model.SplitResult
import com.dotkios.ulaaa.data.model.TripMember

private fun money(value: Double): String = "₹%.2f".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.trip?.title ?: "Trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenChat) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Squad chat")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.trip?.let { trip ->
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DatesSection(trip = trip, onUpdateDates = viewModel::updateDates)
                AdviceSection(
                    advice = state.advice,
                    loading = state.loadingAdvice,
                    error = state.adviceError,
                    onLoad = viewModel::loadAdvice,
                )
            }

            ItinerarySection(
                stops = state.itinerary,
                isGenerating = state.isGeneratingItinerary,
                error = state.itineraryError,
                onGenerate = viewModel::generateItinerary,
                onAddStop = viewModel::addItineraryStop,
                onDeleteStop = viewModel::deleteItineraryStop,
                onClear = viewModel::clearItinerary,
            )

            MembersSection(
                members = state.members,
                addableFriends = state.addableFriends,
                onAdd = viewModel::addMember,
                onRemove = viewModel::deleteMember,
            )

            ChecklistSection(
                items = state.checklist,
                suggesting = state.suggestingChecklist,
                onAdd = viewModel::addChecklistItem,
                onSuggest = viewModel::suggestChecklist,
                onToggle = viewModel::toggleChecklist,
                onDelete = viewModel::deleteChecklistItem,
            )

            ExpensesSection(
                expenses = state.expenses,
                members = state.members,
                split = state.split,
                onAdd = viewModel::addExpense,
                onDelete = viewModel::deleteExpense,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MembersSection(
    members: List<TripMember>,
    addableFriends: List<Friend>,
    onAdd: (Friend) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    SectionCard(title = "Squad (${members.size})") {
        if (members.isEmpty()) {
            Text(
                "Add friends to split expenses with.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            members.forEach { member ->
                AssistChip(
                    onClick = { onRemove(member.id) },
                    label = { Text(member.name) },
                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove") },
                )
            }
            AssistChip(
                onClick = { showPicker = true },
                label = { Text("Add friend") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        }
    }
    if (showPicker) {
        FriendPickerDialog(
            friends = addableFriends,
            onDismiss = { showPicker = false },
            onPick = {
                onAdd(it)
                showPicker = false
            },
        )
    }
}

@Composable
private fun FriendPickerDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onPick: (Friend) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add from friends") },
        text = {
            if (friends.isEmpty()) {
                Text("No friends to add. Add friends from Profile → Friends first.")
            } else {
                Column {
                    friends.forEach { friend ->
                        TextButton(
                            onClick = { onPick(friend) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(friend.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun rupees(value: Int): String = "₹%,d".format(value)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItinerarySection(
    stops: List<ItineraryStop>,
    isGenerating: Boolean,
    error: String?,
    onGenerate: () -> Unit,
    onAddStop: (day: Int, title: String, detail: String, cost: Int) -> Unit,
    onDeleteStop: (String) -> Unit,
    onClear: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    val maxDay = stops.maxOfOrNull { it.day } ?: 0
    val totalBudget = stops.sumOf { it.cost }

    SectionCard(title = "Itinerary") {
        if (isGenerating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    "Dot is planning your trip…",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        if (!isGenerating && stops.isEmpty()) {
            Text(
                "No itinerary yet. Let Dot draft a plan with a budget, or add stops yourself.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (stops.isNotEmpty()) {
            if (totalBudget > 0) {
                Text(
                    "Estimated budget · ${rupees(totalBudget)} / person",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            stops.groupBy { it.day }.toSortedMap().forEach { (day, dayStops) ->
                val daySum = dayStops.sumOf { it.cost }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Day $day",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (daySum > 0) {
                        Text(
                            rupees(daySum),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                dayStops.forEach { stop ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(stop.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (stop.detail.isNotBlank()) {
                                Text(
                                    stop.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (stop.cost > 0) {
                            Text(
                                rupees(stop.cost),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteStop(stop.id) }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Remove stop",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        if (!isGenerating) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGenerate) {
                    Text(if (stops.isEmpty()) "Generate with AI" else "Regenerate")
                }
                AssistChip(
                    onClick = { showAdd = true },
                    label = { Text("Add stop") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                )
                if (stops.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
        }
    }

    if (showAdd) {
        AddStopDialog(
            suggestedDay = maxDay.coerceAtLeast(1),
            maxDay = maxDay,
            onDismiss = { showAdd = false },
            onConfirm = { day, title, detail, cost ->
                onAddStop(day, title, detail, cost)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AddStopDialog(
    suggestedDay: Int,
    maxDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (day: Int, title: String, detail: String, cost: Int) -> Unit,
) {
    var dayText by remember { mutableStateOf(suggestedDay.toString()) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    val day = dayText.toIntOrNull() ?: suggestedDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add stop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Day") },
                    supportingText = { Text("Use day ${maxDay + 1} to start a new day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Place or activity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter(Char::isDigit).take(7) },
                    label = { Text("Cost ₹ (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(day.coerceAtLeast(1), title, detail, costText.toIntOrNull() ?: 0)
                },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChecklistSection(
    items: List<ChecklistItem>,
    suggesting: Boolean,
    onAdd: (String) -> Unit,
    onSuggest: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    SectionCard(title = "Checklist") {
        if (items.isEmpty() && !suggesting) {
            Text(
                "Nothing packed yet. Add items, or let Dot suggest essentials for your trip.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { onToggle(item.id, it) },
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDelete(item.id) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add an item…") },
                singleLine = true,
            )
            IconButton(
                onClick = {
                    onAdd(input)
                    input = ""
                },
                enabled = input.isNotBlank(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add item", tint = MaterialTheme.colorScheme.primary)
            }
        }
        AssistChip(
            onClick = onSuggest,
            enabled = !suggesting,
            label = { Text(if (suggesting) "Suggesting…" else "Suggest with AI") },
            leadingIcon = {
                if (suggesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatesSection(trip: com.dotkios.ulaaa.data.model.Trip, onUpdateDates: (Long, Long) -> Unit) {
    var showEditor by remember { mutableStateOf(false) }
    SectionCard(title = "Dates & duration") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(trip.dateRange, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${trip.days} ${if (trip.days == 1) "day" else "days"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { showEditor = true }) { Text("Edit") }
        }
    }
    if (showEditor) {
        DatesEditorDialog(
            initialStart = trip.startMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
            initialDays = trip.days.coerceAtLeast(1),
            onDismiss = { showEditor = false },
            onConfirm = { start, days ->
                val end = start + (days - 1).coerceAtLeast(0) * 86_400_000L
                onUpdateDates(start, end)
                showEditor = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatesEditorDialog(
    initialStart: Long,
    initialDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, days: Int) -> Unit,
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialStart)
    var daysText by remember { mutableStateOf(initialDays.toString()) }
    val days = daysText.toIntOrNull()?.coerceAtLeast(1) ?: initialDays

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit dates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Start date", style = MaterialTheme.typography.labelLarge)
                DatePicker(state = dateState, title = null, headline = null, showModeToggle = false)
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Number of days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(dateState.selectedDateMillis ?: initialStart, days) },
                enabled = dateState.selectedDateMillis != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AdviceSection(
    advice: com.dotkios.ulaaa.data.model.TripAdvice?,
    loading: Boolean,
    error: String?,
    onLoad: () -> Unit,
) {
    SectionCard(title = "Weather & concerns") {
        when {
            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Checking the forecast…", modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            advice != null -> {
                if (advice.weather.isNotBlank()) {
                    Text("🌤  ${advice.weather}", style = MaterialTheme.typography.bodyMedium)
                }
                if (advice.bestTime.isNotBlank()) {
                    Text(
                        "Best time · ${advice.bestTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                advice.concerns.forEach { concern ->
                    Text("•  $concern", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLoad) { Text("Refresh") }
            }

            else -> {
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                AssistChip(
                    onClick = onLoad,
                    label = { Text("Get weather & tips") },
                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun ExpensesSection(
    expenses: List<Expense>,
    members: List<TripMember>,
    split: SplitResult,
    onAdd: (String, Double, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SectionCard(title = "Expenses") {
        if (expenses.isEmpty()) {
            Text(
                "No expenses yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        expenses.forEach { expense ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Paid by ${expense.paidBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(money(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { onDelete(expense.id) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (expenses.isNotEmpty() && members.isNotEmpty()) {
            SettleUp(split)
        }

        AssistChip(
            onClick = { showDialog = true },
            label = { Text("Add expense") },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
        )
    }

    if (showDialog) {
        AddExpenseDialog(
            members = members,
            onDismiss = { showDialog = false },
            onConfirm = { title, amount, paidBy ->
                onAdd(title, amount, paidBy)
                showDialog = false
            },
        )
    }
}

@Composable
private fun SettleUp(split: SplitResult) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Total ${money(split.total)} · ${money(split.perPerson)} each",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        // Per-person: what they paid vs their equal share, and the net.
        split.balances.forEach { (name, net) ->
            val paid = net + split.perPerson
            val status = when {
                net > 0.01 -> "gets back ${money(net)}"
                net < -0.01 -> "owes ${money(-net)}"
                else -> "settled"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "$name · paid ${money(paid)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        net > 0.01 -> MaterialTheme.colorScheme.primary
                        net < -0.01 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Text(
            "Settle up",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (split.settlements.isEmpty()) {
            Text("All settled up 🎉", style = MaterialTheme.typography.bodyMedium)
        } else {
            split.settlements.forEach { s ->
                Text(
                    "${s.from} → pays ${s.to} ${money(s.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, paidBy: String) -> Unit,
) {
    // Fall back to "Me" so expenses can be tracked before any friends are added.
    val payers = members.map { it.name }.ifEmpty { listOf("Me") }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(payers.first()) }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Paid by", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    payers.forEach { payer ->
                        FilterChip(
                            selected = paidBy == payer,
                            onClick = { paidBy = payer },
                            label = { Text(payer) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, amount, paidBy) },
                enabled = title.isNotBlank() && amount > 0.0 && paidBy.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

