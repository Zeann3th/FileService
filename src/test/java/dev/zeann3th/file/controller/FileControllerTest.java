package dev.zeann3th.file.controller;

import dev.zeann3th.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.CoreMatchers.is;

@WebMvcTest(controllers = FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    private String createAuthHeader(String sub, List<String> roles) {
        String rolesJson = "[" + String.join(",", roles.stream().map(r -> "\"" + r + "\"").toList()) + "]";
        String payloadJson = "{\"sub\":\"" + sub + "\",\"roles\":" + rolesJson + "}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        return "Bearer header." + encodedPayload + ".signature";
    }

    @Test
    void testServePublicFileAnonymous() throws Exception {
        String key = "public/test.txt";
        when(fileService.getContentType(key)).thenReturn("text/plain");
        when(fileService.getFile(key)).thenReturn(new ByteArrayResource("test".getBytes()));

        mockMvc.perform(get("/api/v1/files/" + key))
                .andExpect(status().isOk());
    }

    @Test
    void testServePrivateFileUserAuthorized() throws Exception {
        String sub = "user-123";
        String key = "private/" + sub + "/test.txt";
        String auth = createAuthHeader(sub, List.of("user"));

        when(fileService.getContentType(key)).thenReturn("text/plain");
        when(fileService.getFile(key)).thenReturn(new ByteArrayResource("test".getBytes()));

        mockMvc.perform(get("/api/v1/files/" + key)
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }

    @Test
    void testServePrivateFileUserForbidden() throws Exception {
        String sub = "user-123";
        String key = "private/other-user/test.txt";
        String auth = createAuthHeader(sub, List.of("user"));

        mockMvc.perform(get("/api/v1/files/" + key)
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0005")))
                .andExpect(jsonPath("$.errorType", is("FAILURE")));
    }

    @Test
    void testServePrivateFileAdminAuthorized() throws Exception {
        String sub = "admin-1";
        String otherUserSub = "user-123";
        String key = "private/" + otherUserSub + "/test.txt";
        String auth = createAuthHeader(sub, List.of("admin"));

        when(fileService.getContentType(key)).thenReturn("text/plain");
        when(fileService.getFile(key)).thenReturn(new ByteArrayResource("test".getBytes()));

        mockMvc.perform(get("/api/v1/files/" + key)
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }

    @Test
    void testServePrivateFileAnonymousForbidden() throws Exception {
        String key = "private/user-123/test.txt";
        mockMvc.perform(get("/api/v1/files/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0006")))
                .andExpect(jsonPath("$.errorType", is("FAILURE")));
    }

    @Test
    void testDeletePublicFileAdmin() throws Exception {
        String auth = createAuthHeader("admin-1", List.of("admin"));
        mockMvc.perform(delete("/api/v1/files/public/test.txt")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0000")))
                .andExpect(jsonPath("$.errorType", is("SUCCESS")));
    }

    @Test
    void testDeletePublicFileUserForbidden() throws Exception {
        String auth = createAuthHeader("user-1", List.of("user"));
        mockMvc.perform(delete("/api/v1/files/public/test.txt")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0005")))
                .andExpect(jsonPath("$.errorType", is("FAILURE")));
    }

    @Test
    void testDeletePrivateFileUserAuthorized() throws Exception {
        String sub = "user-123";
        String auth = createAuthHeader(sub, List.of("user"));
        mockMvc.perform(delete("/api/v1/files/private/" + sub + "/test.txt")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0000")));
    }

    @Test
    void testDeletePrivateFileUserForbidden() throws Exception {
        String sub = "user-123";
        String auth = createAuthHeader(sub, List.of("user"));
        mockMvc.perform(delete("/api/v1/files/private/other-user/test.txt")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0005")));
    }

    @Test
    void testDeletePrivateFileAdminAuthorized() throws Exception {
        String auth = createAuthHeader("admin-1", List.of("admin"));
        mockMvc.perform(delete("/api/v1/files/private/user-123/test.txt")
                .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0000")));
    }

    @Test
    void testFileNotFound() throws Exception {
        String key = "public/missing.txt";
        
        io.minio.messages.ErrorResponse errorResponse = org.mockito.Mockito.mock(io.minio.messages.ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        
        io.minio.errors.ErrorResponseException exception = new io.minio.errors.ErrorResponseException(
                errorResponse,
                null, null
        );

        when(fileService.getContentType(key)).thenThrow(exception);

        mockMvc.perform(get("/api/v1/files/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode", is("FS0001")))
                .andExpect(jsonPath("$.message", is("File not found: " + key)));
    }
}
