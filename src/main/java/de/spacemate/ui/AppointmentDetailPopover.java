package de.spacemate.ui;

import de.spacemate.model.*;
import de.spacemate.orchestration.OnboardingOrchestrator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AppointmentDetailPopover {

    private final OnboardingOrchestrator orchestrator;
    private final Popup popup = new Popup();
    private final Runnable onChanged;

    public AppointmentDetailPopover(OnboardingOrchestrator orchestrator, Runnable onChanged) {
        this.orchestrator = orchestrator;
        this.onChanged = onChanged;
        popup.setAutoHide(true);
    }

    public void setOnHidden(Runnable callback) {
        popup.setOnHidden(e -> callback.run());
    }

    public void show(Appointment appointment, Node anchor) {
        VBox content = buildContent(appointment);
        popup.getContent().setAll(content);

        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        double x = bounds.getMaxX() + 8;
        double y = bounds.getMinY();
        popup.show(anchor.getScene().getWindow(), x, y);
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    private VBox buildContent(Appointment appointment) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setMinWidth(260);
        box.setMaxWidth(300);
        box.setStyle("-fx-background-color: white; -fx-border-color: #BBBBBB; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 2, 2);");

        // Header
        Label header = new Label(appointment.getType().displayName());
        header.setFont(Font.font("System", FontWeight.BOLD, 13));
        header.setTextFill(Color.web("#1A1A1A"));

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm");
        String timeRange = appointment.getScheduledAt().format(timeFmt)
                + " – " + appointment.getTimeSlot().getEnd().format(DateTimeFormatter.ofPattern("HH:mm"));
        Label timeLbl = new Label(timeRange);
        timeLbl.setFont(Font.font(11));
        timeLbl.setTextFill(Color.web("#555555"));

        box.getChildren().addAll(header, timeLbl, separator());

        // Customer
        box.getChildren().add(detailRow("Customer", appointment.getCustomer().getFullName()));

        // Staff
        box.getChildren().add(detailRow("Staff", appointment.getConductor().getName()));

        box.getChildren().add(separator());

        // Resources
        List<ResourceAssignment> assignments = orchestrator.getResourceAssignments(appointment.getId());
        List<ResourceRequirement> requirements = orchestrator.getResourceRequirements(appointment.getId());

        ResourceAssignment roomAssignment = assignments.stream()
                .filter(a -> a.getResource().getCategory() == ResourceCategory.ROOM)
                .findFirst().orElse(null);

        boolean needsRoom = requirements.stream()
                .anyMatch(r -> r.category() == ResourceCategory.ROOM);

        if (needsRoom) {
            box.getChildren().add(resourceRow(appointment, roomAssignment,
                    ResourceCategory.ROOM, getRoomTag(requirements)));
        }

        List<ResourceRequirement> equipReqs = requirements.stream()
                .filter(r -> r.category() == ResourceCategory.EQUIPMENT)
                .toList();

        for (ResourceRequirement eqReq : equipReqs) {
            ResourceAssignment eqAssignment = assignments.stream()
                    .filter(a -> a.getResource().getCategory() == ResourceCategory.EQUIPMENT
                            && (eqReq.tag() == null || eqReq.tag().equals(a.getResource().getTag())))
                    .findFirst().orElse(null);
            box.getChildren().add(resourceRow(appointment, eqAssignment,
                    ResourceCategory.EQUIPMENT, eqReq.tag()));
        }

        // Documents section
        box.getChildren().add(separator());
        Label docsHeader = new Label("Documents");
        docsHeader.setFont(Font.font("System", FontWeight.BOLD, 11));
        docsHeader.setTextFill(Color.web("#777777"));
        box.getChildren().add(docsHeader);

        VBox docsContainer = new VBox(4);
        rebuildDocumentRows(appointment, docsContainer);
        box.getChildren().add(docsContainer);

        // Action buttons
        if (appointment.getStatus() == AppointmentStatus.SUGGESTED) {
            box.getChildren().add(separator());
            HBox buttons = new HBox(8);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            Button confirm = new Button("Send");
            confirm.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 4;");
            confirm.setOnAction(e -> {
                orchestrator.sendProposalToCustomer(appointment.getCustomerId(), appointment.getId());
                popup.hide();
                onChanged.run();
            });

            Button cancel = new Button("Cancel");
            cancel.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 4;");
            cancel.setOnAction(e -> {
                orchestrator.discardSuggestion(appointment.getCustomerId(), appointment.getId());
                popup.hide();
                onChanged.run();
            });

            buttons.getChildren().addAll(confirm, cancel);
            box.getChildren().add(buttons);
        } else if (appointment.getStatus() == AppointmentStatus.SENT) {
            box.getChildren().add(separator());
            HBox buttons = new HBox(8);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            Button reminder = new Button("Send Reminder");
            reminder.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 4;");
            reminder.setOnAction(e -> {
                orchestrator.sendReminder(appointment.getCustomerId(), appointment.getId());
                popup.hide();
                onChanged.run();
            });

            Button pmCancel = new Button("PM Cancel");
            pmCancel.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-background-radius: 4;");
            pmCancel.setOnAction(e -> {
                orchestrator.cancelScheduledAppointment(appointment.getCustomerId(), appointment.getId());
                popup.hide();
                onChanged.run();
            });

            buttons.getChildren().addAll(reminder, pmCancel);
            box.getChildren().add(buttons);
        } else if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            box.getChildren().add(separator());
            Button pmCancel = new Button("PM Cancel");
            pmCancel.setMaxWidth(Double.MAX_VALUE);
            pmCancel.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-background-radius: 4;");
            pmCancel.setOnAction(e -> {
                orchestrator.cancelScheduledAppointment(appointment.getCustomerId(), appointment.getId());
                popup.hide();
                onChanged.run();
            });
            box.getChildren().add(pmCancel);
        }

        return box;
    }

    // -------------------------------------------------------------------------
    // Document attachment UI
    // -------------------------------------------------------------------------

    private void rebuildDocumentRows(Appointment appointment, VBox docsContainer) {
        docsContainer.getChildren().clear();

        List<DocumentAttachment> attachments = orchestrator.getDocumentAttachments(appointment.getId());
        List<Document> customerDocs = orchestrator.getDocumentsForCustomer(appointment.getCustomerId());
        Map<UUID, Document> docMap = customerDocs.stream()
                .collect(Collectors.toMap(Document::getId, d -> d, (a, b) -> a));

        for (DocumentAttachment att : attachments) {
            Document doc = docMap.get(att.getDocumentId());
            if (doc == null) continue;

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);

            Label docLabel = new Label(formatCategory(doc.getCategory()));
            docLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            docLabel.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #1B5E20; "
                    + "-fx-padding: 2 6; -fx-background-radius: 3;");

            Button removeBtn = new Button("x");
            removeBtn.setMinSize(18, 18);
            removeBtn.setMaxSize(18, 18);
            removeBtn.setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; "
                    + "-fx-background-radius: 9; -fx-font-size: 9; -fx-font-weight: bold; "
                    + "-fx-cursor: hand; -fx-padding: 0;");
            removeBtn.setOnAction(e -> {
                orchestrator.detachDocumentFromAppointment(appointment.getId(), att.getDocumentId());
                rebuildDocumentRows(appointment, docsContainer);
            });

            row.getChildren().addAll(docLabel, removeBtn);
            docsContainer.getChildren().add(row);
        }

        Button addBtn = new Button("+");
        addBtn.setMinSize(24, 24);
        addBtn.setMaxSize(24, 24);
        addBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #555555; "
                + "-fx-background-radius: 12; -fx-font-size: 14; -fx-font-weight: bold; "
                + "-fx-cursor: hand; -fx-padding: 0;");
        addBtn.setOnAction(e -> showDocumentDropdown(appointment, docsContainer, addBtn));
        docsContainer.getChildren().add(addBtn);
    }

    private void showDocumentDropdown(Appointment appointment, VBox docsContainer, Button addBtn) {
        List<Document> available = orchestrator.getAttachableDocuments(
                appointment.getCustomerId(), appointment.getId());

        if (available.isEmpty()) return;

        ComboBox<Document> combo = new ComboBox<>();
        combo.getItems().addAll(available);
        combo.setPromptText("Select document...");
        combo.setPrefWidth(180);
        combo.setStyle("-fx-font-size: 11;");

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Document d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : formatCategory(d.getCategory()));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Document d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : formatCategory(d.getCategory()));
            }
        });

        int idx = docsContainer.getChildren().indexOf(addBtn);
        docsContainer.getChildren().set(idx, combo);

        popup.setAutoHide(false);

        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                popup.setAutoHide(true);
            }
        });

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                orchestrator.attachDocumentToAppointment(appointment.getId(), newVal.getId());
                rebuildDocumentRows(appointment, docsContainer);
            }
        });

        javafx.application.Platform.runLater(combo::show);
    }

    // -------------------------------------------------------------------------
    // Resource UI (existing)
    // -------------------------------------------------------------------------

    private HBox detailRow(String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#777777"));
        lbl.setMinWidth(70);

        Label val = new Label(value);
        val.setFont(Font.font(11));
        val.setTextFill(Color.web("#1A1A1A"));

        HBox row = new HBox(8, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox resourceRow(Appointment appointment, ResourceAssignment current,
                              ResourceCategory category, String tag) {
        String label;
        if (category == ResourceCategory.ROOM) {
            label = "Room";
        } else if ("VR_HEADSET".equals(tag)) {
            label = "VR";
        } else if ("TRANSLATION_HEADPHONE".equals(tag)) {
            label = "Headphone";
        } else {
            label = "Equipment";
        }

        Label lbl = new Label(label + ":");
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#777777"));
        lbl.setMinWidth(70);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(lbl);

        if (current != null) {
            Button resourceBtn = new Button(current.getResource().getName());
            resourceBtn.setStyle("-fx-background-color: #E8EAF6; -fx-text-fill: #3949AB; "
                    + "-fx-background-radius: 4; -fx-cursor: hand;");
            resourceBtn.setFont(Font.font(11));
            resourceBtn.setOnAction(e -> showResourceDropdown(appointment, category, tag, row, resourceBtn));
            row.getChildren().add(resourceBtn);
        } else {
            Button addBtn = new Button("+");
            addBtn.setMinSize(24, 24);
            addBtn.setMaxSize(24, 24);
            addBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #555555; "
                    + "-fx-background-radius: 12; -fx-font-size: 14; -fx-font-weight: bold; "
                    + "-fx-cursor: hand; -fx-padding: 0;");
            addBtn.setOnAction(e -> showResourceDropdown(appointment, category, tag, row, addBtn));
            row.getChildren().add(addBtn);
        }

        return row;
    }

    private void showResourceDropdown(Appointment appointment, ResourceCategory category,
                                       String tag, HBox row, Node replaceNode) {
        List<Resource> available = orchestrator.getAvailableResourcesForAppointment(
                appointment.getId(), category, tag);

        ComboBox<Resource> combo = new ComboBox<>();
        combo.getItems().addAll(available);
        combo.setPromptText("Select...");
        combo.setPrefWidth(160);
        combo.setStyle("-fx-font-size: 11;");
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Resource r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getName());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Resource r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getName());
            }
        });

        int idx = row.getChildren().indexOf(replaceNode);
        row.getChildren().set(idx, combo);

        popup.setAutoHide(false);

        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                popup.setAutoHide(true);
            }
        });

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                orchestrator.assignResource(appointment.getId(), newVal.getId());
                popup.hide();
                onChanged.run();
            }
        });

        javafx.application.Platform.runLater(combo::show);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getRoomTag(List<ResourceRequirement> requirements) {
        return requirements.stream()
                .filter(r -> r.category() == ResourceCategory.ROOM)
                .findFirst()
                .map(ResourceRequirement::tag)
                .orElse(null);
    }

    private String formatCategory(DocumentCategory category) {
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

    private Region separator() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #EEEEEE;");
        return sep;
    }
}
