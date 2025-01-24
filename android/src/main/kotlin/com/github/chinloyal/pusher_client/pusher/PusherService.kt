package com.github.chinloyal.pusher_client.pusher

import android.util.Log
import com.github.chinloyal.pusher_client.core.contracts.MChannel
import com.github.chinloyal.pusher_client.core.utils.JsonEncodedConnectionFactory
import com.github.chinloyal.pusher_client.pusher.listeners.*
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.connection.ConnectionState
import com.pusher.client.util.HttpAuthorizer
import com.pusher.client.util.UrlEncodedConnectionFactory
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.lang.Exception
import com.google.gson.JsonObject
import com.google.gson.JsonNull



const val CHANNEL_NAME = "com.github.chinloyal/pusher_client"
const val EVENT_STREAM = "com.github.chinloyal/pusher_client_stream"
const val LOG_TAG = "PusherClientPlugin"
const val PRIVATE_PREFIX = "private-"
const val PRIVATE_ENCRYPTED_PREFIX = "private-encrypted-"
const val PRESENCE_PREFIX = "presence-"

class PusherService : MChannel {
    private var _pusherInstance: Pusher? = null

    companion object {
        var enableLogging: Boolean = false
        var eventSink: EventSink? = null
        fun debugLog(msg: String) {
            if(enableLogging) {
                Log.d(LOG_TAG, msg)
            }
        }

        fun errorLog(msg: String) {
            if(enableLogging) {
                Log.e(LOG_TAG, msg)
            }
        }
    }

