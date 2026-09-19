package com.d1inthechamber.blackjack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.*

/** Painted reel emblems, shared across large and small reel windows. */
@Composable
internal fun SlotEmblem(n:Int,room:RoomStyle,modifier:Modifier){
    val symbol=slotSymbols(room)[n]
    Box(modifier,contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
            scale(size.width/100f,size.height/100f,pivot=Offset.Zero){
                val ink=Color(0xFF302027);val gold=Color(0xFFE3AF37);val red=Color(0xFFCB3042)
                val green=Color(0xFF448052);val cream=Color(0xFFFFF1C9)
                fun path(points:List<Offset>,color:Color){val p=Path().apply{moveTo(points[0].x,points[0].y);points.drop(1).forEach{lineTo(it.x,it.y)};close()};drawPath(p,color);drawPath(p,ink,style=Stroke(2.5f))}
                fun oval(color:Color,x:Float,y:Float,w:Float,h:Float){drawOval(color,Offset(x,y),Size(w,h));drawOval(ink,Offset(x,y),Size(w,h),style=Stroke(2.5f))}
                fun box(color:Color,x:Float,y:Float,w:Float,h:Float){drawRoundRect(color,Offset(x,y),Size(w,h),CornerRadius(4f));drawRoundRect(ink,Offset(x,y),Size(w,h),CornerRadius(4f),style=Stroke(2.5f))}
                drawOval(Color.Black.copy(alpha=.15f),Offset(17f,83f),Size(67f,8f))
                when(symbol){
                    "CHERRY"->{
                        val stem=Path().apply{moveTo(33f,61f);quadraticBezierTo(39f,23f,65f,15f);quadraticBezierTo(63f,36f,69f,65f)}
                        drawPath(stem,green,style=Stroke(5f,cap=StrokeCap.Round))
                        oval(green,43f,15f,26f,12f);oval(red,13f,51f,39f,37f);oval(red,51f,54f,36f,34f)
                        drawOval(Color(0xFFFFA1A2),Offset(22f,58f),Size(11f,7f));drawOval(Color(0xFFFFA1A2),Offset(60f,61f),Size(9f,6f))
                    }
                    "BAR"->{box(ink,7f,28f,86f,47f);drawLine(gold,Offset(12f,34f),Offset(88f,34f),3f);drawLine(gold,Offset(12f,69f),Offset(88f,69f),3f)}
                    "BELL"->{oval(gold,41f,13f,18f,17f);oval(ink,42f,73f,17f,15f);val p=Path().apply{moveTo(19f,74f);quadraticBezierTo(31f,64f,31f,43f);cubicTo(31f,16f,70f,16f,70f,43f);quadraticBezierTo(70f,64f,82f,74f);close()};drawPath(p,gold);drawPath(p,ink,style=Stroke(3f));drawLine(cream,Offset(41f,38f),Offset(37f,62f),5f,StrokeCap.Round);box(gold,17f,73f,67f,8f)}
                    "STAR","RIOT"->{path((0..9).map{val a=it*PI/5-PI/2;val r=if(it%2==0)40 else 19;Offset(50+r*cos(a).toFloat(),50+r*sin(a).toFloat())},if(symbol=="RIOT")Color(0xFFE54D94) else gold)}
                    "7"->{path(listOf(Offset(18f,15f),Offset(88f,15f),Offset(88f,32f),Offset(53f,86f),Offset(26f,86f),Offset(60f,38f),Offset(18f,38f)),red);drawLine(cream,Offset(25f,22f),Offset(77f,22f),4f)}
                    "MOON"->{oval(gold,19f,12f,65f,72f);drawOval(Color(0xFFFFF0D7),Offset(44f,6f),Size(52f,57f));drawCircle(gold,4f,Offset(80f,23f));drawCircle(gold,3f,Offset(67f,13f))}
                    "SKULL"->{oval(cream,20f,13f,60f,61f);box(cream,33f,57f,34f,29f);oval(ink,29f,35f,15f,18f);oval(ink,56f,35f,15f,18f);path(listOf(Offset(50f,51f),Offset(43f,65f),Offset(57f,65f)),ink);repeat(3){drawLine(ink,Offset(41f+it*9,74f),Offset(41f+it*9,85f),2f)}}
                    "TICKET"->{path(listOf(Offset(14f,21f),Offset(86f,21f),Offset(86f,39f),Offset(77f,50f),Offset(86f,61f),Offset(86f,79f),Offset(14f,79f),Offset(14f,61f),Offset(23f,50f),Offset(14f,39f)),gold);repeat(5){drawLine(red,Offset(34f,30f+it*9),Offset(34f,34f+it*9),2f)};drawCircle(red,15f,Offset(61f,50f),style=Stroke(4f))}
                    "MASK","JOKER"->{
                        if(symbol=="JOKER")path(listOf(Offset(18f,41f),Offset(9f,8f),Offset(40f,24f),Offset(55f,5f),Offset(64f,25f),Offset(91f,13f),Offset(82f,44f)),Color(0xFF8A477D))
                        oval(cream,20f,29f,62f,57f);path(listOf(Offset(30f,38f),Offset(44f,48f),Offset(27f,57f)),red);path(listOf(Offset(69f,38f),Offset(57f,48f),Offset(74f,57f)),red)
                        drawArc(ink,0f,180f,false,Offset(33f,53f),Size(35f,21f),style=Stroke(5f));drawCircle(red,5f,Offset(50f,57f))
                    }
                    "ANKH"->{oval(gold,33f,9f,34f,40f);oval(ink,43f,18f,14f,22f);box(gold,43f,41f,14f,48f);box(gold,23f,49f,54f,12f)}
                    "SCARAB"->{oval(gold,13f,31f,73f,45f);oval(Color(0xFF2E898E),32f,22f,37f,61f);drawLine(ink,Offset(50f,33f),Offset(50f,81f),3f);oval(gold,39f,12f,23f,20f);repeat(3){drawLine(ink,Offset(24f,42f+it*12),Offset(7f,34f+it*20),3f);drawLine(ink,Offset(75f,42f+it*12),Offset(93f,34f+it*20),3f)}}
                    "EYE"->{val eye=Path().apply{moveTo(9f,47f);quadraticBezierTo(50f,13f,91f,47f);quadraticBezierTo(50f,80f,9f,47f);close()};drawPath(eye,cream);drawPath(eye,ink,style=Stroke(4f));oval(Color(0xFF388C95),35f,30f,31f,34f);oval(ink,44f,36f,13f,22f);drawLine(gold,Offset(28f,65f),Offset(41f,88f),5f);drawLine(gold,Offset(71f,65f),Offset(62f,80f),5f)}
                    "COBRA"->{oval(gold,27f,8f,47f,57f);oval(green,41f,17f,20f,39f);val snake=Path().apply{moveTo(50f,43f);cubicTo(50f,66f,23f,55f,22f,74f);cubicTo(22f,91f,77f,89f,78f,70f)};drawPath(snake,ink,style=Stroke(15f));drawPath(snake,green,style=Stroke(10f));drawCircle(red,3f,Offset(47f,28f));drawCircle(red,3f,Offset(57f,28f))}
                    "PHARAOH","CROWN","CHAMP"->{path(listOf(Offset(16f,26f),Offset(34f,43f),Offset(50f,12f),Offset(67f,43f),Offset(85f,25f),Offset(76f,77f),Offset(24f,77f)),gold);box(gold,23f,73f,54f,12f);oval(red,43f,48f,14f,17f);drawCircle(cream,4f,Offset(31f,64f));drawCircle(cream,4f,Offset(69f,64f))}
                    "BOLT"->{path(listOf(Offset(49f,8f),Offset(77f,8f),Offset(58f,39f),Offset(82f,39f),Offset(30f,94f),Offset(42f,58f),Offset(20f,58f)),gold)}
                    "GEAR"->{path((0..31).map{val a=it*PI/16;val r=if(it%4 in 0..1)42 else 32;Offset(50+r*cos(a).toFloat(),50+r*sin(a).toFloat())},Color(0xFFBA8655));oval(ink,29f,29f,42f,42f);oval(gold,37f,37f,26f,26f)}
                    "COAL","AMBER","GOLD"->{path(listOf(Offset(14f,67f),Offset(28f,28f),Offset(58f,16f),Offset(86f,36f),Offset(80f,79f),Offset(42f,87f)),if(symbol=="COAL")Color(0xFF494555) else gold);drawLine(cream.copy(alpha=.55f),Offset(31f,37f),Offset(57f,24f),4f);drawLine(ink.copy(alpha=.4f),Offset(59f,26f),Offset(48f,71f),3f)}
                    "STEAM"->{box(Color(0xFFB28256),17f,53f,66f,31f);repeat(3){val p=Path().apply{moveTo(29f+it*21,47f);cubicTo(12f+it*21,34f,49f+it*21,27f,29f+it*21,12f)};drawPath(p,Color(0xFF818594),style=Stroke(6f,cap=StrokeCap.Round))}}
                    "TAPE","BOOMBOX"->{box(Color(0xFF576475),9f,24f,82f,55f);box(ink,17f,34f,66f,27f);oval(cream,22f,39f,16f,16f);oval(cream,62f,39f,16f,16f);drawLine(gold,Offset(38f,47f),Offset(62f,47f),3f);box(gold,32f,65f,36f,8f);if(symbol=="BOOMBOX")drawLine(ink,Offset(20f,22f),Offset(74f,9f),4f)}
                    "VINYL"->{oval(ink,10f,10f,80f,80f);repeat(3){drawCircle(Color(0xFF61596B),29f-it*5,Offset(50f,50f),style=Stroke(1f))};oval(red,36f,36f,28f,28f);drawCircle(cream,3f,Offset(50f,50f))}
                    "PALM"->{val trunk=Path().apply{moveTo(43f,88f);quadraticBezierTo(66f,63f,52f,29f)};drawPath(trunk,Color(0xFF9A603D),style=Stroke(9f));listOf(Offset(9f,39f),Offset(22f,14f),Offset(52f,6f),Offset(81f,15f),Offset(93f,42f)).forEach{p->val leaf=Path().apply{moveTo(52f,31f);quadraticBezierTo(p.x,12f,p.x,p.y)};drawPath(leaf,green,style=Stroke(11f,cap=StrokeCap.Round))}}
                    "PIN"->{val pin=Path().apply{moveTo(31f,19f);lineTo(77f,68f);cubicTo(98f,92f,64f,106f,50f,81f);lineTo(20f,35f);quadraticBezierTo(12f,15f,31f,19f)};drawPath(pin,ink,style=Stroke(9f));drawPath(pin,Color(0xFFB1B7BD),style=Stroke(5f));box(red,17f,14f,24f,18f)}
                    "BOOT"->{path(listOf(Offset(24f,12f),Offset(63f,12f),Offset(60f,54f),Offset(85f,65f),Offset(89f,81f),Offset(17f,81f),Offset(17f,61f)),ink);repeat(4){drawLine(gold,Offset(36f,24f+it*9),Offset(54f,24f+it*9),3f)};drawLine(Color(0xFF9A7955),Offset(18f,84f),Offset(88f,84f),6f)}
                    "PICK"->{val pick=Path().apply{moveTo(17f,26f);cubicTo(17f,4f,82f,4f,83f,26f);cubicTo(83f,51f,57f,89f,50f,91f);cubicTo(43f,88f,17f,51f,17f,26f)};drawPath(pick,Color(0xFF54BCCA));drawPath(pick,ink,style=Stroke(3f));drawLine(cream,Offset(32f,24f),Offset(66f,24f),4f)}
                    "LEAF","BLOOM"->{repeat(if(symbol=="BLOOM")8 else 7){i->val a=if(symbol=="BLOOM")i*PI/4 else -PI+i*PI/6;val end=Offset(50+40*cos(a).toFloat(),65+48*sin(a).toFloat());val leaf=Path().apply{moveTo(50f,68f);quadraticBezierTo(end.x-10f,end.y+4f,end.x,end.y);quadraticBezierTo(end.x+10f,end.y+12f,50f,68f)};drawPath(leaf,if(symbol=="BLOOM")Color(0xFFBD72A5) else green);drawPath(leaf,ink,style=Stroke(1.5f))};drawLine(green,Offset(50f,64f),Offset(50f,89f),4f)}
                }
            }
        }
        if(symbol=="BAR")Text("BAR",fontSize=18.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFE2A1))
    }
}
