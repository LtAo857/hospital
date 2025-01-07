<template>
<uni-shadow-root class="trtc-room-template-custom-custom"><template v-if="wxTemplateName === 'custom'">
  <view class="template-custom">
    <view class="players-container">
      <view v-for="(item,index) in (streamList)" :key="item.streamID" v-if="item.src && (item.hasVideo || item.hasAudio)" :class="'view-container player-container '+(item.isVisible?'':'none')" :style="'left:'+(item.xAxis)+';top:'+(item.yAxis)+';width:'+(item.width)+';height:'+(item.height)+';z-index:'+(item.zIndex)+';'">
        <live-player class="player" :id="item.streamID" :data-userid="item.userID" :data-streamid="item.streamID" :data-streamtype="item.streamType" :src="item.src" :mode="item.mode" :autoplay="item.autoplay" :mute-audio="item.muteAudio" :mute-video="item.muteVideo" :orientation="item.orientation" :object-fit="item.objectFit" :background-mute="item.enableBackgroundMute" :min-cache="item.minCache" :max-cache="item.maxCache" :sound-mode="item.soundMode" :enable-recv-message="item.enableRecvMessage" :auto-pause-if-navigate="item.autoPauseIfNavigate" :auto-pause-if-open-native="item.autoPauseIfOpenNative" :debug="debug" @statechange="_$self.$parent.$parent[('_playerStateChange')]($event)" @fullscreenchange="_$self.$parent.$parent[('_playerFullscreenChange')]($event)" @netstatus="_$self.$parent.$parent[('_playerNetStatus')]($event)" @audiovolumenotify="_$self.$parent.$parent[('_playerAudioVolumeNotify')]($event)"></live-player>
      </view>
    </view>
    <view :class="'view-container pusher-container '+(pusher.isVisible?'':'none')" :style="'left:'+(pusher.xAxis)+';top:'+(pusher.yAxis)+';width:'+(pusher.width)+';height:'+(pusher.height)+';z-index:'+(pusher.zIndex)+';'">
      <live-pusher class="pusher" :url="pusher.url" :mode="pusher.mode" :autopush="pusher.autopush" :enable-camera="pusher.enableCamera" :enable-mic="pusher.enableMic" :muted="(!pusher.enableMic)" :enable-agc="pusher.enableAgc" :enable-ans="pusher.enableAns" :enable-ear-monitor="pusher.enableEarMonitor" :auto-focus="pusher.enableAutoFocus" :zoom="pusher.enableZoom" :min-bitrate="pusher.minBitrate" :max-bitrate="pusher.maxBitrate" :video-width="pusher.videoWidth" :video-height="pusher.videoHeight" :beauty="pusher.beautyLevel" :whiteness="pusher.whitenessLevel" :orientation="pusher.videoOrientation" :aspect="pusher.videoAspect" :device-position="pusher.frontCamera" :remote-mirror="pusher.enableRemoteMirror" :local-mirror="pusher.localMirror" :background-mute="pusher.enableBackgroundMute" :audio-quality="pusher.audioQuality" :audio-volume-type="pusher.audioVolumeType" :audio-reverb-type="pusher.audioReverbType" :waiting-image="pusher.waitingImage" :debug="debug" @statechange="_$self.$parent.$parent[('_pusherStateChangeHandler')]($event)" @netstatus="_$self.$parent.$parent[('_pusherNetStatusHandler')]($event)" @error="_$self.$parent.$parent[('_pusherErrorHandler')]($event)" @bgmstart="_$self.$parent.$parent[('_pusherBGMStartHandler')]($event)" @bgmprogress="_$self.$parent.$parent[('_pusherBGMProgressHandler')]($event)" @bgmcomplete="_$self.$parent.$parent[('_pusherBGMCompleteHandler')]($event)" @audiovolumenotify="_$self.$parent.$parent[('_pusherAudioVolumeNotify')]($event)"></live-pusher>
    </view>
  </view>
</template></uni-shadow-root>
</template>

<script>

global['__wxRoute'] = 'trtc-room/template/custom/custom'

Component({})

export default global['__wxComponents']['trtc-room/template/custom/custom']
</script>
<style platform="mp-weixin">
/* 通过方法自定义模式 */
.template-custom{
  /* 绝对定位模式 pusher 和 player 都用绝对定位*/
  width: 100vw;
  height: 100vh;
  position: relative;
}
.template-custom .pusher-container,
.template-custom .player-container{
  position: absolute;
}
</style>