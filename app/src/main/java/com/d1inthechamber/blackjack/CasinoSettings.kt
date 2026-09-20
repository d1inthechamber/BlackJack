package com.d1inthechamber.blackjack

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Stable
internal class CasinoSettings(context:Context?=null) {
    private val prefs=context?.getSharedPreferences("audio",Context.MODE_PRIVATE)
    var music by mutableStateOf(prefs?.getBoolean("music",true)?:true);private set
    var effects by mutableStateOf(prefs?.getBoolean("effects",true)?:true);private set
    var voices by mutableStateOf(prefs?.getBoolean("voices",true)?:true);private set
    var ambience by mutableStateOf(prefs?.getBoolean("ambience",true)?:true);private set
    fun set(key:String,value:Boolean){when(key){"music"->music=value;"effects"->effects=value;"voices"->voices=value;"ambience"->ambience=value;else->return};prefs?.edit()?.putBoolean(key,value)?.apply()}
}
internal val LocalCasinoSettings=staticCompositionLocalOf{CasinoSettings()}

@Composable
internal fun SettingsPage(model:BlackjackViewModel,onBack:()->Unit){
    val settings=model.settings
    CasinoFrame(model,"SETTINGS",onBack){
        Text("AUDIO & ATMOSPHERE",color=model.room.accent,fontSize=20.sp,modifier=Modifier.testTag("settings-content"))
        Text("Every switch uses a high-contrast label and saves immediately.",color=Color(0xFFE7E2EA),fontSize=13.sp)
        listOf(Triple("music","BACKGROUND MUSIC",settings.music),Triple("effects","CARD & CHIP SOUNDS",settings.effects),Triple("voices","DEALER VOICES",settings.voices),Triple("ambience","AMBIENCE ANIMATION",settings.ambience)).forEach{(key,title,value)->
            Surface(color=Color(0xFF17131B),contentColor=Color.White,shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,model.room.accent.copy(alpha=.55f)),modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontSize=14.sp,color=Color.White);Switch(checked=value,onCheckedChange={settings.set(key,it)},modifier=Modifier.semanticsLabel(title))}
            }
        }
        Text("Dealing motion stays on so every card remains easy to follow. Choose Rooms from the main navigation to change the cast and table.",color=Color(0xFFE7E2EA),fontSize=13.sp)
    }
}

@Composable
internal fun RoomsPage(model:BlackjackViewModel,onBack:()->Unit){
    CasinoFrame(model,"ROOMS",onBack){
        Text("CHOOSE YOUR ROOM",color=model.room.accent,fontSize=20.sp,modifier=Modifier.padding(top=12.dp))
        Text("Changes the original character, room, cards, border and music. Every game stays saved.",color=Color(0xFFE7E2EA),fontSize=13.sp,modifier=Modifier.testTag("rooms-content"))
        RoomStyle.entries.forEach{room->
            Card(onClick={model.selectRoom(room)},border=BorderStroke(if(room==model.room)3.dp else 1.dp,room.accent),colors=CardDefaults.cardColors(containerColor=Color(0xFF17131B)),modifier=Modifier.fillMaxWidth()){
                Box(Modifier.fillMaxWidth().height(120.dp)){
                    Image(painterResource(room.background),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.58f)))
                    Column(Modifier.align(Alignment.CenterStart).padding(16.dp)){
                        Text(room.title+if(room==model.room)" ✓" else "",color=room.accent,fontSize=22.sp)
                        Text(room.host,color=Color.White,fontSize=14.sp)
                    }
                    room.cardBack?.let{Image(painterResource(it),null,Modifier.align(Alignment.CenterEnd).padding(12.dp).size(48.dp,72.dp))}
                }
            }
        }
    }
}
private fun Modifier.semanticsLabel(label:String)=this.then(Modifier.semantics { contentDescription=label })

/** Narrow decorative frames sit outside the controls and never intercept touch. */
@Composable
internal fun RoomBorder(room:RoomStyle,modifier:Modifier=Modifier){
    Canvas(modifier.fillMaxSize()){
        val edge=5.dp.toPx();val ink=room.accent.copy(alpha=.72f)
        drawRoundRect(ink,Offset(edge,edge),androidx.compose.ui.geometry.Size(size.width-2*edge,size.height-2*edge),androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),style=Stroke(1.5.dp.toPx()))
        for(right in listOf(false,true)){
            val x=if(right)size.width-edge else edge
            repeat((size.height/52.dp.toPx()).toInt()){i->val y=24.dp.toPx()+i*52.dp.toPx()
                when(room){
                    RoomStyle.VEGAS->{drawCircle(ink,2.dp.toPx(),Offset(x,y));drawCircle(Color(0xFFFADE9A).copy(alpha=.12f),5.dp.toPx(),Offset(x,y))}
                    RoomStyle.CARNIVAL->{val p=Path().apply{moveTo(x,y-5.dp.toPx());lineTo(x+4.dp.toPx(),y);lineTo(x,y+5.dp.toPx());lineTo(x-4.dp.toPx(),y);close()};drawPath(p,if(i%2==0)ink else Color(0xFFB82755))}
                    RoomStyle.EGYPT->{val d=4.dp.toPx();drawLine(ink,Offset(x-d,y-d),Offset(x+d,y-d),2f);drawLine(ink,Offset(x+d,y-d),Offset(x+d,y+d),2f);drawLine(ink,Offset(x+d,y+d),Offset(x-d,y+d),2f)}
                    RoomStyle.IRON->{drawCircle(Color(0xFF493326),4.dp.toPx(),Offset(x,y));drawCircle(ink,3.dp.toPx(),Offset(x,y),style=Stroke(1.dp.toPx()));drawLine(ink,Offset(x-2.dp.toPx(),y),Offset(x+2.dp.toPx(),y),1f)}
                    RoomStyle.WEST->{drawLine(if(i%2==0)ink else Color(0xFF744BA9),Offset(x,y-12.dp.toPx()),Offset(x,y+12.dp.toPx()),3.dp.toPx())}
                    RoomStyle.GREEN->{drawOval(ink,Offset(x-3.dp.toPx(),y-6.dp.toPx()),androidx.compose.ui.geometry.Size(6.dp.toPx(),12.dp.toPx()));drawLine(Color(0xFF253D22),Offset(x,y-4.dp.toPx()),Offset(x,y+4.dp.toPx()),1f)}
                    RoomStyle.PUNK->{val p=Path().apply{moveTo(x-3.dp.toPx(),y-7.dp.toPx());lineTo(x+3.dp.toPx(),y);lineTo(x-2.dp.toPx(),y);lineTo(x+3.dp.toPx(),y+7.dp.toPx())};drawPath(p,if(i%2==0)Color(0xFFEE3FA3) else ink,style=Stroke(2.dp.toPx()))}
                }
            }
        }
        val corner=25.dp.toPx()
        for(x in listOf(edge,size.width-edge))for(y in listOf(edge,size.height-edge)){
            val dx=if(x<size.width/2)1 else -1;val dy=if(y<size.height/2)1 else -1
            drawLine(room.accent,Offset(x,y+dy*corner),Offset(x,y),3.dp.toPx());drawLine(room.accent,Offset(x,y),Offset(x+dx*corner,y),3.dp.toPx())
        }
    }
}
