<template>
<uni-shadow-root class="trtc-room-template-1v1-1v1"><template v-if="wxTemplateName === '1v1'">
  <view class="template-1v1">
    <view v-for="(item,index) in (streamList)" :key="item.streamID" v-if="item.src && (item.hasVideo || item.hasAudio)" :class="'view-container player-container '+(item.isVisible?'':'none')">
      <live-player class="player" :id="item.streamID" :data-userid="item.userID" :data-streamid="item.streamID" :data-streamtype="item.streamType" :src="item.src" mode="RTC" :autoplay="item.autoplay" :mute-audio="item.muteAudio" :mute-video="item.muteVideo" :orientation="item.orientation" :object-fit="item.objectFit" :background-mute="item.enableBackgroundMute" :min-cache="item.minCache" :max-cache="item.maxCache" :sound-mode="item.soundMode" :enable-recv-message="item.enableRecvMessage" :auto-pause-if-navigate="item.autoPauseIfNavigate" :auto-pause-if-open-native="item.autoPauseIfOpenNative" :debug="debug" @statechange="_$self.$parent.$parent[('_playerStateChange')]($event)" @fullscreenchange="_$self.$parent.$parent[('_playerFullscreenChange')]($event)" @netstatus="_$self.$parent.$parent[('_playerNetStatus')]($event)" @audiovolumenotify="_$self.$parent.$parent[('_playerAudioVolumeNotify')]($event)"></live-player>
    </view>
    <view :class="'view-container pusher-container '+(pusher.isVisible?'':'none')+' '+(streamList.length===0? 'fullscreen':'')">
      <live-pusher class="pusher" :url="pusher.url" :mode="pusher.mode" :autopush="pusher.autopush" :enable-camera="pusher.enableCamera" :enable-mic="pusher.enableMic" :muted="(!pusher.enableMic)" :enable-agc="pusher.enableAgc" :enable-ans="pusher.enableAns" :enable-ear-monitor="pusher.enableEarMonitor" :auto-focus="pusher.enableAutoFocus" :zoom="pusher.enableZoom" :min-bitrate="pusher.minBitrate" :max-bitrate="pusher.maxBitrate" :video-width="pusher.videoWidth" :video-height="pusher.videoHeight" :beauty="pusher.beautyLevel" :whiteness="pusher.whitenessLevel" :orientation="pusher.videoOrientation" :aspect="pusher.videoAspect" :device-position="pusher.frontCamera" :remote-mirror="pusher.enableRemoteMirror" :local-mirror="pusher.localMirror" :background-mute="pusher.enableBackgroundMute" :audio-quality="pusher.audioQuality" :audio-volume-type="pusher.audioVolumeType" :audio-reverb-type="pusher.audioReverbType" :waiting-image="pusher.waitingImage" :debug="debug" @statechange="_$self.$parent.$parent[('_pusherStateChangeHandler')]($event)" @netstatus="_$self.$parent.$parent[('_pusherNetStatusHandler')]($event)" @error="_$self.$parent.$parent[('_pusherErrorHandler')]($event)" @bgmstart="_$self.$parent.$parent[('_pusherBGMStartHandler')]($event)" @bgmprogress="_$self.$parent.$parent[('_pusherBGMProgressHandler')]($event)" @bgmcomplete="_$self.$parent.$parent[('_pusherBGMCompleteHandler')]($event)" @audiovolumenotify="_$self.$parent.$parent[('_pusherAudioVolumeNotify')]($event)"></live-pusher>
      <view class="loading" v-if="streamList.length === 0">
        <view class="loading-img">
          <image src="./static/loading.png" class="rotate-img"></image>
        </view>
        <view class="loading-text">等待接听中...</view>
      </view>
    </view>
    <view class="handle-btns">
      <view class="btn-normal" @click="_$self.$parent.$parent[('_toggleAudio')]($event)">
        <image class="btn-image" :src="(pusher.enableMic? './static/audio-true.png': './static/audio-false.png')+' '"></image>
      </view>
      <view class="btn-normal" @click="_$self.$parent.$parent[('switchCamera')]($event)">
        <image class="btn-image" src="./static/switch.png"></image>
      </view>
      
      <view class="btn-normal" @click="_$self.$parent.$parent[('_toggleSoundMode')]($event)">
        <image class="btn-image" :src="(streamList[0].soundMode === 'ear' ? './static/phone.png': './static/speaker-true.png')+' '"></image>
      </view>
    </view>
    <view class="bottom-btns">
      <view class="btn-normal" data-key="beautyLevel" data-value="9|0" data-value-type="number" @click="_$self.$parent.$parent[('_setPuserProperty')]($event)">
        <image class="btn-image" :src="(pusher.beautyLevel == 9 ? './static/beauty-true.png': './static/beauty-false.png')+' '"></image>
      </view>
      <view class="btn-hangup" @click="_$self.$parent.$parent[('_hangUp')]($event)">
        <image class="btn-image" src="./static/hangup.png"></image>
      </view>
      <view class="btn-normal" @click="_$self.$parent.$parent[('_toggleIMPanel')]($event)">
        <image class="btn-image" :src="enableIM? './static/im.png': './static/im-disable.png'"></image>
      </view>
    </view>
  </view>
