package io.github.earth1283.playerCrasherPlus

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.util.ReferenceCountUtil
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

// when even the kick gets ignored. stop replying to anything the target
// sends, only let keepalives through so the connection just sits there
object GhostHandler {

    private const val HANDLER_NAME = "playercrasherplus_ghost"

    fun ghost(plugin: JavaPlugin, player: Player) {
        val channel = NettyUtil.resolveChannel(player)
        if (channel == null) {
            plugin.logger.warning("Could not reach netty channel for ${player.name}, skipping ghost")
            return
        }

        channel.eventLoop().execute {
            try {
                if (channel.pipeline().get(HANDLER_NAME) == null) {
                    channel.pipeline().addBefore("packet_handler", HANDLER_NAME, GhostDuplexHandler())
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to ghost ${player.name}: ${e.message}")
            }
        }
    }

    private class GhostDuplexHandler : ChannelDuplexHandler() {

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (isKeepAlive(msg)) {
                super.channelRead(ctx, msg)
            } else {
                ReferenceCountUtil.release(msg)
            }
        }

        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            if (isKeepAlive(msg)) {
                super.write(ctx, msg, promise)
            } else {
                ReferenceCountUtil.release(msg)
                promise.setSuccess()
            }
        }

        private fun isKeepAlive(msg: Any): Boolean {
            return msg.javaClass.simpleName.contains("KeepAlive", ignoreCase = true)
        }
    }
}