    override fun register(messenger: BinaryMessenger) {
        MethodChannel(messenger, CHANNEL_NAME).setMethodCallHandler { call, result ->
            when (call.method) {
                "init" ->  init(call, result)
                "connect" -> connect(result)
                "disconnect" -> disconnect(result)
                "getSocketId" -> getSocketId(result)
                "subscribe" -> subscribe(call, result)
                "unsubscribe" -> unsubscribe(call, result)
                "bind" -> bind(call, result)
                "unbind" -> unbind(call, result)
                "trigger" -> trigger(call, result)
                else -> result.notImplemented()
            }
        }

        EventChannel(messenger, EVENT_STREAM).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(args: Any?, eventSink: EventSink) {
                PusherService.eventSink = eventSink
                debugLog("Event stream listening...")
            }

            override fun onCancel(args: Any?) {
                debugLog("Event stream cancelled.")
            }
        })
    }

    private fun init(call: MethodCall, result: Result) {
        // toString works because this is json encoded in dart
        val args = JsonObject()
        args.addProperty("initArgs", call.arguments.toString())
        val initArgs = args.getAsJsonObject("initArgs")
        enableLogging = initArgs.get("enableLogging").asBoolean


        if(_pusherInstance == null) {
            val options: JsonObject = args.getAsJsonObject("pusherOptions")
            val pusherOptions = PusherOptions()

            if (options.has("auth") && !options.get("auth").isJsonNull) {
                val auth = options.getAsJsonObject("auth")
                val endpoint: String = auth.get("endpoint").asString
                val headersMap = mutableMapOf<String, String>()
                val headersJson = JsonObject()
                auth.get("headers").asJsonObject.entrySet().forEach { entry ->
                    headersMap[entry.key] = entry.value.asString
                }

                val encodedConnectionFactory = if (headersMap.containsValue("application/json"))
                    JsonEncodedConnectionFactory() else UrlEncodedConnectionFactory()

                val authorizer = HttpAuthorizer(endpoint,  encodedConnectionFactory)
                authorizer.setHeaders(headersMap)

                pusherOptions.authorizer = authorizer
            }

            pusherOptions.setHost(options.get("host").asString)

            if(!options.isNull("cluster")) {
                pusherOptions.setCluster(options.get("cluster").asString)
            }
            if (options.has("cluster") && !options.get("cluster").isJsonNull) {
                val auth = options.getAsJsonObject("cluster")
            }


            pusherOptions.activityTimeout = options.get("activityTimeout").asLong
            pusherOptions.pongTimeout = options.get("pongTimeout").asLong
            pusherOptions.maxReconnectionAttempts = options.get("maxReconnectionAttempts").asInt
            pusherOptions.maxReconnectGapInSeconds = options.get("maxReconnectGapInSeconds").asInt
            pusherOptions.setWsPort(options.get("wsPort").asInt)
            pusherOptions.setWssPort(options.get("wssPort").asInt)
            pusherOptions.isUseTLS = options.get("encrypted").asBoolean

            _pusherInstance = Pusher(args.get("appKey").asString, pusherOptions)

            debugLog("Pusher initialized")
        }

        result.success(null)
    }

    private fun connect(result: Result) {
        _pusherInstance?.connect(ConnectionListener(), ConnectionState.ALL)
        result.success(null)
    }

    private fun disconnect(result: Result) {
        _pusherInstance?.disconnect()
        debugLog("Disconnect")
        result.success(null)
    }

    private fun getSocketId(result: Result) {
        result.success(_pusherInstance?.connection?.socketId)
    }

    private fun subscribe(call: MethodCall, result: Result) {
        try {
            val src = call.arguments as Map<String, Any>
            val args = JsonObject()
            src.forEach { (key, value) ->
                args.addProperty(key, value.toString())
            }

            val channelName: String = args.get("channelName").asString

            when {
                channelName.startsWith(PRIVATE_ENCRYPTED_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateEncryptedChannel(channelName)
                    if (channel == null || !channel.isSubscribed)
                        _pusherInstance?.subscribePrivateEncrypted(channelName, FlutterPrivateEncryptedChannelEventListener.instance)
                }
                channelName.startsWith(PRIVATE_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateChannel(channelName)
                    if (channel == null || !channel.isSubscribed)
                        _pusherInstance?.subscribePrivate(channelName, FlutterPrivateChannelEventListener.instance)
                }
                channelName.startsWith(PRESENCE_PREFIX) -> {
                    val channel = _pusherInstance?.getPresenceChannel(channelName)
                    if (channel == null || !channel.isSubscribed)
                        _pusherInstance?.subscribePresence(channelName, FlutterPresenceChannelEventListener.instance)
                }
                else -> {
                    val channel = _pusherInstance?.getChannel(channelName)
                    if (channel == null || !channel.isSubscribed)
                        _pusherInstance?.subscribe(channelName, FlutterChannelEventListener.instance)
                }
            }

            result.success(null)
        } catch (e: Exception) {
            e.message?.let { errorLog(it) }
            if (enableLogging) e.printStackTrace()
            result.error("SUBSCRIBE_ERROR", e.message, e)
        }
    }

    private fun unsubscribe(call: MethodCall, result: Result) {
        try {
            val src = call.arguments as Map<String, Any>
            val args = JsonObject()
            src.forEach { (key, value) ->
                args.addProperty(key, value.toString())
            }
            val channelName = args.get("channelName").asString

            _pusherInstance?.unsubscribe(channelName)

            debugLog("Unsubscribed: $channelName")
            result.success(null)
        } catch (e: Exception) {
            e.message?.let { errorLog(it) }
            if (enableLogging) e.printStackTrace()
            result.error("UNSUBSCRIBE_ERROR", e.message, e)
        }
    }

    /**
     * Note binding can happen before the channel has been subscribed to
     */
    private fun bind(call: MethodCall, result: Result) {
        try {
            val src = call.arguments as Map<String, Any>
            val args = JsonObject()
            src.forEach { (key, value) ->
                args.addProperty(key, value.toString())
            }
            val channelName: String = args.get("channelName").asString
            val eventName: String = args.get("eventName").asString

            when {
                channelName.startsWith(PRIVATE_ENCRYPTED_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateEncryptedChannel(channelName)
                    channel?.bind(eventName, FlutterPrivateEncryptedChannelEventListener.instance)
                }
                channelName.startsWith(PRIVATE_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateChannel(channelName)
                    channel?.bind(eventName, FlutterPrivateChannelEventListener.instance)
                }
                channelName.startsWith(PRESENCE_PREFIX) -> {
                    val channel = _pusherInstance?.getPresenceChannel(channelName)
                    channel?.bind(eventName, FlutterPresenceChannelEventListener.instance)
                }
                else -> {
                    val channel = _pusherInstance?.getChannel(channelName)
                    channel?.bind(eventName, FlutterChannelEventListener.instance)
                }
            }

            debugLog("[BIND] $eventName")
            result.success(null)
        } catch (e: Exception) {
            e.message?.let { errorLog(it) }
            if (enableLogging) e.printStackTrace()
            result.error("BIND_ERROR", e.message, e)
        }
    }

    private fun unbind(call: MethodCall, result: Result) {
        try {
            val src = call.arguments as Map<String, Any>
            val args = JsonObject()
            src.forEach { (key, value) ->
                args.addProperty(key, value.toString())
            }
            val channelName: String = args.get("channelName").asString
            val eventName: String = args.get("eventName").asString

            when {
                channelName.startsWith(PRIVATE_ENCRYPTED_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateEncryptedChannel(channelName)
                    channel?.unbind(eventName, FlutterPrivateEncryptedChannelEventListener.instance)
                }
                channelName.startsWith(PRIVATE_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateChannel(channelName)
                    channel?.unbind(eventName, FlutterPrivateChannelEventListener.instance)
                }
                channelName.startsWith(PRESENCE_PREFIX) -> {
                    val channel = _pusherInstance?.getPresenceChannel(channelName)
                    channel?.unbind(eventName, FlutterPresenceChannelEventListener.instance)
                }
                else -> {
                    val channel = _pusherInstance?.getChannel(channelName)
                    channel?.unbind(eventName, FlutterChannelEventListener.instance)
                }
            }

            debugLog("[UNBIND] $eventName")
            result.success(null)
        } catch (e: Exception) {
            e.message?.let { errorLog(it) }
            if (enableLogging) e.printStackTrace()
            result.error("UNBIND_ERROR", e.message, e)
        }
    }

    private fun trigger(call: MethodCall, result: Result) {
        try {
            // toString works because this is json encoded in dart
            val args = JsonObject()
            args.addProperty("eventName", call.argument<String>("eventName"))
            args.addProperty("data", call.argument<String>("data"))
            args.addProperty("channelName", call.argument<String>("channelName"))
            val eventName: String = args.get("eventName").asString
            val data: String = args.get("data").asString
            val channelName: String = args.get("channelName").asString
            val errorMessage = "Trigger can only be called on private and presence channels."
            when {
                channelName.startsWith(PRIVATE_ENCRYPTED_PREFIX) -> {
                    result.error("TRIGGER_ERROR", errorMessage, null)
                }
                channelName.startsWith(PRIVATE_PREFIX) -> {
                    val channel = _pusherInstance?.getPrivateChannel(channelName)
                    channel?.trigger(eventName, data)
                }
                channelName.startsWith(PRESENCE_PREFIX) -> {
                    val channel = _pusherInstance?.getPresenceChannel(channelName)
                    channel?.trigger(eventName, data)
                }
                else -> result.error("TRIGGER_ERROR", errorMessage, null)
            }

            debugLog("[TRIGGER] $eventName")
            result.success(null)
        } catch (e: Exception) {
            e.message?.let { errorLog(it) }
            if (enableLogging) e.printStackTrace()
            result.error("TRIGGER_ERROR", e.message, e)
        }
    }

}