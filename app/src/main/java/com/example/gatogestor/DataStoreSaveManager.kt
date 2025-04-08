package com.example.batallanavalgame

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Extensión para Context que crea una instancia de DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "batalla_naval_data")

class DataStoreSaveManager(private val context: Context) {
    private val gson = SaveGameUtils.createGson()

    // Clave para el estado del juego
    private val GAME_STATE_KEY = stringPreferencesKey("game_state")

    /**
     * Guarda el estado del juego en DataStore
     */
    fun guardarPartida(estadoJuego: EstadoPartida) {
        runBlocking {
            try {
                val jsonEstado = gson.toJson(estadoJuego)
                context.dataStore.edit { preferences ->
                    preferences[GAME_STATE_KEY] = jsonEstado
                }
            } catch (e: Exception) {
                android.util.Log.e("DataStoreSaveManager", "Error guardando partida: ${e.message}")
            }
        }
    }

    /**
     * Carga el estado del juego desde DataStore
     */
    fun cargarPartida(): EstadoPartida? {
        return runBlocking {
            try {
                val jsonEstado = context.dataStore.data.map { preferences ->
                    preferences[GAME_STATE_KEY] ?: ""
                }.first()

                if (jsonEstado.isEmpty()) {
                    return@runBlocking null
                }

                gson.fromJson(jsonEstado, EstadoPartida::class.java)
            } catch (e: Exception) {
                android.util.Log.e("DataStoreSaveManager", "Error cargando partida: ${e.message}")
                null
            }
        }
    }

    /**
     * Borra la partida guardada
     */
    fun borrarPartida() {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences.remove(GAME_STATE_KEY)
            }
        }
    }
}