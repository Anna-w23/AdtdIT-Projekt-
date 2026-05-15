package de.spacemate.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Dialog for the flight doctor to enter the result of a medical examination.
 * For initial medicals: returns boolean[requiresSpecialist, requiresLifestyleCoaching, eligible].
 * For final medicals: only returns flightEligible.
 * Also captures a remarks string.
 * Result is packed as Object[]{boolean[], String remarks}.
 */
public class MedicalResultDialog extends Dialog<Object[]> {

    public enum MedicalType { INITIAL, FINAL }

    public MedicalResultDialog(MedicalType type) {
        setTitle(type == MedicalType.INITIAL ? "Initial Medical Result" : "Final Medical Result");
        setHeaderText("Enter examination outcome");

        ButtonType saveButtonType = new ButtonType("Save Result", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        box.setPadding(new Insets(20, 10, 10, 10));

        CheckBox specialistCheck = new CheckBox("Requires specialist consultation (physical issues)");
        CheckBox coachingCheck = new CheckBox("Requires lifestyle coaching (psychological issues)");
        CheckBox eligibleCheck = new CheckBox("Flight eligible");
        eligibleCheck.setSelected(true);

        TextArea remarks = new TextArea();
        remarks.setPromptText("Remarks (optional)");
        remarks.setPrefRowCount(3);

        if (type == MedicalType.INITIAL) {
            box.getChildren().addAll(
                    new Label("Flag any issues found:"),
                    specialistCheck, coachingCheck,
                    new Label("Remarks:"), remarks);
        } else {
            box.getChildren().addAll(
                    eligibleCheck,
                    new Label("Remarks:"), remarks);
        }

        getDialogPane().setContent(box);

        setResultConverter(button -> {
            if (button == saveButtonType) {
                boolean[] flags = type == MedicalType.INITIAL
                        ? new boolean[]{specialistCheck.isSelected(), coachingCheck.isSelected()}
                        : new boolean[]{eligibleCheck.isSelected()};
                return new Object[]{flags, remarks.getText().trim()};
            }
            return null;
        });
    }
}
