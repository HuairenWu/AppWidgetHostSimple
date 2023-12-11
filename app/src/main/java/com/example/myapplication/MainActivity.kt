package com.example.myapplication

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    companion object {
        const val APPWIDGET_BIND_REQUEST_CODE = 103
        const val APPWIDGET_CONFIGURE_REQUEST_CODE = 104
    }

    private val context = this
    private val appWidgetHost by lazy { AppWidgetHost(context, 1024) }
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        demo()
    }

    override fun onStart() {
        super.onStart()
        // 开始监听应用小部件的更新。这意味着当应用小部件发生变化（例如内容更新）时，appWidgetHost将接收到相应的通知。
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        // 停止监听应用小部件的更新。这意味着在活动不可见或即将销毁时，不再接收应用小部件更新的通知。
        appWidgetHost.stopListening()
    }

    private fun demo() {
        val appWidgetProviderInfoList = appWidgetManager.getInstalledProvidersForProfile(Process.myUserHandle())
        val appWidgetProviderInfo = appWidgetProviderInfoList[2]
        addAppWidget(appWidgetProviderInfo)
    }


    private fun addAppWidget(appWidgetProviderInfo: AppWidgetProviderInfo) {
        // 每个应用小部件都需要一个唯一的ID来标识它在系统中的实例。该方法会返回一个整数值，表示新分配的应用小部件ID。这个返回值在后续的代码中被使用。
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        // 会尝试将应用小部件ID绑定到提供者指定的应用小部件。如果绑定成功，它会返回true，表示绑定是允许的。如果绑定不被允许，它会返回false。
        // 返回值isAllowed是一个布尔类型的变量，它用于存储绑定操作的结果。它的作用是告诉开发者绑定操作是否成功，并根据需要执行后续的操作。
        // 例如，如果绑定成功，开发者可以继续添加应用小部件到界面上；如果绑定不被允许，开发者可以采取适当的措施，如显示一个错误消息或请求用户授权。
        val isAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetProviderInfo.provider, null)
        //如果 被允许 那么可以直接创建AppWidgetHostView并添加到视图中
        //否则 就要申请权限
        if (isAllowed) {
            completeAddAppWidgetView(appWidgetId)
        } else {
            showAppWidgetBindDialog(appWidgetId, appWidgetProviderInfo.provider, appWidgetProviderInfo.profile)
        }
    }

    private fun completeAddAppWidgetView(appWidgetId: Int) {
        // 获取与给定应用小部件ID相关联的应用小部件信息。
        // 其中包含了应用小部件的相关信息，如布局资源、配置活动等。
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        // 创建一个应用小部件视图。
        // 返回值appWidgetHostView是一个AppWidgetHostView对象，它代表了应用小部件的视图。这个视图可以被添加到界面上，以显示应用小部件的内容。
        val appWidgetHostView = appWidgetHost.createView(context, appWidgetId, appWidgetProviderInfo)
        val root = findViewById<FrameLayout>(R.id.root)
        root.addView(appWidgetHostView)
    }


    /**
     * 创建一个系统意图（Intent），用于绑定应用小部件。
     * 它将应用小部件的ID、提供者信息和配置文件作为额外的数据添加到意图中，并启动意图以打开系统界面进行绑定操作。
     * 开发者可以在onActivityResult()方法中处理绑定操作的结果。
     */
    private fun showAppWidgetBindDialog(appWidgetId: Int, provider: ComponentName, profile: UserHandle) {
        //系统会弹出一个 确认对话框 有以后都不在提示复选框
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, profile)
        startActivityForResult(intent, APPWIDGET_BIND_REQUEST_CODE)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APPWIDGET_BIND_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            handleAppWidgetAddition(appWidgetId)
        }
        if (requestCode == APPWIDGET_CONFIGURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            completeAddAppWidgetView(appWidgetId)
        }
    }

    private fun handleAppWidgetAddition(appWidgetId: Int) {
        val appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId)
        //  如果应用小部件不需要进行配置，可以直接添加到主屏幕上。
        if (appWidget.configure == null) {
            completeAddAppWidgetView(appWidgetId)
            return
        }

        // 如果应用小部件需要进行配置，将创建一个新的意图（Intent）。这个意图的操作是AppWidgetManager.ACTION_APPWIDGET_CONFIGURE，表示要进行应用小部件的配置操作。
        // 然后，将应用小部件的配置组件（appWidget.configure）设置为意图的组件。
        // 接下来，通过putExtra()方法将应用小部件的ID添加到意图中。
        // 最后，调用startActivityForResult()方法启动意图，打开应用小部件的配置界面。
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        intent.component = appWidget.configure
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        startActivityForResult(intent, APPWIDGET_CONFIGURE_REQUEST_CODE)
    }

}