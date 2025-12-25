package com.tulip.controller;

import com.tulip.controller.admin.AdminInventoryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void showInventoryPage_withAdminRole_returnsCorrectView() throws Exception {
        mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/layouts/layout"))
                .andExpect(model().attribute("pageTitle", "INVENTORY"))
                .andExpect(model().attribute("currentPage", "inventory"))
                .andExpect(model().attribute("contentTemplate", "admin/inventory/index"))
                .andExpect(model().attribute("showSearch", true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void showInventoryPage_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    void showInventoryPage_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login?redirect=*"));
    }
}
