package net.flyingfishflash.loremlist.domain.lrmlist

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

//
// import io.swagger.v3.oas.annotations.Operation
// import io.swagger.v3.oas.annotations.responses.ApiResponse
// import io.swagger.v3.oas.annotations.responses.ApiResponses
// import io.swagger.v3.oas.annotations.tags.Tag
// import jakarta.validation.Valid
// import jakarta.validation.constraints.Min
// import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList
// import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
// import org.springframework.http.HttpStatus
// import org.springframework.http.ResponseEntity
// import org.springframework.web.bind.annotation.DeleteMapping
// import org.springframework.web.bind.annotation.GetMapping
// import org.springframework.web.bind.annotation.PatchMapping
// import org.springframework.web.bind.annotation.PathVariable
// import org.springframework.web.bind.annotation.PostMapping
// import org.springframework.web.bind.annotation.RequestBody
// import org.springframework.web.bind.annotation.RequestMapping
// import org.springframework.web.bind.annotation.RestController
//
@Tag(name = "list controller")
@RestController
@RequestMapping("/lists")
@ApiResponses(value = [ApiResponse(responseCode = "400", description = "Bad Request")])
class LrmListController(private val lrmListService: LrmListService) {
  @PostMapping
  @Operation(summary = "Create a new list")
  fun create(
    @Valid @RequestBody lrmListRequest: LrmListRequest,
  ): LrmList {
    val lrmList = lrmListService.create(lrmListRequest)
    return lrmList
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a list")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "List Deleted"),
      ApiResponse(responseCode = "404", description = "List Not Found"),
    ],
  )
  fun delete(
    @PathVariable("id") @Min(1) id: Long,
  ): ResponseEntity<Any> {
    lrmListService.deleteById(id)
    return ResponseEntity(HttpStatus.NO_CONTENT)
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a list")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List Updated"),
      ApiResponse(responseCode = "204", description = "List Not Updated"),
      ApiResponse(responseCode = "404", description = "List Not Found"),
    ],
  )
  fun patch(
    @PathVariable("id") @Min(1) id: Long,
    @RequestBody patchRequest: Map<String, Any>,
  ): ResponseEntity<LrmList> {
    val(lrmList, patched) = lrmListService.patch(id, patchRequest)
    return if (patched) {
      ResponseEntity(lrmList, HttpStatus.OK)
    } else {
      ResponseEntity(lrmList, HttpStatus.NO_CONTENT)
    }
  }

  @GetMapping
  @Operation(summary = "Retrieve all lists")
  fun findAll(): MutableList<LrmList> = lrmListService.findAll()

  @GetMapping("/{id}")
  @Operation(summary = "Retrieve a single list")
  @ApiResponses(value = [ApiResponse(responseCode = "404", description = "List Not Found")])
  fun findById(
    @PathVariable("id") @Min(1) id: Long,
  ) = lrmListService.findByIdOrListNotFoundException(id)
}
