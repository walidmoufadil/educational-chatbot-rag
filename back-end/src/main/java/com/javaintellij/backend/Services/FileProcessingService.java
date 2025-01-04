package com.javaintellij.backend.Services;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class FileProcessingService {

    private final VectorStore vectorStore;

    public FileProcessingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Traite un fichier PDF et enregistre son contenu dans le vector store.
     * @param file Le fichier PDF à traiter.
     * @throws IOException Si une erreur survient lors du traitement.
     */
    public void processAndStoreFile(MultipartFile file) throws IOException {
        // Valider le fichier
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne peut pas être vide.");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés.");
        }

        // Créer un fichier temporaire
        File tempFile = File.createTempFile("uploaded-", ".pdf");
        tempFile.deleteOnExit(); // Supprime le fichier automatiquement à la fin de l'exécution

        try {
            // Sauvegarder le contenu du MultipartFile dans le fichier temporaire
            file.transferTo(tempFile);

            // Charger le fichier temporaire comme une ressource
            Resource fileResource = new FileSystemResource(tempFile);

            // Lire le fichier PDF avec PagePdfDocumentReader
            PagePdfDocumentReader pdfDocumentReader = new PagePdfDocumentReader(fileResource);
            List<Document> documents = pdfDocumentReader.get();

            // Diviser le contenu en morceaux
            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.split(documents);

            // Ajouter les morceaux au Vector Store
            vectorStore.add(chunks);
        } catch (IOException e) {
            throw new IOException("Erreur lors de la lecture du fichier : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du traitement du fichier PDF : " + e.getMessage(), e);
        }
    }
}
