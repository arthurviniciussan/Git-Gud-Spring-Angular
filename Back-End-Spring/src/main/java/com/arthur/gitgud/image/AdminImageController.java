package com.arthur.gitgud.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload das imagens do artigo. Exige ROLE_ADMIN, como todo /api/admin/**. */
@RestController
@RequestMapping("/api/admin/images")
public class AdminImageController {

    private final ImageService imageService;

    public AdminImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImagemArmazenada upload(@RequestParam("arquivo") MultipartFile arquivo) {
        return imageService.armazenar(arquivo);
    }
}
