package de.hirola.runningplanbuilder.controller;

import de.hirola.runningplanbuilder.Global;
import de.hirola.runningplanbuilder.model.RunningUnitTableObject;
import de.hirola.runningplanbuilder.util.ApplicationResources;
import de.hirola.runningplanbuilder.view.RunningUnitView;
import de.hirola.sportsapplications.SportsLibrary;
import de.hirola.sportsapplications.model.RunningPlanEntry;
import de.hirola.sportsapplications.model.RunningUnit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Copyright 2022 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Controller for the main view (application window) using fxml.
 *
 * @author Michael Schmidt (Hirola)
 * @since v0.1
 */
public class RunningEntryViewController {
    private final ApplicationResources applicationResources
            = ApplicationResources.getInstance(); // bundle for localization, ...

    private final Preferences userPreferences
            = Preferences.userRoot().node(Global.UserPreferencesKeys.USER_ROOT_NODE);
    private SportsLibrary sportsLibrary;
    private RunningPlanEntry runningPlanEntry; // the entry for the view
    private List<RunningUnit> runningUnits; // list of all running units
    private ObservableList<RunningUnitTableObject> runningUnitTableObjects; // list for the table view
    private int trainingDay, trainingWeek; // selected day and week
    private ContextMenu tableViewContextMenu;
    private MenuItem tableViewContextMenuItemEdit;
    private MenuItem tableViewContextMenuItemDelete;
    private RunningUnitView runningUnitView;
    // created with SceneBuilder
    @FXML
    private TextArea infoTextArea;
    @FXML
    private Label weekDayComboBoxLabel;
    @FXML
    private ComboBox<String> weekDayComboBox;
    @FXML
    private Label weekComboBoxLabel;
    @FXML
    private ComboBox<String> weekComboBox;
    @FXML
    private Label runningUnitsLabel;
    @FXML
    private TableView<RunningUnitTableObject> runningUnitsTableView;
    @FXML
    private Button addRunningUnitButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    final EventHandler<ActionEvent> onMenuItemActionEventHandler =
            event -> {
                if (event.getSource() instanceof MenuItem) {
                    // context menu action from table view
                    if (event.getSource().equals(tableViewContextMenuItemEdit)) {
                        // open the view for editing
                        int index = runningUnitsTableView.getSelectionModel().getSelectedIndex();
                        if (index < runningUnits.size()) {
                            RunningUnit runningUnit = runningUnits.get(index);
                            try {
                                // get the running unit from modal dialog
                                if (runningUnitView == null) {
                                    runningUnitView = new RunningUnitView(sportsLibrary);
                                }
                                RunningUnitViewController viewController
                                        = runningUnitView.showViewModal(addRunningUnitButton, runningUnit);
                                runningUnit = viewController.getRunningUnit();
                                if (runningUnit != null) {
                                    addOrUpdateRunningUnit(runningUnit);
                                }
                            } catch (IOException exception) {
                                //TODO: alert
                                exception.printStackTrace();
                            }
                        }
                    }
                    // context menu action from a running unit element
                    if (event.getSource().equals(tableViewContextMenuItemDelete)) {
                        //TODO: ask user
                        // remove the selected running unit
                        int index = runningUnitsTableView.getSelectionModel().getSelectedIndex();
                        removeRunningUnitForIndex(index);
                    }
                }
            };

    public RunningEntryViewController() {}

    public void setSportsLibrary(@NotNull SportsLibrary sportsLibrary) {
        this.sportsLibrary = sportsLibrary;
    }

    @Nullable
    public RunningPlanEntry getRunningPlanEntry() {
        return runningPlanEntry;
    }

    public void setRunningPlanEntry(@Nullable RunningPlanEntry runningPlanEntry) {
        this.runningPlanEntry = runningPlanEntry;
        if (runningPlanEntry != null) {
            runningUnits = runningPlanEntry.getRunningUnits();
            showRunningPlanEntryInView();
        }
    }

    @FXML
    // when the FXML loader is done loading the FXML document, it calls this method of the controller
    private void initialize() {
        runningUnits = new ArrayList<>();
        // list to present running units in table view
        runningUnitTableObjects = FXCollections.observableArrayList();
        // localisation for texte
        setLabel();
        // fill combo boxes
        fillWeekDayComboBox();
        fillWeekComboBox();
        initializeTableView();
        createContextMenuForTableView();
    }

