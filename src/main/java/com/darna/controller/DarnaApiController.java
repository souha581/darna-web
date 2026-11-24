package com.darna.controller;

import com.darna.model.Apartment;
import com.darna.model.AppUser;
import com.darna.model.Charge;
import com.darna.model.RepairRequest;
import com.darna.service.BusinessException;
import com.darna.service.DarnaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for Darna Web (catalog, apartments, charges, repairs).
 */
@RestController
@RequestMapping("/api")
public class DarnaApiController {

    private final DarnaService darnaService;

    public DarnaApiController(DarnaService darnaService) {
        this.darnaService = darnaService;
    }

    @GetMapping("/apartments")
    public List<Apartment> getPublicApartments(@RequestParam(required = false) String typology,
                                               @RequestParam(required = false) Double maxRent) {
        return darnaService.filterPublicCatalog(typology, maxRent);
    }

    @PostMapping("/apartments")
    public ResponseEntity<Apartment> createApartment(@Valid @RequestBody CreateApartmentRequest req) {
        Apartment created = darnaService.createApartment(
                req.residenceName(), req.buildingName(), req.unitNumber(),
                req.floor(), req.surfaceM2(), req.typology(),
                req.monthlyRentTnd(), req.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/charges")
    public List<Charge> getCharges(Authentication authentication) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        return darnaService.getChargesForRole(user);
    }

    @PostMapping("/charges/{id}/pay")
    public Charge payCharge(@PathVariable Long id,
                            @Valid @RequestBody PayChargeRequest req,
                            Authentication authentication) {
        AppUser actor = darnaService.getUserByUsername(authentication.getName());
        return darnaService.payCharge(id, req.amountTnd(), req.paymentMode(), actor);
    }

    @GetMapping("/repairs")
    public List<RepairRequest> getRepairs(Authentication authentication) {
        AppUser user = darnaService.getUserByUsername(authentication.getName());
        return darnaService.getRepairsForRole(user);
    }

    @PostMapping("/repairs")
    public ResponseEntity<RepairRequest> reportRepair(@Valid @RequestBody CreateRepairRequest req,
                                                      Authentication authentication) {
        AppUser resident = darnaService.getUserByUsername(authentication.getName());
        RepairRequest created = darnaService.createRepair(resident, req.category(), req.priority(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    public record CreateApartmentRequest(
            @NotBlank String residenceName,
            @NotBlank String buildingName,
            @NotBlank String unitNumber,
            @Min(0) int floor,
            @Min(10) double surfaceM2,
            @NotBlank String typology,
            @Min(1) double monthlyRentTnd,
            String description
    ) {
    }

    public record PayChargeRequest(
            @NotNull Double amountTnd,
            @NotBlank String paymentMode
    ) {
    }

    public record CreateRepairRequest(
            @NotBlank String category,
            @NotBlank String priority,
            @NotBlank String description
    ) {
    }

    public record IssueChargeRequest(
            @NotNull Long apartmentId,
            @NotBlank String label,
            @NotBlank String period,
            @Min(1) double amountTnd,
            @NotNull LocalDate dueDate
    ) {
    }
}
