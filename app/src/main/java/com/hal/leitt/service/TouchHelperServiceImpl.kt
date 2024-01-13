package com.hal.leitt.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.hal.leitt.R
import com.hal.leitt.entity.PackagePositionDescription
import com.hal.leitt.entity.PackageWidgetDescription
import com.hal.leitt.ktx.Settings
import com.hal.leitt.receiver.PackageChangeReceiver
import com.hal.leitt.receiver.UserPresentReceiver
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


/**
 * ...
 * @author LeiHao
 * @date 2024/1/12
 * @description 自动触控服务具体实现
 */

class TouchHelperServiceImpl(private val service: AccessibilityService) {

    //处理广播信息的Handler
    var receiverHandler: Handler? = null

    //执行跳广告操作的线程池
    private val taskExecutorService: ScheduledExecutorService by lazy {
        Executors.newSingleThreadScheduledExecutor();
    }

    companion object {
        //第一次包位置点击的延迟时间
        private const val PACKAGE_POSITION_CLICK_FIRST_DELAY = 300

        //包位置点击重试的时间间隔
        private const val PACKAGE_POSITION_CLICK_RETRY_INTERVAL = 500

        //包位置点击的最大重试次数
        private const val PACKAGE_POSITION_CLICK_RETRY = 6
    }

    //关键字列表
    private var keyWordList: MutableList<String> = mutableListOf()

    //白名单包名列表 - 需排除
    private var whiteList: MutableSet<String> = mutableSetOf()

    //输入法包名列表 - 需排除
    private var setIMEApps: MutableSet<String> = mutableSetOf()

    //待检测的包名列表 - 需检测
    private var setPackages: MutableSet<String> = mutableSetOf()

    //包变化广播接收器
    private val packageChangeReceiver by lazy {
        PackageChangeReceiver()
    }

    //用户开启屏幕广播接收器
    private val userPresentReceiver by lazy { UserPresentReceiver() }

    //包管理器
    private val packageManager: PackageManager = service.packageManager

    private var currentPackageName = ""
    private var currentActivityName = ""

    //包名及对应控件信息映射
    private var mapPackageWidgets: MutableMap<String, MutableSet<PackageWidgetDescription>> =
        mutableMapOf()

    //包名及对应位置信息映射
    private var mapPackagePositions: MutableMap<String, PackagePositionDescription> = mutableMapOf()

    @Volatile
    private var skipAdRunning = false

    @Volatile
    private var skipAdByActivityPosition = false

    @Volatile
    private var skipAdByActivityWidget = false

    @Volatile
    private var skipAdByKeyword = false

    @Volatile
    private var setTargetedWidgets: MutableSet<PackageWidgetDescription>? = null

    private var clickedWidgets: MutableSet<String> = mutableSetOf()


    /**
     * 建立连接，开始初始化工作
     */
    fun onServiceConnected() {

        Log.e("halo", "==========无障碍服务已启动==========")

        currentPackageName = "Initial PackageName"
        currentActivityName = "Initial ClassName"

        //初始化关键字列表
        keyWordList = Settings.getKeyWords()

        //初始化白名单列表
        whiteList = Settings.getWhiteList()
        updatePackage()

        //初始化包及控件信息映射
        mapPackageWidgets = Settings.getMapPackageWidgets()

        //初始化包及位置信息映射
        mapPackagePositions = Settings.getMapPackagePositions()

        // 初始化接收器
        installReceiverAndHandler()

    }

