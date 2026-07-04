package com.dotkios.ulaaa.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.dotkios.ulaaa.data.model.SplitResult
import com.dotkios.ulaaa.data.model.TripMember

private fun money(value: Double): String = "₹%.2f".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
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
                    text = "${trip.destination} · ${trip.dateRange}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MembersSection(
                members = state.members,
                addableFriends = state.addableFriends,
                onAdd = viewModel::addMember,
                onRemove = viewModel::deleteMember,
            )

            ChecklistSection(
                items = state.checklist,
                onAdd = viewModel::addChecklistItem,
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

@Composable
private fun ChecklistSection(
    items: List<ChecklistItem>,
    onAdd: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    SectionCard(title = "Checklist") {
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
            enabled = members.isNotEmpty(),
            label = { Text(if (members.isEmpty()) "Add friends first" else "Add expense") },
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Total ${money(split.total)} · ${money(split.perPerson)} each",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (split.settlements.isEmpty()) {
            Text("All settled up 🎉", style = MaterialTheme.typography.bodyMedium)
        } else {
            split.settlements.forEach { s ->
                Text(
                    "${s.from} pays ${s.to} ${money(s.amount)}",
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
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(members.firstOrNull()?.name ?: "") }
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
                    members.forEach { member ->
                        FilterChip(
                            selected = paidBy == member.name,
                            onClick = { paidBy = member.name },
                            label = { Text(member.name) },
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

