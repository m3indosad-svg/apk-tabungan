package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginStudent: (String, String) -> Unit,
    onRegisterStudent: (String, String, String, String) -> Unit,
    onLoginAdmin: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Siswa, 1: Admin
    var nisInput by remember { mutableStateOf("2026101") }
    var studentPasswordInput by remember { mutableStateOf("123456") }
    var isStudentPasswordVisible by remember { mutableStateOf(false) }

    var adminUsernameInput by remember { mutableStateOf("admin") }
    var adminPasswordInput by remember { mutableStateOf("admin123") }
    var isAdminPasswordVisible by remember { mutableStateOf(false) }

    var showRegisterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Tabungan Siswa",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Sistem Kas & ACC Penarikan",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Backend"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual banner
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.banner_savings),
                        contentDescription = "Banner Tabungan Siswa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.35f)
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Surface(
                            color = PrimaryNavy.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "GOOGLE SHEETS SYNC READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aman, Transparan & Terintegrasi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Tabs for Siswa / Admin
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Siswa", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("tab_siswa"),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Admin Tabungan", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("tab_admin"),
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedTab == 0) {
                // Siswa Form
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Masuk Sebagai Siswa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        OutlinedTextField(
                            value = nisInput,
                            onValueChange = { nisInput = it },
                            label = { Text("Nomor Induk Siswa (NIS)") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_nis_input")
                        )

                        OutlinedTextField(
                            value = studentPasswordInput,
                            onValueChange = { studentPasswordInput = it },
                            label = { Text("Kata Sandi") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isStudentPasswordVisible = !isStudentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isStudentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isStudentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_pass_input")
                        )

                        Button(
                            onClick = {
                                onLoginStudent(nisInput, studentPasswordInput)
                            },
                            enabled = !isLoading && nisInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("student_login_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk Tabungan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Register button
                        OutlinedButton(
                            onClick = { showRegisterDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_register_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daftar Siswa Baru")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Quick Test Accounts
                        Text(
                            text = "Akun Contoh Siap Pakai (1-Klik):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    nisInput = "2026101"
                                    studentPasswordInput = "123456"
                                },
                                label = { Text("Ahmad Dani (2026101)", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = {
                                    nisInput = "2026102"
                                    studentPasswordInput = "123456"
                                },
                                label = { Text("Siti (2026102)", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Admin Form
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Masuk Sebagai Petugas Admin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        OutlinedTextField(
                            value = adminUsernameInput,
                            onValueChange = { adminUsernameInput = it },
                            label = { Text("Username Admin") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_username_input")
                        )

                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = { adminPasswordInput = it },
                            label = { Text("Kata Sandi Admin") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isAdminPasswordVisible = !isAdminPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isAdminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isAdminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pass_input")
                        )

                        Button(
                            onClick = {
                                onLoginAdmin(adminUsernameInput, adminPasswordInput)
                            },
                            enabled = !isLoading && adminUsernameInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_login_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk Panel Admin", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Akun Petugas Default:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AssistChip(
                            onClick = {
                                adminUsernameInput = "admin"
                                adminPasswordInput = "admin123"
                            },
                            label = { Text("Admin Tabungan (admin / admin123)", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        StudentRegisterDialog(
            onRegister = { nis, nama, kelas, pass ->
                onRegisterStudent(nis, nama, kelas, pass)
                showRegisterDialog = false
            },
            onDismiss = { showRegisterDialog = false }
        )
    }
}

@Composable
fun StudentRegisterDialog(
    onRegister: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nis by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var kelas by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pendaftaran Siswa Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = nis,
                    onValueChange = { nis = it },
                    label = { Text("NIS Siswa") },
                    placeholder = { Text("Contoh: 2026105") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_nis_input")
                )

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap Siswa") },
                    placeholder = { Text("Contoh: Rizky Pratama") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_nama_input")
                )

                OutlinedTextField(
                    value = kelas,
                    onValueChange = { kelas = it },
                    label = { Text("Kelas") },
                    placeholder = { Text("Contoh: 10 IPA 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_kelas_input")
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Kata Sandi") },
                    placeholder = { Text("Minimal 4 karakter") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_pass_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (nis.isNotBlank() && nama.isNotBlank() && kelas.isNotBlank() && pass.isNotBlank()) {
                            onRegister(nis, nama, kelas, pass)
                        }
                    },
                    enabled = nis.isNotBlank() && nama.isNotBlank() && kelas.isNotBlank() && pass.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_submit_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Daftar Sekarang", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
