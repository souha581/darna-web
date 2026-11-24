package com.darna.config;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.RepairRequest;
import com.darna.model.Role;
import com.darna.repository.ApartmentRepository;
import com.darna.repository.AppUserRepository;
import com.darna.repository.ChargeRepository;
import com.darna.repository.RepairRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds sample Tunisian residence data and demo accounts for local exploration.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final ChargeRepository chargeRepository;
    private final RepairRequestRepository repairRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.owner-username:owner}")
    private String ownerUsername;

    @Value("${app.seed.owner-password:}")
    private String ownerPassword;

    @Value("${app.seed.resident-username:resident}")
    private String residentUsername;

    @Value("${app.seed.resident-password:}")
    private String residentPassword;

    @Value("${app.seed.ouvrier-username:ouvrier}")
    private String ouvrierUsername;

    @Value("${app.seed.ouvrier-password:}")
    private String ouvrierPassword;

    public DataInitializer(AppUserRepository userRepository,
                           ApartmentRepository apartmentRepository,
                           ChargeRepository chargeRepository,
                           RepairRequestRepository repairRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.chargeRepository = chargeRepository;
        this.repairRepository = repairRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || apartmentRepository.count() > 0) {
            return;
        }

        Apartment aptA101 = new Apartment("Horizon", "A1", "A101", 1, 68.0, "S+1", 950.0,
                "S+1 lumineux avec balcon, climatisation, fibre optique et parking sous-sol.", true);
        Apartment aptA204 = new Apartment("Horizon", "A1", "A204", 2, 115.0, "S+2", 1450.0,
                "Grand S+2 haut standing avec suite parentale et cuisine equipee.", true);
        Apartment aptB102 = new Apartment("Horizon", "B1", "B102", 1, 45.0, "S+0", 650.0,
                "Studio moderne proche universite et commodites a Mahdia.", true);
        Apartment apt11 = new Apartment("Horizon", "A1", "11", 3, 95.0, "S+2", 650.0,
                "Appartement occupe par Souha.", false);

        apartmentRepository.saveAll(List.of(aptA101, aptA204, aptB102, apt11));

        if (ownerPassword != null && !ownerPassword.isBlank()) {
            userRepository.save(new AppUser(ownerUsername, passwordEncoder.encode(ownerPassword),
                    "Amir Menzli (Proprietaire)", "55123456", Role.OWNER));
        }

        AppUser resident = null;
        if (residentPassword != null && !residentPassword.isBlank()) {
            resident = new AppUser(residentUsername, passwordEncoder.encode(residentPassword),
                    "Souha Bouagila", "55987654", Role.RESIDENT);
            resident.setApartmentId(apt11.getId());
            resident = userRepository.save(resident);
        }

        AppUser worker = null;
        if (ouvrierPassword != null && !ouvrierPassword.isBlank()) {
            worker = new AppUser(ouvrierUsername, passwordEncoder.encode(ouvrierPassword),
                    "Mariem Khlifi", "22445566", Role.OUVRIER);
            worker.setSpecialty("Plomberie & Nettoyage");
            worker = userRepository.save(worker);
        }

        Charge paidCharge = new Charge(apt11, "Cotisation Mensuelle", "Novembre 2026",
                650.0, LocalDate.now().plusDays(10));
        paidCharge.setPaidAmountTnd(650.0);
        paidCharge.setLastPaymentMode("CARTE");
        paidCharge.refreshStatus();

        Charge unpaidCharge = new Charge(apt11, "Facture d'Eau", "Novembre 2026",
                45.0, LocalDate.now().plusDays(5));
        unpaidCharge.refreshStatus();
        chargeRepository.saveAll(List.of(paidCharge, unpaidCharge));

        if (resident != null && worker != null) {
            RepairRequest repair = new RepairRequest(
                    apt11, resident, "Plomberie", "Moyenne", "Fuite legere sous le lavabo de la cuisine"
            );
            repair.setAssignedWorker(worker);
            repair.setStatus(com.darna.model.RepairStatus.EN_COURS);
            repairRepository.save(repair);
        }

        LOG.info("Seeded Darna Web sample residence, apartments, charges, and repairs.");
    }
}
