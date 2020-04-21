package com.mirego.trikot.datasources

import com.mirego.trikot.datasources.testutils.assertError
import com.mirego.trikot.datasources.testutils.assertValue
import com.mirego.trikot.foundation.concurrent.AtomicReference
import com.mirego.trikot.foundation.concurrent.dispatchQueue.SynchronousDispatchQueue
import com.mirego.trikot.streams.StreamsConfiguration
import com.mirego.trikot.streams.cancellable.CancellableManager
import com.mirego.trikot.streams.reactive.subscribe
import com.mirego.trikot.streams.reactive.BehaviorSubjectImpl
import com.mirego.trikot.streams.reactive.executable.ExecutablePublisher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class BaseDataSourceTests {
    var cancellableManager: CancellableManager? = null
    val simpleCachableId = "ABC"
    val networkResult = "network"
    val cacheResult = "cache"

    @BeforeTest
    fun before() {
        StreamsConfiguration.publisherExecutionDispatchQueue = SynchronousDispatchQueue()
        StreamsConfiguration.serialSubscriptionDispatchQueue = SynchronousDispatchQueue()
        cancellableManager = CancellableManager()
    }

    @Test
    fun givenRequestWhenNoCacheDataSourceThenNetworkDataSourceIsUsed() {
        val networkDataSourceReadPublisher = ReadFromCachePublisher()
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher))

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }

        networkDataSourceReadPublisher.dispatchResult(networkResult)

        value.value!!.assertValue(networkResult)
    }

    @Test
    fun givenAStartedRequestWhenRequestingTheSameRequestThenSamePublisherIsReturned() {
        val readFromCachePublisher = ReadFromCachePublisher()
        val basicDataSource = BasicDataSource(mutableMapOf(simpleCachableId to readFromCachePublisher))

        val pub1 = basicDataSource.read(FakeRequest(simpleCachableId))
        val pub2 = basicDataSource.read(FakeRequest(simpleCachableId))

        assertTrue { pub1 == pub2 }
    }

    @Test
    fun givenRequestWhenMissingCacheThenNetworkReadPublisherIsUsed() {
        val networkDataSourceReadPublisher = ReadFromCachePublisher()
        val cacheDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchError(Throwable()) }
        val cacheDataSource = BasicDataSource(mutableMapOf(simpleCachableId to cacheDataSourceReadPublisher))
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher), cacheDataSource)

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }
        networkDataSourceReadPublisher.dispatchResult(networkResult)

        value.value!!.assertValue(networkResult)
    }

    @Test
    fun givenRequestWhenHittingCacheThenCacheReadPublisherIsUsed() {
        val networkDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(networkResult) }
        val cacheDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(cacheResult) }
        val cacheDataSource = BasicDataSource(mutableMapOf(simpleCachableId to cacheDataSourceReadPublisher))
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher), cacheDataSource)

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }

        value.value!!.assertValue(cacheResult)
    }

    @Test
    fun givenRequestWhenRefreshingThenResultIsFetchedFromTheNetworkAndSavedToCache() {
        val networkDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(networkResult) }
        val cacheDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(cacheResult) }
        val cacheDataSource = BasicDataSource(mutableMapOf(simpleCachableId to cacheDataSourceReadPublisher))
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher), cacheDataSource)

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        val refreshedValue = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }
        val beforeRefreshValue = value.value
        networkDataSource.read(FakeRequest(simpleCachableId, DataSourceRequest.Type.REFRESH_CACHE)).subscribe(cancellableManager!!) {
            refreshedValue.compareAndSet(refreshedValue.value, it)
        }

        beforeRefreshValue!!.assertValue(cacheResult)
        value.value!!.assertValue(networkResult)
        refreshedValue.value!!.assertValue(networkResult)
    }

    @Test
    fun givenCachedDataWhenRefreshingWithoutAnySubscriberThenNextSubscriberReceiveRefreshedData() {
        val networkDataSourceReadPublisher = ReadFromCachePublisher()
        val cacheDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(cacheResult) }
        val cacheDataSource = BasicDataSource(mutableMapOf(simpleCachableId to cacheDataSourceReadPublisher))
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher), cacheDataSource)

        networkDataSource.read(FakeRequest(simpleCachableId, DataSourceRequest.Type.REFRESH_CACHE))

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }

        networkDataSourceReadPublisher.dispatchResult(networkResult)

        value.value!!.assertValue(networkResult)
    }

    @Test
    fun whenRefreshingWithCachedDataWhenAnErrorOccursThenCachedDataIsReturnedWithAnError() {
        val expectedError = Throwable()
        val networkDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchError(expectedError) }
        val cacheDataSourceReadPublisher = ReadFromCachePublisher().also { it.dispatchResult(cacheResult) }
        val cacheDataSource = BasicDataSource(mutableMapOf(simpleCachableId to cacheDataSourceReadPublisher))
        val networkDataSource = BasicDataSource(mutableMapOf(simpleCachableId to networkDataSourceReadPublisher), cacheDataSource)

        val value = AtomicReference<DataState<String, Throwable>?>(null)
        networkDataSource.read(FakeRequest(simpleCachableId)).subscribe(cancellableManager!!) {
            value.compareAndSet(value.value, it)
        }

        networkDataSource.read(FakeRequest(simpleCachableId, DataSourceRequest.Type.REFRESH_CACHE))

        value.value!!.assertError(expectedError, cacheResult)
    }

    data class FakeRequest(override val cachableId: Any, override val requestType: DataSourceRequest.Type = DataSourceRequest.Type.USE_CACHE) : DataSourceRequest

    class BasicDataSource(publishers: MutableMap<Any, ReadFromCachePublisher>, fallbackDataSource: DataSource<FakeRequest, String>? = null) : BaseDataSource<FakeRequest, String>(fallbackDataSource) {
        private val internalPublishers = AtomicReference(publishers)

        override fun internalRead(request: FakeRequest): ExecutablePublisher<String> {
            return internalPublishers.value[request.cachableId]!!
        }

        override fun save(request: FakeRequest, data: String?) {
            super.save(request, data)
            val publisher = ReadFromCachePublisher().also { it.dispatchResult(data) }
            val hasOldPublisher = internalPublishers.value[request.cachableId] != null
            val mutable = internalPublishers.value.toMutableMap()
            mutable[request.cachableId] = publisher
            internalPublishers.compareAndSet(internalPublishers.value, mutable)

            if (hasOldPublisher) {
                refreshPublisherWithId(request.cachableId)
            }
        }
    }

    class ReadFromCachePublisher : ExecutablePublisher<String>, BehaviorSubjectImpl<String>() {
        val resultValue = AtomicReference<String?>(null)
        val errorValue = AtomicReference<Throwable?>(null)
        val executed = AtomicReference<Boolean>(false)

        override fun cancel() {
        }

        override fun execute() {
            executed.compareAndSet(executed.value, true)
            dispatchResultIfNeeded()
        }

        fun dispatchResult(result: String?) {
            resultValue.compareAndSet(resultValue.value, result)
            dispatchResultIfNeeded()
        }

        fun dispatchError(error: Throwable) {
            errorValue.compareAndSet(errorValue.value, error)
            dispatchResultIfNeeded()
        }

        private fun dispatchResultIfNeeded() {
            if (executed.value) {
                errorValue.value?.let {
                    error = errorValue.value
                } ?: run { value = resultValue.value }
            }
        }
    }
}
