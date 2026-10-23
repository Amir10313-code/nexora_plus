package com.mahirex.nexoraplus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahirex.nexoraplus.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDateTime

abstract class BaseViewModel(protected val c: AppContainer) : ViewModel() {

    /** Id of the selected account (0 while none exists yet). */
    val accountId: StateFlow<Long> =
        c.accountRepository.activeAccountId.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> perAccount(block: (Long) -> Flow<T>): Flow<T> =
        c.accountRepository.activeAccountId.flatMapLatest { block(it) }

    protected fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    /** Today's epoch day; emits again after midnight. */
    protected fun todayFlow(): Flow<Long> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate().toEpochDay())
            val next = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(maxOf(1_000L, Duration.between(now, next).toMillis() + 500L))
        }
    }
}
