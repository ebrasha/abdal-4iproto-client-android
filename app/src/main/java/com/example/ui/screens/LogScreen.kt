/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : LogScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:59:00
 * Description : Live diagnostics screen that shows structured tunnel log cards with copy, clear and exit actions.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.AppViewModel
import com.example.ui.components.LogEntryCard
import com.example.util.TunnelLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val logEntries by TunnelLogger.entries.collectAsStateWithLifecycle()
    val loggingEnabled by viewModel.loggingEnabled.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(logEntries.size) {
        if (logEntries.isEmpty()) {
            return@LaunchedEffect
        }
        delay(400L)
        val lastIndex = logEntries.lastIndex
        if (lastIndex >= 0) {
            listState.scrollToItem(lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        scope.launch {
                            val dump = withContext(Dispatchers.Default) {
                                TunnelLogger.dump()
                            }
                            clipboardManager.setText(AnnotatedString(dump))
                            Toast.makeText(
                                context,
                                context.getString(R.string.log_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = logEntries.isNotEmpty()
                ) {
                    Text(stringResource(R.string.copy_log))
                }
                TextButton(onClick = { TunnelLogger.clear() }) {
                    Text(stringResource(R.string.clear_log))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onBackClick) {
                    Text(stringResource(R.string.exit))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    ) { padding ->
        when {
            !loggingEnabled -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.logs_disabled),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            logEntries.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.logs_empty),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logEntries, key = { it.id }) { entry ->
                        LogEntryCard(entry = entry)
                    }
                }
            }
        }
    }
}
