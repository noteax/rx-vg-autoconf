import mmarquee.automation.UIAutomation
import mmarquee.automation.controls.*
import mmarquee.automation.controls.mouse.AutomationMouse
import mmarquee.automation.uiautomation.ToggleState
import java.util.regex.Pattern

fun main(args: Array<String>) {
    // args
    val applicationPath = args[0]
    val numberOfCards = args[1].toInt()
    val gpuFreq = args[2].toInt()
    val memFreq = args[3].toInt()
    val tempTarget = args[4].toInt()
    val powerLimit = args[5].toInt()

    println(applicationPath)

    val automation = UIAutomation.getInstance()
    automation.launch(applicationPath).waitForInputIdle()

    for (i in 0 until numberOfCards) {
        val currentState = configureHbcc(automation, i)
        if (!currentState) {
            configureHbcc(automation, i)
        }
        println("HBCC configuration $i complete")
    }

    val window = openGlobalSettings(automation)
    for (i in 0 until numberOfCards) {
        configureWattman(window, i, numberOfCards,
                gpuFreq, memFreq, tempTarget, powerLimit)
    }

}

fun getWindow(automation: UIAutomation): AutomationWindow {
    val windowName = "RADEON SETTINGS RADEON SETTINGS"
    return automation.getDesktopWindow(windowName, 180)
}

fun openGlobalSettings(automation: UIAutomation): AutomationWindow {
    val maximizeButton = "maximize/restore button"
    val gamesButton = "main app list - Gaming_\$_Gaming"
    val globalButton = "Game 0_\$_Global Settings"

    val window = getWindow(automation)
    window.focus()

    window.getButton(maximizeButton).click()
    window.getButton(gamesButton).click()
    Thread.sleep(1500)
    window.getButton(globalButton).click()
    return window
}

fun configureHbcc(automation: UIAutomation, index: Int): Boolean {
    val hbccButton = "Game Manager - Global Settings page button - ${index}_\$_Global Graphics\n(Radeon RX Vega)"
    val hbccCheckBoxPattern = "virtualmemory .* ComboBoxToggle.*"
    val hbccApplyButton = "virtual memory bar - apply button"
    val hbccApplyConfirmButton = "virtual memory confirm dialog - dialog button - Confirm"

    val window = openGlobalSettings(automation)

    window.getButton(hbccButton).click()
    Thread.sleep(3000)

    println("HBCC $index button clicked. Looking for toggle...")

    val toggle = findElement(window, hbccCheckBoxPattern) as AutomationCheckBox
    val currentStatus = toggle.toggleState == ToggleState.On
    println("Found toggle. State: $currentStatus")

    toggle.toggle()

    window.getButton(hbccApplyButton).click()
    window.getButton(hbccApplyConfirmButton).click()

    Thread.sleep(3000)

    println("Completed configuration round")
    return !currentStatus
}

fun configureWattman(window: AutomationWindow, index: Int, cardsCount: Int,
                     gpuFreq: Int,
                     memFreq: Int,
                     tempTarget: Int,
                     powerLimit: Int) {
    val wattmanButton = "Game Manager - Global Settings page button - ${cardsCount + index}_\$_Global WattMan\n(Radeon RX Vega)"
    val scrollBar = "game manager overdrive scrollbar"
    val gpuFreqSlider = "GPU Frequency Slider_.*"
    val memoryFreqSlider = "Memory Frequency Slider_.*"
    val tempToggle = "Temperature Temperature.*"
    val tempTargetSlider = "Temperature Target Slider_.*"
    val powerLimitSlider = "Temperature TDP Limit  Slider_.*"
    val applyButton = "Game Manager - Global Settings - apply button"

    val cpuFreqRange = Pair(-30, 30)
    val memFreqRange = Pair(700, 1500)
    val tempTargetRange = Pair(35, 85)
    val powerLimitRange = Pair(-50, 50)

    window.getButton(wattmanButton).click()
    Thread.sleep(3000)

    val scrollBarControl = window.getControlByName(scrollBar)
    for (i in 0..13) scrollBarControl.invoke()

    setSliderVal(findElement(window, gpuFreqSlider) as AutomationSlider, gpuFreq, cpuFreqRange)
    println("Configure gpuFreq to $gpuFreq")

    setSliderVal(findElement(window, memoryFreqSlider) as AutomationSlider, memFreq, memFreqRange)
    println("Configure memFreq to $memFreq")

    val tempToggleV = findElement(window, tempToggle) as AutomationCheckBox
    if (tempToggleV.toggleState != ToggleState.On) tempToggleV.toggle()
    println("Set temp to manual")

    setSliderVal(findElement(window, tempTargetSlider) as AutomationSlider, tempTarget, tempTargetRange, false)
    println("Set tempTarget to $tempTarget")

    setSliderVal(findElement(window, powerLimitSlider) as AutomationSlider, powerLimit, powerLimitRange)
    println("Set powerLimit to $powerLimit")

    try {
        window.getButton(applyButton).click()
        println("Wattman configuration $index complete")
    } catch (e: Exception) {
        println("Couldn't find apply button. No changes? Wattman configuration $index complete")
    }
}

fun findElement(window: AutomationWindow, namePattern: String): AutomationBase? {
    val pattern = Pattern.compile(namePattern)
    return window.getChildren(true).find { v -> pattern.matcher(v.name).matches() }
}

fun setSliderVal(slider: AutomationSlider, value: Int, range: Pair<Int, Int>, horizontal: Boolean = true) {
    val rect = slider.boundingRectangle
    val bounds = if (horizontal) Pair(rect.left, rect.right) else Pair(rect.bottom, rect.top)
    val newVal = getSliderValPix(value, range, bounds)

    val point = if (horizontal) Pair(newVal, slider.clickablePoint.y) else Pair(slider.clickablePoint.x, newVal)

    AutomationMouse.getInstance().setLocation(point.first, point.second)
    AutomationMouse.getInstance().leftClick()
}

fun getSliderValPix(value: Int, range: Pair<Int, Int>, bounds: Pair<Int, Int>): Int {
    val border = 13
    val perValPixels = (bounds.second - bounds.first - border * 2) / (range.second - range.first).toDouble()
    return bounds.first + border + ((value - range.first) * perValPixels).toInt()
}