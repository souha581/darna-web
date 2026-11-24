package com.darna.controller;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.RepairRequest;
import com.darna.model.Role;
import com.darna.service.BusinessException;
import com.darna.service.DarnaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Web UI controller covering Visitor Catalog, Auth, Dashboard, Apartments, Charges, and Repairs.
 */
@Controller
public class DarnaWebController {

    private final DarnaService darnaService;

    public DarnaWebController(DarnaService darnaService) {
        this.darnaService = darnaService;
    }

    @GetMapping({"/", "/catalog"})
    public String publicCatalog(@RequestParam(required = false) String typology,
                                @RequestParam(required = false) Double maxRent,
                                Authentication authentication,
                                Model model) {
        List<Apartment> available = darnaService.filterPublicCatalog(typology, maxRent);
        model.addAttribute("apartments", available);
        model.addAttribute("selectedTypology", typology == null ? "ALL" : typology);
        model.addAttribute("maxRent", maxRent);
        model.addAttribute("isAuthenticated", authentication != null && authentication.isAuthenticated());
        return "catalog";
    }

    @PostMapping("/rent")
    public String rentApartment(@RequestParam Long apartmentId,
                                @RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String phone,
                                @RequestParam String password,
                                @RequestParam String paymentMode,
                                RedirectAttributes redirectAttributes) {
        try {
            darnaService.rentApartmentAsVisitor(apartmentId, fullName, username, phone, password, paymentMode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Location confirmee ! Votre compte resident (" + username + ") est actif. Connectez-vous.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/catalog";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String username,
                           @RequestParam(required = false) String phone,
                           @RequestParam Role role,
                           @RequestParam String password,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            darnaService.registerUser(username, fullName, phone, password, role);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Compte cree avec succes. Vous pouvez maintenant vous connecter.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        List<Apartment> allApartments = darnaService.getAllApartments();
        List<Charge> charges = darnaService.getChargesForRole(user);
        List<RepairRequest> repairs = darnaService.getRepairsForRole(user);

        double totalRecovered = charges.stream().mapToDouble(Charge::getPaidAmountTnd).sum();
        double totalRemaining = charges.stream().mapToDouble(Charge::getRemainingAmountTnd).sum();
        long occupiedCount = allApartments.stream().filter(a -> !a.isAvailable()).count();

        model.addAttribute("user", user);
        model.addAttribute("totalApartments", allApartments.size());
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("totalRecovered", String.format("%.2f", totalRecovered));
        model.addAttribute("totalRemaining", String.format("%.2f", totalRemaining));
        model.addAttribute("repairsCount", repairs.size());
        model.addAttribute("chargesCount", charges.size());
        return "dashboard";
    }

    @GetMapping("/apartments")
    public String listApartments(Authentication authentication, Model model) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("apartments", darnaService.getAllApartments());
        return "apartments";
    }

    @PostMapping("/apartments/add")
    public String addApartment(@RequestParam String residenceName,
                               @RequestParam String buildingName,
                               @RequestParam String unitNumber,
                               @RequestParam int floor,
                               @RequestParam double surfaceM2,
                               @RequestParam String typology,
                               @RequestParam double monthlyRentTnd,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        try {
            darnaService.createApartment(residenceName, buildingName, unitNumber,
                    floor, surfaceM2, typology, monthlyRentTnd, description);
            redirectAttributes.addFlashAttribute("successMessage", "Appartement ajoute au patrimoine.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/apartments";
    }

    @PostMapping("/apartments/{id}/delete")
    public String deleteApartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            darnaService.deleteApartment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appartement supprime.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/apartments";
    }

    @GetMapping("/charges")
    public String listCharges(Authentication authentication, Model model) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("charges", darnaService.getChargesForRole(user));
        model.addAttribute("apartments", darnaService.getAllApartments());
        return "charges";
    }

    @PostMapping("/charges/issue")
    public String issueCharge(@RequestParam Long apartmentId,
                              @RequestParam String label,
                              @RequestParam String period,
                              @RequestParam double amountTnd,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                              RedirectAttributes redirectAttributes) {
        try {
            darnaService.issueCharge(apartmentId, label, period, amountTnd, dueDate);
            redirectAttributes.addFlashAttribute("successMessage", "Charge emise avec succes.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/charges";
    }

    @PostMapping("/charges/{id}/pay")
    public String payCharge(@PathVariable Long id,
                            @RequestParam double amountTnd,
                            @RequestParam String paymentMode,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        AppUser actor = darnaService.getUserByUsername(authentication.getName());
        try {
            darnaService.payCharge(id, amountTnd, paymentMode, actor);
            redirectAttributes.addFlashAttribute("successMessage", "Paiement enregistre avec succes.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/charges";
    }

    @GetMapping("/repairs")
    public String listRepairs(Authentication authentication, Model model) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("repairs", darnaService.getRepairsForRole(user));
        model.addAttribute("workers", darnaService.getWorkers());
        return "repairs";
    }

    @PostMapping("/repairs/report")
    public String reportRepair(@RequestParam String category,
                               @RequestParam String priority,
                               @RequestParam String description,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        AppUser resident = darnaService.getUserByUsername(authentication.getName());
        try {
            darnaService.createRepair(resident, category, priority, description);
            redirectAttributes.addFlashAttribute("successMessage", "Incident signale au proprietaire.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/repairs";
    }

    @PostMapping("/repairs/assign")
    public String assignRepair(@RequestParam Long repairId,
                               @RequestParam Long workerId,
                               RedirectAttributes redirectAttributes) {
        try {
            darnaService.assignWorker(repairId, workerId);
            redirectAttributes.addFlashAttribute("successMessage", "Ouvrier affecte a l'intervention.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/repairs";
    }

    @PostMapping("/repairs/close")
    public String closeRepair(@RequestParam Long repairId,
                              @RequestParam double costTnd,
                              @RequestParam String report,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        AppUser worker = darnaService.getUserByUsername(authentication.getName());
        try {
            darnaService.closeRepair(repairId, costTnd, report, worker);
            redirectAttributes.addFlashAttribute("successMessage", "Intervention cloturee.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/repairs";
    }
}
