package xyz.gnarbot.gnar.commands.executors.music

import xyz.gnarbot.gnar.Bot
import xyz.gnarbot.gnar.commands.Category
import xyz.gnarbot.gnar.commands.Command
import xyz.gnarbot.gnar.commands.CommandExecutor
import xyz.gnarbot.gnar.music.MusicManager
import xyz.gnarbot.gnar.utils.Context

@Command(
        aliases = arrayOf("play"),
        usage = "-(url|YT search)",
        description = "Joins and play music in a channel.",
        category = Category.MUSIC
)
class PlayCommand : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        val manager = context.guildData.musicManager
        
        val botChannel = context.guild.selfMember.voiceState.channel
        val userChannel = context.guild.getMember(context.message.author).voiceState.channel

        if (botChannel != null && botChannel != userChannel) {
            context.send().error("The bot is already playing music in another channel.").queue()
            return
        }

        if (userChannel == null) {
            context.send().error("You must be in a voice channel to play music.").queue()
            return
        }

        if (args.isEmpty()) {
            if (manager.player.isPaused) {
                manager.player.isPaused = false
                context.send().embed("Play Music") {
                    setColor(Bot.CONFIG.musicColor)
                    setDescription("Music is now playing.")
                }.action().queue()
            } else if (manager.player.playingTrack != null) {
                context.send().error("Music is already playing.").queue()
            } else if (manager.scheduler.queue.isEmpty()) {
                context.send().embed("Empty Queue") {
                    setColor(Bot.CONFIG.musicColor)
                    setDescription("There is no music queued right now. Add some songs with `play -song|url`.")
                }.action().queue()
            }
            return
        }

        if ("https://" in args[0] || "http://" in args[0]) {
            manager.loadAndPlay(context, args[0])
        } else {
            val query = args.joinToString(" ").trim()

            if (query.startsWith("scsearch:", ignoreCase = true)
                    || query.startsWith("ytsearch:", ignoreCase = true)) {
                manager.loadAndPlay(context, query)
                return
            } else if (query.startsWith("youtube ", ignoreCase = true)) {
                manager.loadAndPlay(context, query.replaceFirst("youtube ", "ytsearch:", ignoreCase = true))
                return
            } else if (query.startsWith("soundcloud ", ignoreCase = true)) {
                manager.loadAndPlay(context, query.replaceFirst("soundcloud ", "scsearch:", ignoreCase = true))
                return
            }

            MusicManager.search("ytsearch:$query", 1) { results ->
                if (results.isEmpty()) {
                    context.send().error("No YouTube results returned for `${query.replace('+', ' ')}`.").queue()
                    return@search
                }

                val result = results[0]

                manager.loadAndPlay(context, result.info.uri)
            }
        }
    }
}
