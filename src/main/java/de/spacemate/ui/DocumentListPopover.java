package de.spacemate.ui;

import de.spacemate.model.Document;
import de.spacemate.model.DocumentCategory;
import de.spacemate.model.DocumentType;
import de.spacemate.orchestration.OnboardingOrchestrator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Rectangle2D;
import javafx.stage.Popup;
import javafx.stage.Screen;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DocumentListPopover {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static final double POPUP_WIDTH = 380;
    private static final double POPUP_MAX_HEIGHT = 440;

    private final OnboardingOrchestrator orchestrator;
    private final Popup popup = new Popup();

    public DocumentListPopover(OnboardingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        popup.setAutoHide(true);
    }

    public void show(UUID customerId, String customerName, Node anchor) {
        VBox content = buildContent(customerId, customerName);
        popup.getContent().setAll(content);

        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        double x = bounds.getMinX() + bounds.getWidth() / 2 - POPUP_WIDTH / 2;
        double y = bounds.getMaxY() + 4;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        x = Math.max(screenBounds.getMinX() + 8, Math.min(x, screenBounds.getMaxX() - POPUP_WIDTH - 8));
        y = Math.min(y, screenBounds.getMaxY() - POPUP_MAX_HEIGHT - 8);

        popup.show(anchor.getScene().getWindow(), x, y);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    private VBox buildContent(UUID customerId, String customerName) {
        Label header = new Label("Documents — " + customerName);
        header.setFont(Font.font("System", FontWeight.BOLD, 13));
        header.setTextFill(Color.web("#1A1A1A"));

        VBox listBox = new VBox(6);

        List<Document> documents = orchestrator.getDocumentsForCustomer(customerId);
        documents = documents.stream()
                .sorted(Comparator.comparing(Document::getCreatedAt).reversed())
                .toList();

        List<Document> reports = documents.stream()
                .filter(d -> d.getType() == DocumentType.REPORT)
                .toList();
        List<Document> correspondence = documents.stream()
                .filter(d -> d.getType() == DocumentType.CORRESPONDENCE)
                .toList();

        if (reports.isEmpty() && correspondence.isEmpty()) {
            Label empty = new Label("No documents yet.");
            empty.setFont(Font.font(11));
            empty.setTextFill(Color.web("#999999"));
            listBox.getChildren().add(empty);
        }

        if (!reports.isEmpty()) {
            listBox.getChildren().add(sectionHeader("Reports", "#1B5E20"));
            for (Document doc : reports) {
                listBox.getChildren().add(documentRow(doc, "#F1F8E9", "#2E7D32", "#1B5E20"));
            }
        }

        if (!correspondence.isEmpty()) {
            if (!reports.isEmpty()) {
                listBox.getChildren().add(separator());
            }
            listBox.getChildren().add(sectionHeader("Correspondence", "#263238"));
            for (Document doc : correspondence) {
                boolean isIndemnity = doc.getCategory() == DocumentCategory.INDEMNITY_AGREEMENT
                        || doc.getCategory() == DocumentCategory.INDEMNITY_RESPONSE;
                if (isIndemnity) {
                    listBox.getChildren().add(documentRow(doc, "#FFEBEE", "#C62828", "#8E0000"));
                } else {
                    listBox.getChildren().add(documentRow(doc, "#F5F5F5", "#37474F", "#263238"));
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(360);
        scrollPane.setFocusTraversable(false);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(8, header, separator(), scrollPane);
        container.setPadding(new Insets(12));
        container.setMinWidth(POPUP_WIDTH);
        container.setMaxWidth(POPUP_WIDTH);
        container.setMaxHeight(POPUP_MAX_HEIGHT);
        container.setStyle("-fx-background-color: white; -fx-border-color: #BBBBBB; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 2, 2);");
        return container;
    }

    private HBox documentRow(Document doc, String bgColor, String accentColor, String darkAccent) {
        String directionIcon = switch (doc.getDirection()) {
            case INBOUND -> "←";
            case OUTBOUND -> "→";
        };

        Label icon = new Label(directionIcon);
        icon.setFont(Font.font("System", FontWeight.BOLD, 14));
        icon.setStyle("-fx-text-fill: " + darkAccent + ";");
        icon.setMinWidth(16);

        Label categoryLbl = new Label(formatCategory(doc.getCategory()));
        categoryLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        categoryLbl.setStyle("-fx-text-fill: " + darkAccent + ";");

        Label timeLbl = new Label(doc.getCreatedAt().format(TIME_FMT));
        timeLbl.setFont(Font.font(11));
        timeLbl.setStyle("-fx-text-fill: #333333;");

        VBox info = new VBox(1, categoryLbl, timeLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.setFont(Font.font("System", FontWeight.BOLD, 10));
        viewBtn.setFocusTraversable(false);
        viewBtn.setStyle("-fx-background-color: white; -fx-text-fill: " + accentColor + "; "
                + "-fx-border-color: " + accentColor + "; -fx-border-width: 1; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> showDocumentContent(doc));

        HBox row = new HBox(8, icon, info, viewBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 4;");
        return row;
    }

    private void showDocumentContent(Document doc) {
        DocumentContentDialog.show(doc, popup.getOwnerWindow());
    }

    private Label sectionHeader(String text, String color) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(color));
        lbl.setPadding(new Insets(2, 0, 2, 0));
        return lbl;
    }

    private Region separator() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #EEEEEE;");
        return sep;
    }

    private String formatCategory(DocumentCategory category) {
        return category.displayName();
    }
}
