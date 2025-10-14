package com.hwj.cook.global

import android.Manifest
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import dev.icerock.moko.mvvm.dispatcher.EventsDispatcher
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.storage.STORAGE
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * @author by jason-何伟杰，2024/4/22
 * des:android权限申请
 */
@Composable
private fun AskPer(
    permissionType: Permission,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    val viewModel = getViewModel(key = "x", factory = viewModelFactory {
        SampleViewModel(
            eventsDispatcher = eventsDispatcherOnMain(),
            permissionsController = PermissionsController(MainApplication.appContext),
            permissionType = permissionType //只能单个权限？
        )
    })

    val coroutineScope = rememberCoroutineScope()
    val eventsListener = remember {
        object : SampleViewModel.EventListener {
            override fun onSuccess() {
                coroutineScope.launch {
                    printD("permission suc!")
                    grantedAction()
                }
            }

            override fun onDenied(exception: DeniedException) {
                printD("permission denied")
                deniedAction()
            }

            override fun onDeniedAlways(exception: DeniedAlwaysException) {
                printD("permission den always $exception") //第一次安装走这里了？
//                viewModel.permissionsController.openAppSettings() //跳转权限设置
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(true) {
        viewModel.eventsDispatcher.bind(lifecycleOwner, eventsListener)
    }
    BindEffect(viewModel.permissionsController)
//    val permissionState by viewModel.permissionState.collectAsState() //观察权限的变化
//    Column {
//        Text("permission state> $permissionState")
//        Button(onClick = { viewModel.onRequestPermissionButtonPressed() }) {
//            Text(" request permission")
//        }
//    }

    viewModel.onRequestPermissionButtonPressed() //触发请求
}

fun createExecutorMain(): Executor {
    val mainLoop = Looper.getMainLooper()
    val mainHandler = Handler(mainLoop)
    return Executor { mainHandler.post(it) }
}

inline fun <reified T : Any> eventsDispatcherOnMain(): EventsDispatcher<T> {
    return EventsDispatcher(createExecutorMain())
}

//Android权限申请
@Composable
fun askPermission(permission: Permission, grantedAction: () -> Unit, deniedAction: () -> Unit) {
    AskPer(permission, grantedAction, deniedAction)
}

@Composable
fun purePermission() {
    val requestList = mutableListOf<String>()
    requestList.add(Manifest.permission.READ_MEDIA_IMAGES)
    requestList.add(Manifest.permission.READ_MEDIA_VIDEO)

    (LocalActivity.current as? FragmentActivity)?.let {
        //Activity的类型转变不兼容，尴尬
        PermissionX.init(it)
            .permissions(requestList)
            .onExplainRequestReason { scope, deniedList ->
                val msg = "同意以下权限使用："
                scope.showRequestReasonDialog(deniedList, msg, "同意", "取消")
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) { //重新回调
                    printD("同意》")
                } else {
                    printD("warning>")
                }
            }
    }
}