</template></uni-shadow-root>
</template>

<script>

global['__wxRoute'] = 'trtc-room/template/1v1/1v1'

Component({})

export default global['__wxComponents']['trtc-room/template/1v1/1v1']
</script>
<style platform="mp-weixin">
/* 1v1 视频电话模式 */
.template-1v1{
  width: 100vw;
  height: 100vh;
  position: relative;
}
.template-1v1 .pusher-container{
  width: 240rpx;
  height: 320rpx;
  position: absolute;
  right: 20rpx;
  top: 160rpx;
  z-index: 2;
}

.template-1v1 .pusher-container.fullscreen{
  width: 100vw;
  height: 100vh;
  top: 0;
  right: 0;
}

.template-1v1 .loading {
  position: absolute;
  top: 40vh;
  left: 50vw;
  transform: translate(-50%, 0);
  width: 300rpx;
  height: 250rpx;
  border-radius: 12rpx;
  background: rgba(0,0,0,0.6);
  color: white;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
}
.template-1v1 .loading-img {
  height: 200rpx;
  display:flex;
  justify-content: center;
  align-items: center;
  animation: rotate 2s linear infinite;
}
.template-1v1 .rotate-img {
  width:160rpx;
  height: 160rpx;
}
.template-1v1 .loading-text {
  width: 100%;
  padding-top: 40rpx;
  text-align: center;
}
@keyframes rotate {
  0%{ transform: rotate(0deg);}
  50%{ transform: rotate(180deg);}
  100%{ transform: rotate(360deg);}
}
.template-1v1 .player-container:nth-child(1){
  width: 100vw;
  height: 100vh;
}

.template-1v1 .handle-btns {
  position: absolute;
  z-index: 3;
  bottom: 15vh;
  width: 100vw;
  display: flex;
  flex-direction: row;
  justify-content: space-around;
}

.template-1v1 .bottom-btns {
  position: absolute;
  z-index: 3;
  bottom: 3vh;
  width: 100vw;
  display: flex;
  flex-direction: row;
  justify-content: space-around;
}

/* .template-1v1 image {
  width: 4vh;
  height: 4vh;
} */

.template-1v1 .btn-normal {
  width: 8vh;
  height: 8vh;
  box-sizing: border-box;
  display: flex;
  background: white;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
}
.template-1v1 .btn-hangup .btn-image,
.template-1v1 .btn-normal .btn-image{
  width: 4vh;
  height: 4vh;
}
.template-1v1 .btn-hangup  {
  width: 8vh;
  height: 8vh;
  background: #f75c45;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
}
</style>