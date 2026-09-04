package com.miyako.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaseExtensionsUnitTest {
  @Test
  fun test_orInit() {
    val obj: MutableList<Int>? = null
    val result =
      obj.orInit {
        mutableListOf(1, 2, 3)
      }
    val result1 =
      result.orInit {
        emptyList()
      }
    assertNull(obj)
    assertNotNull(result)
    assertNotSame(result1, emptyList<Int>())
  }

  @Test
  fun test_ifTrue() {
    val cnt = 10
    (cnt == 10).ifTrue {
      assertTrue(true)
      return
    }
    assertTrue(false)
  }

  @Test
  fun test_ifFalse() {
    val cnt = 10
    (cnt == 1).ifFalse {
      assertFalse(false)
      return
    }
    assertTrue(true)
  }

  @Test
  fun test_unsafeLazy() {
    val obj by unsafeLazy { emptyList<Int>() }
    assertNotNull(obj)
    assertEquals(obj, emptyList<Int>())
  }

  @Test
  fun test_hex() {
    val objHex = 255.hex
    assertTrue(objHex.substringBefore('@').isNotEmpty())
    assertEquals("0x000000FF", objHex.substringAfter('@'))
  }

  @Test
  fun test_cast() {
    val tmp = 233
    var cnt = 0
    tmp.cast<Int> {
      cnt = 2
    }
    assertEquals(2, cnt)

    tmp.cast<Double, Unit>({ cnt = 3 }) {
      cnt = 4
    }
    assertFalse(4 == cnt)

    tmp.cast<Number> {
      cnt = 5
    }
    assertEquals(5, cnt)

    val result = tmp.cast<Int, Int>({ 22 }) { 33 }
    assertEquals(33, result)
  }

  @Test
  fun test_cast_null() {
    val tmp: Int? = null
    var cnt = 0
    tmp.cast<Int> {
      cnt = 2
    }
    assertEquals(0, cnt)

    tmp.cast<Double, Unit>({ cnt = 3 }) {
      cnt = 4
    }
    assertFalse(4 == cnt)

    tmp.cast<Number> {
      cnt = 5
    }
    assertFalse(5 == cnt)

    val result = tmp.cast<Int, Int>({ 22 }) { 33 }
    assertEquals(22, result)
  }
}
