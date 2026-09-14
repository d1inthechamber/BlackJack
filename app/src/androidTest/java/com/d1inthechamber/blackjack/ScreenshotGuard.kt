package com.d1inthechamber.blackjack

internal fun clearLauncherDialog(){
    val a=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
    repeat(4){
        val root=a.rootInActiveWindow?:return
        if(root.findAccessibilityNodeInfosByText("Quickstep").isEmpty() || root.findAccessibilityNodeInfosByText("responding").isEmpty())return
        val close=root.findAccessibilityNodeInfosByText("Close app").firstOrNull()?:return
        var target:android.view.accessibility.AccessibilityNodeInfo?=close
        while(target!=null&&!target.isClickable)target=target.parent
        if(target?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)!=true){
            val bounds=android.graphics.Rect();close.getBoundsInScreen(bounds)
            a.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").use{java.io.FileInputStream(it.fileDescriptor).use{it.readBytes()}}
        }
        Thread.sleep(400)
    }
}
