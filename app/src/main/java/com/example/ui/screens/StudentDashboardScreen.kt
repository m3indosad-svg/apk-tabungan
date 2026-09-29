package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.StudentEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    student: StudentEntity?,
    transactions: List<TransactionEntity>,
    onDeposit: (Long, String) -> Unit,
    onWithdraw: (Long, String) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var isSaldoVisible by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "Setoran" -> tx.tipe.equals("Setoran", ignoreCase = true)
                "Penarikan" -> tx.tipe.contains("tarik", ignoreCase = true)
                "Menunggu ACC" -> tx.status.contains("PENDING", ignoreCase = true) || tx.status.contains("MENUNGGU", ignoreCase = true)
                "Disetujui" -> tx.status.contains("DISETUJUI", ignoreCase = true) || tx.status.contains("BERHASIL", ignoreCase = true)
                "Ditolak" -> tx.status.contains("TOLAK", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.keterangan.contains(searchQuery, ignoreCase = true) ||
                    tx.id.contains(searchQuery, ignoreCase = true) ||
                    tx.tanggal.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Buku Tabungan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${student?.nama ?: "Siswa"} • ${student?.kelas ?: ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sinkronisasi / Pengaturan")
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                        Icon(Icons.Default.Logout, contentDescription = "Keluar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Digital Student Pass / ATM-Style Balance Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_balance_card"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        PrimaryNavyDark,
                                        PrimaryNavy,
                                        Color(0xFF283593)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Row of Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "KARTU TABUNGAN SISWA",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = student?.kelas ?: "KELAS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Student Info
                            Column {
                                Text(
                                    text = student?.nama ?: "Nama Siswa",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NIS: ${student?.nis ?: "-"}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            // Saldo Aktif
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Saldo Aktif Saat Ini",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSaldoVisible) CurrencyUtils.formatRupiah(student?.saldo ?: 0L) else "Rp ••••••••",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                IconButton(
                                    onClick = { isSaldoVisible = !isSaldoVisible },
                                    modifier = Modifier.testTag("toggle_saldo_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Tampilkan / Sembunyikan Saldo",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Pending Warning Banner if any
                            if ((student?.pendingPenarikan ?: 0L) > 0) {
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = StatusPending,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Menunggu ACC Admin: ${CurrencyUtils.formatRupiah(student?.pendingPenarikan ?: 0L)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFFE65100)
                                            )
                                            Text(
                                                text = "Saldo baru akan terpotong setelah disetujui petugas.",
                                                fontSize = 10.sp,
                                                color = Color(0xFFBF360C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions: Setor & Tarik
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showDepositDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_setor_saldo")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Setor Saldo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { showWithdrawDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_tarik_saldo")
                    ) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tarik Saldo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Income / Expense Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatCard(
                        title = "Total Pemasukan",
                        value = CurrencyUtils.formatRupiah(student?.totalPemasukan ?: 0L),
                        icon = Icons.Default.ArrowDownward,
                        iconBgColor = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF2E7D32),
                        helperText = "Setoran Disetujui",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Total Pengeluaran",
                        value = CurrencyUtils.formatRupiah(student?.totalPengeluaran ?: 0L),
                        icon = Icons.Default.ArrowUpward,
                        iconBgColor = Color(0xFFFFEBEE),
                        iconColor = Color(0xFFD32F2F),
                        helperText = "Penarikan Disetujui",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Transaction Header & Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Riwayat Transaksi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredTransactions.size} Transaksi",
                            fontSize = 12.sp,
                            color = NeutralTextSecondary
                        )
                    }

                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari transaksi atau tanggal...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input")
                    )

                    // Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("Semua", "Setoran", "Penarikan", "Menunggu ACC")
                        filters.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = NeutralTextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Belum Ada Transaksi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Transaksi setoran atau penarikan Anda akan tercatat di sini secara transparan.",
                                fontSize = 12.sp,
                                color = NeutralTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        showStudentInfo = false,
                        onClick = { selectedTxForDetail = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Deposit Dialog
    if (showDepositDialog) {
        DepositDialog(
            currentSaldo = student?.saldo ?: 0L,
            onConfirm = { amount, desc ->
                onDeposit(amount, desc)
                showDepositDialog = false
            },
            onDismiss = { showDepositDialog = false }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentSaldo = student?.saldo ?: 0L,
            onConfirm = { amount, desc ->
                onWithdraw(amount, desc)
                showWithdrawDialog = false
            },
            onDismiss = { showWithdrawDialog = false }
        )
    }

    // Detail Dialog
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { selectedTxForDetail = null }
        )
    }
}

