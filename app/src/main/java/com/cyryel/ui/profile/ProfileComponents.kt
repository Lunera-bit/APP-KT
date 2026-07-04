package com.cyryel.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.cyryel.data.auth.AuthRepository
import com.cyryel.data.user.Address
import com.cyryel.data.user.UserRepository
import com.cyryel.ui.theme.AmarilloVibrante
import com.cyryel.ui.theme.AzulRey
import com.cyryel.ui.theme.AzulReyClaro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val photoUrl: String? = null,
    val userDni: String = "",
    val userRuc: String = "",
    val addresses: List<Address> = emptyList(),
    val isEditing: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editDni: String = "",
    val editRuc: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = userRepository.getUser(userId)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = user?.name ?: "",
                        userEmail = user?.email ?: authRepository.getCurrentUserEmail() ?: "",
                        userPhone = user?.phone ?: "",
                        photoUrl = user?.photoUrl,
                        userDni = user?.documentNumber ?: "",
                        userRuc = user?.ruc ?: "",
                        addresses = user?.addresses ?: emptyList()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userEmail = authRepository.getCurrentUserEmail() ?: "",
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }

    fun toggleEditing() {
        val current = _uiState.value
        if (current.isEditing) {
            _uiState.update { it.copy(isEditing = false) }
        } else {
            _uiState.update {
                it.copy(
                    isEditing = true,
                    editName = it.userName,
                    editPhone = it.userPhone,
                    editDni = it.userDni,
                    editRuc = it.userRuc
                )
            }
        }
    }

    fun onEditNameChange(value: String) {
        _uiState.update { it.copy(editName = value) }
    }

    fun onEditPhoneChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(9)
        _uiState.update { it.copy(editPhone = filtered) }
    }

    fun onEditDniChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(editDni = filtered) }
    }

    fun onEditRucChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(11)
        _uiState.update { it.copy(editRuc = filtered) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }

            val updates = mutableMapOf<String, Any>()
            if (state.editName != state.userName) updates["name"] = state.editName
            if (state.editPhone != state.userPhone) updates["phone"] = state.editPhone
            val trimmedDni = state.editDni.trim()
            val trimmedRuc = state.editRuc.trim()
            if (trimmedDni != state.userDni) updates["documentNumber"] = trimmedDni
            if (trimmedRuc != state.userRuc) updates["ruc"] = trimmedRuc

            if (updates.isEmpty()) {
                _uiState.update { it.copy(isSaving = false, isEditing = false) }
                return@launch
            }

            val result = userRepository.updateUser(userId, updates)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        userName = it.editName,
                        userPhone = it.editPhone,
                        userDni = it.editDni.trim(),
                        userRuc = it.editRuc.trim(),
                        saveSuccess = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    uiState: ProfileUiState,
    onToggleEdit: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onRucChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    photoUrl = uiState.photoUrl,
                    name = uiState.userName,
                    size = 80.dp,
                    textSize = 28.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            if (uiState.isEditing) {
                OutlinedTextField(
                    value = uiState.editName,
                    onValueChange = onNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.editPhone,
                    onValueChange = onPhoneChange,
                    label = { Text("Telefono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.editDni,
                    onValueChange = onDniChange,
                    label = { Text("DNI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.editRuc,
                    onValueChange = onRucChange,
                    label = { Text("RUC") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulRey)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Guardar")
                        }
                    }
                }
            } else {
                Text(
                    text = uiState.userName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = uiState.userEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.userPhone.isNotBlank()) {
                    Text(
                        text = uiState.userPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                if (uiState.userDni.isNotBlank()) {
                    Text(
                        text = "DNI: ${uiState.userDni}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                if (uiState.userRuc.isNotBlank()) {
                    Text(
                        text = "RUC: ${uiState.userRuc}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onToggleEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar perfil")
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    photoUrl: String?,
    name: String,
    size: Dp = 80.dp,
    textSize: TextUnit = 28.sp
) {
    if (photoUrl.isNullOrBlank()) {
        val initials = name.take(2).uppercase().ifBlank { "?" }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AmarilloVibrante),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                color = AzulRey,
                textAlign = TextAlign.Center
            )
        }
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AddressCard(address: Address) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = AzulReyClaro,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (address.name.isNotBlank()) {
                    Text(
                        text = address.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AzulRey
                    )
                }
                Text(
                    text = address.street,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (address.city.isNotBlank()) {
                    Text(
                        text = address.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (address.reference.isNotBlank()) {
                    Text(
                        text = address.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (address.isDefault) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "(Default)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AzulRey,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
