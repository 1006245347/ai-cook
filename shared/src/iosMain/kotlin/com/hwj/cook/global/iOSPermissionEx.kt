package com.hwj.ai.global

import androidx.compose.runtime.Composable
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.ios.PermissionsController


@Composable
 fun askPermission(permission: Permission, grantedAction: () -> Unit,deniedAction:()->Unit) {
    AskPer(permission, grantedAction,deniedAction)
}

@Composable
private fun AskPer(permission: Permission, grantedAction: () -> Unit,deniedAction: () -> Unit) {
    val viewModel = PermissionsViewModel(
        permissionsController = PermissionsController(),
        permission = permission
    )
    viewModel.onRequest (permission,grantedAction={grantedAction()},deniedAction={
        deniedAction()
    })
}