package com.styl15hh1.rn301controller

import com.styl15hh1.rn301controller.data.model.*
import com.styl15hh1.rn301controller.data.network.*
import com.styl15hh1.rn301controller.data.protocol.*
import kotlinx.coroutines.delay

internal class NetRadioFake : YamahaTransport {
    val commands=mutableListOf<YamahaCommand>()
    val path=mutableListOf<String>()
    val tree=mutableMapOf("" to listOf("Locations" to "Container"),
        "Locations" to listOf("Europe" to "Container"),
        "Locations/Europe" to listOf("Radio" to "Item"))
    var page=1
    var busyReads=0
    var busyStatus="Busy"
    var busyAfterSelection=0
    var menuStatus="Ready"
    var source="NET RADIO"
    var playback="Play"
    var stuck=false
    var fault:ErrorKind?=null
    var listDelay=0L
    var wrongPage=false
    var changeAfterPages=false
    private fun escape(s:String)=s.replace("&","&amp;").replace("<","&lt;")
    override suspend fun resolve(address:ReceiverAddress)="192.168.1.55"
    override suspend fun description(ip:String)="""<Unit_Description Unit_Name="R-N301"/>"""
    override suspend fun command(ip:String,command:YamahaCommand):String {
        commands+=command
        val items=tree[path.joinToString("/")].orEmpty()
        return when(command) {
            YamahaCommand.Config -> configXml
            YamahaCommand.Status -> statusXml(source=source)
            is YamahaCommand.Input -> {source=command.id;ack}
            YamahaCommand.NetRadioList -> {
                fault?.let{throw YamahaException(ReceiverError(it))}
                delay(listDelay)
                radioListXml(layer=path.size+1,current=if(items.isEmpty()) 0 else (page-1)*8+1,max=items.size,
                    title=escape(path.lastOrNull()?:"Internet Radio"),
                    status=if(busyReads-- > 0) busyStatus else menuStatus,
                    entries=items.drop((page-1)*8).take(8).mapIndexed { index,item ->
                        "<Line_${index+1}><Txt>${escape(item.first)}</Txt><Attribute>${item.second}</Attribute></Line_${index+1}>"
                    }.joinToString(""))
            }
            is YamahaCommand.NetRadioSelect -> {
                val item=items[(page-1)*8+command.line-1]
                if(item.second=="Container" && !stuck){path+=item.first;page=1}
                busyReads=busyAfterSelection
                ack
            }
            YamahaCommand.NetRadioBack -> {if(path.isNotEmpty() && !stuck) path.removeAt(path.lastIndex);page=1;ack}
            is YamahaCommand.NetRadioPage -> {
                if(!stuck) page+=if(command.next) (if(wrongPage) 2 else 1) else -1
                if(changeAfterPages) tree[path.joinToString("/")]=items.reversed()
                ack
            }
            YamahaCommand.NetRadioInfo -> radioInfoXml(playback)
            is YamahaCommand.NetRadioControl -> { if(command.action==PlayerAction.STOP) playback="Stop";ack }
            else -> ack
        }
    }
    private val ack="""<YAMAHA_AV rsp="PUT" RC="0"/>"""
}
