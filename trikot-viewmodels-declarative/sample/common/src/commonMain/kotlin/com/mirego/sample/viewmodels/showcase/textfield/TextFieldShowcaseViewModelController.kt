package com.mirego.sample.viewmodels.showcase.textfield

import com.mirego.trikot.kword.I18N
import com.mirego.trikot.viewmodels.declarative.controller.VMDViewModelController

class TextFieldShowcaseViewModelController(i18N: I18N) : VMDViewModelController<TextFieldShowcaseViewModel, TextFieldShowcaseNavigationDelegate>() {
    override val viewModel: TextFieldShowcaseViewModelImpl = TextFieldShowcaseViewModelImpl(i18N, cancellableManager)

    override fun onCreate() {
        super.onCreate()
        viewModel.closeButton.setAction { navigationDelegate?.close() }
    }
}
