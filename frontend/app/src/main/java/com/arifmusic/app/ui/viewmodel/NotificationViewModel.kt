package com.arifmusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arifmusic.app.data.model.Notification
import com.arifmusic.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadNotifications()
    }

    fun loadNotifications() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                notificationRepository.getNotifications().collect { notifications ->
                    _notifications.value = notifications
                }
                _unreadCount.value = notificationRepository.getUnreadCount()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
                // Update the notification in the list
                val updatedList = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                _notifications.value = updatedList
                _unreadCount.value = notificationRepository.getUnreadCount()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead()
                // Update all notifications in the list
                val updatedList = _notifications.value.map { it.copy(isRead = true) }
                _notifications.value = updatedList
                _unreadCount.value = 0
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
                // Remove the notification from the list
                val updatedList = _notifications.value.filter { it.id != notificationId }
                _notifications.value = updatedList
                _unreadCount.value = notificationRepository.getUnreadCount()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.clearAllNotifications()
                _notifications.value = emptyList()
                _unreadCount.value = 0
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
