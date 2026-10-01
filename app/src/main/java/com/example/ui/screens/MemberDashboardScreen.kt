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
import com.example.data.local.MemberEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    member: MemberEntity?,
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
                                text = "${member?.namaLengkap ?: "Anggota"} • Rek: ${member?.noRek ?: "-"}",
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
                // Digital Member Pass / ATM-Style Balance Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_balance_card"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0D1B2A),
                                        Color(0xFF1B263B),
                                        Color(0xFF415A77)
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
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "KARTU TABUNGAN ANGGOTA",
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
                                        text = "ANGGOTA AKTIF",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Member Info: Nama Lengkap, No Rekening, Alamat
                            Column {
                                Text(
                                    text = member?.namaLengkap ?: "Nama Anggota",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No. Rek: ${member?.noRek ?: "-"}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!member?.alamat.isNullOrBlank()) {
                                    Text(
                                        text = "Alamat: ${member.alamat}",
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.15f),
                                thickness = 0.8.dp
                            )

                            // Saldo Aktif
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
                                            text = "SALDO TABUNGAN",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.8.sp
                                        )
                                        IconButton(
                                            onClick = { isSaldoVisible = !isSaldoVisible },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSaldoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Saldo",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isSaldoVisible) CurrencyUtils.formatRupiah(member?.saldo ?: 0L) else "Rp ••••••••",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                val pending = member?.pendingPenarikan ?: 0L
                                if (pending > 0L) {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "Menunggu ACC",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                            Text(
                                                text = CurrencyUtils.formatRupiah(pending),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons: Setor Saldo & Tarik Saldo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showDepositDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("student_deposit_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Setor Saldo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { showWithdrawDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("student_withdraw_button")
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tarik Saldo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Summary Stats Cards (Total Pemasukan & Pengeluaran)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatCard(
                        title = "Total Masuk",
                        value = CurrencyUtils.formatRupiah(member?.totalPemasukan ?: 0L),
                        icon = Icons.Default.ArrowDownward,
                        iconBgColor = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF2E7D32),
                        helperText = "Simpanan Disetujui",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        title = "Total Keluar",
                        value = CurrencyUtils.formatRupiah(member?.totalPengeluaran ?: 0L),
                        icon = Icons.Default.ArrowOutward,
                        iconBgColor = Color(0xFFFFEBEE),
                        iconColor = Color(0xFFC62828),
                        helperText = "Penarikan Disetujui",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search and Filters Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Riwayat Transaksi Anggota",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari tanggal, keperluan, kode TX...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input")
                    )

                    // Filter Chips Row
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

            // Transaction Items
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum Ada Transaksi",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mulai menabung dengan menekan tombol Setor Saldo di atas.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        showMemberInfo = false,
                        onClick = { selectedTxForDetail = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog Setor Saldo
    if (showDepositDialog) {
        DepositDialog(
            onDismiss = { showDepositDialog = false },
            onConfirm = { nominal, ket ->
                onDeposit(nominal, ket)
                showDepositDialog = false
            }
        )
    }

    // Dialog Tarik Saldo
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentBalance = member?.saldo ?: 0L,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { nominal, ket ->
                onWithdraw(nominal, ket)
                showWithdrawDialog = false
            }
        )
    }

    // Detail Transaction Dialog
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { selectedTxForDetail = null }
        )
    }
}

// Compatibility wrapper
@Composable
fun StudentDashboardScreen(
    student: MemberEntity?,
    transactions: List<TransactionEntity>,
    onDeposit: (Long, String) -> Unit,
    onWithdraw: (Long, String) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) = MemberDashboardScreen(
    member = student,
    transactions = transactions,
    onDeposit = onDeposit,
    onWithdraw = onWithdraw,
    onLogout = onLogout,
    onOpenSettings = onOpenSettings,
    modifier = modifier
)

@Composable
private fun DepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var keteranganText by remember { mutableStateOf("Setoran Tabungan Anggota") }
    val amount = amountText.toLongOrNull() ?: 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                    }
                    Text(
                        text = "Setor Saldo Tabungan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) amountText = input
                    },
                    label = { Text("Nominal Setoran (Rp)") },
                    placeholder = { Text("Misal: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )

                QuickAmountSelector(
                    onAmountSelected = { amountText = it.toString() }
                )

                OutlinedTextField(
                    value = keteranganText,
                    onValueChange = { keteranganText = it },
                    label = { Text("Keterangan / Berita Setoran") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Setoran langsung berstatus DISETUJUI dan menambah saldo anggota.",
                            fontSize = 11.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(amount, keteranganText) },
                        enabled = amount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_deposit_button")
                    ) {
                        Text("Setor Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WithdrawDialog(
    currentBalance: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var keteranganText by remember { mutableStateOf("") }
    val amount = amountText.toLongOrNull() ?: 0L
    val isAmountValid = amount in 1..currentBalance

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircle,
                            contentDescription = null,
                            tint = Color(0xFFC62828)
                        )
                    }
                    Text(
                        text = "Tarik Saldo Tabungan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Maksimum Penarikan:", fontSize = 12.sp, color = NeutralTextSecondary)
                        Text(
                            text = CurrencyUtils.formatRupiah(currentBalance),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) amountText = input
                    },
                    label = { Text("Nominal Penarikan (Rp)") },
                    placeholder = { Text("Misal: 25000") },
                    isError = amount > currentBalance,
                    supportingText = {
                        if (amount > currentBalance) {
                            Text("Nominal melebihi saldo tabungan Anda!", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input")
                )

                QuickAmountSelector(
                    onAmountSelected = {
                        if (it <= currentBalance) amountText = it.toString()
                    }
                )

                OutlinedTextField(
                    value = keteranganText,
                    onValueChange = { keteranganText = it },
                    label = { Text("Keperluan Penarikan") },
                    placeholder = { Text("Misal: Kebutuhan darurat, belanja, dll.") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_note_input")
                )

                // Info notice regarding ACC status
                Surface(
                    color = StatusPendingBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusPending,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Penarikan akan berstatus PENDING hingga disetujui (ACC) oleh Pengurus. Saldo Anda belum berkurang sampai diverifikasi.",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(amount, keteranganText.ifBlank { "Penarikan Tabungan Anggota" }) },
                        enabled = isAmountValid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_withdraw_button")
                    ) {
                        Text("Kirim Pengajuan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rincian Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    StatusBadge(status = transaction.status)
                }

                HorizontalDivider()

                DetailItem(label = "Nomor Transaksi", value = transaction.id)
                DetailItem(label = "No. Rekening", value = transaction.noRek)
                DetailItem(label = "Nama Anggota", value = transaction.namaAnggota)
                if (transaction.alamatAnggota.isNotBlank()) {
                    DetailItem(label = "Alamat", value = transaction.alamatAnggota)
                }
                DetailItem(label = "Jenis Transaksi", value = transaction.tipe)
                DetailItem(
                    label = "Nominal",
                    value = CurrencyUtils.formatRupiah(transaction.nominal),
                    valueColor = if (transaction.tipe.contains("tarik", ignoreCase = true)) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
                DetailItem(label = "Tanggal", value = transaction.tanggal)
                DetailItem(label = "Keperluan", value = transaction.keterangan.ifBlank { "-" })

                if (transaction.catatanAdmin.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Catatan Pengurus:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = transaction.catatanAdmin,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = NeutralTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
