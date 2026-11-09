package com.d1inthechamber.blackjack

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class ArmKinematicsTest {
    @Test fun reachAndRetractionNeverStretchBones() {
        val shoulder=ArmPoint(32f,50f)
        for(i in 0..200) {
            val target=ArmPoint(-50f+i*2f,-30f+i*4f)
            val pose=rigidArmPose(shoulder,target,120f,110f)
            assertEquals(120f,hypot(pose.elbow.x-shoulder.x,pose.elbow.y-shoulder.y),.01f)
            assertEquals(110f,hypot(pose.wrist.x-pose.elbow.x,pose.wrist.y-pose.elbow.y),.01f)
        }
    }
    @Test fun reachableWristMeetsTheTarget() {
        val target=ArmPoint(120f,160f)
        val pose=rigidArmPose(ArmPoint(0f,0f),target,120f,110f)
        assertEquals(target.x,pose.wrist.x,.01f)
        assertEquals(target.y,pose.wrist.y,.01f)
    }
    @Test fun targetAtShoulderRemainsFinite() {
        val pose=rigidArmPose(ArmPoint(0f,0f),ArmPoint(0f,0f),100f,100f)
        assertTrue(pose.elbow.x.isFinite()&&pose.elbow.y.isFinite())
        assertTrue(pose.wrist.x.isFinite()&&pose.wrist.y.isFinite())
    }
}
