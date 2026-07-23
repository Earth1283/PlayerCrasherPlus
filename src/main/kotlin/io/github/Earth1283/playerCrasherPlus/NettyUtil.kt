package io.github.earth1283.playerCrasherPlus

import io.netty.channel.Channel
import org.bukkit.entity.Player
import java.lang.reflect.Field

object NettyUtil {

    fun resolveChannel(player: Player): Channel? {
        return try {
            val craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
            val serverPlayer = craftPlayerClass.getMethod("getHandle").invoke(craftPlayerClass.cast(player))
            val packetListener = findFieldByName(serverPlayer, "connection") ?: return null
            val connection = findFieldByName(packetListener, "connection") ?: return null
            findFieldByType(connection, Channel::class.java) as? Channel
        } catch (e: Exception) {
            null
        }
    }

    private fun findFieldByName(obj: Any, name: String): Any? {
        var cls: Class<*>? = obj.javaClass
        while (cls != null) {
            val field: Field = try {
                cls.getDeclaredField(name)
            } catch (e: NoSuchFieldException) {
                cls = cls.superclass
                continue
            }
            field.isAccessible = true
            return field.get(obj)
        }
        return null
    }

    private fun findFieldByType(obj: Any, type: Class<*>): Any? {
        var cls: Class<*>? = obj.javaClass
        while (cls != null) {
            for (field in cls.declaredFields) {
                if (type.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    return field.get(obj)
                }
            }
            cls = cls.superclass
        }
        return null
    }
}
