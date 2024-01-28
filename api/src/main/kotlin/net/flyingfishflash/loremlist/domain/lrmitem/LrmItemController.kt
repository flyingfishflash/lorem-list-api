package net.flyingfishflash.loremlist.domain.lrmitem

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItem
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "list item controller")
@RestController
@RequestMapping("/item")
@ApiResponses(value = [ApiResponse(responseCode = "400", description = "Bad Request")])
class LrmItemController(val lrmItemService: LrmItemService) {
  @PostMapping
  @Operation(summary = "Create a new list item")
  fun create(
    @Valid @RequestBody lrmItemRequest: LrmItemRequest,
  ): LrmItem {
    val lrmListItem = lrmItemService.create(lrmItemRequest)
    return lrmListItem
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a list")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "List Item Deleted"),
      ApiResponse(responseCode = "404", description = "List Item Not Found"),
    ],
  )
  fun delete(
    @PathVariable("id") @Min(1) id: Long,
  ): ResponseEntity<Any> {
    lrmItemService.deleteSingleById(id)
    return ResponseEntity(HttpStatus.NO_CONTENT)
  }

  @GetMapping
  @Operation(summary = "Retrieve all list items")
  fun findAll(): List<LrmItem> = lrmItemService.findAll()

  @PostMapping("/{id}/to-list")
  @Operation(summary = "Assign Item to List")
  fun toList(
    @PathVariable("id") @Min(1) id: Long,
    @Min(1) @RequestBody listId: Long,
  ) {
    lrmItemService.assignItemToList(listId, id)
  }
}
