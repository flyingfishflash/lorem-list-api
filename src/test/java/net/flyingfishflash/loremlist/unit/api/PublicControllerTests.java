package net.flyingfishflash.loremlist.unit.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import net.flyingfishflash.loremlist.api.LrmListApiService;
import net.flyingfishflash.loremlist.api.LrmListPublicController;
import net.flyingfishflash.loremlist.api.data.request.LrmListCreateRequest;
import net.flyingfishflash.loremlist.api.data.response.ApiServiceResponse;
import net.flyingfishflash.loremlist.api.data.response.LrmListResponse;
import net.flyingfishflash.loremlist.core.configuration.SerializationConfig;
import net.flyingfishflash.loremlist.core.configuration.WebSecurityConfiguration;
import net.flyingfishflash.loremlist.core.response.structure.DispositionOfSuccess;
import net.flyingfishflash.loremlist.domain.lrmlist.LrmList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** PublicController Unit Tests */
@WebMvcTest(controllers = LrmListPublicController.class)
@Import({SerializationConfig.class, WebSecurityConfiguration.class})
class PublicControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LrmListApiService mockLrmListService;

  private final UUID id0 = UUID.fromString("00000000-0000-4000-a000-000000000000");
  private final LrmListCreateRequest lrmListCreateRequest =
      new LrmListCreateRequest("Lorem List Name", "Lorem List Description", true);
  private final Instant now = Clock.System.INSTANCE.now();
  private final String irrelevantMessage = "message is irrelevant";

  private LrmList lrmList() {
    return new LrmList(
        id0,
        lrmListCreateRequest.name(),
        lrmListCreateRequest.description(),
        lrmListCreateRequest.isPublic(),
        "Lorem Ipsum Owner",
        now,
        "Lorum Ipsum Created By",
        now,
        "Lorem Ipsum Updated By",
        Set.of());
  }

  @Test
  void listsAreFound() throws Exception {
    List<LrmListResponse> mockReturn = List.of(LrmListResponse.fromLrmList(lrmList()));
    when(mockLrmListService.findByPublicExcludeItems())
        .thenReturn(new ApiServiceResponse<>(mockReturn, irrelevantMessage));
    String instance = "/public/lists";

    mockMvc
        .perform(get(instance).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
        .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
        .andExpect(jsonPath("$.message").value(irrelevantMessage))
        .andExpect(jsonPath("$.instance").value(instance))
        .andExpect(jsonPath("$.size").value(mockReturn.size()))
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.[0].name").value(lrmList().name()))
        .andExpect(jsonPath("$.content.[0].description").value(lrmList().description()))
        .andExpect(jsonPath("$.content.[0].items").isEmpty());

    verify(mockLrmListService, times(1)).findByPublicExcludeItems();
  }

  @Test
  void listsAreFoundIncludeItemsFalse() throws Exception {
    List<LrmListResponse> mockReturn = List.of(LrmListResponse.fromLrmList(lrmList()));
    when(mockLrmListService.findByPublicExcludeItems())
        .thenReturn(new ApiServiceResponse<>(mockReturn, irrelevantMessage));
    String instance = "/public/lists?includeItems=false";

    mockMvc
        .perform(get(instance).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
        .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
        .andExpect(jsonPath("$.message").value(irrelevantMessage))
        .andExpect(jsonPath("$.instance").value("/public/lists"))
        .andExpect(jsonPath("$.size").value(mockReturn.size()))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.[0].name").value(lrmList().name()))
        .andExpect(jsonPath("$.content.[0].description").value(lrmList().description()))
        .andExpect(jsonPath("$.content.[0].items").isEmpty());

    verify(mockLrmListService, times(1)).findByPublicExcludeItems();
  }

  @Test
  void listsAreFoundIncludeItemsTrue() throws Exception {
    List<LrmListResponse> mockReturn = List.of(LrmListResponse.fromLrmList(lrmList()));
    when(mockLrmListService.findByPublic())
        .thenReturn(new ApiServiceResponse<>(mockReturn, irrelevantMessage));
    String instance = "/public/lists?includeItems=true";

    mockMvc
        .perform(get(instance).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.disposition").value(DispositionOfSuccess.SUCCESS.nameAsLowercase()))
        .andExpect(jsonPath("$.method").value(HttpMethod.GET.name().toLowerCase()))
        .andExpect(jsonPath("$.message").value(irrelevantMessage))
        .andExpect(jsonPath("$.instance").value("/public/lists"))
        .andExpect(jsonPath("$.size").value(mockReturn.size()))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.[0].name").value(lrmList().name()))
        .andExpect(jsonPath("$.content.[0].description").value(lrmList().description()))
        .andExpect(jsonPath("$.content.[0].items").isArray())
        .andExpect(jsonPath("$.content.[0].items").isEmpty());

    verify(mockLrmListService, times(1)).findByPublic();
  }
}
