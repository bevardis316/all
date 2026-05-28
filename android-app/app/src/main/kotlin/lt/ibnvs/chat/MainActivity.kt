package lt.ibnvs.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val messages = mutableListOf<Message>()
    private val history  = mutableListOf<Map<String, String>>()

    private lateinit var adapter: ChatAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recyclerView)
        input    = findViewById(R.id.inputField)
        send     = findViewById(R.id.sendButton)

        adapter = ChatAdapter(messages)
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recycler.adapter = adapter

        addBot("Sveiki! Esu IBNVS virtualus konsultantas. Klauskite apie nuotekų sistemas.")

        send.setOnClickListener { onSend() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { onSend(); true } else false
        }
    }

    private fun onSend() {
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        input.setText("")
        hideKeyboard()
        sendMessage(text)
    }

    private fun sendMessage(text: String) {
        addMessage(Message(text, Message.Type.USER))

        if (history.size >= 14) {
            history.clear()
            addBot("Pokalbis per ilgas – pradedamas naujas.")
        }

        val typing = Message("Rašo…", Message.Type.TYPING)
        addMessage(typing)
        send.isEnabled = false
        input.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ApiClient.ask(text, history.toList())
            }

            removeTyping()
            send.isEnabled = true
            input.isEnabled = true

            when (result) {
                is ApiClient.Result.Success -> {
                    history += mapOf("role" to "user",      "content" to text)
                    history += mapOf("role" to "assistant", "content" to result.text)
                    if (history.size > 16) {
                        val trimmed = history.drop(history.size - 16)
                        history.clear()
                        history.addAll(trimmed)
                    }
                    addBot(result.text)
                }
                is ApiClient.Result.Error -> addBot(result.message)
            }
        }
    }

    private fun addMessage(msg: Message) {
        messages += msg
        adapter.notifyItemInserted(messages.lastIndex)
        recycler.scrollToPosition(messages.lastIndex)
    }

    private fun addBot(text: String) = addMessage(Message(text, Message.Type.BOT))

    private fun removeTyping() {
        val idx = messages.indexOfLast { it.type == Message.Type.TYPING }
        if (idx >= 0) {
            messages.removeAt(idx)
            adapter.notifyItemRemoved(idx)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
