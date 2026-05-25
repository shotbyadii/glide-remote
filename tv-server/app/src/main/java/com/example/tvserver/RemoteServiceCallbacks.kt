package com.example.tvserver

object RemoteServiceCallbacks {
    var onPositionUpdated: ((Float, Float) -> Unit)? = null
    var onClickReceived: ((Boolean) -> Unit)? = null
    var onKeyReceived: ((String) -> Unit)? = null
    var onBrightnessChanged: ((Int) -> Unit)? = null
    var onVolumeChanged: ((Int) -> Unit)? = null
    var onPictureModeChanged: ((String) -> Unit)? = null
    var onLaunchSetting: ((String) -> Unit)? = null
}
