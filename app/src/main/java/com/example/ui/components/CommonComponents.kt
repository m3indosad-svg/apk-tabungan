package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.uppercase()
    val (bgColor, textColor, icon, label) = when {
        normalized.contains("DISETUJUI") || normalized.contains("APPROVED") || normalized.contains("BERHASIL") -> {
            Tuple4(
                StatusApprovedBg,
                StatusApproved,
                Icons.Default.CheckCircle,
                "DISETUJUI"
            )
        }
        normalized.contains("PENDING") || normalized.contains("MENUNGGU") -> {
            Tuple4(
                StatusPendingBg,
                StatusPending,
                Icons.Default.Schedule,
                "MENUNGGU ACC"
            )
        }
        else -> {
            Tuple4(
                StatusRejectedBg,
                StatusRejected,
                Icons.Default.Cancel,
                "DITOLAK"
            )
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    showStudentInfo: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTarik = transaction.tipe.contains("tarik", ignoreCase = true) ||
            transaction.tipe.equals("Penarikan", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isTarik) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTarik) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                            contentDescription = transaction.tipe,
                            tint = if (isTarik) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isTarik) "Penarikan Dana" else "Setoran Saldo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showStudentInfo) {
                            Text(
                                text = "${transaction.namaSiswa} (${transaction.kelasSiswa})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = transaction.tanggal,
                                fontSize = 12.sp,
                                color = NeutralTextSecondary
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val amountColor = if (isTarik) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    val prefix = if (isTarik) "- " else "+ "
                    Text(
                        text = prefix + CurrencyUtils.formatRupiah(transaction.nominal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StatusBadge(status = transaction.status)
                }
            }

            if (transaction.keterangan.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Keperluan: ${transaction.keterangan}",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (showStudentInfo) {
                        Text(
                            text = transaction.tanggal,
                            fontSize = 11.sp,
                            color = NeutralTextSecondary
                        )
                    }
                }
            }

            if (transaction.catatanAdmin.isNotBlank() && transaction.catatanAdmin != "Otomatis Disetujui") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan Admin: ${transaction.catatanAdmin}",
                    fontSize = 11.sp,
                    color = if (transaction.status.contains("TOLAK", ignoreCase = true)) StatusRejected else NeutralTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    helperText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeutralTextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (helperText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = helperText,
                    fontSize = 11.sp,
                    color = NeutralTextSecondary
                )
            }
        }
    }
}

@Composable
fun QuickAmountSelector(
    onAmountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickAmounts = listOf(10000L, 20000L, 50000L, 100000L, 250000L, 500000L)
    Column(modifier = modifier) {
        Text(
            text = "Pilihan Cepat Nominal:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = NeutralTextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickAmounts.take(3).forEach { amount ->
                OutlinedButton(
                    onClick = { onAmountSelected(amount) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatNumber(amount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickAmounts.takeLast(3).forEach { amount ->
                OutlinedButton(
                    onClick = { onAmountSelected(amount) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatNumber(amount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
