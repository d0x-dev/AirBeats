package com.darkxvenom.airbeats.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.darkxvenom.airbeats.extensions.toEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Cache for DataStore values to avoid blocking calls
private val dataStoreCache = mutableMapOf<String, Any?>()

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    val cacheKey = key.name
    @Suppress("UNCHECKED_CAST")
    if (dataStoreCache.containsKey(cacheKey)) return dataStoreCache[cacheKey] as? T
    return null
}

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = get(key) ?: defaultValue

// Suspend version for critical paths
suspend fun <T> DataStore<Preferences>.getSuspend(key: Preferences.Key<T>): T? {
    val cacheKey = key.name
    @Suppress("UNCHECKED_CAST")
    if (dataStoreCache.containsKey(cacheKey)) return dataStoreCache[cacheKey] as? T
    val value = data.first()[key]
    dataStoreCache[cacheKey] = value
    return value
}

suspend fun <T> DataStore<Preferences>.getSuspend(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = getSuspend(key) ?: defaultValue

// Initialize cache - call this from App.onCreate()
suspend fun DataStore<Preferences>.initializeCache() {
    val prefs = data.first()
    prefs.asMap().forEach { (key, value) ->
        dataStoreCache[key.name] = value
    }
}

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val state =
        remember {
            context.dataStore.data
                .map { it[key] ?: defaultValue }
                .distinctUntilChanged()
        }.collectAsState(context.dataStore[key] ?: defaultValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    // Update cache immediately
                    dataStoreCache[key.name] = value
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialValue = context.dataStore[key].toEnum(defaultValue = defaultValue)
    val state =
        remember {
            context.dataStore.data
                .map { it[key].toEnum(defaultValue = defaultValue) }
                .distinctUntilChanged()
        }.collectAsState(initialValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    // Update cache immediately
                    dataStoreCache[key.name] = value.name
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value.name
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
