package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.TabunganSiswaTheme
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.UserRole

class MainActivity : ComponentActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TabunganSiswaTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val userRole by viewModel.userRole.collectAsStateWithLifecycle()
                val currentStudent by viewModel.currentStudent.collectAsStateWithLifecycle()
                val currentTransactions by viewModel.currentStudentTransactions.collectAsStateWithLifecycle()
                val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
                val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
                val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
                val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsStateWithLifecycle()
                val totalKas by viewModel.totalKasSekolah.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

                var showSettingsDialog by remember { mutableStateOf(false) }

                LaunchedEffect(snackBarMessage) {
                    snackBarMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackBar()
                    }
                }

                // Handle system back button gracefully
                BackHandler(enabled = userRole != UserRole.NONE) {
                    viewModel.logout()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    when (userRole) {
                        UserRole.NONE -> {
                            LoginScreen(
                                onLoginStudent = { nis, pass -> viewModel.loginStudent(nis, pass) },
                                onRegisterStudent = { nis, nama, kelas, pass ->
                                    viewModel.registerStudent(nis, nama, kelas, pass)
                                },
                                onLoginAdmin = { user, pass -> viewModel.loginAdmin(user, pass) },
                                onOpenSettings = { showSettingsDialog = true },
                                isLoading = isLoading,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        UserRole.SISWA -> {
                            StudentDashboardScreen(
                                student = currentStudent,
                                transactions = currentTransactions,
                                onDeposit = { amount, desc -> viewModel.submitDeposit(amount, desc) },
                                onWithdraw = { amount, desc -> viewModel.submitWithdrawal(amount, desc) },
                                onLogout = { viewModel.logout() },
                                onOpenSettings = { showSettingsDialog = true },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        UserRole.ADMIN -> {
                            AdminDashboardScreen(
                                admin = currentAdmin,
                                totalKas = totalKas,
                                allStudents = allStudents,
                                allTransactions = allTransactions,
                                pendingWithdrawals = pendingWithdrawals,
                                onApproveWithdrawal = { id, nis, note ->
                                    viewModel.approveWithdrawal(id, nis, note)
                                },
                                onRejectWithdrawal = { id, nis, reason ->
                                    viewModel.rejectWithdrawal(id, nis, reason)
                                },
                                onAdminAddTx = { amount, desc, nis, isSetoran ->
                                    if (isSetoran) {
                                        viewModel.submitDeposit(amount, desc, nis)
                                    } else {
                                        viewModel.submitWithdrawal(amount, desc, nis)
                                    }
                                },
                                onLogout = { viewModel.logout() },
                                onOpenSettings = { showSettingsDialog = true },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        initialUrl = viewModel.getScriptUrl(),
                        onSaveUrl = { viewModel.saveScriptUrl(it) },
                        onPing = { onResult -> viewModel.pingBackend(onResult) },
                        onResetData = { viewModel.resetDemoData() },
                        onDismiss = { showSettingsDialog = false }
                    )
                }
            }
        }
    }
}
