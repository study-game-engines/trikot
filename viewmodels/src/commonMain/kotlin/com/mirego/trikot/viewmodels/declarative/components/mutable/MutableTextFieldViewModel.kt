package com.mirego.trikot.viewmodels.declarative.components.mutable

import com.mirego.trikot.streams.cancellable.CancellableManager
import com.mirego.trikot.streams.reactive.PublishSubjectImpl
import com.mirego.trikot.streams.reactive.first
import com.mirego.trikot.streams.reactive.observeOn
import com.mirego.trikot.streams.reactive.subscribe
import com.mirego.trikot.viewmodels.declarative.components.TextFieldViewModel
import com.mirego.trikot.viewmodels.declarative.internal.PublishedProperty
import com.mirego.trikot.viewmodels.declarative.internal.published
import com.mirego.trikot.viewmodels.declarative.mutable.MutableViewModel
import com.mirego.trikot.viewmodels.declarative.properties.KeyboardAutoCapitalization
import com.mirego.trikot.viewmodels.declarative.properties.KeyboardReturnKeyType
import com.mirego.trikot.viewmodels.declarative.properties.KeyboardType
import com.mirego.trikot.viewmodels.declarative.properties.TextContentType
import com.mirego.trikot.viewmodels.declarative.utilities.DispatchQueues
import kotlin.reflect.KProperty
import org.reactivestreams.Publisher

class MutableTextFieldViewModel(cancellableManager: CancellableManager) :
    MutableViewModel(cancellableManager), TextFieldViewModel {

    private val textDelegate = published("", this)
    override var text: String by textDelegate

    private val placeholderDelegate = published("", this)
    override var placeholder: String by placeholderDelegate

    private val keyboardTypeDelegate = published(KeyboardType.Default, this)
    override var keyboardType: KeyboardType by keyboardTypeDelegate

    private val keyboardReturnKeyTypeDelegate = published(KeyboardReturnKeyType.Default, this)
    override var keyboardReturnKeyType: KeyboardReturnKeyType by keyboardReturnKeyTypeDelegate

    private val contentTypeDelegate = published(null as TextContentType?, this)
    override var contentType: TextContentType? by contentTypeDelegate

    private val autoCorrectDelegate = published(true, this)
    override var autoCorrect: Boolean by autoCorrectDelegate

    private val autoCapitalizationDelegate = published(KeyboardAutoCapitalization.Sentences, this)
    override var autoCapitalization: KeyboardAutoCapitalization by autoCapitalizationDelegate

    val onReturnKeyTapPublisher = PublishSubjectImpl<Unit>()
    override val onReturnKeyTap = {
        onReturnKeyTapPublisher.value = Unit
    }

    override fun onValueChange(text: String) {
        this.text = text
    }

    fun addReturnKeyTapAction(action: () -> Unit) {
        onReturnKeyTapPublisher
            .observeOn(DispatchQueues.uiQueue)
            .subscribe(
                cancellableManager,
                onNext = {
                    action()
                }
            )
    }

    fun <T> addReturnKeyTapAction(publisher: Publisher<T>, action: (T) -> Unit) {
        onReturnKeyTapPublisher
            .subscribe(
                cancellableManager,
                onNext = {
                    publisher
                        .first()
                        .observeOn(DispatchQueues.uiQueue)
                        .subscribe(
                            cancellableManager,
                            onNext = { result ->
                                action(result)
                            }
                        )
                }
            )
    }

    fun bindPlaceholder(publisher: Publisher<String>) {
        updatePropertyPublisher(
            this::placeholder,
            cancellableManager,
            publisher
        )
    }

    fun bindKeyboardType(publisher: Publisher<KeyboardType>) {
        updatePropertyPublisher(
            this::keyboardType,
            cancellableManager,
            publisher
        )
    }

    fun bindKeyboardReturnKeyType(publisher: Publisher<KeyboardReturnKeyType>) {
        updatePropertyPublisher(
            this::keyboardReturnKeyType,
            cancellableManager,
            publisher
        )
    }

    fun bindContentType(publisher: Publisher<TextContentType?>) {
        updatePropertyPublisher(
            this::contentType,
            cancellableManager,
            publisher
        )
    }

    fun bindAutoCorrect(publisher: Publisher<Boolean>) {
        updatePropertyPublisher(
            this::autoCorrect,
            cancellableManager,
            publisher
        )
    }

    fun bindAutoCapitalization(publisher: Publisher<KeyboardAutoCapitalization>) {
        updatePropertyPublisher(
            this::autoCapitalization,
            cancellableManager,
            publisher
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V> publishedProperty(property: KProperty<V>): PublishedProperty<V>? =
        when (property.name) {
            this::text.name -> textDelegate as PublishedProperty<V>
            this::placeholder.name -> placeholderDelegate as PublishedProperty<V>
            this::keyboardType.name -> keyboardTypeDelegate as PublishedProperty<V>
            this::keyboardReturnKeyType.name -> keyboardReturnKeyTypeDelegate as PublishedProperty<V>
            this::contentType.name -> contentTypeDelegate as PublishedProperty<V>
            this::autoCorrect.name -> autoCorrectDelegate as PublishedProperty<V>
            this::autoCapitalization.name -> autoCapitalizationDelegate as PublishedProperty<V>
            else -> super.publishedProperty(property)
        }
}
