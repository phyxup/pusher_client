package com.github.chinloyal.pusher_client.pusher.listeners

import android.os.Handler
import android.os.Looper
import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.debugLog
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.eventSink
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.channel.PusherEvent
import java.lang.Exception
import org.json.JSONObject

open class FlutterBaseChannelEventListener: ChannelEventListener {
    private val eventStreamJson = JSONObject();

    override fun onEvent(event: PusherEvent) {
        Handler(Looper.getMainLooper()).post {
            try {
                val eventJson = JSONObject()
                eventJson.put("channelName", event.channelName)
                eventJson.put("eventName", event.eventName)
                eventJson.put("userId", event.userId) // Handles null
                eventJson.put("data", event.data) // Handles null

                eventStreamJson.put("pusherEvent", eventJson)

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
        val eventData = JSONObject()
        eventData.put("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.put("channel", channelName)
        eventData.put("user_id", null)
        eventData.put("data", null)

        this.onEvent(PusherEvent(eventData))
        debugLog("[PUBLIC] Subscribed: $channelName")

    }
}