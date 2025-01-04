package com.javaintellij.frontend;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class HelloController {

    @FXML
    private TextField userInput;

    @FXML
    private VBox chatBox;

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private Label pdfStatusLabel;


    @FXML
    public void handleSendMessage() {
        String message = userInput.getText();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // Ajouter le message de l'utilisateur à la boîte de chat
        addUserMessage(message);  // true pour l'utilisateur, false pour erreur

        // Appeler le service de chat et obtenir la réponse du bot
        fetchApiData(message);

        // Effacer le champ de saisie
        userInput.clear();

        // Faire défiler vers le bas
        chatScrollPane.layout();
        chatScrollPane.setVvalue(1.0);
    }

    public void addUserMessage(String message) {
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("user-message");

        HBox messageContainer = new HBox(messageLabel);
        messageContainer.setAlignment(Pos.BASELINE_LEFT); // Align user message to the left
        chatBox.getChildren().add(messageContainer);
        animateMessageAddition(messageContainer);
    }

    private void animateMessageAddition(HBox messageContainer, Label messageLabel, String message) {
        // Effet de fade-in pour le container
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), messageContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Animation de texte caractère par caractère
        Timeline textAnimation = new Timeline();
        final StringBuilder displayedText = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(80 * (i + 1)), // Intervalle entre chaque caractère
                    event -> {
                        displayedText.append(message.charAt(index)); // Ajouter un caractère au texte affiché
                        messageLabel.setText(displayedText.toString()); // Mettre à jour le Label dans l'UI
                    }
            );
            textAnimation.getKeyFrames().add(keyFrame);
        }

        textAnimation.play(); // Démarrer l'animation de texte
    }

    public void addBotMessage(String message, Boolean error) {
        Label messageLabel = new Label();
        messageLabel.setWrapText(true); // Permet de gérer les textes longs

        if (error) {
            messageLabel.getStyleClass().add("error-message");
        } else {
            messageLabel.getStyleClass().add("bot-message");
        }

        HBox messageContainer = new HBox(messageLabel);
        messageContainer.setAlignment(Pos.BASELINE_RIGHT); // Align bot message to the right
        chatBox.getChildren().add(messageContainer);

        animateMessageAddition(messageContainer, messageLabel, message); // Ajout de l'animation personnalisée
    }



    private void animateMessageAddition(HBox messageContainer) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), messageContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }


    private void fetchApiData(String message) {
        // Créer un nouveau Task pour effectuer la requête HTTP sur un thread d'arrière-plan
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    // Encodage du message pour éviter des problèmes avec les caractères spéciaux
                    String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");

                    // URL de l'API avec le paramètre "message"
                    String apiUrl = "http://localhost:9090/chat/ask?message=" + encodedMessage;

                    // Appel à l'API avec une requête GET
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(apiUrl))
                            .header("Accept", "application/json") // Assurez-vous que l'API accepte ce format
                            .build();

                    // Envoi de la requête et récupération de la réponse
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // Vérifier si la réponse est réussie (code HTTP 200)
                    if (response.statusCode() == 200) {
                        return response.body(); // Retourner la réponse du chatbot
                    } else {
                        // Si la réponse est un code d'erreur, retourner un message d'erreur
                        return "Error: " + response.statusCode() + " - " + response.body();
                    }

                } catch (IOException e) {
                    // Gestion des erreurs si la requête échoue
                    return "Error: Unable to fetch response from the server.";
                }
            }
        };

        // Lorsque la tâche est terminée, ajoutez la réponse du bot à la boîte de chat
        task.setOnSucceeded(e -> {
            String response = task.getValue();
            System.out.println("Response: " + response); // Debug
            addBotMessage(response, response.startsWith("Error:"));
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                System.out.println("Task Exception: " + exception.getMessage());
            }
            addBotMessage("Bot: Error fetching response", true);
        });

        // Exécuter la tâche sur un nouveau thread
        new Thread(task).start();
    }

    public void handleAddPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            // Mettre à jour le statut avant de commencer l'upload
            pdfStatusLabel.setText("Uploading PDF: " + selectedFile.getName());

            // Lancer la tâche d'upload du fichier
            uploadPDF(selectedFile);
        } else {
            pdfStatusLabel.setText("No PDF uploaded");
        }
    }

    private void uploadPDF(File file) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    // Créer une requête HTTP POST
                    HttpPost httpPost = new HttpPost("http://localhost:9090/pdf/upload");

                    // Construire l'entité multipart avec le fichier
                    HttpEntity multipartEntity = MultipartEntityBuilder.create()
                            .addBinaryBody("file", file, ContentType.APPLICATION_PDF, file.getName()) // Champ "file"
                            .build();

                    httpPost.setEntity(multipartEntity);

                    // Envoyer la requête
                    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                        int statusCode = response.getCode();
                        String responseMessage = response.getEntity() != null
                                ? new String(response.getEntity().getContent().readAllBytes())
                                : "";

                        if (statusCode == 200) {
                            return "PDF uploaded successfully: " + file.getName();
                        } else {
                            return "Error uploading PDF (HTTP " + statusCode + "): " + responseMessage;
                        }
                    }
                } catch (Exception e) {
                    return "Error: Unable to upload the PDF. " + e.getMessage();
                }
            }
        };

        // Mise à jour du statut sur le thread principal lorsque la tâche réussit
        task.setOnSucceeded(e -> pdfStatusLabel.setText(task.getValue()));

        // Gérer les erreurs sur le thread principal
        task.setOnFailed(e -> pdfStatusLabel.setText("Error: Failed to upload the PDF."));

        // Exécuter la tâche dans un thread séparé
        new Thread(task).start();
    }
}
