package com.miyako.core

import org.junit.Assert
import org.junit.Test

class BaseExtensionsUnitTest {

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

  @Test
  fun test_orInit() {
    val obj: MutableList<Int>? = null
    val result = obj.orInit {
      mutableListOf(1, 2, 3)
    }
    val result1 = result.orInit {
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

  @Test
  fun test_cast() {
    val tmp = 233
    var cnt = 0
    tmp.cast<Int> {
      cnt = 2
    }
    Assert.assertEquals(2, cnt)

    tmp.cast<Double, Unit>({ cnt = 3 }) {
      cnt = 4
    }
    Assert.assertNotEquals(4, cnt)

    tmp.cast<Number> {
      cnt = 5
    }
    Assert.assertEquals(5, cnt)

    val result = tmp.cast<Int, Int>({ 22 }) { 33 }
    Assert.assertEquals(33, result)
  }

  @Test
  fun test_cast_null() {
    val tmp: Int? = null
    var cnt = 0
    tmp.cast<Int> {
      cnt = 2
    }
    Assert.assertEquals(0, cnt)

    tmp.cast<Double, Unit>({ cnt = 3 }) {
      cnt = 4
    }
    Assert.assertNotEquals(4, cnt)

    tmp.cast<Number> {
      cnt = 5
    }
    Assert.assertNotEquals(5, cnt)

    val result = tmp.cast<Int, Int>({ 22 }) { 33 }
    Assert.assertEquals(22, result)
  }
}
