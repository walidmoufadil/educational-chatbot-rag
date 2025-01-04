package com.javaintellij.backend.web;

import com.javaintellij.backend.Services.FileProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/pdf")
public class PDFUploadController {

    private final FileProcessingService fileProcessingService;

    public PDFUploadController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    /**
     * API POST pour traiter et stocker un fichier PDF.
     * @param file Le fichier PDF à uploader.
     * @return Une réponse indiquant le succès ou l'échec.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndProcessPDF(@RequestParam("file") MultipartFile file) {
        try {
            // Appeler le service pour traiter le fichier
            fileProcessingService.processAndStoreFile(file);

            // Réponse de succès
            return ResponseEntity.ok("Fichier traité et stocké avec succès.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur inattendue : " + e.getMessage());
        }
    }
}
