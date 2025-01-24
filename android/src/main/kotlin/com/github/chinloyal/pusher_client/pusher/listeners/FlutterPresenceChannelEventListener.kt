package com.github.chinloyal.pusher_client.pusher.listeners

import com.github.chinloyal.pusher_client.core.utils.Constants
import com.github.chinloyal.pusher_client.pusher.PusherService
import com.pusher.client.channel.PresenceChannelEventListener
import com.pusher.client.channel.PusherEvent
import com.pusher.client.channel.User
import java.lang.Exception
import com.google.gson.JsonObject

class FlutterPresenceChannelEventListener: FlutterBaseChannelEventListener(), PresenceChannelEventListener {
    companion object {
        val instance = FlutterPresenceChannelEventListener()
    }

    override fun onUsersInformationReceived(channelName: String, users: MutableSet<User>) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", null)
        eventData.addProperty("data", users.toString())

        this.onEvent(PusherEvent(eventData))
    }

    override fun userUnsubscribed(channelName: String, user: User) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.MEMBER_REMOVED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", user.id)
        eventData.addProperty("data", null)

        this.onEvent(PusherEvent(eventData))
    }

    override fun userSubscribed(channelName: String, user: User) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.MEMBER_ADDED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", user.id)
        eventData.addProperty("data", null)

        this.onEvent(PusherEvent(eventData))
    }

    override fun onAuthenticationFailure(message: String, e: Exception) {
        PusherService.errorLog(message)
        if(PusherService.enableLogging) e.printStackTrace()
    }

    override fun onSubscriptionSucceeded(channelName: String) {
        val eventData = JsonObject()
        eventData.addProperty("event", Constants.SUBSCRIPTION_SUCCEEDED.value)
        eventData.addProperty("channel", channelName)
        eventData.addProperty("user_id", null)
        eventData.addProperty("data", null)

        this.onEvent(PusherEvent(eventData))
        PusherService.debugLog("[PRESENCE] Subscribed: $channelName")
    }
}