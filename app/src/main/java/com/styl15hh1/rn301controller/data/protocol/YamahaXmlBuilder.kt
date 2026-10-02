package com.styl15hh1.rn301controller.data.protocol

object YamahaXmlBuilder {
    fun build(command: YamahaCommand): String {
        val body = when (command) {
            is YamahaCommand.NetworkConfig -> "<${command.source.name}><Config>GetParam</Config></${command.source.name}>"
            YamahaCommand.NetRadioList -> "<NET_RADIO><List_Info>GetParam</List_Info></NET_RADIO>"
            YamahaCommand.NetRadioInfo -> "<NET_RADIO><Play_Info>GetParam</Play_Info></NET_RADIO>"
            is YamahaCommand.NetRadioSelect -> {
                require(command.line in 1..8)
                "<NET_RADIO><List_Control><Direct_Sel>Line_${command.line}</Direct_Sel></List_Control></NET_RADIO>"
            }
            YamahaCommand.NetRadioBack -> "<NET_RADIO><List_Control><Cursor>Return</Cursor></List_Control></NET_RADIO>"
            is YamahaCommand.NetRadioPage -> "<NET_RADIO><List_Control><Page>${if (command.next) "Down" else "Up"}</Page></List_Control></NET_RADIO>"
            is YamahaCommand.NetRadioControl -> {
                require(command.action in setOf(com.styl15hh1.rn301controller.data.model.PlayerAction.PLAY, com.styl15hh1.rn301controller.data.model.PlayerAction.STOP))
                "<NET_RADIO><Play_Control><Playback>${command.action.wire}</Playback></Play_Control></NET_RADIO>"
            }
            YamahaCommand.ServerList -> "<SERVER><List_Info>GetParam</List_Info></SERVER>"
            YamahaCommand.ServerInfo -> "<SERVER><Play_Info>GetParam</Play_Info></SERVER>"
            is YamahaCommand.ServerSelect -> {
                require(command.line in 1..8)
                "<SERVER><List_Control><Direct_Sel>Line_${command.line}</Direct_Sel></List_Control></SERVER>"
            }
            YamahaCommand.ServerBack -> "<SERVER><List_Control><Cursor>Return</Cursor></List_Control></SERVER>"
            is YamahaCommand.ServerPage -> "<SERVER><List_Control><Page>${if (command.next) "Down" else "Up"}</Page></List_Control></SERVER>"
            is YamahaCommand.ServerControl -> "<SERVER><Play_Control><Playback>${command.action.wire}</Playback></Play_Control></SERVER>"
            YamahaCommand.TunerConfig -> "<Tuner><Config>GetParam</Config></Tuner>"
            YamahaCommand.SpotifyInfo -> "<Spotify><Play_Info>GetParam</Play_Info></Spotify>"
            is YamahaCommand.SpotifyControl -> {
                require(command.action in com.styl15hh1.rn301controller.data.player.PlayerSources.controls("Spotify"))
                "<Spotify><Play_Control><Playback>${command.action.wire}</Playback></Play_Control></Spotify>"
            }
            is YamahaCommand.SetBand -> "<Tuner><Play_Control><Tuning><Band>${command.band.name}</Band></Tuning></Play_Control></Tuner>"
            is YamahaCommand.SetFmMode -> "<Tuner><Play_Control><FM_Mode>${command.mode.wire}</FM_Mode></Play_Control></Tuner>"
            is YamahaCommand.TuneAm -> {
                require(command.value in 1..3000)
                "<Tuner><Play_Control><Tuning><Band>AM</Band><Freq><AM><Val>${command.value}</Val><Exp>0</Exp><Unit>kHz</Unit></AM></Freq></Tuning></Play_Control></Tuner>"
            }
            is YamahaCommand.SeekAm -> "<Tuner><Play_Control><Tuning><Freq><AM><Val>${if (command.up) "Auto Up" else "Auto Down"}</Val></AM></Freq></Tuning></Play_Control></Tuner>"
            is YamahaCommand.TuneFm -> {
                require(command.value in 1..20000)
                "<Tuner><Play_Control><Tuning><Band>FM</Band><Freq><FM><Val>${command.value}</Val><Exp>2</Exp><Unit>MHz</Unit></FM></Freq></Tuning></Play_Control></Tuner>"
            }
            is YamahaCommand.SeekFm -> "<Tuner><Play_Control><Tuning><Freq><FM><Val>${if (command.up) "Auto Up" else "Auto Down"}</Val></FM></Freq></Tuning></Play_Control></Tuner>"
            YamahaCommand.Status -> "<Main_Zone><Basic_Status>GetParam</Basic_Status></Main_Zone>"
            YamahaCommand.TunerInfo -> "<Tuner><Play_Info>GetParam</Play_Info></Tuner>"
            YamahaCommand.TunerPresets -> "<Tuner><Play_Control><Preset><Preset_Sel_Item>GetParam</Preset_Sel_Item></Preset></Play_Control></Tuner>"
            is YamahaCommand.SetPreset -> {
                require(command.number in 1..40)
                "<Tuner><Play_Control><Preset><Preset_Sel>${command.number}</Preset_Sel></Preset></Play_Control></Tuner>"
            }
            YamahaCommand.Config -> "<System><Config>GetParam</Config></System>"
            YamahaCommand.Inputs -> "<Main_Zone><Input><Input_Sel_Item>GetParam</Input_Sel_Item></Input></Main_Zone>"
            is YamahaCommand.Power -> "<System><Power_Control><Power>${if (command.on) "On" else "Standby"}</Power></Power_Control></System>"
            is YamahaCommand.Mute -> "<Main_Zone><Volume><Mute>${if (command.on) "On" else "Off"}</Mute></Volume></Main_Zone>"
            is YamahaCommand.Input -> "<Main_Zone><Input><Input_Sel>${escape(command.id)}</Input_Sel></Input></Main_Zone>"
            is YamahaCommand.SetVolume -> command.volume.let {
                "<Main_Zone><Volume><Lvl><Val>${it.value}</Val><Exp>${it.exponent}</Exp><Unit>${escape(it.unit)}</Unit></Lvl></Volume></Main_Zone>"
            }
        }
        val method = if (command is YamahaCommand.NetworkConfig || command == YamahaCommand.NetRadioList || command == YamahaCommand.NetRadioInfo || command == YamahaCommand.Status || command == YamahaCommand.Config || command == YamahaCommand.Inputs || command == YamahaCommand.TunerInfo || command == YamahaCommand.TunerPresets || command == YamahaCommand.TunerConfig || command == YamahaCommand.SpotifyInfo || command == YamahaCommand.ServerInfo || command == YamahaCommand.ServerList) "GET" else "PUT"
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><YAMAHA_AV cmd=\"$method\">$body</YAMAHA_AV>"
    }
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
