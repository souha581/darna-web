package com.darna.service;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.ChargeStatus;
import com.darna.model.RepairRequest;
import com.darna.model.RepairStatus;
import com.darna.model.Role;
import com.darna.repository.ApartmentRepository;
import com.darna.repository.AppUserRepository;
import com.darna.repository.ChargeRepository;
import com.darna.repository.RepairRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DarnaServiceTest {

    @Mock
    private ApartmentRepository apartmentRepository;

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private RepairRequestRepository repairRepository;

    @Mock
    private AppUserRepository userRepository;

    private DarnaService darnaService;

    @BeforeEach
    void setUp() {
        darnaService = new DarnaService(
                apartmentRepository,
                chargeRepository,
                repairRepository,
                userRepository,
                new BCryptPasswordEncoder()
        );
    }

    @Test
    void filterPublicCatalogFiltersByTypologyAndMaxRent() {
        Apartment a1 = new Apartment("Horizon", "A1", "A101", 1, 68.0, "S+1", 950.0, "Desc", true);
        Apartment a2 = new Apartment("Horizon", "A1", "A204", 2, 115.0, "S+2", 1450.0, "Desc", true);
        when(apartmentRepository.findByAvailableTrue()).thenReturn(List.of(a1, a2));

        List<Apartment> result = darnaService.filterPublicCatalog("S+1", 1000.0);

        assertEquals(1, result.size());
        assertEquals("A101", result.get(0).getUnitNumber());
    }

    @Test
    void payChargeSupportsPartialThenFullPaymentAndRejectsOverpayment() {
        Apartment apt = new Apartment("Horizon", "A1", "11", 3, 95.0, "S+2", 650.0, "Desc", false);
        apt.setId(1L);
        Charge charge = new Charge(apt, "Eau", "Novembre 2026", 120.0, LocalDate.now().plusDays(5));
        charge.setId(10L);
        AppUser resident = new AppUser("souha", "hash", "Souha", "55112233", Role.RESIDENT);
        resident.setApartmentId(1L);

        when(chargeRepository.findById(10L)).thenReturn(Optional.of(charge));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(inv -> inv.getArgument(0));

        Charge partial = darnaService.payCharge(10L, 40.0, "CARTE", resident);
        assertEquals(80.0, partial.getRemainingAmountTnd(), 0.01);
        assertEquals(ChargeStatus.NON_PAYE, partial.getStatus());

        assertThrows(BusinessException.class,
                () -> darnaService.payCharge(10L, 100.0, "CARTE", resident));

        Charge paid = darnaService.payCharge(10L, 80.0, "VIREMENT", resident);
        assertEquals(0.0, paid.getRemainingAmountTnd(), 0.01);
        assertEquals(ChargeStatus.PAYE, paid.getStatus());
    }

    @Test
    void rentApartmentAsVisitorLocksUnitAndCreatesResident() {
        Apartment apt = new Apartment("Horizon", "A1", "A101", 1, 68.0, "S+1", 950.0, "Desc", true);
        apt.setId(2L);

        when(apartmentRepository.findById(2L)).thenReturn(Optional.of(apt));
        when(userRepository.existsByUsername("amine")).thenReturn(false);
        when(apartmentRepository.save(any(Apartment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser created = darnaService.rentApartmentAsVisitor(
                2L, "Amine Trabelsi", "amine", "55112233", "secret123", "CARTE"
        );

        assertFalse(apt.isAvailable());
        assertEquals(Role.RESIDENT, created.getRole());
        assertEquals(2L, created.getApartmentId());
    }

    @Test
    void closeRepairSetsCostReportAndTerminatesStatus() {
        Apartment apt = new Apartment("Horizon", "A1", "11", 3, 95.0, "S+2", 650.0, "Desc", false);
        AppUser resident = new AppUser("souha", "hash", "Souha", "55112233", Role.RESIDENT);
        AppUser worker = new AppUser("mariem", "hash", "Mariem", "22445566", Role.OUVRIER);
        RepairRequest repair = new RepairRequest(apt, resident, "Plomberie", "Haute", "Fuite");
        repair.setId(5L);
        repair.setAssignedWorker(worker);
        repair.setStatus(RepairStatus.EN_COURS);

        when(repairRepository.findById(5L)).thenReturn(Optional.of(repair));
        when(repairRepository.save(any(RepairRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RepairRequest closed = darnaService.closeRepair(5L, 75.0, "Joint remplace", worker);

        assertEquals(RepairStatus.TERMINEE, closed.getStatus());
        assertEquals(75.0, closed.getInterventionCostTnd());
        assertEquals("Joint remplace", closed.getClosingReport());
    }
}
