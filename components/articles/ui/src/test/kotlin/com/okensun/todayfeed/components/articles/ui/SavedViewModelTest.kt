package com.okensun.todayfeed.components.articles.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.okensun.todayfeed.components.articles.api.Article
import com.okensun.todayfeed.core.designsystem.ContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric only so a real `SQLiteException` can be thrown. Nothing here draws anything. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SavedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val articles = FakeArticleRepository()

    @Before
    fun useTestMain() = Dispatchers.setMain(dispatcher)

    @After
    fun releaseMain() = Dispatchers.resetMain()

    /**
     * What the retry is for. The failure has to be caught inside `flatMapLatest`, or it ends the
     * flow of retries with it: the button then raises a number nobody is collecting and the
     * screen stays on the error until the reader leaves it for five seconds.
     */
    @Test
    fun `a retry after a failed read reads again`() =
        runTest(dispatcher) {
            articles.readFails = true
            val viewModel = SavedViewModel(articles)

            viewModel.state.test {
                val failed = awaitSettled()
                assertTrue("settled on $failed", failed is ContentState.Error)

                articles.readFails = false
                articles.hold(previewArticle("s1"))
                viewModel.onRetry()

                val reread = awaitSettled()
                assertTrue("settled on $reread", reread is ContentState.Content)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Storage that rejects a write must not reach the top. Uncaught inside `viewModelScope` it
     * becomes the thread's problem, and that is the app going down.
     */
    @Test
    fun `a save that storage rejects does not reach the top`() =
        runTest(dispatcher) {
            val loose = mutableListOf<Throwable>()
            val handler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { _, thrown -> loose += thrown }
            try {
                articles.writeFails = true

                SavedViewModel(articles).onToggleSave(previewArticle("s1")).join()

                assertEquals(emptyList<Throwable>(), loose)
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(handler)
            }
        }

    /** Loading is the value it starts on, so a test that cares about the answer steps past it. */
    private suspend fun ReceiveTurbine<ContentState<List<Article>>>.awaitSettled(): ContentState<List<Article>> {
        var state = awaitItem()
        while (state is ContentState.Loading) state = awaitItem()
        return state
    }
}
