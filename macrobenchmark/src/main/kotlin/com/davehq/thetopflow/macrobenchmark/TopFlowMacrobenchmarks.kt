package com.davehq.thetopflow.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.davehq.thetopflow"
private const val WAIT_MS = 5_000L

@LargeTest
@RunWith(AndroidJUnit4::class)
class TopFlowStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()
        device.waitForNotesGrid()
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class TopFlowInteractionBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun noteGridScroll() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.ensureBenchmarkNotes()
        }
    ) {
        device.waitForNotesGrid()
        device.findObject(By.text("The Top Flow"))?.swipe(Direction.UP, 0.8f)
        device.waitForIdle()
        device.findObject(By.text("The Top Flow"))?.swipe(Direction.DOWN, 0.8f)
        device.waitForIdle()
    }

    @Test
    fun openNote() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.ensureBenchmarkNotes()
        }
    ) {
        device.waitForNotesGrid()
        device.wait(Until.findObject(By.descContains("Note card")), WAIT_MS)?.click()
        device.wait(Until.findObject(By.desc("Note editor")), WAIT_MS)
        device.wait(Until.findObject(By.text("Notes")), WAIT_MS)?.click()
        device.waitForNotesGrid()
    }

    @Test
    fun typeText() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.ensureBenchmarkNotes()
        }
    ) {
        device.waitForNotesGrid()
        device.wait(Until.findObject(By.descContains("Note card")), WAIT_MS)?.click()
        device.waitForEditor()
        device.tapEditorBody()
        device.executeShellCommand("input text fast%20material%20flow")
        device.waitForIdle()
    }

    @Test
    fun searchNotes() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.ensureBenchmarkNotes()
        }
    ) {
        device.waitForNotesGrid()
        val search = device.wait(Until.findObject(By.desc("Search notes")), WAIT_MS)
        search?.click()
        device.executeShellCommand("input text flow")
        device.waitForIdle()
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class TopFlowBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        startActivityAndWait()
        device.ensureBenchmarkNotes()
        device.waitForNotesGrid()
        device.findObject(By.text("The Top Flow"))?.swipe(Direction.UP, 0.8f)
        device.waitForIdle()
        device.wait(Until.findObject(By.descContains("Note card")), WAIT_MS)?.click()
        device.waitForEditor()
        device.tapEditorBody()
        device.executeShellCommand("input text profile%20flow")
        device.wait(Until.findObject(By.text("Notes")), WAIT_MS)?.click()
        device.waitForNotesGrid()
        device.wait(Until.findObject(By.desc("Search notes")), WAIT_MS)?.click()
        device.executeShellCommand("input text flow")
        device.waitForIdle()
    }
}

private fun UiDevice.waitForNotesGrid() {
    wait(Until.findObject(By.text("The Top Flow")), WAIT_MS)
}

private fun UiDevice.ensureBenchmarkNotes() {
    waitForNotesGrid()
    if (findObject(By.descContains("Note card")) != null) return
    repeat(12) { index ->
        wait(Until.findObject(By.text("New note")), WAIT_MS)?.click()
        waitForEditor()
        tapEditorTitle()
        executeShellCommand("input keyevent KEYCODE_MOVE_END")
        executeShellCommand("input text Benchmark%20${index + 1}")
        tapEditorBody()
        executeShellCommand("input text Smooth%20Pixel%20flow%20note%20${index + 1}")
        pressBack()
        wait(Until.findObject(By.text("Notes")), WAIT_MS)?.click()
        waitForNotesGrid()
    }
}

private fun UiDevice.waitForEditor() {
    wait(Until.findObject(By.text("Notes")), WAIT_MS)
}

private fun UiDevice.tapEditorTitle() {
    click(displayWidth / 2, (displayHeight * 0.20f).toInt())
    waitForIdle()
}

private fun UiDevice.tapEditorBody() {
    click(displayWidth / 2, (displayHeight * 0.58f).toInt())
    waitForIdle()
}
