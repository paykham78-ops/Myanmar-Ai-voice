package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun BurmeseTextInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onClear: () -> Unit,
    maxLength: Int = 5000,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label & Character Counter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(id = R.string.input_label),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Text(
                text = stringResource(id = R.string.char_count_format, "%,d".format(text.length)),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (text.length > maxLength * 0.9) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Multiline Text Area
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= maxLength) onTextChanged(it) },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.input_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    lineHeight = 22.sp
                )
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("clear_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("myanmar_text_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            maxLines = 8
        )

        // Quick Myanmar sample chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "နမူနာများ:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val samples = listOf(
                "မင်္ဂလာပါ" to "မင်္ဂလာပါ ခင်ဗျာ။ မြန်မာ AI Voice မှ ကြိုဆိုပါတယ်။",
                "သဘာဝအသံ" to "မြန်မာစာကို သဘာဝကျကျ အသံဖန်တီးပေးနိုင်ပါသည်။ စမ်းသပ်နားဆင်ကြည့်ပါ။",
                "ရာသီဥတု" to "ဒီကနေ့ ရာသီဥတုက အရမ်းသာယာလှပနေပြီး စိတ်ချမ်းသာစရာ ကောင်းပါတယ်။"
            )
            samples.forEach { (label, fullText) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable { onTextChanged(fullText) }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
