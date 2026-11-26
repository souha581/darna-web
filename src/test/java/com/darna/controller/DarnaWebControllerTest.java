package com.darna.controller;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.RepairRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
class DarnaWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Apartment freeApt;
    private Apartment occupiedApt;
    private AppUser worker;
    private Charge charge;
    private RepairRequest repair;

    @BeforeEach
    void setup() {
        repairRepository.deleteAll();
        chargeRepository.deleteAll();
        apartmentRepository.deleteAll();
        userRepository.deleteAll();

        freeApt = apartmentRepository.save(
                new Apartment("Horizon", "A1", "A101", 1, 68.0, "S+1", 950.0, "Libre", true)
        );
        occupiedApt = apartmentRepository.save(
                new Apartment("Horizon", "A1", "11", 3, 95.0, "S+2", 650.0, "Occupe", false)
        );

        userRepository.save(new AppUser("owner", passwordEncoder.encode("owner123"), "Owner", "55000001", Role.OWNER));
        AppUser resident = new AppUser("resident", passwordEncoder.encode("res123"), "Resident",
                "55000002", Role.RESIDENT);
        resident.setApartmentId(occupiedApt.getId());
        resident = userRepository.save(resident);

        worker = new AppUser("ouvrier", passwordEncoder.encode("ouv123"), "Ouvrier", "22000003", Role.OUVRIER);
        worker.setSpecialty("Plomberie");
        worker = userRepository.save(worker);

        charge = chargeRepository.save(
                new Charge(occupiedApt, "Eau", "Novembre 2026", 45.0, LocalDate.now().plusDays(5))
        );
        repair = repairRepository.save(
                new RepairRequest(occupiedApt, resident, "Plomberie", "Moyenne", "Fuite")
        );
    }

    @Test
    void visitorCanBrowseCatalogRegisterAndRentApartment() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "New User")
                        .param("username", "newuser")
                        .param("phone", "55123123")
                        .param("role", "OWNER")
                        .param("password", "pass1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/rent")
                        .with(csrf())
                        .param("apartmentId", freeApt.getId().toString())
                        .param("fullName", "Amine")
                        .param("username", "amine")
                        .param("phone", "55998877")
                        .param("password", "pass1234")
                        .param("paymentMode", "CARTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void ownerWorkflowsWorkAcrossDashboardApartmentsChargesAndRepairs() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        mockMvc.perform(get("/apartments")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("apartments"));

        mockMvc.perform(post("/apartments/add")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .with(csrf())
                        .param("residenceName", "Horizon")
                        .param("buildingName", "B1")
                        .param("unitNumber", "B301")
                        .param("floor", "3")
                        .param("surfaceM2", "80")
                        .param("typology", "S+2")
                        .param("monthlyRentTnd", "1050")
                        .param("description", "Calme"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apartments"));

        mockMvc.perform(post("/apartments/" + freeApt.getId() + "/delete")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/apartments"));

        mockMvc.perform(get("/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(view().name("charges"));

        mockMvc.perform(post("/charges/issue")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .with(csrf())
                        .param("apartmentId", occupiedApt.getId().toString())
                        .param("label", "Syndic")
                        .param("period", "Decembre 2026")
                        .param("amountTnd", "60")
                        .param("dueDate", LocalDate.now().plusDays(10).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/charges"));

        mockMvc.perform(post("/repairs/assign")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .with(csrf())
                        .param("repairId", repair.getId().toString())
                        .param("workerId", worker.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repairs"));
    }

    @Test
    void residentAndWorkerWebFlowsWork() throws Exception {
        mockMvc.perform(post("/charges/" + charge.getId() + "/pay")
                        .with(SecurityMockMvcRequestPostProcessors.user("resident").roles("RESIDENT"))
                        .with(csrf())
                        .param("amountTnd", "45")
                        .param("paymentMode", "CARTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/charges"));

        mockMvc.perform(post("/repairs/report")
                        .with(SecurityMockMvcRequestPostProcessors.user("resident").roles("RESIDENT"))
                        .with(csrf())
                        .param("category", "Electricite")
                        .param("priority", "Haute")
                        .param("description", "Disjoncteur"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repairs"));

        mockMvc.perform(post("/repairs/assign")
                        .with(SecurityMockMvcRequestPostProcessors.user("owner").roles("OWNER"))
                        .with(csrf())
                        .param("repairId", repair.getId().toString())
                        .param("workerId", worker.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/repairs")
                        .with(SecurityMockMvcRequestPostProcessors.user("ouvrier").roles("OUVRIER")))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs"));

        mockMvc.perform(post("/repairs/close")
                        .with(SecurityMockMvcRequestPostProcessors.user("ouvrier").roles("OUVRIER"))
                        .with(csrf())
                        .param("repairId", repair.getId().toString())
                        .param("costTnd", "50")
                        .param("report", "Repare"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repairs"));
    }
}
