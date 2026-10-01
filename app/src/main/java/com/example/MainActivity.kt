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
                val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
                val currentTransactions by viewModel.currentMemberTransactions.collectAsStateWithLifecycle()
                val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
                val allMembers by viewModel.allMembers.collectAsStateWithLifecycle()
                val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
                val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsStateWithLifecycle()
                val totalKas by viewModel.totalKasTabungan.collectAsStateWithLifecycle()
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
                                onLoginMember = { noRek, pass -> viewModel.loginMember(noRek, pass) },
                                onRegisterMember = { noRek, nama, alamat, pass ->
                                    viewModel.registerMember(noRek, nama, alamat, pass)
                                },
                                onLoginAdmin = { user, pass -> viewModel.loginAdmin(user, pass) },
                                onOpenSettings = { showSettingsDialog = true },
                                isLoading = isLoading,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        UserRole.ANGGOTA -> {
                            MemberDashboardScreen(
                                member = currentMember,
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
                                allMembers = allMembers,
                                allTransactions = allTransactions,
                                pendingWithdrawals = pendingWithdrawals,
                                onApproveWithdrawal = { id, noRek, note ->
                                    viewModel.approveWithdrawal(id, noRek, note)
                                },
                                onRejectWithdrawal = { id, noRek, reason ->
                                    viewModel.rejectWithdrawal(id, noRek, reason)
                                },
                                onAdminAddTx = { amount, desc, noRek, isSetoran ->
                                    if (isSetoran) {
                                        viewModel.submitDeposit(amount, desc, noRek)
                                    } else {
                                        viewModel.submitWithdrawal(amount, desc, noRek)
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
