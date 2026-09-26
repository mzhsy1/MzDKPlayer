package org.mz.mzdkplayer.tool//import java.io.InputStream
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
//data class FlacHeader(
//    val signature: String,
//    val blockSizeMin: Int,
//    val blockSizeMax: Int,
//    val frameSizeMin: Int,
//    val frameSizeMax: Int,
//    val sampleRate: Int,
//    val channels: Int,
//    val bitsPerSample: Int,
//    val totalSamples: Long,
//    val md5: ByteArray,
//    val fileSize: Long? = null // 文件大小用于计算实际比特率
//) {
//    // 计算原始未压缩音频的比特率
//    val rawBitrate: Long
//        get() = (sampleRate.toLong() * channels * bitsPerSample).toLong()
//
//    // 如果提供了文件大小，可以计算实际的平均比特率
//    val actualBitrate: Double?
//        get() = if (fileSize != null && totalSamples > 0) {
//            val durationSeconds = totalSamples.toDouble() / sampleRate
//            val actualBitrate = (fileSize * 8.0) / durationSeconds
//            actualBitrate
//        } else null
//}
//
//fun readFlacHeader(inputStream: InputStream, fileSize: Long? = null): FlacHeader {
//    val buffer = ByteArray(512)
//    val bytesRead = inputStream.read(buffer)
//
//    if (bytesRead < 42) {
//        throw IllegalArgumentException("Input stream is too short to contain a valid FLAC header")
//    }
//
//    val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)
//
//    // Check FLAC signature
//    val signatureBytes = ByteArray(4)
//    byteBuffer.get(signatureBytes)
//    val signature = String(signatureBytes)
//
//    if (signature != "fLaC") {
//        throw IllegalArgumentException("Invalid FLAC signature: $signature")
//    }
//
//    // Metadata block header (1st block - STREAMINFO)
//    val firstBlockHeader = byteBuffer.int
//    val blockType = (firstBlockHeader and 0x7F000000) ushr 24
//    val blockSize = firstBlockHeader and 0x00FFFFFF
//
//    if (blockType != 0) { // 0 is STREAMINFO
//        throw IllegalArgumentException("First block is not STREAMINFO (type: $blockType)")
//    }
//
//    if (blockSize < 34) {
//        throw IllegalArgumentException("STREAMINFO block is too small: $blockSize")
//    }
//
//    // Parse STREAMINFO
//    val blockSizeMin = byteBuffer.short.toInt() and 0xFFFF
//    val blockSizeMax = byteBuffer.short.toInt() and 0xFFFF
//    val frameSizeMin = byteBuffer.int
//    val frameSizeMax = byteBuffer.int
//
//    val sampleRateField = byteBuffer.int
//    val channelField = byteBuffer.int
//
//    // Extract sample rate from first 20 bits of sampleRateField
//    val sampleRate = (sampleRateField ushr 12) and 0xFFFFF
//
//    // Extract channels (next 3 bits) - add 1 as it's stored as (channels-1)
//    val channels = ((sampleRateField and 0x00000E00) ushr 9) + 1
//
//    // Extract bits per sample (next 5 bits) - add 1 as it's stored as (bits-1)
//    val bitsPerSample = ((sampleRateField and 0x000001F0) ushr 4) + 1
//
//    // Extract upper 4 bits of total samples from sampleRateField
//    val totalSamplesUpper = (sampleRateField and 0x0000000F).toLong()
//
//    // Extract lower 32 bits of total samples from channelField
//    val totalSamplesLower = (channelField.toLong() and 0xFFFFFFFFL)
//
//    // Combine upper 4 bits and lower 32 bits to get 36-bit total samples
//    val totalSamples = (totalSamplesUpper shl 32) or totalSamplesLower
//
//    // Read MD5 signature (16 bytes)
//    val md5 = ByteArray(16)
//    byteBuffer.get(md5)
//
//    return FlacHeader(
//        signature = signature,
//        blockSizeMin = blockSizeMin,
//        blockSizeMax = blockSizeMax,
//        frameSizeMin = frameSizeMin,
//        frameSizeMax = frameSizeMax,
//        sampleRate = sampleRate,
//        channels = channels,
//        bitsPerSample = bitsPerSample,
//        totalSamples = totalSamples,
//        md5 = md5,
//        fileSize = fileSize
//    )
//}
//
//// Example usage
//fun main() {
//    val flacFile = java.io.File("example.flac") // Replace with actual file path
//    if (flacFile.exists()) {
//        try {
//            val fileInputStream = flacFile.inputStream()
//            val header = readFlacHeader(fileInputStream, flacFile.length())
//
//            println("FLAC Header Information:")
//            println("Signature: ${header.signature}")
//            println("Sample Rate: ${header.sampleRate} Hz")
//            println("Channels: ${header.channels}")
//            println("Bits per Sample: ${header.bitsPerSample}")
//            println("Total Samples: ${header.totalSamples}")
//
//            // 计算各种比特率
//            println("Raw Bitrate: ${header.rawBitrate / 1000} kbps") // 未压缩比特率
//            println("Raw Bitrate: ${header.rawBitrate} bps")
//
//            if (header.actualBitrate != null) {
//                println("Actual Average Bitrate: ${String.format("%.2f", header.actualBitrate!! / 1000)} kbps")
//                println("Actual Average Bitrate: ${String.format("%.2f", header.actualBitrate)} bps")
//            }
//
//            val compressionRatio = if (header.actualBitrate != null) {
//                header.rawBitrate.toDouble() / header.actualBitrate!!
//            } else null
//
//            if (compressionRatio != null) {
//                println("Compression Ratio: ${String.format("%.2f", compressionRatio)}:1")
//            }
//
//            val duration = header.totalSamples.toDouble() / header.sampleRate
//            println("Duration: ${String.format("%.2f", duration)} seconds")
//
//        } catch (e: Exception) {
//            println("Error reading FLAC header: ${e.message}")
//        }
//    } else {
//        println("FLAC file not found")
//    }
//}
//
//
//
