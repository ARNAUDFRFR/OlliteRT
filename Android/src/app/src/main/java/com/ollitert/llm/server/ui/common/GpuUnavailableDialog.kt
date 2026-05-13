/*
 * Copyright 2025-2026 @NightMean (https://github.com/NightMean)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ollitert.llm.server.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.ollitert.llm.server.R
import com.ollitert.llm.server.ui.theme.customColors

private const val LITERT_LM_ISSUES_URL = "https://github.com/google-ai-edge/LiteRT-LM/issues"

@Composable
fun GpuUnavailableDialog(
  onDismiss: () -> Unit,
  showDontShowAgain: Boolean = false,
  onDontShowAgainChecked: ((Boolean) -> Unit)? = null,
) {
  val uriHandler = LocalUriHandler.current
  var dontShowAgain by remember { mutableStateOf(false) }

  val bodyText = buildAnnotatedString {
    append(stringResource(R.string.gpu_unavailable_dialog_body))
    append(" ")
    withLink(
      link = LinkAnnotation.Url(
        url = LITERT_LM_ISSUES_URL,
        styles = TextLinkStyles(
          style = SpanStyle(
            color = MaterialTheme.customColors.linkColor,
            textDecoration = TextDecoration.Underline,
          ),
        ),
        linkInteractionListener = { uriHandler.openUri(LITERT_LM_ISSUES_URL) },
      ),
    ) {
      append(stringResource(R.string.gpu_unavailable_dialog_link_text))
    }
  }

  AlertDialog(
    onDismissRequest = {
      if (showDontShowAgain) onDontShowAgainChecked?.invoke(dontShowAgain)
      onDismiss()
    },
    title = { Text(stringResource(R.string.gpu_unavailable_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = bodyText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showDontShowAgain) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          ) {
            Checkbox(
              checked = dontShowAgain,
              onCheckedChange = { dontShowAgain = it },
            )
            Text(
              text = stringResource(R.string.gpu_unavailable_dialog_dont_show_again),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        if (showDontShowAgain) onDontShowAgainChecked?.invoke(dontShowAgain)
        onDismiss()
      }) {
        Text(stringResource(R.string.ok))
      }
    },
  )
}
