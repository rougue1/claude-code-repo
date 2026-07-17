package com.localchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.localchat.app.ui.AppRoot
import com.localchat.app.ui.chat.ChatViewModel
import com.localchat.app.ui.chat.ChatViewModelFactory
import com.localchat.app.ui.theme.LocalChatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory((application as LocalChatApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalChatTheme {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}
