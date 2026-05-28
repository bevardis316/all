package lt.ibnvs.chat

data class Message(val text: String, val type: Type) {
    enum class Type { USER, BOT, TYPING }
}
