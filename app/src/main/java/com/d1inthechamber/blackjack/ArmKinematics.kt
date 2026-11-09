package com.d1inthechamber.blackjack

import kotlin.math.*

internal data class ArmPoint(val x:Float,val y:Float)
internal data class ArmPose(val elbow:ArmPoint,val wrist:ArmPoint)

/** A two-bone reach: clamp unreachable targets rather than stretching either bone. */
internal fun rigidArmPose(shoulder:ArmPoint,target:ArmPoint,upper:Float,lower:Float):ArmPose {
    require(upper>0f && lower>0f)
    val dx=target.x-shoulder.x;val dy=target.y-shoulder.y
    val distance=hypot(dx,dy)
    val reach=distance.coerceIn(abs(upper-lower)+.001f,upper+lower-.001f)
    val angle=if(distance<.001f) (PI/2).toFloat() else atan2(dy,dx)
    val bend=acos(((upper*upper+reach*reach-lower*lower)/(2*upper*reach)).coerceIn(-1f,1f))
    return ArmPose(ArmPoint(shoulder.x+cos(angle+bend)*upper,shoulder.y+sin(angle+bend)*upper),
        ArmPoint(shoulder.x+cos(angle)*reach,shoulder.y+sin(angle)*reach))
}
