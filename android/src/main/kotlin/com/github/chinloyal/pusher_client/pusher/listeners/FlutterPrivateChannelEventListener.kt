package com.github.chinloyal.pusher_client.pusher.listeners

import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.enableLogging
import com.github.chinloyal.pusher_client.pusher.PusherService.Companion.errorLog
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.channel.PusherEvent
import java.lang.Exception
import com.google.gson.JsonObject

class FlutterPrivateChannelEventListener: FlutterBaseChannelEventListener(), PrivateChannelEventListener {
    companion object {
        val instance = FlutterPrivateChannelEventListener()
    }

    override fun onAuthenticationFailure(message: String, e: Exception) {
        errorLog(message)
        if(enableLogging) e.printStackTrace()
    }

    override fun onSubscriptionSucceeded(channelName: String) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", JsonNull.INSTANCE)
        eventData.addProperty("data", JsonNull.INSTANCE)

        this.onEvent(PusherEvent(eventData))
        PusherService.debugLog("[PRIVATE] Subscribed: $channelName")
    }
}