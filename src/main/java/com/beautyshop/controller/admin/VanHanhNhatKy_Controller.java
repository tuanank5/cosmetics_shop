package com.beautyshop.controller.admin;

import com.beautyshop.service.DonNhatKy_Service;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/operations/audit")
public class VanHanhNhatKy_Controller {
    private final DonNhatKy_Service service;
    public VanHanhNhatKy_Controller(DonNhatKy_Service service) { this.service = service; }

    @PostMapping("/retention")
    public DonNhatKy_Service.KetQua chay(@RequestParam UUID correlation,
            @RequestParam(defaultValue="true") boolean dryRun, @RequestParam(defaultValue="180") int days,
            @RequestParam(defaultValue="100") int batch) {
        try {
            return service.chay(correlation, dryRun, days, batch);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }
}
