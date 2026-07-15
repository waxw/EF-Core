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
  fun test_withNull() {
    val obj: Any? = null
    var cnt = 0
    val result = obj.withNull {
      cnt = 1
    }
    Assert.assertTrue(result)
    Assert.assertEquals(cnt, 1)

    val obj1: Any? = 233
    var cnt1 = 0
    val result1 = obj1.withNull {
      cnt = 1
    }
    Assert.assertFalse(result1)
    Assert.assertEquals(cnt1, 0)
  }


  @Test
  fun test_withNotNull() {
    val obj: Any? = 255
    var cnt = 0
    val result = obj.withNotNull {
      cnt = 1
    }
    Assert.assertTrue(result)
    Assert.assertEquals(cnt, 1)

    val obj1: Any? = null
    var cnt1 = 0
    val result1 = obj1.withNotNull {
      cnt = 1
    }
    Assert.assertFalse(result1)
    Assert.assertEquals(cnt1, 0)
  }

  @Test
  fun test_instance() {
    val tmp = 233
    var cnt = 0
    tmp.instance<Int> {
      cnt = 2
    }
    Assert.assertEquals(2, cnt)

    tmp.instance<Double>({ cnt = 3 }) {
      cnt = 4
    }
    Assert.assertNotEquals(4, cnt)

    tmp.instance<Number> {
      cnt = 5
    }
    Assert.assertEquals(5, cnt)

    val result = tmp.instance<Int, Int>({ 22 }) { 33 }
    Assert.assertEquals(33, result)
  }

  @Test
  fun test_instance_null() {
    val tmp: Int? = null
    var cnt = 0
    tmp.instance<Int> {
      cnt = 2
    }
    Assert.assertEquals(0, cnt)

    tmp.instance<Double>({ cnt = 3 }) {
      cnt = 4
    }
    Assert.assertNotEquals(4, cnt)

    tmp.instance<Number> {
      cnt = 5
    }
    Assert.assertNotEquals(5, cnt)

    val result = tmp.instance<Int, Int>({ 22 }) { 33 }
    Assert.assertEquals(22, result)
  }
}
