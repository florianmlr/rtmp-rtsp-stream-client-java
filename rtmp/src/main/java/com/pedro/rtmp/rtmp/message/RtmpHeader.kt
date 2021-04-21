package com.pedro.rtmp.rtmp.message

import android.util.Log
import com.pedro.rtmp.rtmp.chunk.ChunkType
import com.pedro.rtmp.utils.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * Created by pedro on 20/04/21.
 */
class RtmpHeader(var timeStamp: Int = 0, var messageLength: Int = 0, var messageType: MessageType? = null,
                 var messageStreamId: Int = 0, var basicHeader: BasicHeader? = null) {

  companion object {

    private val TAG = "RtmpHeader"

    /**
     * Check ChunkType class to know header structure
     */
    @Throws(IOException::class)
    fun readHeader(input: InputStream): RtmpHeader {
      val basicHeader = BasicHeader.parseBasicHeader(input.read().toByte())
      Log.i(TAG, "$basicHeader")
      var timeStamp = 0
      var messageLength = 0
      var messageType: MessageType? = null
      var messageStreamId = 0
      when (basicHeader.chunkType) {
        ChunkType.TYPE_0 -> {
          timeStamp = input.readUInt24()
          messageLength = input.readUInt24()
          messageType = RtmpMessage.getMarkType(input.read())
          messageStreamId = input.readUInt32LittleEndian()
          //extended timestamp
          if (timeStamp >= 0xffffff) {
            timeStamp = input.readUInt32()
          }
        }
        ChunkType.TYPE_1 -> {
          timeStamp = input.readUInt24()
          messageLength = input.readUInt24()
          messageType = RtmpMessage.getMarkType(input.read())
          //extended timestamp
          if (timeStamp >= 0xffffff) {
            timeStamp = input.readUInt32()
          }
        }
        ChunkType.TYPE_2 -> {
          timeStamp = input.readUInt24()
          //extended timestamp
          if (timeStamp >= 0xffffff) {
            timeStamp = input.readUInt32()
          }
        }
        ChunkType.TYPE_3 -> {
          //extended timestamp
          if (timeStamp >= 0xffffff) {
            timeStamp = input.readUInt32()
          }
          //No header to read
        }
      }
      return RtmpHeader(timeStamp, messageLength, messageType, messageStreamId, basicHeader)
    }
  }

  @Throws(IOException::class)
  fun writeHeader(output: OutputStream) {
    basicHeader?.let {
      writeHeader(it, output)
    }
  }

  /**
   * Check ChunkType class to know header structure
   */
  fun writeHeader(basicHeader: BasicHeader, output: OutputStream) {
    // Write basic header byte
    output.write((basicHeader.chunkType.mark.toInt() shl 6) or basicHeader.chunkStreamId.mark.toInt())
    when (basicHeader.chunkType) {
      ChunkType.TYPE_0 -> {
        output.writeUInt24(min(timeStamp, 0xffffff))
        output.writeUInt24(messageLength)
        messageType?.let { messageType ->
          output.write(messageType.mark.toInt())
        }
        output.writeUInt32LittleEndian(messageStreamId)
        //extended timestamp
        if (timeStamp > 0xffffff) {
          output.writeUInt32(timeStamp)
        }
      }
      ChunkType.TYPE_1 -> {
        output.writeUInt24(min(timeStamp, 0xffffff))
        output.writeUInt24(messageLength)
        messageType?.let { messageType ->
          output.write(messageType.mark.toInt())
        }
        //extended timestamp
        if (timeStamp > 0xffffff) {
          output.writeUInt32(timeStamp)
        }
      }
      ChunkType.TYPE_2 -> {
        output.writeUInt24(min(timeStamp, 0xffffff))
        //extended timestamp
        if (timeStamp > 0xffffff) {
          output.writeUInt32(timeStamp)
        }
      }
      ChunkType.TYPE_3 -> {
        //extended timestamp
        if (timeStamp > 0xffffff) {
          output.writeUInt32(timeStamp)
        }
      }
    }
  }

  fun getPacketLength(): Int = messageLength

  override fun toString(): String {
    return "RtmpHeader(timeStamp=$timeStamp, messageLength=$messageLength, messageType=$messageType, messageStreamId=$messageStreamId, basicHeader=$basicHeader)"
  }
}