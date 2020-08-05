package app.coinverse.feedViewModel

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import app.coinverse.utils.TEST_COROUTINE_DISPATCHER_KEY
import app.coinverse.utils.TEST_COROUTINE_DISPATCHER_NAMESPACE
import app.coinverse.utils.TEST_COROUTINE_SCOPE_KEY
import app.coinverse.utils.TEST_COROUTINE_SCOPE_NAMESPACE
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

@ExperimentalCoroutinesApi
class FeedViewTestExtension : BeforeEachCallback, AfterEachCallback, ParameterResolver {

    override fun beforeEach(context: ExtensionContext?) {
        // Set TestCoroutineDispatcher.
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
        // Reset TestCoroutineDispatcher.
        Dispatchers.resetMain()
        context?.root
                ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
                ?.get(TEST_COROUTINE_DISPATCHER_KEY, TestCoroutineDispatcher::class.java)!!
                .cleanupTestCoroutines()
        context.root
                ?.getStore(TEST_COROUTINE_SCOPE_NAMESPACE)
                ?.get(TEST_COROUTINE_SCOPE_KEY, TestCoroutineScope::class.java)!!
                .cleanupTestCoroutines()
        // Clear LiveData Executor
        ArchTaskExecutor.getInstance().setDelegate(null)
        unmockkAll()
    }

    override fun supportsParameter(
            parameterContext: ParameterContext?,
            extensionContext: ExtensionContext?
    ) =
            parameterContext?.parameter?.type == TestCoroutineDispatcher::class.java
                    || parameterContext?.parameter?.type == TestCoroutineScope::class.java

    override fun resolveParameter(
            parameterContext: ParameterContext?,
            extensionContext: ExtensionContext?
    ) =
            if (parameterContext?.parameter?.type == TestCoroutineDispatcher::class.java)
                getTestCoroutineDispatcher(extensionContext).let { dipatcher ->
                    if (dipatcher == null) saveAndReturnTestCoroutineDispatcher(extensionContext)
                    else dipatcher
                }
            else if (parameterContext?.parameter?.type == TestCoroutineScope::class.java)
                getTestCoroutineScope(extensionContext).let { scope ->
                    if (scope == null) saveAndReturnTestCoroutineScope(extensionContext)
                    else scope
                }
            else null

    private fun getTestCoroutineDispatcher(context: ExtensionContext?) = context?.root
            ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
            ?.get(TEST_COROUTINE_DISPATCHER_KEY, TestCoroutineDispatcher::class.java)

    private fun saveAndReturnTestCoroutineDispatcher(extensionContext: ExtensionContext?) =
            TestCoroutineDispatcher().apply {
                extensionContext?.root
                        ?.getStore(TEST_COROUTINE_DISPATCHER_NAMESPACE)
                        ?.put(TEST_COROUTINE_DISPATCHER_KEY, this)
            }

    private fun getTestCoroutineScope(context: ExtensionContext?) = context?.root
            ?.getStore(TEST_COROUTINE_SCOPE_NAMESPACE)
            ?.get(TEST_COROUTINE_SCOPE_KEY, TestCoroutineScope::class.java)

    private fun saveAndReturnTestCoroutineScope(context: ExtensionContext?) =
            TestCoroutineScope(getTestCoroutineDispatcher(context)!!).apply {
                context?.root
                        ?.getStore(TEST_COROUTINE_SCOPE_NAMESPACE)
                        ?.put(TEST_COROUTINE_SCOPE_KEY, this)
            }
}