package com.example.room

import com.example.data.MessageDataSource
import com.example.data.model.Message
import io.ktor.http.cio.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class RoomController(
    private val messageDataSource: MessageDataSource
) {

    private val members = ConcurrentHashMap<String, Member>()

    fun onJoin(
        username: String,
        sessionId: String,
        socket: WebSocketSession
    ) {
        if (members.containsKey(username)){
            throw MemberAlreadyExistsException()
        }
        members[username] = Member(
            username = username,
            sessionId = sessionId,
            socket = socket
        )
    }

    suspend fun sendMessage(senderUserName: String, message: String){
        val messageEntity = Message(
            text = message,
            username = senderUserName,
            timestamp = System.currentTimeMillis()
        )
        messageDataSource.insertMessage(messageEntity)
        members.values.forEach{ member ->
            val parsedMessage = Json.encodeToString(messageEntity)
            member.socket.send(Frame.Text(parsedMessage))
        }
    }

    suspend fun getAllMessages(): List<Message>{
        return messageDataSource.getAllMessages()
    }

    suspend fun tryDisconnect(username: String){
        members[username]?.socket?.close()
        if(members.containsKey(username)){
            members.remove(username)
        }
    }

}