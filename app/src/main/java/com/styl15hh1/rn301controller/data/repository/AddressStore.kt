package com.styl15hh1.rn301controller.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AddressStore {
    suspend fun read(): String
    suspend fun write(address: String)
}
class PreferencesAddressStore(context: Context) : AddressStore {
    private val preferences = context.getSharedPreferences("receiver", Context.MODE_PRIVATE)
    override suspend fun read(): String = withContext(Dispatchers.IO) { preferences.getString("address", "") ?: "" }
    override suspend fun write(address: String) = withContext(Dispatchers.IO) {
        preferences.edit { putString("address", address) }
    }
}
