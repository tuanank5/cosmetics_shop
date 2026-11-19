package com.beautyshop.controller;

import com.beautyshop.service.AnhBienTheSanPham_Service;
import com.beautyshop.service.BienTheSanPham_Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
class AnhBienTheSanPham_Controller {
    private final BienTheSanPham_Service bienTheService;

    AnhBienTheSanPham_Controller(BienTheSanPham_Service bienTheService) {
        this.bienTheService = bienTheService;
    }

    @GetMapping(AnhBienTheSanPham_Service.URL_PREFIX + "{filename}")
    ResponseEntity<InputStreamResource> anh(@PathVariable String filename) throws IOException {
        var path = bienTheService.anhCongKhai(filename);
        if (path.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            var body = new InputStreamResource(Files.newInputStream(path.get(), LinkOption.NOFOLLOW_LINKS));
            MediaType mime = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok().contentType(mime).body(body);
        } catch (NoSuchFileException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
