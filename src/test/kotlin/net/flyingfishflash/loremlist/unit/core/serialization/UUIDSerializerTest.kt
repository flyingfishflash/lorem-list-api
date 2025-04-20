package net.flyingfishflash.loremlist.unit.core.serialization

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.encoding.Decoder
import net.flyingfishflash.loremlist.core.exceptions.CoreException
import net.flyingfishflash.loremlist.core.serialization.UuidSerializer
import org.springframework.http.HttpStatus

class UUIDSerializerTest :
  DescribeSpec({
    describe("deserialize") {
      context("when input is an invalid UUID string") {
        it("throws CoreException with BAD_REQUEST status") {
          val invalidUuidString = "not-a-uuid"
          val decoder = mockk<Decoder>()
          every { decoder.decodeString() } returns invalidUuidString
          val exception = shouldThrow<CoreException> { UuidSerializer.deserialize(decoder) }
          exception.httpStatus shouldBe HttpStatus.BAD_REQUEST
          exception.message shouldBe "Error deserializing UUID."
          exception.cause.shouldBeInstanceOf<IllegalArgumentException>()
          verify { decoder.decodeString() }
        }
      }
    }
  })