    private fun installReceiverAndHandler() {

        //广播注册
        service.registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        Log.e("halo", "用户屏幕开启广播已注册")
        val actions = IntentFilter()
        actions.addAction(Intent.ACTION_PACKAGE_ADDED)
        actions.addAction(Intent.ACTION_PACKAGE_REMOVED)
        actions.addDataScheme("package")
        service.registerReceiver(packageChangeReceiver, actions)
        Log.e("halo", "系统包变化广播已注册")

        // 处理广播事件
        receiverHandler = Handler(Looper.getMainLooper()) { msg: Message ->
            when (msg.what) {
                //关键字更新
                TouchHelperService.ACTION_REFRESH_KEYWORDS -> {
                    Log.e("halo", "关键字刷新: $keyWordList")
                    keyWordList = Settings.getKeyWords()
                }
                //包状态更新
                TouchHelperService.ACTION_REFRESH_PACKAGE -> {
                    Log.e("halo", "白名单刷新: $whiteList")
                    whiteList = Settings.getWhiteList()
                    updatePackage()
                }

                TouchHelperService.ACTION_REFRESH_CUSTOMIZED_ACTIVITY -> {
                    Log.e("halo", "包及对应控件信息映射刷新: $mapPackageWidgets")
                    mapPackageWidgets = Settings.getMapPackageWidgets()
                    Log.e("halo", "包及对应位置信息映射刷新: $mapPackageWidgets")
                    mapPackagePositions = Settings.getMapPackagePositions()
                }
                //打开采集按钮弹窗
                TouchHelperService.ACTION_ACTIVITY_CUSTOMIZATION -> {
                    Log.e("halo", "打开采集按钮弹窗")
                    showActivityCustomizationDialog()
                }

                TouchHelperService.ACTION_START_SKIP_AD -> {
                    startSkipAdProcess()
                }

                TouchHelperService.ACTION_STOP_SKIP_AD -> {
                    stopSkipAdProcessInner()
                }
            }
            true
        }
    }

    /**
     * 按钮采集弹窗
     */
    private var isShowing = false

