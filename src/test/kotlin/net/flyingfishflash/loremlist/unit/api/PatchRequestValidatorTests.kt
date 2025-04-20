package net.flyingfishflash.loremlist.unit.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder
import net.flyingfishflash.loremlist.api.validation.PatchRequestValidator

class PatchRequestValidatorTests :
  DescribeSpec({

    lateinit var validator: PatchRequestValidator
    lateinit var context: ConstraintValidatorContext
    lateinit var violationBuilder: ConstraintViolationBuilder

    beforeEach {
      MockKAnnotations.init(this)
      validator = PatchRequestValidator()
      context = mockk(relaxed = true)
      violationBuilder = mockk(relaxed = true)

      every { context.buildConstraintViolationWithTemplate(any()) } returns violationBuilder
    }

    describe("PatchRequestValidator") {

      it("should return false if input is null") {
        val result = validator.isValid(null, context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("patch request cannot be null") }
      }

      it("should return false if key is longer than 64 characters") {
        val longKey = "k".repeat(65)
        val result = validator.isValid(mapOf(longKey to "value"), context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("Key '$longKey' must be 1 to 64 characters long") }
      }

//    it("should return false if value is null") {
//      val result = validator.isValid(mapOf("name" to null), context)
//      result.shouldBeFalse()
//      verify { context.buildConstraintViolationWithTemplate("Value for key 'name' must not be null") }
//    }

      it("should return false if value is not a string or boolean") {
        val result = validator.isValid(mapOf("name" to 123), context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("Value for key 'name' must be a boolean or string") }
      }

      it("should return false for blank string value") {
        val result = validator.isValid(mapOf("name" to "   "), context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("String value for key 'name' must not be blank or whitespace") }
      }

      it("should return false if string is too long for 'name' field") {
        val longValue = "a".repeat(65)
        val result = validator.isValid(mapOf("name" to longValue), context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("Value for field 'name' must be a string from 1 to 64 characters long") }
      }

      it("should return false if string is too long for 'description' field") {
        val longDesc = "a".repeat(2049)
        val result = validator.isValid(mapOf("description" to longDesc), context)
        result.shouldBeFalse()
        verify { context.buildConstraintViolationWithTemplate("Value for field 'description' must be a string from 1 to 2048 characters long") }
      }

      it("should return true for valid boolean value") {
        val result = validator.isValid(mapOf("public" to true), context)
        result.shouldBeTrue()
      }

      it("should return true for valid string fields") {
        val validData = mapOf(
          "name" to "Valid Name",
          "description" to "A valid description",
          "customField" to "custom value",
        )

        val result = validator.isValid(validData, context)
        result.shouldBeTrue()
      }
    }
  })
