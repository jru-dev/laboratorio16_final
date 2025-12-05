package com.tecsup.galvanguerrero.eventplanner_azanero.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tecsup.galvanguerrero.eventplanner_azanero.data.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EventViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        listenToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar el listener cuando el ViewModel se destruye
        listenerRegistration?.remove()
        Log.d("EventViewModel", "ViewModel cleared y listener removido")
    }

    fun reinitializeListener() {
        Log.d("EventViewModel", "Reinicializando listener para nuevo usuario")
        listenerRegistration?.remove()
        _events.value = emptyList()
        listenToEvents()
    }

    private fun listenToEvents() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("EventViewModel", "Usuario no autenticado")
            return
        }

        Log.d("EventViewModel", "Escuchando eventos para userId: $userId")

        // Remover listener anterior si existe
        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("events")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventViewModel", "Error al escuchar eventos: ${error.message}")
                    _operationState.value = OperationState.Error(error.message ?: "Error al cargar eventos")
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("EventViewModel", "Snapshot es null")
                    return@addSnapshotListener
                }

                Log.d("EventViewModel", "Documentos recibidos: ${snapshot.documents.size}")

                val eventList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val event = Event(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            date = doc.getTimestamp("date") ?: Timestamp.now(),
                            description = doc.getString("description") ?: "",
                            userId = doc.getString("userId") ?: ""
                        )
                        Log.d("EventViewModel", "Evento leído: ${event.title}")
                        event
                    } catch (e: Exception) {
                        Log.e("EventViewModel", "Error al parsear evento: ${e.message}")
                        null
                    }
                }

                Log.d("EventViewModel", "Total eventos parseados: ${eventList.size}")
                _events.value = eventList
                _isLoading.value = false
            }
    }

    fun createEvent(title: String, date: Timestamp, description: String) {
        if (title.isBlank()) {
            _operationState.value = OperationState.Error("El título es requerido")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _operationState.value = OperationState.Error("Usuario no autenticado")
            return
        }

        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading

                val event = hashMapOf(
                    "title" to title,
                    "date" to date,
                    "description" to description,
                    "userId" to userId
                )

                Log.d("EventViewModel", "Creando evento con userId: $userId")

                val docRef = firestore.collection("events").add(event).await()

                Log.d("EventViewModel", "Evento creado con ID: ${docRef.id}")

                _operationState.value = OperationState.Success("Evento creado exitosamente")
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error al crear evento: ${e.message}")
                _operationState.value = OperationState.Error(e.message ?: "Error al crear evento")
            }
        }
    }

    fun updateEvent(eventId: String, title: String, date: Timestamp, description: String) {
        if (title.isBlank()) {
            _operationState.value = OperationState.Error("El título es requerido")
            return
        }

        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading

                val updates = hashMapOf(
                    "title" to title,
                    "date" to date,
                    "description" to description
                )

                firestore.collection("events")
                    .document(eventId)
                    .update(updates as Map<String, Any>)
                    .await()

                Log.d("EventViewModel", "Evento actualizado: $eventId")

                _operationState.value = OperationState.Success("Evento actualizado exitosamente")
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error al actualizar evento: ${e.message}")
                _operationState.value = OperationState.Error(e.message ?: "Error al actualizar evento")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading
                firestore.collection("events").document(eventId).delete().await()

                Log.d("EventViewModel", "Evento eliminado: $eventId")

                _operationState.value = OperationState.Success("Evento eliminado exitosamente")
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error al eliminar evento: ${e.message}")
                _operationState.value = OperationState.Error(e.message ?: "Error al eliminar evento")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    // Método para recargar manualmente los eventos
    fun reloadEvents() {
        _isLoading.value = true
        reinitializeListener()
    }
}

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}