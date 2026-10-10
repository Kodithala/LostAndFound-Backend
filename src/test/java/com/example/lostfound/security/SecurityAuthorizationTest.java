package com.example.lostfound.security;

import com.example.lostfound.dto.RegisterRequest;
import com.example.lostfound.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
@SuppressWarnings("null")
public class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Guest accessing admin API directly returns HTTP 401 Unauthorized")
    public void testGuestAccessAdminApiReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Please log in to continue."));
    }

    @Test
    @WithMockUser(username = "student@college.com", roles = {"USER"})
    @DisplayName("USER accessing admin API directly returns HTTP 403 Forbidden")
    public void testUserAccessAdminApiReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access Denied — You don't have permission to access the Admin Dashboard."));
    }

    @Test
    @WithMockUser(username = "staff@college.com", roles = {"STAFF"})
    @DisplayName("STAFF accessing admin API directly returns HTTP 403 Forbidden")
    public void testStaffAccessAdminApiReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access Denied — You don't have permission to access the Admin Dashboard."));
    }

    @Test
    @WithMockUser(username = "student@college.com", roles = {"USER"})
    @DisplayName("USER accessing user management directory returns HTTP 403 Forbidden")
    public void testUserAccessUserDirectoryReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin@college.com", roles = {"ADMIN"})
    @DisplayName("ADMIN accessing admin API directly returns HTTP 200 OK")
    public void testAdminAccessAdminApiReturns200() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER attempting to register as ADMIN is rejected with HTTP 400")
    public void testRegisterAsAdminRejected() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Hacker Admin");
        registerRequest.setEmail("hacker@college.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("+1-555-9999");
        registerRequest.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Public registration as ADMIN is not permitted!"));
    }
}
