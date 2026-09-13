package com.d1inthechamber.blackjack

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class RoomStyle(val id:String,val title:String,val host:String,val background:Int,val dealer:Int,
    val cardBack:Int?,val accent:Color,val button:Color,val paper:Color) {
    VEGAS("vegas","Old Vegas","The house card sharp",R.drawable.casino_dingy_1970s,R.drawable.dealer_old_vegas,null,Color(0xFFD8BC79),Color(0xFFC79A20),Color(0xFFFFFCF3)),
    CARNIVAL("carnival","Midnight Carnival","The carnival host",R.drawable.room_carnival,R.drawable.dealer_carnival,R.drawable.back_carnival,Color(0xFFE1B659),Color(0xFFBD9041),Color(0xFFFFF5DF)),
    EGYPT("egypt","Pharaoh’s Palace","Tutankhamun",R.drawable.room_egypt,R.drawable.dealer_egypt,R.drawable.back_egypt,Color(0xFFF0CD75),Color(0xFFD0AF52),Color(0xFFFFF6DB)),
    IRON("iron","Ironworks","Ebenezer Scrooge",R.drawable.room_iron,R.drawable.dealer_iron,R.drawable.back_iron,Color(0xFFDEAB81),Color(0xFFC39367),Color(0xFFF4EADB)),
    WEST("west","West Coast ’94","Eazy-E",R.drawable.room_west,R.drawable.dealer_west,R.drawable.back_west,Color(0xFFB5D6CD),Color(0xFF86B8AC),Color(0xFFF1F7F5)),
    PUNK("punk","The Backroom","The punk rocker",R.drawable.room_punk,R.drawable.poses_punk,R.drawable.back_punk,Color(0xFF6EDBE6),Color(0xFF48BDCE),Color(0xFFF6F1E8));
    companion object { fun fromId(id:String?)=entries.firstOrNull { it.id==id } ?: VEGAS }
}
val LocalRoomStyle=staticCompositionLocalOf { RoomStyle.VEGAS }
class RoomPreferences(context:Context) {
    private val prefs=context.getSharedPreferences("room-style",Context.MODE_PRIVATE)
    fun load()=RoomStyle.fromId(prefs.getString("selected",null))
    fun save(room:RoomStyle) { prefs.edit().putString("selected",room.id).apply() }
}
