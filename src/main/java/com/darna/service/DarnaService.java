package com.darna.service;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.RepairRequest;
import com.darna.model.RepairStatus;
import com.darna.model.Role;
import com.darna.repository.ApartmentRepository;
import com.darna.repository.AppUserRepository;
import com.darna.repository.ChargeRepository;
import com.darna.repository.RepairRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Core business service for apartments, direct rentals, co-ownership charges, and repairs.
 */
@Service
public class DarnaService {

    private static final Logger LOG = LoggerFactory.getLogger(DarnaService.class);

    private final ApartmentRepository apartmentRepository;
    private final ChargeRepository chargeRepository;
    private final RepairRequestRepository repairRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DarnaService(ApartmentRepository apartmentRepository,
                        ChargeRepository chargeRepository,
                        RepairRequestRepository repairRepository,
                        AppUserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.apartmentRepository = apartmentRepository;
        this.chargeRepository = chargeRepository;
        this.repairRepository = repairRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Apartment> filterPublicCatalog(String typology, Double maxRentTnd) {
        return apartmentRepository.findByAvailableTrue().stream()
                .filter(apt -> typology == null || typology.isBlank()
                        || "ALL".equalsIgnoreCase(typology)
                        || apt.getTypology().equalsIgnoreCase(typology.trim()))
                .filter(apt -> maxRentTnd == null || apt.getMonthlyRentTnd() <= maxRentTnd)
                .toList();
    }

    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }

    public Apartment getApartment(Long id) {
        return apartmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Appartement introuvable (id=" + id + ")"));
    }

    @Transactional
    public Apartment createApartment(String residenceName,
                                     String buildingName,
                                     String unitNumber,
                                     int floor,
                                     double surfaceM2,
                                     String typology,
                                     double monthlyRentTnd,
                                     String description) {
        Apartment apt = new Apartment(
                residenceName.trim(),
                buildingName.trim(),
                unitNumber.trim(),
                floor,
                surfaceM2,
                typology.trim(),
                monthlyRentTnd,
                description == null ? "" : description.trim(),
                true
        );
        Apartment saved = apartmentRepository.save(apt);
        LOG.info("Apartment created: id={}, location='{}'", saved.getId(), saved.getFullLocation());
        return saved;
    }

    @Transactional
    public void deleteApartment(Long id) {
        Apartment apt = getApartment(id);
        if (!apt.isAvailable()) {
            throw new BusinessException("Impossible de supprimer un appartement actuellement loue");
        }
        apartmentRepository.delete(apt);
        LOG.info("Apartment deleted: id={}", id);
    }

    /**
     * Direct visitor rental: pays 1st month, locks apartment, and creates RESIDENT account atomically.
     */
    @Transactional
    public AppUser rentApartmentAsVisitor(Long apartmentId,
                                          String fullName,
                                          String username,
                                          String phone,
                                          String rawPassword,
                                          String paymentMode) {
        Apartment apt = getApartment(apartmentId);
        if (!apt.isAvailable()) {
            throw new BusinessException("Cet appartement n'est plus disponible");
        }
        String normalizedUser = username.trim().toLowerCase();
        if (userRepository.existsByUsername(normalizedUser)) {
            throw new BusinessException("Ce nom d'utilisateur / email existe deja : " + normalizedUser);
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("Le mot de passe doit contenir au moins 6 caracteres");
        }

        apt.setAvailable(false);
        apartmentRepository.save(apt);

        AppUser resident = new AppUser(normalizedUser, passwordEncoder.encode(rawPassword),
                fullName.trim(), phone.trim(), Role.RESIDENT);
        resident.setApartmentId(apt.getId());
        AppUser savedUser = userRepository.save(resident);

        Charge firstMonth = new Charge(
                apt,
                "1er mois de loyer (" + paymentMode + ")",
                "Location directe",
                apt.getMonthlyRentTnd(),
                LocalDate.now()
        );
        firstMonth.setPaidAmountTnd(apt.getMonthlyRentTnd());
        firstMonth.setLastPaymentMode(paymentMode);
        firstMonth.refreshStatus();
        chargeRepository.save(firstMonth);

        LOG.info("Direct rental completed: apartmentId={}, resident='{}', mode='{}'",
                apt.getId(), savedUser.getUsername(), paymentMode);
        return savedUser;
    }

    public List<Charge> getChargesForRole(AppUser user) {
        if (user.getRole() == Role.OWNER) {
            return chargeRepository.findAllByOrderByDueDateDesc();
        }
        if (user.getApartmentId() == null) {
            return List.of();
        }
        return chargeRepository.findByApartmentIdOrderByDueDateDesc(user.getApartmentId());
    }

