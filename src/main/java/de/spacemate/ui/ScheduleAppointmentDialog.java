package de.spacemate.ui;

import de.spacemate.model.Staff;
import de.spacemate.model.TimeSlot;
import de.spacemate.repository.StaffRepository;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ScheduleAppointmentDialog extends Dialog<UUID[]> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE dd MMM yyyy HH:mm");

    public ScheduleAppointmentDialog(List<TimeSlot> availableSlots, StaffRepository staffRepository) {
        setTitle("Schedule Appointment");
        setHeaderText("Select an available time slot");

        ButtonType bookButtonType = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(bookButtonType, ButtonType.CANCEL);

        ListView<TimeSlot> slotList = new ListView<>();
        slotList.getItems().addAll(availableSlots);
        slotList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TimeSlot slot, boolean empty) {
                super.updateItem(slot, empty);
                if (empty || slot == null) {
                    setText(null);
                } else {
                    Staff staff = staffRepository.findById(slot.getStaffId()).orElse(null);
                    String staffName = staff != null ? staff.getName() : slot.getStaffId().toString();
                    setText(staffName + "  –  " + slot.getStart().format(FMT));
                }
            }
        });
        slotList.setPrefHeight(200);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.add(slotList, 0, 0);
        getDialogPane().setContent(grid);

        javafx.scene.Node bookButton = getDialogPane().lookupButton(bookButtonType);
        bookButton.setDisable(true);
        slotList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) ->
                bookButton.setDisable(n == null));

        setResultConverter(button -> {
            if (button == bookButtonType) {
                TimeSlot selected = slotList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    return new UUID[]{selected.getStaffId(), selected.getId()};
                }
            }
            return null;
        });
    }
}
