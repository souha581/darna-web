package com.darna.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.Role;
import com.darna.repository.ApartmentRepository;
import com.darna.repository.AppUserRepository;
import com.darna.repository.ChargeRepository;
import com.darna.repository.RepairRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class DarnaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepairRequestRepository repairRepository;

    @Autowired
    private ChargeRepository chargeRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Apartment occupiedApt;
    private Charge openCharge;

    @BeforeEach
    void resetDatabase() {
        repairRepository.deleteAll();
        chargeRepository.deleteAll();
        apartmentRepository.deleteAll();
        userRepository.deleteAll();

        apartmentRepository.save(new Apartment("Horizon", "A1", "A101", 1, 68.0, "S+1", 950.0, "Libre", true));
        occupiedApt = apartmentRepository.save(
                new Apartment("Horizon", "A1", "11", 3, 95.0, "S+2", 650.0, "Occupe", false)
        );

        userRepository.save(new AppUser("owner", passwordEncoder.encode("ownerpass"), "Owner", "55000001", Role.OWNER));
        AppUser resident = new AppUser("resident", passwordEncoder.encode("respass"), "Resident",
                "55000002", Role.RESIDENT);
        resident.setApartmentId(occupiedApt.getId());
        userRepository.save(resident);

        openCharge = chargeRepository.save(
                new Charge(occupiedApt, "Eau", "Novembre 2026", 50.0, LocalDate.now().plusDays(5))
        );
    }

    @Test
    void actuatorHealthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void publicCatalogReturnsOnlyAvailableApartments() throws Exception {
        mockMvc.perform(get("/api/apartments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].unitNumber").value("A101"));
    }

    @Test
    void onlyOwnerCanCreateApartmentViaApi() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new DarnaApiController.CreateApartmentRequest(
                        "Horizon", "B1", "B202", 2, 85.0, "S+2", 1100.0, "Vue mer"
                )
        );

        mockMvc.perform(post("/api/apartments")
                        .with(SecurityMockMvcRequestPostProcessors.user("resident").roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/apartments")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitNumber").value("B202"));
    }

    @Test
    void residentCanPayChargeAndReportRepairViaApi() throws Exception {
        mockMvc.perform(post("/api/charges/" + openCharge.getId() + "/pay")
                        .with(SecurityMockMvcRequestPostProcessors.user("resident").roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountTnd\": 50.0, \"paymentMode\": \"CARTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYE"));

        mockMvc.perform(post("/api/repairs")
                        .with(SecurityMockMvcRequestPostProcessors.user("resident").roles("RESIDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"Electricite\", \"priority\": \"Haute\", \"description\": \"Panne\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ENVOYEE"));
    }
}
