package com.alainmtz.work_group_tasks.ui.viewmodels

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String?
)

class ContactsViewModel : ViewModel() {
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val fetchedContacts = withContext(Dispatchers.IO) {
                    val contactsList = mutableListOf<Contact>()
                    val contentResolver = context.contentResolver
                    val cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                    )

                    cursor?.use {
                        val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        while (it.moveToNext()) {
                            val id = if (idIndex != -1) it.getString(idIndex) else ""
                            val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                            val number = if (numberIndex != -1) it.getString(numberIndex) else null
                            contactsList.add(Contact(id, name, number))
                        }
                    }
                    contactsList
                }
                _contacts.value = fetchedContacts
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