    @Transactional
    public Charge issueCharge(Long apartmentId, String label, String period, double amountTnd, LocalDate dueDate) {
        if (amountTnd <= 0) {
            throw new BusinessException("Le montant doit etre superieur a 0 TND");
        }
        Apartment apt = getApartment(apartmentId);
        Charge charge = new Charge(apt, label.trim(), period.trim(), amountTnd, dueDate);
        Charge saved = chargeRepository.save(charge);
        LOG.info("Charge issued: id={}, apartment={}, amount={} TND", saved.getId(), apt.getUnitNumber(), amountTnd);
        return saved;
    }

    @Transactional
    public Charge payCharge(Long chargeId, double paymentTnd, String paymentMode, AppUser actor) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new BusinessException("Charge introuvable (id=" + chargeId + ")"));

        if (actor.getRole() == Role.RESIDENT
                && (actor.getApartmentId() == null || !actor.getApartmentId().equals(charge.getApartment().getId()))) {
            throw new BusinessException("Acces refuse a cette charge");
        }
        if (paymentTnd <= 0) {
            throw new BusinessException("Le montant regle doit etre positif");
        }
        double remaining = charge.getRemainingAmountTnd();
        if (remaining <= 0.001) {
            throw new BusinessException("Cette charge est deja integralement payee");
        }
        if (paymentTnd > remaining + 0.01) {
            throw new BusinessException("Le paiement depasse le reste du (" + remaining + " TND)");
        }

        charge.setPaidAmountTnd(charge.getPaidAmountTnd() + paymentTnd);
        charge.setLastPaymentMode(paymentMode);
        charge.refreshStatus();
        Charge saved = chargeRepository.save(charge);
        LOG.info("Charge payment recorded: chargeId={}, amount={} TND, status={}",
                saved.getId(), paymentTnd, saved.getStatus());
        return saved;
    }

    public List<RepairRequest> getRepairsForRole(AppUser user) {
        return switch (user.getRole()) {
            case OWNER -> repairRepository.findAllByOrderByCreatedAtDesc();
            case RESIDENT -> repairRepository.findByResidentUsernameOrderByCreatedAtDesc(user.getUsername());
            case OUVRIER -> repairRepository.findByAssignedWorkerUsernameOrderByCreatedAtDesc(user.getUsername());
        };
    }

    @Transactional
    public RepairRequest createRepair(AppUser resident, String category, String priority, String description) {
        if (resident.getApartmentId() == null) {
            throw new BusinessException("Aucun appartement assigne a ce resident");
        }
        Apartment apt = getApartment(resident.getApartmentId());
        RepairRequest repair = new RepairRequest(apt, resident, category.trim(), priority.trim(), description.trim());
        RepairRequest saved = repairRepository.save(repair);
        LOG.info("Repair reported: id={}, apt={}, category={}", saved.getId(), apt.getUnitNumber(), category);
        return saved;
    }

    @Transactional
    public RepairRequest assignWorker(Long repairId, Long workerId) {
        RepairRequest repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new BusinessException("Signalement introuvable (id=" + repairId + ")"));
        AppUser worker = userRepository.findById(workerId)
                .filter(u -> u.getRole() == Role.OUVRIER)
                .orElseThrow(() -> new BusinessException("Ouvrier introuvable (id=" + workerId + ")"));

        repair.setAssignedWorker(worker);
        repair.setStatus(RepairStatus.EN_COURS);
        RepairRequest saved = repairRepository.save(repair);
        LOG.info("Repair assigned: repairId={}, worker='{}'", saved.getId(), worker.getUsername());
        return saved;
    }

    @Transactional
    public RepairRequest closeRepair(Long repairId, double costTnd, String report, AppUser worker) {
        RepairRequest repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new BusinessException("Signalement introuvable (id=" + repairId + ")"));
        if (repair.getAssignedWorker() == null
                || !repair.getAssignedWorker().getUsername().equals(worker.getUsername())) {
            throw new BusinessException("Intervention non assignee a cet ouvrier");
        }
        if (costTnd < 0) {
            throw new BusinessException("Le cout d'intervention ne peut pas etre negatif");
        }
        repair.setInterventionCostTnd(costTnd);
        repair.setClosingReport(report.trim());
        repair.setStatus(RepairStatus.TERMINEE);
        RepairRequest saved = repairRepository.save(repair);
        LOG.info("Repair closed: repairId={}, cost={} TND", saved.getId(), costTnd);
        return saved;
    }

    public List<AppUser> getWorkers() {
        return userRepository.findByRole(Role.OUVRIER);
    }

    public AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + username));
    }

    @Transactional
    public AppUser registerUser(String username, String fullName, String phone, String rawPassword, Role role) {
        String normalized = username.trim().toLowerCase();
        if (userRepository.existsByUsername(normalized)) {
            throw new BusinessException("Nom d'utilisateur deja utilise : " + normalized);
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BusinessException("Le mot de passe doit contenir au moins 6 caracteres");
        }
        AppUser user = new AppUser(normalized, passwordEncoder.encode(rawPassword),
                fullName.trim(), phone == null ? "" : phone.trim(), role);
        return userRepository.save(user);
    }
}
