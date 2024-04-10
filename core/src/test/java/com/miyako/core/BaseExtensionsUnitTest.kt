package com.miyako.core

import org.junit.Assert
import org.junit.Test

class BaseExtensionsUnitTest {
  @Test
  fun test_init() {
    val obj: MutableList<Int>? = null
    val result = obj.init {
      mutableListOf(1, 2, 3)
    }
    val result1 = result.init {
      emptyList()
    }
    Assert.assertNull(obj)
    Assert.assertNotNull(result)
    Assert.assertNotSame(result1, emptyList<Int>())
  }

  @Test
  fun test_ifTrue() {
    val cnt = 10
    (cnt == 10).ifTrue {
      Assert.assertTrue(true)
      return
    }
    Assert.assertTrue(false)
  }

  @Test
  fun test_ifFalse() {
    val cnt = 10
    (cnt == 1).ifFalse {
      Assert.assertFalse(false)
      return
    }
    Assert.assertTrue(true)
  }

  @Test
  fun test_unsafeLazy() {
    val obj by unsafeLazy { emptyList<Int>() }
    Assert.assertNotNull(obj)
    Assert.assertEquals(obj, emptyList<Int>())
  }

  @Test
  fun test_hex() {
    val objHex = 255.hex
    Assert.assertEquals(objHex, "Integer@0x000000FF")
    Assert.assertNotSame(objHex, "Integer@0x000000FF")
  }
}
