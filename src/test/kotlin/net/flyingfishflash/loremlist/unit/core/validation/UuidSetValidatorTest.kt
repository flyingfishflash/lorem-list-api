package net.flyingfishflash.loremlist.unit.core.validation

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintValidatorContext
import net.flyingfishflash.loremlist.core.validation.UuidSetValidator
import net.flyingfishflash.loremlist.core.validation.ValidUuidSet
import java.util.*

class UuidSetValidatorTest :
  DescribeSpec({

    describe("UuidSetValidator") {
      val mockBuilder = mockk<ConstraintValidatorContext.ConstraintViolationBuilder>(relaxed = true)
      val mockContext = mockk<ConstraintValidatorContext>(relaxed = true)

      beforeTest {
        clearAllMocks()
        every { mockContext.buildConstraintViolationWithTemplate(any()) } returns mockBuilder
      }

      describe("initialization") {
        it("sets allowEmpty to true when specified in annotation") {
          val validator = UuidSetValidator()
          val annotation = mockk<ValidUuidSet>()
          every { annotation.allowEmpty } returns true
          validator.initialize(annotation)
          val field = UuidSetValidator::class.java.getDeclaredField("allowEmpty")
          field.isAccessible = true
          field.getBoolean(validator) shouldBe true
        }

        it("sets allowEmpty to false when specified in annotation") {
          val validator = UuidSetValidator()
          val annotation = mockk<ValidUuidSet>()
          every { annotation.allowEmpty } returns false
          validator.initialize(annotation)
          val field = UuidSetValidator::class.java.getDeclaredField("allowEmpty")
          field.isAccessible = true
          field.getBoolean(validator) shouldBe false
        }
      }

      describe("isValid") {
        val validator = UuidSetValidator()

        context("when value is null") {
          it("returns false and adds violation message") {
            val result = validator.isValid(null, mockContext)
            result shouldBe false
            verify { mockContext.buildConstraintViolationWithTemplate("UUID set must not be null") }
            verify { mockBuilder.addConstraintViolation() }
          }
        }

        context("when value is empty") {
          it("returns true when allowEmpty is true") {
            val annotation = mockk<ValidUuidSet>()
            every { annotation.allowEmpty } returns true
            validator.initialize(annotation)
            val result = validator.isValid(emptyList<UUID>(), mockContext)
            result shouldBe true
            verify { mockContext.disableDefaultConstraintViolation() }
            verify(exactly = 0) { mockBuilder.addConstraintViolation() }
          }

          it("returns false when allowEmpty is false") {
            val annotation = mockk<ValidUuidSet>()
            every { annotation.allowEmpty } returns false
            validator.initialize(annotation)
            val result = validator.isValid(emptyList<UUID>(), mockContext)
            result shouldBe false
            verify { mockContext.buildConstraintViolationWithTemplate("UUID set must not be empty") }
            verify { mockBuilder.addConstraintViolation() }
          }
        }

        context("when collection contains null values") {
          it("returns false and adds appropriate violation message") {
            val uuidList = listOf(UUID.randomUUID(), null, UUID.randomUUID())
            val result = validator.isValid(uuidList, mockContext)
            result shouldBe false
            verify { mockContext.disableDefaultConstraintViolation() }
            verify { mockContext.buildConstraintViolationWithTemplate("Value at index 1 is null") }
            verify { mockBuilder.addConstraintViolation() }
          }
        }

        context("when collection contains non-UUID objects") {
          it("returns false and adds appropriate violation message") {
            val mixedList = listOf(UUID.randomUUID(), "not-a-uuid", UUID.randomUUID())
            val result = validator.isValid(mixedList, mockContext)
            result shouldBe false
            verify { mockContext.disableDefaultConstraintViolation() }
            verify { mockContext.buildConstraintViolationWithTemplate("Value at index 1 is not a UUID object") }
            verify { mockBuilder.addConstraintViolation() }
          }
        }

        context("when collection contains invalid UUIDs") {
          it("returns false for UUIDs that don't match the regex pattern") {
            // Custom UUID that doesn't match the pattern for version 3 or 4
            val invalidUuid = mockk<UUID>()
            val uuidList = listOf(UUID.randomUUID(), invalidUuid)
            every { invalidUuid.toString() } returns "00000000-0000-2000-0000-000000000000" // Version 2
            val result = validator.isValid(uuidList, mockContext)
            result shouldBe false
            verify { mockContext.disableDefaultConstraintViolation() }
            verify {
              mockContext.buildConstraintViolationWithTemplate(
                "Value '00000000-0000-2000-0000-000000000000' at index 1 is not a valid version 3 or version 4 UUID",
              )
            }
            verify { mockBuilder.addConstraintViolation() }
          }
        }

        context("when collection contains only valid UUIDs") {
          it("returns true for a collection with valid version 4 UUIDs") {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            val validList = listOf(uuid1, uuid2)
            val result = validator.isValid(validList, mockContext)
            result shouldBe true
            verify { mockContext.disableDefaultConstraintViolation() }
            verify(exactly = 0) { mockBuilder.addConstraintViolation() }
          }

          it("returns true for a collection with valid version 3 UUIDs") {
            val v3Uuid1 = mockk<UUID>()
            val v3Uuid2 = mockk<UUID>()
            val validList = listOf(v3Uuid1, v3Uuid2)
            every { v3Uuid1.toString() } returns "00000000-0000-3000-8000-000000000000"
            every { v3Uuid2.toString() } returns "11111111-1111-3111-a111-111111111111"
            val result = validator.isValid(validList, mockContext)
            result shouldBe true
            verify { mockContext.disableDefaultConstraintViolation() }
            verify(exactly = 0) { mockBuilder.addConstraintViolation() }
          }

          it("returns true for a mixed collection of valid version 3 and 4 UUIDs") {
            val v4Uuid = UUID.randomUUID()
            val v3Uuid = mockk<UUID>()
            val validList = listOf(v4Uuid, v3Uuid)
            every { v3Uuid.toString() } returns "00000000-0000-3000-8000-000000000000"
            val result = validator.isValid(validList, mockContext)
            result shouldBe true
            verify { mockContext.disableDefaultConstraintViolation() }
            verify(exactly = 0) { mockBuilder.addConstraintViolation() }
          }
        }
      }
    }
  })
