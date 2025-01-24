package com.github.chinloyal.pusher_client.pusher.listeners

import android.os.Handler
import android.os.Looper
import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.debugLog
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.eventSink
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import java.lang.Exception
import com.google.gson.JsonObject


open class FlutterBaseChannelEventListener: ChannelEventListener {
    private val eventStreamJson = JsonObject();

    override fun onEvent(event: PusherEvent) {
        Handler(Looper.getMainLooper()).post {
            try {
                val eventJson = JsonObject()
                eventJson.addProperty("channelName", event.channelName)
                eventJson.addProperty("eventName", event.eventName)
                eventJson.addProperty("userId", event.userId) // Handles null
                eventJson.addProperty("data", event.data) // Handles null

                eventStreamJson.add("pusherEvent", eventJson)

                eventSink?.success(eventStreamJson.toString())
                debugLog("""
                |[ON_EVENT] Channel: ${event.channelName}, EventName: ${event.eventName},
                |Data: ${event.data}, User Id: ${event.userId}
                """.trimMargin())
            } catch (e: Exception) {
                eventSink?.error("ON_EVENT_ERROR", e.message, e)
            }

        }
    }

    override fun onSubscriptionSucceeded(channelName: String) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", JsonNull.INSTANCE)
        eventData.addProperty("data", JsonNull.INSTANCE)

        this.onEvent(PusherEvent(eventData))
        debugLog("[PUBLIC] Subscribed: $channelName")

    }
}