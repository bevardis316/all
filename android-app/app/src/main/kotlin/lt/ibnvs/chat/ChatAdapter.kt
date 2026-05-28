package lt.ibnvs.chat

import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT  = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].type == Message.Type.USER) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserVH(inf.inflate(R.layout.item_user, parent, false))
            else      -> BotVH(inf.inflate(R.layout.item_bot,  parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserVH -> holder.text.text = msg.text
            is BotVH  -> {
                val processed = processLinks(msg.text)
                holder.text.text = processed
                Linkify.addLinks(holder.text, Linkify.WEB_URLS)
            }
        }
    }

    override fun getItemCount() = messages.size

    private fun processLinks(text: String): String = text
        .replace("[DIAGNOSTIKA]",   "manonuotekos.lt/diag/diagnose.php")
        .replace("[KONSTRUKTORIUS]","manonuotekos.lt/kon/")
        .replace("[ŽEMĖLAPIS]",     "manonuotekos.lt/map/")
        .replace(Regex("\\[KON:([A-Z0-9]+)\\]")) { "manonuotekos.lt/kon/#obj=${it.groupValues[1]}" }

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.messageText)
    }

    class BotVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.messageText)
    }
}
