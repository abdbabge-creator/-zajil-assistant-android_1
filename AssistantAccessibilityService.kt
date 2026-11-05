// خدمة إمكانية الوصول لتنفيذ النقرات الحقيقية والتنقل بين التطبيقات والرجوع للشاشة الرئيسية

package com.smartassistant.arabic

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AssistantAccessibilityService: Performs real clicks on screen elements,
 * presses Home & Back buttons, and automates UI actions after user consent.
 */
class AssistantAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AssistantAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Observes UI changes if needed
    }

    override fun onInterrupt() {
        instance = null
    }

    /**
     * Return to Android Home Screen
     */
    fun performGoHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Press Android Back Button
     */
    fun performGoBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Open Android Notifications
     */
    fun performOpenNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Click any button or view matching text
     */
    fun clickByText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val matchingNodes = rootNode.findAccessibilityNodeInfosByText(targetText)

        for (node in matchingNodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else if (node.parent != null && node.parent.isClickable) {
                return node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    /**
     * Simulate physical finger click at screen coordinates (X, Y)
     */
    fun clickAtCoordinates(x: Float, y: Float) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 100))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
