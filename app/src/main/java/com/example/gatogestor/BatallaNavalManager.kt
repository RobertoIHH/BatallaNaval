package com.example.batallanavalgame

import android.view.View
import android.widget.*
import java.util.*
import android.util.Log
import android.content.Context
import android.widget.Toast

class BatallaNavalManager(private val context: Context) {
    private val internalSaveManager = SaveGameManager(context)
    private val dataStoreSaveManager = DataStoreSaveManager(context)
    private val externalSaveManager = ExternalStorageSaveManager(context)

    enum class SaveLocation {
        INTERNAL, DATASTORE, EXTERNAL
    }

    // Ubicación de guardado por defecto
    private var saveLocation = SaveLocation.INTERNAL

    fun setSaveLocation(location: SaveLocation) {
        saveLocation = location
    }

    fun guardarPartida(estadoPartida: EstadoPartida, vista: View? = null) {
        try {
            // Guardar en la ubicación seleccionada
            val success = when (saveLocation) {
                SaveLocation.INTERNAL -> {
                    internalSaveManager.guardarPartida(estadoPartida)
                    true
                }
                SaveLocation.DATASTORE -> {
                    dataStoreSaveManager.guardarPartida(estadoPartida)
                    true
                }
                SaveLocation.EXTERNAL -> {
                    externalSaveManager.guardarPartida(estadoPartida)
                }
            }

            if (success) {
                Toast.makeText(context, R.string.partida_guardada, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al guardar partida", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("BatallaNavalManager", "Error guardando partida: ${e.message}")
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cargarPartida(): EstadoPartida? {
        try {
            // Intentar cargar desde la ubicación seleccionada
            val estadoPartida = when (saveLocation) {
                SaveLocation.INTERNAL -> internalSaveManager.cargarPartida()
                SaveLocation.DATASTORE -> dataStoreSaveManager.cargarPartida()
                SaveLocation.EXTERNAL -> externalSaveManager.cargarPartida()
            }

            // Si no se encontró en la ubicación seleccionada, intentar las otras
            if (estadoPartida == null && saveLocation != SaveLocation.INTERNAL) {
                Log.d("BatallaNavalManager", "Intentando cargar desde almacenamiento interno...")
                return internalSaveManager.cargarPartida()
            }

            if (estadoPartida == null && saveLocation != SaveLocation.DATASTORE) {
                Log.d("BatallaNavalManager", "Intentando cargar desde DataStore...")
                return dataStoreSaveManager.cargarPartida()
            }

            if (estadoPartida == null && saveLocation != SaveLocation.EXTERNAL) {
                Log.d("BatallaNavalManager", "Intentando cargar desde almacenamiento externo...")
                return externalSaveManager.cargarPartida()
            }

            return estadoPartida

        } catch (e: Exception) {
            Log.e("BatallaNavalManager", "Error cargando partida: ${e.message}")
            Toast.makeText(context, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }
}