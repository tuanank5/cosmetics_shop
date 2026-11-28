package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.repository.TraCuuNhatKy_Repository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/audit")
public class NhatKyQuanTri_Controller {
    private static final String PATH = "/admin/audit";
    private final TraCuuNhatKy_Repository repository;
    public NhatKyQuanTri_Controller(TraCuuNhatKy_Repository repository) { this.repository = repository; }

    private Map<String,String> loc(Map<String,String> request) {
        var loc = new LinkedHashMap<String,String>();
        for (String key : List.of("q","actor","type","result","date","sort")) {
            String v = request.getOrDefault(key,"").strip();
            if (v.length() > 80 || (key.equals("actor") && !v.matches("[0-9]{0,18}"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
            if (!v.isEmpty()) loc.put(key,v);
        }
        try {
            if (loc.containsKey("date")) {
                int year = LocalDate.parse(loc.get("date")).getYear();
                if (year < 1000 || year > 9999) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (DateTimeParseException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        return loc;
    }

    @GetMapping
    public String list(@RequestParam Map<String,String> request, Model model) {
        var loc = loc(request);
        var page = repository.trang(loc, Optional.ofNullable(NguonDieuHuong.trangNguon(request,"page")).orElse(0));
        model.addAttribute("rows",page.getContent());
        loc.put("page",String.valueOf(page.getNumber()));
        model.addAttribute("loc",loc);
        model.addAttribute("query",NguonDieuHuong.coThamSo("",loc));
        model.addAttribute("pager",PhanTrang.tu(page,n -> NguonDieuHuong.trangDanhSach(PATH,loc,n)));
        return "admin/audit/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, @RequestParam Map<String,String> request, Model model) {
        model.addAttribute("row",repository.chiTiet(id));
        model.addAttribute("back",NguonDieuHuong.trangDanhSach(PATH,loc(request),
            Optional.ofNullable(NguonDieuHuong.trangNguon(request,"page")).orElse(0))+"#audit-"+id);
        return "admin/audit/detail";
    }
}