    @SuppressLint("ClickableViewAccessibility")
    private fun showActivityCustomizationDialog() {
        if (isShowing) return
        val windowManager =
            service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val b = metrics.heightPixels > metrics.widthPixels
        val width = if (b) metrics.widthPixels else metrics.heightPixels
        val height = if (b) metrics.heightPixels else metrics.widthPixels

        //窗体描述
        val widgetDescription = PackageWidgetDescription(
            "", "", "", "", "", "", Rect(), clickable = false, onlyClick = false
        )
        //位置描述
        val positionDescription = PackagePositionDescription("", "", 0, 0)

        val inflater = LayoutInflater.from(service)
        val viewCustomization: View =
            inflater.inflate(R.layout.dialog_activity_customization_layout, null)
        val tvPackageName = viewCustomization.findViewById<TextView>(R.id.tv_package_name)
        val tvActivityName = viewCustomization.findViewById<TextView>(R.id.tv_activity_name)
        val tvWidgetInfo = viewCustomization.findViewById<TextView>(R.id.tv_widget_info)
        val tvPositionInfo = viewCustomization.findViewById<TextView>(R.id.tv_position_info)
        val btShowOutline = viewCustomization.findViewById<Button>(R.id.button_show_outline)
        val btAddWidget = viewCustomization.findViewById<Button>(R.id.button_add_widget)
        val btShowTarget = viewCustomization.findViewById<Button>(R.id.button_show_target)
        val btAddPosition = viewCustomization.findViewById<Button>(R.id.button_add_position)
        val btQuit = viewCustomization.findViewById<Button>(R.id.button_quit)

        val viewTarget: View = inflater.inflate(R.layout.layout_accessibility_node_desc, null)
        val layoutOverlayOutline = viewTarget.findViewById<FrameLayout>(R.id.frame)

        val imageTarget = ImageView(service)
        imageTarget.setImageResource(R.drawable.drawable_target)

        val customizationParams = WindowManager.LayoutParams()
        customizationParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        customizationParams.format = PixelFormat.TRANSPARENT
        customizationParams.gravity = Gravity.START or Gravity.TOP
        customizationParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        customizationParams.width = width
        customizationParams.height = height / 5
        customizationParams.x = (metrics.widthPixels - customizationParams.width) / 2
        customizationParams.y = metrics.heightPixels - customizationParams.height
        customizationParams.alpha = 0.95f

        val outlineParams = WindowManager.LayoutParams()
        outlineParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        outlineParams.format = PixelFormat.TRANSPARENT
        outlineParams.gravity = Gravity.START or Gravity.TOP
        outlineParams.width = metrics.widthPixels
        outlineParams.height = metrics.heightPixels
        outlineParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        outlineParams.alpha = 0f

        val targetParams = WindowManager.LayoutParams()
        targetParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        targetParams.format = PixelFormat.TRANSPARENT
        targetParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        targetParams.gravity = Gravity.START or Gravity.TOP
        targetParams.width = width / 4
        targetParams.height = width / 4
        targetParams.x = (metrics.widthPixels - targetParams.width) / 2
        targetParams.y = (metrics.heightPixels - targetParams.height) / 2
        targetParams.alpha = 0f

        viewCustomization.setOnTouchListener(object : OnTouchListener {
            var x = 0
            var y = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = event.rawX.roundToInt()
                        y = event.rawY.roundToInt()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        customizationParams.x =
                            (customizationParams.x + (event.rawX - x)).roundToInt()
                        customizationParams.y =
                            (customizationParams.y + (event.rawY - y)).roundToInt()
                        x = event.rawX.roundToInt()
                        y = event.rawY.roundToInt()
                        windowManager.updateViewLayout(viewCustomization, customizationParams)
                    }
                }
                return true
            }
        })

        imageTarget.setOnTouchListener(object : OnTouchListener {
            var x = 0
            var y = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        btAddPosition.isEnabled = true
                        targetParams.alpha = 0.9f
                        windowManager.updateViewLayout(imageTarget, targetParams)
                        x = event.rawX.roundToInt()
                        y = event.rawY.roundToInt()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = (targetParams.x + (event.rawX - x)).roundToInt()
                        targetParams.y = (targetParams.y + (event.rawY - y)).roundToInt()
                        x = event.rawX.roundToInt()
                        y = event.rawY.roundToInt()
                        windowManager.updateViewLayout(imageTarget, targetParams)
                        positionDescription.packageName = currentPackageName
                        positionDescription.activityName = currentActivityName
                        positionDescription.x = targetParams.x + width
                        positionDescription.y = targetParams.y + height
                        tvPackageName.text = positionDescription.packageName
                        tvActivityName.text = positionDescription.activityName
                        tvPositionInfo.text =
                            "X轴：" + positionDescription.x + "    " + "Y轴：" + positionDescription.y + "    " + "(其他参数默认)"
                    }

                    MotionEvent.ACTION_UP -> {
                        targetParams.alpha = 0.5f
                        windowManager.updateViewLayout(imageTarget, targetParams)
                    }
                }
                return true
            }
        })

        btShowOutline.setOnClickListener(View.OnClickListener { v ->
            val button = v as Button
            if (outlineParams.alpha == 0f) {
                val root = service.rootInActiveWindow ?: return@OnClickListener
                widgetDescription.packageName = currentPackageName
                widgetDescription.activityName = currentActivityName
                layoutOverlayOutline.removeAllViews()
                val roots = ArrayList<AccessibilityNodeInfo>()
                roots.add(root)
                val nodeList = ArrayList<AccessibilityNodeInfo>()
                findAllNode(roots, nodeList, "")
                nodeList.sortWith { a, b ->
                    val rectA = Rect()
                    val rectB = Rect()
                    a.getBoundsInScreen(rectA)
                    b.getBoundsInScreen(rectB)
                    rectB.width() * rectB.height() - rectA.width() * rectA.height()
                }
                for (e in nodeList) {
                    val temRect = Rect()
                    e.getBoundsInScreen(temRect)
                    val params = FrameLayout.LayoutParams(temRect.width(), temRect.height())
                    params.leftMargin = temRect.left
                    params.topMargin = temRect.top
                    val img = ImageView(service)
                    img.setBackgroundResource(R.drawable.node)
                    img.isFocusableInTouchMode = true
                    img.setOnClickListener { it.requestFocus() }
                    img.onFocusChangeListener = OnFocusChangeListener { it, hasFocus ->
                        if (hasFocus) {
                            widgetDescription.position = temRect
                            widgetDescription.clickable = e.isClickable
                            widgetDescription.className = e.className.toString()
                            val cId: CharSequence? = e.viewIdResourceName
                            widgetDescription.idName = cId?.toString() ?: ""
                            val cDesc = e.contentDescription
                            widgetDescription.description = cDesc?.toString() ?: ""
                            val cText = e.text
                            widgetDescription.text = cText?.toString() ?: ""
                            btAddWidget.isEnabled = true
                            tvPackageName.text = widgetDescription.packageName
                            tvActivityName.text = widgetDescription.activityName
                            tvWidgetInfo.text =
                                "click:" + (if (e.isClickable) "true" else "false") + " " + "bonus:" + temRect.toShortString() + " " + "id:" + (cId?.toString()
                                    ?.substring(cId.toString().indexOf("id/") + 3)
                                    ?: "null") + " " + "desc:" + (cDesc?.toString()
                                    ?: "null") + " " + "text:" + (cText?.toString() ?: "null")
                            it.setBackgroundResource(R.drawable.node_focus)
                        } else {
                            it.setBackgroundResource(R.drawable.node)
                        }
                    }
                    layoutOverlayOutline.addView(img, params)
                }
                outlineParams.alpha = 0.5f
                outlineParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(viewTarget, outlineParams)
                tvPackageName.text = widgetDescription.packageName
                tvActivityName.text = widgetDescription.activityName
                button.text = "隐藏布局"
            } else {
                outlineParams.alpha = 0f
                outlineParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(viewTarget, outlineParams)
                btAddWidget.isEnabled = false
                button.text = "显示布局"
            }
        })

        btShowTarget.setOnClickListener { v ->
            Log.e("halo", "点击了显示准心按钮")
            val button = v as Button
            if (targetParams.alpha == 0f) {
                positionDescription.packageName = currentPackageName
                positionDescription.activityName = currentActivityName
                targetParams.alpha = 0.5f
                targetParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(imageTarget, targetParams)
                tvPackageName.text = positionDescription.packageName
                tvActivityName.text = positionDescription.activityName
                button.text = "隐藏准心"
            } else {
                targetParams.alpha = 0f
                targetParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(imageTarget, targetParams)
                btAddPosition.isEnabled = false
                button.text = "显示准心"
            }
        }

        btAddWidget.setOnClickListener {
            val temWidget = PackageWidgetDescription(widgetDescription)
            var set: MutableSet<PackageWidgetDescription>? =
                mapPackageWidgets[widgetDescription.packageName]
            if (set == null) {
                set = HashSet()
                set.add(temWidget)
                mapPackageWidgets[widgetDescription.packageName] = set
            } else {
                set.add(temWidget)
            }
            btAddWidget.isEnabled = false
            tvPackageName.text = widgetDescription.packageName + " (以下控件数据已保存)"
            Settings.setMapPackageWidgets(mapPackageWidgets)
        }

        btAddPosition.setOnClickListener {
            mapPackagePositions[positionDescription.packageName] =
                PackagePositionDescription(positionDescription)
            btAddPosition.isEnabled = false
            tvPackageName.text = positionDescription.packageName + " (以下坐标数据已保存)"
            Settings.setMapPackagePositions(mapPackagePositions)
        }

        btQuit.setOnClickListener {
            windowManager.removeViewImmediate(viewTarget)
            windowManager.removeViewImmediate(viewCustomization)
            windowManager.removeViewImmediate(imageTarget)
            isShowing = false
        }

        windowManager.addView(viewTarget, outlineParams)
        windowManager.addView(viewCustomization, customizationParams)
        windowManager.addView(imageTarget, targetParams)
        isShowing = true
    }

    private fun describeAccessibilityNode(e: AccessibilityNodeInfo?): String {
        if (e == null) {
            return "null"
        }
        var result = "Node"
        result += " class =" + e.className.toString()
        val rect = Rect()
        e.getBoundsInScreen(rect)
        result += String.format(
            " Position=[%d, %d, %d, %d]", rect.left, rect.right, rect.top, rect.bottom
        )
        val id: CharSequence? = e.viewIdResourceName
        if (id != null) {
            result += " ResourceId=$id"
        }
        val description = e.contentDescription
        if (description != null) {
            result += " Description=$description"
        }
        val text = e.text
        if (text != null) {
            result += " Text=$text"
        }
        return result
    }


    /**
     * 查找所有的控件
     */
    private fun findAllNode(
        roots: List<AccessibilityNodeInfo>, list: MutableList<AccessibilityNodeInfo>, indent: String
    ) {
        val childrenList = java.util.ArrayList<AccessibilityNodeInfo>()
        for (e in roots) {
            list.add(e)
            for (n in 0 until e.childCount) {
                childrenList.add(e.getChild(n))
            }
        }
        if (childrenList.isNotEmpty()) {
            findAllNode(childrenList, list, "$indent  ")
        }
    }

    /**
     * 刷新要检测的包列表状态，在启动时查找所有包。也会在系统安装/卸载应用时触发
     */
    private fun updatePackage() {
        Log.e("halo", "==========开始过滤包列表=========")
        //临时包列表，里面存放一些需要排除的包名
        val setTemps: MutableSet<String> = mutableSetOf()

        var resolveInfoList: List<ResolveInfo>
        //查询设备上所有的启动器（Launcher）应用程序，并放入 setPackages 中
        var intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setPackages.add(e.activityInfo.packageName)
        }
        Log.e("halo", "待过滤时，setPackages的大小: " + setPackages.size)

        //查询设备上所有的主屏幕（Home）应用程序，，并放入 setTemps 中
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setTemps.add(e.activityInfo.packageName)
        }
        Log.e("halo", "主屏幕过滤后，setTemps的大小: " + setTemps.size)

        //查询设备上所有的输入法（imm）应用程序，并放入 setIMEApps 中
        val inputMethodInfoList =
            (service.getSystemService(AccessibilityService.INPUT_METHOD_SERVICE) as InputMethodManager).inputMethodList
        for (e in inputMethodInfoList) {
            setIMEApps.add(e.packageName)
        }
        Log.e("halo", "输入法过滤后，setIMEApps的大小: " + setIMEApps.size)

        //将当前应用程序和系统设置应用程序加入临时过滤集合
        setTemps.add(service.packageName)
        setTemps.add("com.android.settings")
        Log.e("halo", "添加自身和设置之后，setTemps的大小: " + setTemps.size)

        //移除白名单+输入法+临时列表
        setPackages.removeAll(whiteList)
        setPackages.removeAll(setIMEApps)
        setPackages.removeAll(setTemps)

        Log.e("halo", "移除白名单+输入法+临时列表之后，setPackages 最终的大小：" + setPackages.size)

        Log.e("halo", "==========结束过滤包列表=========")

    }

    /**
     * 开始处理无障碍事件
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val tempPkgName = event.packageName
        val tempClassName = event.className
        if (tempPkgName == null || tempClassName == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = tempPkgName.toString()
            Log.e("halo", "启动了: $pkgName")
            //如果是输入法
            if (setIMEApps.contains(pkgName)) return
            val actName = tempClassName.toString()
            //判断是否是系统应用
            val isActivity = !actName.startsWith("android.") && !actName.startsWith("androidx.")
            if (currentPackageName != pkgName) {
                if (isActivity) {
                    //其他包下的activity一定是一个新的activity
                    currentPackageName = pkgName
                    currentActivityName = actName
                    stopSkipAdProcess()
                    if (setPackages.contains(pkgName)) {
                        Log.e("halo", "该应用需要检测广告")
                        startSkipAdProcess()
                    }
                }
            } else {
                // 在当前包
                if (isActivity) {
                    if (currentActivityName != actName) {
                        currentActivityName = actName
                        return
                    }
                }
            }
            //通过位置跳过
            if (skipAdByActivityPosition) {
                skipAdByActivityPosition = false
                val packagePositionDescription = mapPackagePositions[currentPackageName]
                packagePositionDescription?.let {
                    Log.e("halo", "有位置信息，开始检测位置信息")
                    // try to click the position in the activity for multiple times

                    // try to click the position in the activity for multiple times
                    val futures = arrayOf<Future<*>?>(null)
                    futures[0] = taskExecutorService.scheduleAtFixedRate(
                        object : Runnable {
                            var num = 0
                            override fun run() {
                                if (num < PACKAGE_POSITION_CLICK_RETRY) {
                                    if (currentActivityName == packagePositionDescription.activityName) {
                                        Log.e("halo", "根据位置跳过了广告")
                                        click(
                                            packagePositionDescription.x,
                                            packagePositionDescription.y,
                                            40
                                        )
                                    }
                                    num++
                                } else {
                                    futures[0]!!.cancel(true)
                                }
                            }
                        },
                        PACKAGE_POSITION_CLICK_FIRST_DELAY.toLong(),
                        PACKAGE_POSITION_CLICK_RETRY_INTERVAL.toLong(),
                        TimeUnit.MILLISECONDS
                    )
                }
            }

            // 通过控件跳过
            if (skipAdByActivityWidget) {
                skipAdByActivityWidget = false
                setTargetedWidgets = mapPackageWidgets[currentPackageName]
            }
            setTargetedWidgets?.let {
                Log.e("halo", "有控件信息，开始检测控件信息")
                taskExecutorService.execute {
                    iterateNodesToSkipAd(
                        service.rootInActiveWindow, it
                    )
                }
            }
            // 通过关键字跳过
            if (skipAdByKeyword) {
                taskExecutorService.execute {
                    iterateNodesToSkipAd(
                        service.rootInActiveWindow, null
                    )
                }
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (!setPackages.contains(tempPkgName.toString())) {
                return
            }
            setTargetedWidgets?.let {
                taskExecutorService.execute {
                    iterateNodesToSkipAd(
                        event.source!!, it
                    )
                }
            }
            if (skipAdByKeyword) {
                taskExecutorService.execute {
                    iterateNodesToSkipAd(
                        event.source!!, null
                    )
                }
            }
        }
    }

    /**
     * 遍历AccessibilityNodeInfo节点以跳过广告
     * @param root 根节点
     * @param set 传入set时按控件判断，空则按关键词判断
     */
    private fun iterateNodesToSkipAd(
        root: AccessibilityNodeInfo, set: MutableSet<PackageWidgetDescription>?
    ) {
        val topNodes = java.util.ArrayList<AccessibilityNodeInfo>()
        topNodes.add(root)
        val childNodes = java.util.ArrayList<AccessibilityNodeInfo>()
        var total = topNodes.size
        var index = 0
        var node: AccessibilityNodeInfo
        var handled: Boolean
        while (index < total && skipAdRunning) {
            node = topNodes[index++]
            handled = if (set == null) skipAdByKeywords(node) else skipAdByTargetedWidget(node, set)
            if (handled) {
                node.recycle()
                break
            }
            for (n in 0 until node.childCount) {
                childNodes.add(node.getChild(n))
            }
            node.recycle()
            if (index == total) {
                topNodes.clear()
                topNodes.addAll(childNodes)
                childNodes.clear()
                index = 0
                total = topNodes.size
            }
        }
        while (index < total) {
            node = topNodes[index++]
            node.recycle()
        }
        index = 0
        total = childNodes.size
        while (index < total) {
            node = childNodes[index++]
            node.recycle()
        }
    }

    /**
     * 通过关键字跳过节点
     */
    private fun skipAdByKeywords(node: AccessibilityNodeInfo): Boolean {
        val description = node.contentDescription
        val text = node.text
        if (TextUtils.isEmpty(description) && TextUtils.isEmpty(text)) {
            return false
        }
        var isFound = false
        for (keyword in keyWordList) {
            // 内容包含关键字, 并且长度不能太长
            if (text != null && text.toString().length <= keyword.length + 6 && text.toString()
                    .contains(keyword)
            ) {
                isFound = true
            } else if (description != null && description.toString().length <= keyword.length + 6 && description.toString()
                    .contains(keyword)
            ) {
                isFound = true
            }
            if (isFound) {
                break
            }
        }
        if (isFound) {
            val nodeDesc: String = describeAccessibilityNode(node)
            if (!clickedWidgets.contains(nodeDesc)) {
                clickedWidgets.add(nodeDesc)
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) {
                    Log.e("halo", "通过检测关键字跳过了广告")
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    click(rect.centerX(), rect.centerY(), 20)
                }
                return true
            }
        }
        return false
    }

    /**
     * 通过目标控件跳过节点
     */
    private fun skipAdByTargetedWidget(
        node: AccessibilityNodeInfo, set: MutableSet<PackageWidgetDescription>
    ): Boolean {
        //节点在屏幕上的位置和大小
        val temRect = Rect()
        node.getBoundsInScreen(temRect)
        //View的唯一标识符
        val cId: CharSequence? = node.viewIdResourceName
        //node节点的内容描述
        val cDescribe: CharSequence? = node.contentDescription
        //node节点的文本内容
        val cText: CharSequence? = node.text
        for (e in set) {
            var isFound = false
            if (temRect == e.position) {
                isFound = true
            } else if (cId != null && e.idName.isNotEmpty() && cId.toString() == e.idName) {
                isFound = true
            } else if (cDescribe != null && e.description.isNotEmpty() && cDescribe.toString()
                    .contains(e.description)
            ) {
                isFound = true
            } else if (cText != null && e.text.isNotEmpty() && cText.toString().contains(e.text)) {
                isFound = true
            }
            if (isFound) {
                val nodeDesc: String = describeAccessibilityNode(node)
                if (!clickedWidgets.contains(nodeDesc)) {
                    clickedWidgets.add(nodeDesc)
                    Log.e("halo", "通过指定控件跳过了广告")
                    if (e.onlyClick) {
                        click(temRect.centerX(), temRect.centerY(), 20)
                    } else {
                        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            if (!node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                click(temRect.centerX(), temRect.centerY(), 20)
                            }
                        }
                    }
                    if (setTargetedWidgets === set) setTargetedWidgets = null
                    return true
                }
            }
        }
        return false
    }


    /**
     * 模拟点击
     */
    private fun click(x: Int, y: Int, duration: Long): Boolean {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val builder = GestureDescription.Builder().addStroke(StrokeDescription(path, 0, duration))
        return service.dispatchGesture(builder.build(), null, null)
    }

    /**
     * 停止跳广告进程
     */
    private fun stopSkipAdProcess() {
        stopSkipAdProcessInner()
        receiverHandler!!.removeMessages(TouchHelperService.ACTION_STOP_SKIP_AD)
    }

    /**
     * 开启跳广告进程
     */
    private fun startSkipAdProcess() {
        Log.e("halo", "开始跳广告进程")
        skipAdRunning = true
        skipAdByActivityPosition = true
        skipAdByActivityWidget = true
        skipAdByKeyword = true
        setTargetedWidgets = null
        clickedWidgets.clear()
        receiverHandler!!.removeMessages(TouchHelperService.ACTION_STOP_SKIP_AD)
        receiverHandler!!.sendEmptyMessageDelayed(
            TouchHelperService.ACTION_STOP_SKIP_AD, Settings.getAdDetectionDuration() * 1000L
        )
    }

    /**
     * 停止跳过广告进程
     */
    private fun stopSkipAdProcessInner() {
        Log.e("halo", "停止跳广告进程")
        skipAdRunning = false
        skipAdByActivityPosition = false
        skipAdByActivityWidget = false
        skipAdByKeyword = false
        setTargetedWidgets = null
    }

    fun onUnbind() {
        service.unregisterReceiver(userPresentReceiver)
        Log.e("halo", "用户屏幕开启广播已注销")
        service.unregisterReceiver(packageChangeReceiver)
        Log.e("halo", "系统包变化广播已注销")
        Log.e("halo", "==========无障碍服务已关闭==========")
    }

}