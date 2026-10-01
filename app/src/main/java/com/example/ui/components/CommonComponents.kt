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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MemberEntity
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
    showMemberInfo: Boolean = false,
    showStudentInfo: Boolean = showMemberInfo,
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
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
                            text = if (isTarik) "Penarikan Tabungan" else "Setoran Simpanan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showMemberInfo || showStudentInfo) {
                            Text(
                                text = "${transaction.namaAnggota} • Rek: ${transaction.noRek}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        fontSize = 14.sp,
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
                    if (showMemberInfo || showStudentInfo) {
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
                    text = "Catatan Pengurus: ${transaction.catatanAdmin}",
                    fontSize = 11.sp,
                    color = if (transaction.status.contains("TOLAK", ignoreCase = true)) StatusRejected else NeutralTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MemberCard(
    member: MemberEntity?,
    isSaldoVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_card_info"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F2027),
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "KARTU TABUNGAN ANGGOTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Koperasi & Komunitas",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Account Number chip
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Rek: ${member?.noRek ?: "---"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Member Identity: Nama & Alamat
                Text(
                    text = member?.namaLengkap ?: "Nama Anggota",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (!member?.alamat.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = member.alamat,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Balance Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SALDO AKTIF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.8.sp
                            )
                            IconButton(
                                onClick = onToggleVisibility,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Saldo",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSaldoVisible) CurrencyUtils.formatRupiah(member?.saldo ?: 0L) else "Rp ••••••••",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold
                        )
                    }

                    // Pending ACC notice if any
                    val pending = member?.pendingPenarikan ?: 0L
                    if (pending > 0L) {
                        Surface(
                            color = StatusPendingBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Menunggu ACC",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPending
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(pending),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusPending
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Wrapper for compatibility
@Composable
fun StudentCard(
    student: MemberEntity?,
    isSaldoVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) = MemberCard(
    member = student,
    isSaldoVisible = isSaldoVisible,
    onToggleVisibility = onToggleVisibility,
    modifier = modifier
)

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
    val quickAmounts = listOf(25000L, 50000L, 100000L, 250000L, 500000L, 1000000L)
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