@Composable
fun DepositDialog(
    currentSaldo: Long,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val amount = amountText.toLongOrNull() ?: 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = SecondaryTeal)
                        Text(
                            text = "Setor Saldo Tabungan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Saldo Saat Ini: ${CurrencyUtils.formatRupiah(currentSaldo)}",
                    fontSize = 12.sp,
                    color = NeutralTextSecondary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Nominal Setoran (Rp)") },
                    placeholder = { Text("Contoh: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )

                QuickAmountSelector(
                    onAmountSelected = { amountText = it.toString() }
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keterangan (Opsional)") },
                    placeholder = { Text("Contoh: Sisa uang saku mingguan") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_note_input")
                )

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Setoran langsung DISETUJUI dan saldo bertambah secara real-time.",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (amount > 0) {
                            onConfirm(amount, noteText.ifBlank { "Setoran Tabungan" })
                        }
                    },
                    enabled = amount > 0,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deposit_submit_button")
                ) {
                    Text("Konfirmasi Setor Saldo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WithdrawDialog(
    currentSaldo: Long,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val amount = amountText.toLongOrNull() ?: 0L
    val isOverBalance = amount > currentSaldo

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text(
                            text = "Ajukan Penarikan Dana",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Saldo Anda Saat Ini:", fontSize = 12.sp)
                        Text(
                            text = CurrencyUtils.formatRupiah(currentSaldo),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryNavy
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Nominal Penarikan (Rp)") },
                    placeholder = { Text("Contoh: 25000") },
                    isError = isOverBalance,
                    supportingText = {
                        if (isOverBalance) {
                            Text("Nominal melebihi saldo aktif Anda!", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input")
                )

                QuickAmountSelector(
                    onAmountSelected = { amountText = it.toString() }
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keperluan / Keterangan Penarikan") },
                    placeholder = { Text("Contoh: Beli buku pelajaran / seragam") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_note_input")
                )

                Surface(
                    color = StatusPendingBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusPending, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Penarikan berstatus PENDING hingga disetujui (ACC) oleh Admin. Saldo belum terpotong sekarang.",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (amount > 0 && !isOverBalance) {
                            onConfirm(amount, noteText.ifBlank { "Penarikan Siswa" })
                        }
                    },
                    enabled = amount > 0 && !isOverBalance,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("withdraw_submit_button")
                ) {
                    Text("Kirim Pengajuan Penarikan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val isTarik = transaction.tipe.contains("tarik", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rincian Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = (if (isTarik) "- " else "+ ") + CurrencyUtils.formatRupiah(transaction.nominal),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTarik) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                    StatusBadge(status = transaction.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                DetailItemRow(label = "Nomor Transaksi", value = transaction.id)
                DetailItemRow(label = "Siswa", value = "${transaction.namaSiswa} (${transaction.nis})")
                DetailItemRow(label = "Kelas", value = transaction.kelasSiswa)
                DetailItemRow(label = "Jenis Transaksi", value = transaction.tipe)
                DetailItemRow(label = "Tanggal", value = transaction.tanggal)
                DetailItemRow(label = "Keterangan", value = transaction.keterangan.ifBlank { "-" })
                DetailItemRow(label = "Catatan Petugas / Admin", value = transaction.catatanAdmin.ifBlank { "-" })

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = NeutralTextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
