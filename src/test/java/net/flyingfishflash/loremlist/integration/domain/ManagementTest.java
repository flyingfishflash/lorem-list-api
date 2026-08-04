package net.flyingfishflash.loremlist.integration.domain;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;

@SpringBootTest
class ManagementTest extends AbstractIntegrationTest {

  @Test
  void health() throws Exception {
    String instance = "/management/health";
    performRequest(HttpMethod.GET, instance)
        .andExpectAll(
            status().isOk(), jsonPath("$.length()").value(1), jsonPath("$.status").value("UP"));
  }

  @Test
  void info() throws Exception {
    String instance = "/management/info";
    performRequest(HttpMethod.GET, instance)
        .andExpectAll(
            status().isOk(),
            jsonPath("$.build.length()").value(8),
            jsonPath("$.build.name").value("lorem-list api"));
  }
}