    @FXML
    // use for onAction by the FXML loader
    private void onAction(ActionEvent event) {
        if (event.getSource().equals(addRunningUnitButton)) {
            try {
                // get the running unit from modal dialog
                if (runningUnitView == null) {
                    runningUnitView = new RunningUnitView(sportsLibrary);
                }
                RunningUnitViewController viewController
                        = runningUnitView.showViewModal(addRunningUnitButton, null);
                RunningUnit runningUnit = viewController.getRunningUnit();
                if (runningUnit != null) {
                    // a new running unit is created or
                    // an existing running unit is updated perhaps
                    addOrUpdateRunningUnit(runningUnit);
                }
            } catch (IOException exception) {
                //TODO: alert
                exception.printStackTrace();
            }
            return;
        }
        if (event.getSource().equals(saveButton)) {
            saveRunningPlanEntry();
            close();
            return;
        }
        if (event.getSource().equals(cancelButton)) {
            // get a handle to the stage
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
        if (event.getSource().equals(weekDayComboBox)) {
            trainingDay = weekDayComboBox.getSelectionModel().getSelectedIndex() + 1;
            return;
        }
        if (event.getSource().equals(weekComboBox)) {
            trainingWeek = weekComboBox.getSelectionModel().getSelectedIndex() + 1;
        }
    }

    private void setLabel() {
        infoTextArea.setText(applicationResources.getString("entryNodeView.infoText"));
        weekDayComboBoxLabel.setText(applicationResources.getString("entryNodeView.weekDayLabelText"));
        weekComboBoxLabel.setText(applicationResources.getString("entryNodeView.weekLabelText"));
        runningUnitsLabel.setText(applicationResources.getString("entryNodeView.runningUnitsLabelText"));
        addRunningUnitButton.setText(applicationResources.getString("entryNodeView.addRunningUnitButtonText"));
        saveButton.setText(applicationResources.getString("action.save"));
        cancelButton.setText(applicationResources.getString("action.cancel"));
    }

    private void fillWeekDayComboBox() {
        weekDayComboBox.getItems().add(0, applicationResources.getString("monday"));
        weekDayComboBox.getItems().add(1, applicationResources.getString("tuesday"));
        weekDayComboBox.getItems().add(2, applicationResources.getString("wednesday"));
        weekDayComboBox.getItems().add(3, applicationResources.getString("thursday"));
        weekDayComboBox.getItems().add(4, applicationResources.getString("friday"));
        weekDayComboBox.getItems().add(5, applicationResources.getString("saturday"));
        weekDayComboBox.getItems().add(6, applicationResources.getString("sunday"));
        // select the monday
        weekDayComboBox.getSelectionModel().select(0);
    }

    private void fillWeekComboBox() {
        for (int i = 0; i < Global.MAX_COUNT_OF_WEEKS; i++) {
            weekComboBox.getItems().add(i,
                    applicationResources.getString("misc.week") + " " + (i + 1));
        }
        // select the first week
        weekComboBox.getSelectionModel().select(0);
    }

    private void initializeTableView() {
        // view depends on import format - JSON or iCAL
        final boolean iCALMode = userPreferences.getBoolean(Global.UserPreferencesKeys.ICAL_MODE, false);
        // a placeholder, if no running units in entry exists
        runningUnitsTableView.setPlaceholder(
                new Label(applicationResources.getString("entryNodeView.table.defaultLabelText")));
        runningUnitsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        double minWith = Global.RunningUnitTableViewPreferences.DURATION_COLUMN_PREF_WIDTH
                + Global.RunningUnitTableViewPreferences.MOVEMENT_TYPE_KEY_COLUMN_PREF_WIDTH;
        if (iCALMode) {
            minWith += Global.RunningUnitTableViewPreferences.RUNNING_INFOS_COLUMN_PREF_WIDTH;
        } else {
            minWith += Global.RunningUnitTableViewPreferences.MOVEMENT_TYPE_NAME_COLUMN_PREF_WIDTH;
        }
        runningUnitsTableView.setMinSize(minWith, Region.USE_PREF_SIZE);
        // TODO: in this version only a single row can be selected
        runningUnitsTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // the table column header
        TableColumn<RunningUnitTableObject, String> durationColumn
                = new TableColumn<>(applicationResources.getString("entryNodeView.table.column.duration.headerText"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationColumn.setPrefWidth(Global.RunningUnitTableViewPreferences.DURATION_COLUMN_PREF_WIDTH);
        runningUnitsTableView.getColumns().add(durationColumn);
        TableColumn<RunningUnitTableObject, String> movementTypeKeyColumn
                = new TableColumn<>(applicationResources.getString("entryNodeView.table.column.movementType.key.headerText"));
        movementTypeKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        movementTypeKeyColumn.setPrefWidth(Global.RunningUnitTableViewPreferences.MOVEMENT_TYPE_KEY_COLUMN_PREF_WIDTH);
        runningUnitsTableView.getColumns().add(movementTypeKeyColumn);
        if (iCALMode) {
            // iCAL
            TableColumn<RunningUnitTableObject, String> movementRunningInfoColumn
                    = new TableColumn<>(applicationResources.getString("entryNodeView.table.column.movementType.runningInfos.headerText"));
            movementRunningInfoColumn.setCellValueFactory(new PropertyValueFactory<>("runningInfos"));
            movementRunningInfoColumn.setPrefWidth(Global.RunningUnitTableViewPreferences.RUNNING_INFOS_COLUMN_PREF_WIDTH);
            // resize the last column - use the available width
            movementRunningInfoColumn.prefWidthProperty().bind(
                    runningUnitsTableView.widthProperty()
                            .subtract(durationColumn.widthProperty())
                            .subtract(movementTypeKeyColumn.widthProperty())
                            .subtract(2)  // a border stroke?
            );
            runningUnitsTableView.getColumns().add(movementRunningInfoColumn);
        } else {
            // JSON
            TableColumn<RunningUnitTableObject, String> movementTypeNameColumn
                    = new TableColumn<>(applicationResources.getString("entryNodeView.table.column.movementType.name.headerText"));
            movementTypeNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            movementTypeNameColumn.setPrefWidth(Global.RunningUnitTableViewPreferences.MOVEMENT_TYPE_NAME_COLUMN_PREF_WIDTH);
            // resize the last column - use the available width
            movementTypeNameColumn.prefWidthProperty().bind(
                    runningUnitsTableView.widthProperty()
                            .subtract(durationColumn.widthProperty())
                            .subtract(movementTypeKeyColumn.widthProperty())
                            .subtract(2)  // a border stroke?
            );
            runningUnitsTableView.getColumns().add(movementTypeNameColumn);
        }
    }

    private void createContextMenuForTableView() {
        // creating a context menu
        tableViewContextMenu = new ContextMenu();
        // creating the menu Items for the context menu
        tableViewContextMenuItemEdit = new MenuItem(applicationResources.getString("action.edit"));
        tableViewContextMenuItemEdit.setOnAction(onMenuItemActionEventHandler);
        tableViewContextMenuItemDelete = new MenuItem(applicationResources.getString("action.delete"));
        tableViewContextMenuItemDelete.setOnAction(onMenuItemActionEventHandler);
        tableViewContextMenu.getItems().addAll(tableViewContextMenuItemEdit, tableViewContextMenuItemDelete);
    }

    private void showRunningPlanEntryInView() {
        if (runningPlanEntry != null) {
            infoTextArea.setText(runningPlanEntry.getRemarks()
                    .orElse(applicationResources.getString("runningplanentry.remarks.default")));
            // set values for day and week, select the combo boxes
            trainingDay = runningPlanEntry.getDay() ;
            trainingWeek = runningPlanEntry.getWeek();
            if (weekDayComboBox.getItems().size() > trainingDay - 1) {
                weekDayComboBox.getSelectionModel().select(trainingDay - 1);
            }
            if (weekComboBox.getItems().size() > trainingWeek - 1) {
                weekComboBox.getSelectionModel().select(trainingWeek - 1);
            }
            // fill the table object list from running units
            for (RunningUnit runningUnit : runningUnits) {
                runningUnitTableObjects.add(new RunningUnitTableObject(runningUnit));
            }
            // add data to table view
            runningUnitsTableView.getItems().addAll(runningUnitTableObjects);
            // add context menu to table view
            if (runningUnitTableObjects.size() > 0) {
                runningUnitsTableView.setContextMenu(tableViewContextMenu);
            }
        }
    }

    private void addOrUpdateRunningUnit(RunningUnit unit) {
        // if the running unit is new, add it to the list
        if (!runningUnits.contains(unit)) {
            // add to the running unit list of entry
            runningUnits.add(unit);
            // add to table object list
            runningUnitTableObjects.add(new RunningUnitTableObject(unit));
        } else {
            // the unit can be updated - update the table objects
            // using the easiest way
            runningUnitsTableView.getItems().clear();
            runningUnitTableObjects.clear();
            for (RunningUnit unit1: runningUnits) {
                runningUnitTableObjects.add(new RunningUnitTableObject(unit1));
            }
            runningUnitsTableView.getItems().addAll(runningUnitTableObjects);
        }
        // add context menu to table view
        if (runningUnitTableObjects.size() == 1) {
            runningUnitsTableView.setContextMenu(tableViewContextMenu);
        }
        // refresh the table view
        runningUnitsTableView.getItems().clear();
        runningUnitsTableView.getItems().addAll(runningUnitTableObjects);
    }

    private void removeRunningUnitForIndex(int index) {
        if (index < runningUnits.size()) {
            // remove the entry from both lists
            runningUnits.remove(index);
            runningUnitTableObjects.remove(index);
            // refresh the table view
            runningUnitsTableView.getItems().clear();
            runningUnitsTableView.getItems().addAll(runningUnitTableObjects);
        }
        if (runningUnitTableObjects.size() == 0) {
            runningUnitsTableView.setContextMenu(null);
        }
    }

    private void saveRunningPlanEntry() {
        if (runningPlanEntry == null) {
            runningPlanEntry = new RunningPlanEntry();
        }
        runningPlanEntry.setDay(trainingDay);
        runningPlanEntry.setWeek(trainingWeek);
        runningPlanEntry.setRunningUnits(runningUnits);
        close();
    }

    private void close() {
        // get a handle to the stage
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
