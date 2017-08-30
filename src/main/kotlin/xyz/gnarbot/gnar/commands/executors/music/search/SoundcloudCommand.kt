package xyz.gnarbot.gnar.commands.executors.music.search

import com.jagrosh.jdautilities.selector
import xyz.gnarbot.gnar.Bot
import xyz.gnarbot.gnar.commands.Category
import xyz.gnarbot.gnar.commands.Command
import xyz.gnarbot.gnar.commands.Scope
import xyz.gnarbot.gnar.commands.executors.music.embedTitle
import xyz.gnarbot.gnar.music.MusicLimitException
import xyz.gnarbot.gnar.music.MusicManager
import xyz.gnarbot.gnar.music.TrackContext
import xyz.gnarbot.gnar.utils.Context
import xyz.gnarbot.gnar.utils.Utils
import java.awt.Color

@Command(
        id = 63,
        aliases = arrayOf("soundcloud", "sc"),
        usage = "(query...)",
        description = "Search and see SoundCloud results.",
        scope = Scope.TEXT,
        category = Category.MUSIC
)
class SoundcloudCommand : xyz.gnarbot.gnar.commands.CommandExecutor() {
    override fun execute(context: Context, label: String, args: Array<String>) {
        if (!Bot.CONFIG.searchEnabled) {
            context.send().error("Search is currently disabled. Try direct links instead.").queue()
            return
        }

        if (args.isEmpty()) {
            context.send().error("Input a query to search Soundcloud.").queue()
            return
        }

        val query = args.joinToString(" ")

        MusicManager.search("scsearch:$query", 5) { results ->
            if (results.isEmpty()) {
                context.send().error("No search results for `$query`.").queue()
                return@search
            }

            val botChannel = context.guild.selfMember.voiceState.channel
            val userChannel = context.voiceChannel

            if (!Bot.CONFIG.musicEnabled || userChannel == null || botChannel != null && botChannel != userChannel) {
                context.send().embed {
                    setAuthor("SoundCloud Results", "https://soundcloud.com", "https://soundcloud.com/favicon.ico")
                    thumbnail { "https://gnarbot.xyz/assets/img/soundcloud.png" }
                    color { Color(255, 110, 0) }

                    desc {
                        buildString {
                            for (result in results) {

                                val title = result.info.embedTitle
                                val url = result.info.uri
                                val length = Utils.getTimestamp(result.duration)
                                val author = result.info.author

                                append("**[$title]($url)**\n")
                                append("**`").append(length).append("`** by **").append(author).append("**\n")
                            }
                        }
                    }

                    setFooter("Want to play one of these music tracks? Join a voice channel and reenter this command.", null)
                }.action().queue()
                return@search
            } else {
                Bot.getWaiter().selector {
                    title { "SoundCloud Results" }
                    desc  { "Select one of the following options to play them in your current music channel." }
                    color { Color(255, 110, 0) }

                    setUser(context.user)

                    for (result in results) {
                        addOption("`${Utils.getTimestamp(result.info.length)}` **[${result.info.embedTitle}](${result.info.uri})**") {
                            if (context.member.voiceState.inVoiceChannel()) {
                                val manager = try {
                                    Bot.getPlayers().get(context.guild)
                                } catch (e: MusicLimitException) {
                                    e.sendToContext(context)
                                    return@addOption
                                }

                                manager.loadAndPlay(
                                        context,
                                        result.info.uri,
                                        TrackContext(
                                                context.member.user.idLong,
                                                context.channel.idLong
                                        )
                                )
                            } else {
                                context.send().error("You're not in a voice channel anymore!").queue()
                            }
                        }
                    }
                }.display(context.channel)
            }
        }
    }
}



