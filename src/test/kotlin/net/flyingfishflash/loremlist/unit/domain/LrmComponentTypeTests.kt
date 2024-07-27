package net.flyingfishflash.loremlist.unit.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.flyingfishflash.loremlist.domain.LrmComponentType

class LrmComponentTypeTests : DescribeSpec({

  describe("invert()") {
    it("inverse of Item is List") {
      val inverse = LrmComponentType.List.invert()
      inverse.shouldBe(LrmComponentType.Item)
    }

    it("inverse of List is Item") {
      val inverse = LrmComponentType.Item.invert()
      inverse.shouldBe(LrmComponentType.List)
    }
  }
})
