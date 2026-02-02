package com.example.aiassistant.page

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aiassistant.data.model.PickedFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerPage(
    onBack: () -> Unit,
    onPicked: (List<PickedFile>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val resolver = context.contentResolver

    val pickedFiles = remember { mutableStateListOf<PickedFile>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            pickedFiles.clear()
            pickedFiles.addAll(
                uris.map { uri ->
                    resolver.toPickedFile(uri)
                },
            )
        },
    )

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("*/*"))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "选择文件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onPicked(pickedFiles.toList()) },
                        enabled = pickedFiles.isNotEmpty(),
                    ) {
                        Text(text = "完成(${pickedFiles.size})")
                    }
                },
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            TextButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                Text(text = "重新选择")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (pickedFiles.isEmpty()) {
                Text(text = "未选择文件", color = colors.onSurfaceVariant)
            }

            pickedFiles.forEach { file ->
                Surface(
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.fileName,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val sub = buildString {
                                if (file.mimeType != null) append(file.mimeType)
                                if (file.sizeBytes != null) {
                                    if (isNotEmpty()) append(" · ")
                                    append("${file.sizeBytes}B")
                                }
                            }
                            if (sub.isNotBlank()) {
                                Text(
                                    text = sub,
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ContentResolver.toPickedFile(uri: Uri): PickedFile {
    val mimeType = getType(uri)
    var fileName: String? = null
    var sizeBytes: Long? = null

    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
        if (c.moveToFirst()) {
            if (nameIdx >= 0) fileName = c.getString(nameIdx)
            if (sizeIdx >= 0) sizeBytes = c.getLong(sizeIdx)
        }
    }

    val resolvedName = fileName ?: (uri.lastPathSegment ?: "文件")
    return PickedFile(
        contentUri = uri.toString(),
        fileName = resolvedName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )
}
