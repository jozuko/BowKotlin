package com.studio.jozu.bow.domain.extension

import io.reactivex.rxjava3.disposables.Disposable

object DisposableListEx {
    fun MutableList<Disposable>?.dispose() {
        this?.forEach {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        this?.clear()
    }
}