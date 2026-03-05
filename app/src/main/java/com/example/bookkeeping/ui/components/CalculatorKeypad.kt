package com.example.bookkeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.bookkeeping.R

/**
 * 数字键盘组件
 *
 * 布局：
 *   [7][8][9] | [⌫]
 *   [4][5][6] | [+]
 *   [1][2][3] | [-]
 *   [.][0][    完成    ]
 */
@Composable
fun CalculatorKeypad(
    amount: String,
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onOperator: (Char) -> Unit,
    onEqual: () -> Unit,
    onSubmit: () -> Unit,
    onHide: () -> Unit,
    enabled: Boolean = true,
    calculatorExpression: String = "",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // 上部：3×3 数字 + 右列操作（⌫ + -）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // 左侧 3 列 × 3 行
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    KeypadButton("7", onClick = { onDigit('7') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("8", onClick = { onDigit('8') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("9", onClick = { onDigit('9') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    KeypadButton("4", onClick = { onDigit('4') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("5", onClick = { onDigit('5') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("6", onClick = { onDigit('6') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    KeypadButton("1", onClick = { onDigit('1') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("2", onClick = { onDigit('2') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                    KeypadButton("3", onClick = { onDigit('3') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
                }
            }
            // 右列：⌫  +  -
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                KeypadButton("⌫", onClick = onBackspace, isAction = true, enabled = enabled, modifier = Modifier.fillMaxWidth().height(44.dp))
                KeypadButton("+", onClick = { onOperator('+') }, isOperator = true, enabled = enabled, modifier = Modifier.fillMaxWidth().height(44.dp))
                KeypadButton("-", onClick = { onOperator('-') }, isOperator = true, enabled = enabled, modifier = Modifier.fillMaxWidth().height(44.dp))
            }
        }

        // 底部行：.  0  完成
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            KeypadButton(".", onClick = onDot, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
            KeypadButton("0", onClick = { onDigit('0') }, enabled = enabled, modifier = Modifier.weight(1f).height(44.dp))
            KeypadButton(
                stringResource(R.string.keypad_done),
                onClick = onSubmit,
                enabled = enabled,
                isConfirm = true,
                modifier = Modifier.weight(2f).height(44.dp),
            )
        }

        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isOperator: Boolean = false,
    isAction: Boolean = false,
    isConfirm: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isConfirm  -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isAction   -> MaterialTheme.colorScheme.errorContainer
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isConfirm  -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        isAction   -> MaterialTheme.colorScheme.onErrorContainer
        else       -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .background(
                color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = if (isConfirm) 16.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
