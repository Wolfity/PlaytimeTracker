package me.wolfity.playtime.commands

import org.bukkit.Bukkit
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

data class UserCommandParameter(val name: String)

class UserParameterType : ParameterType<BukkitCommandActor, UserCommandParameter> {
    override fun parse(p0: MutableStringStream, p1: ExecutionContext<BukkitCommandActor>): UserCommandParameter {
        return UserCommandParameter(p0.readString())
    }

    override fun defaultSuggestions(): SuggestionProvider<BukkitCommandActor> {
        return SuggestionProvider { _ ->
            Bukkit.getOnlinePlayers().map { it.name }
        }
    }
}