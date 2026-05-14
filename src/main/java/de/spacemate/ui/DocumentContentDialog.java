package de.spacemate.ui;

import de.spacemate.model.Document;
import de.spacemate.model.DocumentCategory;
import de.spacemate.model.DocumentType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DocumentContentDialog {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public static void show(Document doc, Window owner) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        VBox content = buildContent(doc, dialog);
        Scene scene = new Scene(content);
        dialog.setScene(scene);
        dialog.setWidth(480);
        dialog.setHeight(520);
        dialog.centerOnScreen();
        dialog.show();
    }

    private static VBox buildContent(Document doc, Stage dialog) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-border-color: #BBBBBB; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 2, 4);");

        String accentColor = getAccentColor(doc);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(formatCategory(doc.getCategory()));
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web(accentColor));
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Button closeBtn = new Button("X");
        closeBtn.setFont(Font.font("System", FontWeight.BOLD, 12));
        closeBtn.setMinSize(28, 28);
        closeBtn.setMaxSize(28, 28);
        closeBtn.setStyle("-fx-background-color: #E0E0E0; -fx-background-radius: 14; "
                + "-fx-cursor: hand; -fx-text-fill: #555555;");
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(titleLbl, closeBtn);

        Label typeBadge = new Label(doc.getType() == DocumentType.REPORT ? "REPORT" : "CORRESPONDENCE");
        typeBadge.setFont(Font.font("System", FontWeight.BOLD, 9));
        typeBadge.setTextFill(Color.web(accentColor));
        typeBadge.setPadding(new Insets(2, 6, 2, 6));
        typeBadge.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 4; "
                + "-fx-border-color: " + accentColor + "; -fx-border-width: 1; -fx-border-radius: 4;");

        String dirStr = switch (doc.getDirection()) {
            case INBOUND -> "Inbound";
            case OUTBOUND -> "Outbound";
        };
        Label metaLbl = new Label(dirStr + "  |  " + doc.getCreatedAt().format(TIME_FMT));
        metaLbl.setFont(Font.font(10));
        metaLbl.setTextFill(Color.web("#777777"));

        HBox metaRow = new HBox(8, typeBadge, metaLbl);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #EEEEEE;");

        Text contentText = new Text(doc.getContent());
        contentText.setFont(Font.font("System", 12));
        contentText.setFill(Color.web("#1A1A1A"));

        TextFlow contentFlow = new TextFlow(contentText);
        contentFlow.setMaxWidth(440);

        VBox contentBox = new VBox(8, contentFlow);

        Map<String, String> metadata = doc.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            Region metaSep = new Region();
            metaSep.setMinHeight(1);
            metaSep.setMaxHeight(1);
            metaSep.setStyle("-fx-background-color: #EEEEEE;");
            contentBox.getChildren().add(metaSep);

            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                Text kvText = new Text(formatMetadataKey(entry.getKey()) + ": " + entry.getValue());
                kvText.setFont(Font.font("System", 11));
                kvText.setFill(Color.web("#555555"));
                TextFlow kvFlow = new TextFlow(kvText);
                contentBox.getChildren().add(kvFlow);
            }
        }

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(header, metaRow, sep, scroll);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private static String formatCategory(DocumentCategory category) {
        return switch (category) {
            case QUESTIONNAIRE -> "Questionnaire";
            case AI_LEGAL_REPORT -> "AI Legal Report";
            case AI_MEDICAL_REPORT -> "AI Medical Report";
            case AI_TRAINER_REPORT -> "AI Trainer Report";
            case MEDICAL_REPORT -> "Medical Report";
            case SPECIALIST_REPORT -> "Specialist Report";
            case TRAINING_REPORT -> "Training Report";
            case APPOINTMENT_PROPOSAL -> "Appointment Proposal";
            case APPOINTMENT_RESPONSE -> "Appointment Response";
            case INDEMNITY_AGREEMENT -> "Indemnity Agreement";
            case INDEMNITY_RESPONSE -> "Indemnity Response";
        };
    }

    private static String formatMetadataKey(String key) {
        return switch (key) {
            case "needsExtendedTraining" -> "Extended Training";
            case "appointmentId" -> "Appointment ID";
            case "outcome" -> "Outcome";
            default -> key;
        };
    }

    private static String getAccentColor(Document doc) {
        boolean isIndemnity = doc.getCategory() == DocumentCategory.INDEMNITY_AGREEMENT
                || doc.getCategory() == DocumentCategory.INDEMNITY_RESPONSE;
        if (isIndemnity) return "#C62828";
        if (doc.getType() == DocumentType.REPORT) return "#2E7D32";
        return "#37474F";
    }
}
