package com.example.bookkeeping.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.bookkeeping.R

/**
 * 备注和照片输入组件
 */
@Composable
fun NoteAndPhotoSection(
    note: String,
    onNoteChange: (String) -> Unit,
    photoUri: String?,
    onPhotoSelect: () -> Unit,
    onPhotoRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 备注输入框
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_note_optional)) },
            placeholder = { Text(stringResource(R.string.placeholder_note)) },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
        )

        // 照片选择或预览
        if (photoUri != null) {
            PhotoPreviewCard(
                photoUri = photoUri,
                onRemove = onPhotoRemove,
            )
        } else {
            PhotoSelectButton(
                onClick = onPhotoSelect,
            )
        }
    }
}

/**
 * 照片选择按钮
 */
@Composable
fun PhotoSelectButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = stringResource(R.string.photo_add_desc),
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.photo_add_label),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * 照片预览卡片
 */
@Composable
fun PhotoPreviewCard(
    photoUri: String,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        AsyncImage(
            model = photoUri,
            contentDescription = stringResource(R.string.photo_preview_desc),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )

        // 右上角删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.photo_delete_desc),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}
