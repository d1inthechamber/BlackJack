package com.d1inthechamber.blackjack

import kotlin.math.*

/** Camera-facing sculpted relief, UV-mapped to the original painted dealer.
 * This intentionally preserves the illustration; it is not a 360-degree character.
 * Columns: XYZ, UV, normal XYZ, neck/left-arm/right-arm skin weights.
 */
internal object DealerMesh {
    const val STRIDE = 11
    const val ASPECT = 1230f / 1278f
    fun x(u: Float) = (u - .5f) * 2f * ASPECT
    fun y(v: Float) = 1f - 2f * v
    private fun smooth(a: Float, b: Float, value: Float): Float {
        val t = ((value-a)/(b-a)).coerceIn(0f,1f)
        return t*t*(3f-2f*t)
    }
    private fun mound(u:Float,v:Float,cx:Float,cy:Float,rx:Float,ry:Float):Float {
        val r = ((u-cx)/rx).pow(2) + ((v-cy)/ry).pow(2)
        return exp(-r*1.8f)
    }
    fun depth(u:Float,v:Float):Float =
        .23f*mound(u,v,.56f,.67f,.31f,.47f) +
        .32f*mound(u,v,.455f,.195f,.175f,.215f) +
        .13f*mound(u,v,.425f,.225f,.04f,.05f) + // Nose bridge and tip.
        .05f*mound(u,v,.485f,.27f,.065f,.06f) + // Cheek and smile.
        .14f*mound(u,v,.53f,.355f,.095f,.12f) +
        .33f*mound(u,v,.145f,.735f,.135f,.16f) + // Reaching palm.
        .25f*mound(u,v,.70f,.745f,.15f,.15f) +
        .12f*mound(u,v,.23f,.52f,.13f,.17f) +
        .13f*mound(u,v,.84f,.56f,.14f,.19f)

    fun build(columns:Int=96,rows:Int=100):FloatArray {
        val data=FloatArray(columns*rows*6*STRIDE)
        var at=0
        fun vertex(u:Float,v:Float) {
            val e=.002f
            val dzdx=(depth(u+e,v)-depth(u-e,v))/(4f*e*ASPECT)
            val dzdy=(depth(u,v-e)-depth(u,v+e))/(4f*e)
            val length=sqrt(dzdx*dzdx+dzdy*dzdy+1f)
            val neck=(1f-smooth(.29f,.43f,v))*smooth(.23f,.32f,u)*(1f-smooth(.60f,.67f,u))
            val left=(1f-smooth(.24f,.34f,u))*smooth(.40f,.69f,v)*(1f-neck)
            val right=smooth(.64f,.81f,u)*smooth(.43f,.72f,v)*(1f-neck)
            for(f in floatArrayOf(x(u),y(v),depth(u,v),u,v,-dzdx/length,-dzdy/length,1f/length,neck,left,right)) data[at++]=f
        }
        for(row in 0 until rows) for(col in 0 until columns) {
            val u=col.toFloat()/columns;val v=row.toFloat()/rows
            val u1=(col+1f)/columns;val v1=(row+1f)/rows
            vertex(u,v);vertex(u,v1);vertex(u1,v1)
            vertex(u,v);vertex(u1,v1);vertex(u1,v)
        }
        return data
    }
}
