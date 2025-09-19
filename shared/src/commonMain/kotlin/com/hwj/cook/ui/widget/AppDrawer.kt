package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.ui.viewmodel.ConversationViewModel
import com.hwj.cook.ui.viewmodel.MainVm
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun AppDrawer(
    navigator: Navigator, onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit
) {
    AppDrawerIn(
        navigator, onConversationClicked, onNewConversationClicked, onThemeClicked,
        deleteConversation = { id -> }, sessionList = null
    )
}

@Composable
fun AppDrawerIn(
    navigator: Navigator, onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    deleteConversation: (String) -> Unit,
    sessionList: MutableList<String>?, //agent
) {
    val canJump = remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))//影响键盘？

        Column(Modifier.height(100.dp).background(color = cOrangeFFB8664())) {}
    }
}

@Composable
private fun ColumnScope.HistoryConversations(onConversationClicked: (String) -> Unit,
                                             onNewConversationClicked: () -> Unit,
                                             deleteConversation: (String) -> Unit,
                                             sessionList: MutableList<String>?){
    val subScope = rememberCoroutineScope()
    val mainVm= koinViewModel(MainVm::class)
    val conversationVm= koinViewModel(ConversationViewModel::class)
    val isDark =  mainVm.darkState.collectAsState().value
    val listState = rememberLazyListState()
//    val curChatId by conversationVm.currentConversationState.collectAsState()
}