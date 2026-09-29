package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.StudentEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AdminModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    admin: AdminModel?,
    totalKas: Long,
    allStudents: List<StudentEntity>,
    allTransactions: List<TransactionEntity>,
    pendingWithdrawals: List<TransactionEntity>,
    onApproveWithdrawal: (String, String, String) -> Unit,
    onRejectWithdrawal: (String, String, String) -> Unit,
    onAdminAddTx: (Long, String, String, Boolean) -> Unit, // amount, desc, nis, isSetoran
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ACC Penarikan, 1: Siswa, 2: Semua Transaksi
    var selectedTxForApproval by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTxForRejection by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedStudentForAction by remember { mutableStateOf<StudentEntity?>(null) }

    var studentSearch by remember { mutableStateOf("") }
    var txSearch by remember { mutableStateOf("") }
    var txFilterType by remember { mutableStateOf("Semua") }

    val filteredStudents = remember(allStudents, studentSearch) {
        if (studentSearch.isBlank()) allStudents else {
            allStudents.filter {
                it.nama.contains(studentSearch, ignoreCase = true) ||
                        it.nis.contains(studentSearch, ignoreCase = true) ||
                        it.kelas.contains(studentSearch, ignoreCase = true)
            }
        }
    }

    val filteredTransactions = remember(allTransactions, txSearch, txFilterType) {
        allTransactions.filter { tx ->
            val matchesType = when (txFilterType) {
                "Setoran" -> tx.tipe.equals("Setoran", ignoreCase = true)
                "Penarikan" -> tx.tipe.contains("tarik", ignoreCase = true)
                "Disetujui" -> tx.status.contains("DISETUJUI", ignoreCase = true)
                "Pending" -> tx.status.contains("PENDING", ignoreCase = true)
                "Ditolak" -> tx.status.contains("TOLAK", ignoreCase = true)
                else -> true
            }
            val matchesSearch = txSearch.isBlank() ||
                    tx.namaSiswa.contains(txSearch, ignoreCase = true) ||
                    tx.nis.contains(txSearch, ignoreCase = true) ||
                    tx.id.contains(txSearch, ignoreCase = true) ||
                    tx.keterangan.contains(txSearch, ignoreCase = true)
            matchesType && matchesSearch
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
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = AdminCrimson,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Panel Admin Tabungan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${admin?.nama ?: "Administrator"} • ${admin?.role ?: "Super Admin"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sinkronisasi Cloud")
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("admin_logout_button")) {
                        Icon(Icons.Default.Logout, contentDescription = "Keluar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Total Kas & Pending Banner
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_kas_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL SALDO KAS SEKOLAH",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${allStudents.size} Siswa",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatRupiah(totalKas),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Simpanan Bersih Seluruh Siswa Terdaftar",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Navigation Tabs with Pending Badge
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("ACC Penarikan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            if (pendingWithdrawals.isNotEmpty()) {
                                Badge(
                                    containerColor = Color(0xFFC62828),
                                    contentColor = Color.White
                                ) {
                                    Text("${pendingWithdrawals.size}")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("admin_tab_acc"),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text("Daftar Siswa", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    },
                    modifier = Modifier
                        .background(
                            if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("admin_tab_siswa"),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text("Semua Transaksi", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    },
                    modifier = Modifier
                        .background(
                            if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("admin_tab_ledger"),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: ACC Penarikan
                    if (pendingWithdrawals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "Tidak Ada Pengajuan Pending",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Semua permintaan penarikan siswa telah diproses dan disetujui (ACC).",
                                        fontSize = 12.sp,
                                        color = NeutralTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    color = StatusPendingBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = StatusPending, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "Terdapat ${pendingWithdrawals.size} permintaan penarikan menunggu persetujuan Anda.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            items(pendingWithdrawals, key = { it.id }) { tx ->
                                PendingWithdrawalItem(
                                    transaction = tx,
                                    onApprove = { selectedTxForApproval = tx },
                                    onReject = { selectedTxForRejection = tx }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Daftar Siswa
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = studentSearch,
                            onValueChange = { studentSearch = it },
                            placeholder = { Text("Cari Siswa / NIS / Kelas...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_student_input")
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredStudents, key = { it.nis }) { student ->
                                StudentAdminCard(
                                    student = student,
                                    onClick = { selectedStudentForAction = student }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Semua Transaksi
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = txSearch,
                            onValueChange = { txSearch = it },
                            placeholder = { Text("Cari NIS / Nama / Transaksi...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_all_tx_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf("Semua", "Setoran", "Penarikan", "Disetujui", "Pending", "Ditolak")
                            filters.take(4).forEach { filter ->
                                FilterChip(
                                    selected = txFilterType == filter,
                                    onClick = { txFilterType = filter },
                                    label = { Text(filter, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { tx ->
                                TransactionCard(
                                    transaction = tx,
                                    showStudentInfo = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Approve Dialog
    selectedTxForApproval?.let { tx ->
        ApproveConfirmDialog(
            transaction = tx,
            onConfirm = { note ->
                onApproveWithdrawal(tx.id, tx.nis, note)
                selectedTxForApproval = null
            },
            onDismiss = { selectedTxForApproval = null }
        )
    }

    // Reject Dialog
    selectedTxForRejection?.let { tx ->
        RejectConfirmDialog(
            transaction = tx,
            onConfirm = { reason ->
                onRejectWithdrawal(tx.id, tx.nis, reason)
                selectedTxForRejection = null
            },
            onDismiss = { selectedTxForRejection = null }
        )
    }

    // Student Action Dialog (Admin directly adding deposit/withdrawal for student)
    selectedStudentForAction?.let { student ->
        StudentAdminActionDialog(
            student = student,
            onAddTx = { amount, desc, isSetoran ->
                onAdminAddTx(amount, desc, student.nis, isSetoran)
                selectedStudentForAction = null
            },
            onDismiss = { selectedStudentForAction = null }
        )
    }
}

@Composable
fun PendingWithdrawalItem(
    transaction: TransactionEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = transaction.namaSiswa,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "NIS: ${transaction.nis} • ${transaction.kelasSiswa}",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatRupiah(transaction.nominal),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = transaction.tanggal,
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                }
            }

            if (transaction.keterangan.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keperluan: ${transaction.keterangan}",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_reject_${transaction.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tolak", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_approve_${transaction.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Setujui (ACC)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudentAdminCard(
    student: StudentEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_card_${student.nis}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.nama.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = student.nama,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "NIS: ${student.nis} • ${student.kelas}",
                            fontSize = 12.sp,
                            color = NeutralTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatRupiah(student.saldo),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryNavy
                    )
                    Text(
                        text = "Saldo Aktif",
                        fontSize = 11.sp,
                        color = NeutralTextSecondary
                    )
                }
            }

            if (student.pendingPenarikan > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = StatusPendingBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Pending ACC: ${CurrencyUtils.formatRupiah(student.pendingPenarikan)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ApproveConfirmDialog(
    transaction: TransactionEntity,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("Disetujui Admin - Dana diserahkan di TU") }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text(
                        text = "ACC Penarikan Siswa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Konfirmasi persetujuan penarikan berikut:",
                    fontSize = 13.sp,
                    color = NeutralTextSecondary
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Siswa: ${transaction.namaSiswa} (${transaction.nis})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(text = "Kelas: ${transaction.kelasSiswa}", fontSize = 12.sp)
                        Text(
                            text = "Nominal: ${CurrencyUtils.formatRupiah(transaction.nominal)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                        Text(text = "Keperluan: ${transaction.keterangan.ifBlank { "-" }}", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Admin") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Setelah disetujui, saldo siswa akan otomatis berkurang secara resmi sesuai aturan sistem.",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { onConfirm(note) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Setujui (ACC)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RejectConfirmDialog(
    transaction: TransactionEntity,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("Saldo tidak mencukupi / Konfirmasi orang tua belum ada") }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFC62828))
                    Text(
                        text = "Tolak Penarikan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Tolak pengajuan penarikan sebesar ${CurrencyUtils.formatRupiah(transaction.nominal)} dari ${transaction.namaSiswa}:",
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Penolakan") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Saldo siswa TIDAK akan dipotong. Status transaksi akan ditandai DITOLAK.",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { onConfirm(reason) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tolak Pengajuan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAdminActionDialog(
    student: StudentEntity,
    onAddTx: (Long, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isSetoran by remember { mutableStateOf(true) }
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
                    Text(
                        text = "Kelola Tabungan Siswa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
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
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "${student.nama} (${student.kelas})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "NIS: ${student.nis}", fontSize = 12.sp, color = NeutralTextSecondary)
                        Text(
                            text = "Saldo Aktif: ${CurrencyUtils.formatRupiah(student.saldo)}",
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryNavy,
                            fontSize = 14.sp
                        )
                    }
                }

                // Type Toggle: Setoran or Penarikan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isSetoran = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSetoran) SecondaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSetoran) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Setor Langsung", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { isSetoran = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSetoran) Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isSetoran) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tarik Kas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                QuickAmountSelector(
                    onAmountSelected = { amountText = it.toString() }
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keterangan Petugas") },
                    placeholder = { Text(if (isSetoran) "Setoran Tunai di TU" else "Penarikan Tunai di TU") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (amount > 0) {
                            onAddTx(amount, noteText.ifBlank { if (isSetoran) "Setoran Petugas TU" else "Penarikan Petugas TU" }, isSetoran)
                        }
                    },
                    enabled = amount > 0,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSetoran) SecondaryTeal else Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isSetoran) "Proses Setoran" else "Proses Penarikan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
