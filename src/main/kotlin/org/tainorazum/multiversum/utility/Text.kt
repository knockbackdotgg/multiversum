package org.tainorazum.multiversum.utility

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

object Text {

    fun NEXT_LINE() = Component.text("\n")

    fun toSmallText(text: String): String {
        val alphabetShort = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ⁰¹²³⁴⁵⁶⁷⁸⁹"
        val alphabetNormal = "abcdefghijklmnopqrstuvwxyz0123456789"
        val normalizedText = text.lowercase()

        return normalizedText.map { ch ->
            val lower = ch.lowercaseChar()
            val index = alphabetNormal.indexOf(lower)
            if (index != -1) alphabetShort[index] else ch
        }.joinToString("")
    }

    fun toSmallTextComponent(text: String, color: TextColor): Component {
        val alphabetShort = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ"
        val alphabetNormal = "abcdefghijklmnopqrstuvwxyz"
        val normalizedText = text.lowercase()

        return Component.text(
            normalizedText.map { ch ->
                val lower = ch.lowercaseChar()
                val index = alphabetNormal.indexOf(lower)
                if (index != -1) alphabetShort[index] else ch
            }.joinToString(""), color
        )


    }

}