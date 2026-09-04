package com.miyako.core

import org.junit.Assert
import org.junit.Test

class DebugerTest {
  @Test
  fun test_debug() {
    val target: String? = null
    var cnt = 0
    target.debug {
      cnt++
    }

    Assert.assertTrue(cnt == 1)

    val target1: String = "123"
    cnt = 0
    target1.debug {
      cnt++
    }
    Assert.assertTrue(cnt == 1)
  }
}
