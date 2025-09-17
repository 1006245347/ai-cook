package com.hwj.ai.global

import dev.icerock.moko.mvvm.dispatcher.EventsDispatcher
import dev.icerock.moko.mvvm.dispatcher.EventsDispatcherOwner
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.ios.PermissionsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class iOSPermissionVm(
    override val eventsDispatcher: EventsDispatcher<EventListener>,
    val permissionsController: PermissionsController,
    private val permissionType: Permission

) : ViewModel(), EventsDispatcherOwner<iOSPermissionVm.EventListener> {

    val permissionState = MutableStateFlow(PermissionState.NotDetermined)

    init {
        viewModelScope.launch {
            permissionState.update { permissionsController.getPermissionState(permissionType) }
            printD(permissionState.toString())
        }
    }

    /**
     * An example of using [PermissionsController] in common code.
     */
    fun onRequestPermissionButtonPressed() {
        requestPermission(permissionType)
    }

    private fun requestPermission(permission: Permission) {
        viewModelScope.launch {
            try {
                permissionsController.getPermissionState(permission)
                    .also { printD("pre provide $it") }

                // Calls suspend function in a coroutine to request some permission.
                permissionsController.providePermission(permission)
                // If there are no exceptions, permission has been granted successfully.

                eventsDispatcher.dispatchEvent { onSuccess() }
            } catch (deniedAlwaysException: DeniedAlwaysException) {
                eventsDispatcher.dispatchEvent { onDeniedAlways(deniedAlwaysException) }
            } catch (deniedException: DeniedException) {
                eventsDispatcher.dispatchEvent { onDenied(deniedException) }
            } finally {
                permissionState.update {
                    permissionsController.getPermissionState(permission)
                        .also { printD("post provide $it") } //有可能 granted 或 denied
                }
            }
        }
    }

    interface EventListener {

        fun onSuccess()

        fun onDenied(exception: DeniedException)

        fun onDeniedAlways(exception: DeniedAlwaysException)
    }
}

//iOS权限申请
class PermissionsViewModel(
    val permissionsController: PermissionsController,
    val permission: Permission
) : ViewModel() {
    val permissionState = MutableStateFlow(PermissionState.NotDetermined)
    val curPermission = MutableStateFlow(permission)

    init {
        viewModelScope.launch {
            permissionState.update { permissionsController.getPermissionState(permission) }

        }
    }

    fun onRequest(
        newPermission: Permission? = null,
        grantedAction: (PermissionsViewModel) -> Unit,deniedAction:(PermissionsViewModel)->Unit
    ) {
        viewModelScope.launch {
            try {
                if (newPermission == null) {
                    curPermission.update { permission }
                } else {
                    curPermission.update { newPermission }
                }
//                printLogW("cur>${curPermission.value}")
                permissionsController.getPermissionState(curPermission.value)
//                    .also { printLogW("before>$it ${isMainThread()}") }
                permissionsController.providePermission(curPermission.value)
//                printLogW("${curPermission.value} is success!")
                grantedAction(this@PermissionsViewModel)
            } catch (deniedAlwaysException: DeniedAlwaysException) {
                printD("DeniedAlwaysException")
            } catch (deniedException: DeniedException) {
                printD("DeniedException")
                deniedAction(this@PermissionsViewModel)
            } finally {
                permissionState.update {
                    permissionsController.getPermissionState(curPermission.value)
//                        .also { printLogW("finally>${curPermission.value} $it") }
                }
            }
        }
    }

    suspend fun isPermissionGranted(permission: Permission): Boolean {
        return permissionsController.isPermissionGranted(permission)
    }
}