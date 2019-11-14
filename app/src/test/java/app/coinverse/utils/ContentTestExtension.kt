package app.coinverse.utils

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import app.coinverse.content.ContentRepository
import app.coinverse.content.ContentViewModel
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.*

class ContentTestExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
        AfterEachCallback, ParameterResolver {

    override fun beforeAll(context: ExtensionContext?) {
        // Repository is used across all the tests.
        mockkObject(ContentRepository)
    }

    override fun afterAll(context: ExtensionContext?) {
        unmockkAll()
    }

    override fun beforeEach(context: ExtensionContext?) {
        // Set Coroutine Dispatcher.
        Dispatchers.setMain(context?.root
                ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
                ?.get(TEST_COROUTINE_DISPATCHER_KEY, TestCoroutineDispatcher::class.java)!!)

        // Set LiveData Executor.
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    override fun afterEach(context: ExtensionContext?) {
        // Reset Coroutine Dispatcher.
        Dispatchers.resetMain()
        context?.root
                ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
                ?.get(TEST_COROUTINE_DISPATCHER_KEY, TestCoroutineDispatcher::class.java)!!
                .cleanupTestCoroutines()

        // Clear LiveData Executor
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    override fun supportsParameter(parameterContext: ParameterContext?,
                                   extensionContext: ExtensionContext?) =
            parameterContext?.parameter?.type == TestCoroutineDispatcher::class.java ||
                    parameterContext?.parameter?.type == ContentViewModel::class.java

    override fun resolveParameter(parameterContext: ParameterContext?,
                                  extensionContext: ExtensionContext?) =
            if (parameterContext?.parameter?.type == TestCoroutineDispatcher::class.java)
                getTestCoroutineDispatcher(extensionContext).let { dipatcher ->
                    if (dipatcher == null) saveAndReturnTestCoroutineDispatcher(extensionContext)
                    else dipatcher
                }
            else getViewModel(extensionContext).let { viewModel ->
                if (viewModel == null) saveAndReturnContentViewModel(extensionContext)
                else viewModel
            }

    private fun getTestCoroutineDispatcher(context: ExtensionContext?) = context?.root
            ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
            ?.get(TEST_COROUTINE_DISPATCHER_KEY, TestCoroutineDispatcher::class.java)

    private fun saveAndReturnTestCoroutineDispatcher(extensionContext: ExtensionContext?) =
            TestCoroutineDispatcher().apply {
                extensionContext?.root
                        ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
                        ?.put(TEST_COROUTINE_DISPATCHER_KEY, this)
            }

    private fun getViewModel(context: ExtensionContext?) = context?.root
            ?.getStore(VIEWMODEL_NAMESPACE)
            ?.get(CONTENT_VIEWMODEL_KEY, ContentViewModel::class.java)

    private fun saveAndReturnContentViewModel(extensionContext: ExtensionContext?) =
            ContentViewModel().apply {
                extensionContext?.root
                        ?.getStore(VIEWMODEL_NAMESPACE)
                        ?.put(CONTENT_VIEWMODEL_KEY, ContentViewModel())
            }
}