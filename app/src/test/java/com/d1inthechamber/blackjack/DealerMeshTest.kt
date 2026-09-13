package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class DealerMeshTest {
    @Test fun reliefHasFiniteDepthNormalsAndValidSkinningWeights() {
        val data=DealerMesh.build()
        var minDepth=Float.MAX_VALUE;var maxDepth=-Float.MAX_VALUE
        for(i in data.indices step DealerMesh.STRIDE) {
            for(j in 0 until DealerMesh.STRIDE) assertTrue(data[i+j].isFinite())
            assertTrue(data[i+3] in 0f..1f);assertTrue(data[i+4] in 0f..1f)
            val length=sqrt(data[i+5]*data[i+5]+data[i+6]*data[i+6]+data[i+7]*data[i+7])
            assertEquals(1f,length,.001f)
            val sum=data[i+8]+data[i+9]+data[i+10]
            assertTrue(sum in 0f..1.0001f)
            minDepth=minOf(minDepth,data[i+2]);maxDepth=maxOf(maxDepth,data[i+2])
        }
        assertTrue("Must have sculpted depth, not a flat quad",maxDepth-minDepth>.25f)
    }
}
