package com.thinkerror.xiaozhi

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 9兼容音频播放器
 * 使用AudioTrack和MediaPlayer提供基础音频播放功能
 */
class AudioPlayerCompat private constructor(
    private val context: Context
) : MethodCallHandler {

    companion object {
        private const val TAG = "AudioPlayerCompat"
        private const val CHANNEL_NAME = "xiaozhi/audio_player"
        
        @JvmStatic
        fun registerWith(flutterEngine: FlutterEngine, context: Context) {
            val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            channel.setMethodCallHandler(AudioPlayerCompat(context))
        }
    }

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var sampleRate = 16000
    private var channels = 1
    private var bufferSize = 1024

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> {
                initialize()
                result.success(null)
            }
            "start" -> {
                val args = call.arguments as Map<String, Any>
                sampleRate = (args["sampleRate"] as Int).toInt()
                channels = (args["channels"] as Int).toInt()
                bufferSize = (args["bufferSize"] as Int).toInt()
                
                startPlayback()
                result.success(null)
            }
            "writeAudio" -> {
                val args = call.arguments as Map<String, Any>
                val data = args["data"] as ByteArray
                
                writeAudioData(data)
                result.success(null)
            }
            "playFile" -> {
                val args = call.arguments as Map<String, Any>
                val path = args["path"] as String
                
                playFile(path)
                result.success(null)
            }
            "isPlaying" -> {
                result.success(isPlaying)
            }
            "stop" -> {
                stopPlayback()
                result.success(null)
            }
            "release" -> {
                release()
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    /**
     * 初始化音频播放器
     */
    private fun initialize() {
        Log.d(TAG, "Initializing AudioPlayerCompat for Android ${Build.VERSION.RELEASE}")
        
        try {
            // 优先尝试使用AudioTrack进行低延迟播放
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM
            )
            
            Log.d(TAG, "AudioTrack initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}")
            audioTrack = null
        }
    }

    /**
     * 开始播放
     */
    private fun startPlayback() {
        Log.d(TAG, "Starting playback: sampleRate=$sampleRate, channels=$channels")
        
        audioTrack?.let { track ->
            try {
                track.play()
                isPlaying = true
                Log.d(TAG, "AudioTrack playback started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioTrack: ${e.message}")
            }
        }
    }

    /**
     * 写入音频数据
     */
    private fun writeAudioData(data: ByteArray) {
        audioTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    // 将字节数据转换为16位PCM
                    val shortBuffer = ByteBuffer.wrap(data)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                    
                    val shorts = ShortArray(data.size / 2)
                    shortBuffer.get(shorts)
                    
                    val bytesWritten = track.write(shorts, 0, shorts.size)
                    if (bytesWritten < 0) {
                        Log.w(TAG, "AudioTrack write error: $bytesWritten")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing audio data: ${e.message}")
                }
            }
        }
    }

    /**
     * 播放文件（回退方案）
     */
    private fun playFile(filePath: String) {
        Log.d(TAG, "Playing file: $filePath")
        
        try {
            releaseMediaPlayer()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    Log.d(TAG, "File playback completed")
                    this@AudioPlayerCompat.isPlaying = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    this@AudioPlayerCompat.isPlaying = false
                    true
                }
                prepare()
                start()
            }
            this@AudioPlayerCompat.isPlaying = true
            
            Log.d(TAG, "File playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play file: ${e.message}")
            isPlaying = false
        }
    }

    /**
     * 停止播放
     */
    private fun stopPlayback() {
        Log.d(TAG, "Stopping playback")
        
        audioTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    track.pause()
                    track.flush()
                    Log.d(TAG, "AudioTrack stopped")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioTrack: ${e.message}")
                }
            }
        }
        
        releaseMediaPlayer()
        isPlaying = false
    }

    /**
     * 释放MediaPlayer资源
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d(TAG, "MediaPlayer released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
            } finally {
                mediaPlayer = null
            }
        }
    }

    /**
     * 释放所有资源
     */
    private fun release() {
        Log.d(TAG, "Releasing AudioPlayerCompat")
        
        stopPlayback()
        
        audioTrack?.let { track ->
            try {
                track.release()
                Log.d(TAG, "AudioTrack released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioTrack: ${e.message}")
            } finally {
                audioTrack = null
            }
        }
        
        isPlaying = false
    }
}