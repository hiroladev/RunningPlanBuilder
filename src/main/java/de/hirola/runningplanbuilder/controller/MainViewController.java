package de.hirola.runningplanbuilder.controller;

import de.hirola.runningplanbuilder.Global;
import de.hirola.runningplanbuilder.RunningPlanBuilder;
import de.hirola.runningplanbuilder.model.*;
import de.hirola.runningplanbuilder.util.ApplicationResources;
import de.hirola.runningplanbuilder.view.PreferencesView;
import de.hirola.runningplanbuilder.view.RunningEntryView;
import de.hirola.runningplanbuilder.view.RunningPlanView;
import de.hirola.sportsapplications.SportsLibrary;
import de.hirola.sportsapplications.SportsLibraryException;
import de.hirola.sportsapplications.model.RunningPlan;
import de.hirola.sportsapplications.model.RunningPlanEntry;
import de.hirola.sportsapplications.util.TemplateLoader;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * Copyright 2022 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Controller for the main view (application window) using fxml.
 *
 * @author Michael Schmidt (Hirola)
 * @since v.0.1
 */
public class MainViewController {

    private final ApplicationResources applicationResources
            = ApplicationResources.getInstance(); // bundle for localization, ...
    private Preferences userPreferences;
    private boolean debugMode;
    private boolean useLastDirectory;
    private String lastDirectoryPath;
    private SportsLibrary sportsLibrary;
    private RunningPlan runningPlan; // actual running plan for the application
    private List<RunningPlanEntry> runningPlanEntries;
    private RunningPlanEntry runningPlanEntry; // actual edited running plan entry
    private ObservableList<RunningPlanEntryTableObject> runningPlanEntryTableObjects; // list for the table view
    private HostServices hostServices;
    private RunningPlanView runningPlanView;
    private RunningEntryView runningEntryView;
    private PreferencesView preferencesView;
    private ContextMenu tableViewContextMenu;
    private MenuItem tableViewContextMenuItemEdit;
    private MenuItem tableViewContextMenuItemDelete;


    // main app menu
    // created with SceneBuilder
    @FXML
    // the reference will be injected by the FXML loader
    private MenuBar mainMenuBar;
    @FXML
    // the reference will be injected by the FXML loader
    private Menu menuFile;  // file menu
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemNew;
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemOpen;
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemSave;
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemQuit;
    @FXML
    // the reference will be injected by the FXML loader
    private Menu menuEdit; // edit menu
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemEditRunningPlan;
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemEditPreferences;
    @FXML
    // the reference will be injected by the FXML loader
    private Menu menuHelp; // help menu
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemDebug;
    @FXML
    // the reference will be injected by the FXML loader
    private MenuItem menuItemAbout;

    // tool "menu"
    @FXML
    private Arc runningPlanMenuElement;
    @FXML
    private Label runningPlanTemplateNodeLabel;
    @FXML
    private Rectangle runningEntryMenuElement;
    @FXML
    private Label runningEntryNodeLabel;
    @FXML
    private Label toolMenuInfoLabel;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private TableView<RunningPlanEntryTableObject> runningPlanEntryTableView;

    final EventHandler<ActionEvent> onMenuItemActionEventHandler =
            event -> {
                if (event.getSource() instanceof MenuItem) {
                    // context menu action from table view
                    if (event.getSource().equals(tableViewContextMenuItemEdit)) {
                        // open the view for editing
                        int index = runningPlanEntryTableView.getSelectionModel().getSelectedIndex();
                        if (index < runningPlanEntries.size()) {
                            runningPlanEntry = runningPlanEntries.get(index);
                            showRunningEntryView();
                        }
                    }
                    // context menu action from a running unit element
                    if (event.getSource().equals(tableViewContextMenuItemDelete)) {
                        //TODO: ask user
                        // remove the selected running unit
                        int index = runningPlanEntryTableView.getSelectionModel().getSelectedIndex();
                        removeRunningEntryForIndex(index);
                    }
                }
            };

    public MainViewController() {}

    public void setHostServices(@NotNull HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    // when the FXML loader is done loading the FXML document, it calls this method of the controller
    private void initialize() throws InstantiationException, SportsLibraryException {
        runningPlanEntries = new ArrayList<>();
        runningPlanEntryTableObjects = FXCollections.observableArrayList();
        loadUserPreferences();
        // initialize sports library
        File appDirectory = SportsLibrary.initializeAppDirectory(Global.PACKAGE_NAME);
        sportsLibrary = SportsLibrary.getInstance(debugMode, appDirectory, null);
        // set nodes to javax default colors
        runningPlanMenuElement.setFill(Global.RUNNING_PLAN_TEMPLATE_NODE_COLOR);
        runningEntryMenuElement.setFill(Global.RUNNING_UNIT_NODE_COLOR);
        setMenuLabel();  // localisation the menu (item) labels
        setToolMenuLabel(); // localisation the tool "menu" item labels
        initializeTableView();
        createContextMenuForTableView();
        // disable different menu items
        canEdited();
    }

    @FXML
    // use for onAction by the FXML loader
    private void onAction(ActionEvent event) {
        if (event.getSource().equals(menuItemNew)) {
            if (runningPlan != null) {
                resetRunningPlan();
            }
            if (runningPlan == null) {
                // running plan was reset
                showRunningPlanView();
            }
        }
        if (event.getSource().equals(menuItemOpen)) {
            if (runningPlan != null) {
                if (continueOperation()) {
                    importJSONFromFile();
                }
            } else {
                importJSONFromFile();
            }
        }
        if (event.getSource().equals(menuItemSave)) {
            exportToJSONFile();
        }
        if (event.getSource().equals(menuItemQuit)) {
            if (runningPlan != null) {
                if (continueOperation()) {
                    Stage stage = (Stage) mainSplitPane.getScene().getWindow();
                    stage.close();
                }
            } else {
                Stage stage = (Stage) mainSplitPane.getScene().getWindow();
                stage.close();
            }
        }
        if (event.getSource().equals(menuItemEditRunningPlan)) {
            showRunningPlanView();
        }
        if (event.getSource().equals(menuItemEditPreferences)) {
            showPreferencesDialog();
        }
        if (event.getSource().equals(menuItemDebug)) {
            showDebugDialog();
        }
        if (event.getSource().equals(menuItemAbout)) {
            showAboutDialog();
        }
    }

    @FXML
    // use for onMouseClicked by the FXML loader
    private void onMouseClicked(MouseEvent event) {
        if (event.getSource().equals(runningPlanMenuElement)) {
            // create a new running plan
            showRunningPlanView();
            return;
        }
        if (event.getSource().equals(runningEntryMenuElement)) {
            if (runningPlan != null) {
                // create a new running entry
                runningPlanEntry = null;
                showRunningEntryView();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(applicationResources.getString("app.name")
                        + " "
                        + applicationResources.getString("app.version"));
                alert.setHeaderText(applicationResources.getString("alert.runningplan.null"));
                alert.showAndWait();
            }
        }
    }

    private void setMenuLabel() {
        menuFile.setText(applicationResources.getString("mainMenuBar.menuFile"));
        menuItemNew.setText(applicationResources.getString("mainMenuBar.menuFile.menuItemNew"));
        menuItemOpen.setText(applicationResources.getString("mainMenuBar.menuFile.menuItemOpen"));
        menuItemSave.setText(applicationResources.getString("mainMenuBar.menuFile.menuItemSave"));
        menuItemQuit.setText(applicationResources.getString("mainMenuBar.menuFile.menuItemQuit"));
        menuEdit.setText(applicationResources.getString("mainMenuBar.menuEdit"));
        menuItemEditRunningPlan.setText(applicationResources.getString("mainMenuBar.menuEdit.menuItemTemplate"));
        menuItemEditPreferences.setText(applicationResources.getString("mainMenuBar.menuEdit.menuItemPreferences"));
        menuHelp.setText(applicationResources.getString("mainMenuBar.menuHelp"));
        menuItemDebug.setText(applicationResources.getString("mainMenuBar.menuHelp.menuItemDebug"));
        menuItemAbout.setText(applicationResources.getString("mainMenuBar.menuHelp.menuItemAbout"));
    }

    private void setToolMenuLabel() {
        runningPlanTemplateNodeLabel.setText(applicationResources.getString("mainToolMenu.newTemplate"));
        runningEntryNodeLabel.setText(applicationResources.getString("mainToolMenu.runningUnit"));
        toolMenuInfoLabel.setText(applicationResources.getString("mainToolMenu.info"));
    }

    private void initializeTableView() {
        // a placeholder, if no running entries in plan exists
        runningPlanEntryTableView.setPlaceholder(
                new Label(applicationResources.getString("mainView.table.defaultLabelText")));
        runningPlanEntryTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // TODO: in this version only a single row can be selected
        runningPlanEntryTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // the table column header
        TableColumn<RunningPlanEntryTableObject, String> weekColumn
                = new TableColumn<>(applicationResources.getString("mainView.table.column.1.headerText"));
        weekColumn.setCellValueFactory(new PropertyValueFactory<>("weekString"));
        weekColumn.setPrefWidth(Global.WEEK_COLUMN_PREF_WIDTH);
        TableColumn<RunningPlanEntryTableObject, String> dayColumn
                = new TableColumn<>(applicationResources.getString("mainView.table.column.2.headerText"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("dayString"));
        dayColumn.setPrefWidth(Global.DAY_COLUMN_PREF_WIDTH);
        TableColumn<RunningPlanEntryTableObject, String> durationColumn
                = new TableColumn<>(applicationResources.getString("mainView.table.column.3.headerText"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationString"));
        durationColumn.setPrefWidth(Global.DURATION_COLUMN_PREF_WIDTH);
        TableColumn<RunningPlanEntryTableObject, String> runningUnitsColumn
                = new TableColumn<>(applicationResources.getString("mainView.table.column.4.headerText"));
        runningUnitsColumn.setCellValueFactory(new PropertyValueFactory<>("runningUnitsString"));
        runningUnitsColumn.setPrefWidth(Global.RUNNING_UNIT_COLUMN_PREF_WIDTH);
        runningPlanEntryTableView.getColumns().add(weekColumn);
        runningPlanEntryTableView.getColumns().add(dayColumn);
        runningPlanEntryTableView.getColumns().add(durationColumn);
        runningPlanEntryTableView.getColumns().add(runningUnitsColumn);
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

    private void showRunningPlanView() {
        // show dialog
        if (runningPlanView == null) {
            runningPlanView = new RunningPlanView();
        }
        try {
            RunningPlanViewController viewController = runningPlanView.showView(mainSplitPane, runningPlan);
            runningPlan = viewController.getRunningPlan();
            canEdited();
        } catch (IOException exception) {
            //TODO: Alert
            exception.printStackTrace();
        }
    }

    private void showRunningEntryView() {
        // show dialog
        try {
            // get the running plan entry from modal dialog
            if (runningEntryView == null) {
                runningEntryView = new RunningEntryView(sportsLibrary);
            }
            RunningEntryViewController viewController
                    = runningEntryView.showViewModal(mainSplitPane, runningPlanEntry);
            runningPlanEntry = viewController.getRunningPlanEntry();
            if (runningPlanEntry != null) {
                addRunningPlanEntry(runningPlanEntry);
            }
            // refresh the table view
            runningPlanEntryTableView.getItems().clear();
            runningPlanEntryTableView.getItems().addAll(runningPlanEntryTableObjects);
        } catch (IOException exception) {
            //TODO: alert
            exception.printStackTrace();
        }
    }

    private void showDebugDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(applicationResources.getString("app.name")
                + " "
                + applicationResources.getString("app.version"));
        alert.setHeaderText(applicationResources.getString("debug.alert.header"));
        VBox vBox = new VBox();
        Label label = new Label(applicationResources.getString("debug.alert.info"));
        Hyperlink hyperlink = new Hyperlink(applicationResources.getString("debug.alert.info.url"));
        hyperlink.setOnAction((event) -> hostServices.showDocument(hyperlink.getText()));
        vBox.getChildren().addAll(label, hyperlink);
        alert.getDialogPane().contentProperty().set(vBox);
        alert.showAndWait();
    }

    private void showPreferencesDialog() {
        // show dialog
        try {
            // get the running plan entry from modal dialog
            if (preferencesView == null) {
                preferencesView = new PreferencesView();
            }
            PreferencesViewController viewController
                    = preferencesView.showViewModal(mainSplitPane, userPreferences);
            userPreferences = viewController.getUserPreferences();
        } catch (IOException exception) {
            //TODO: alert
            exception.printStackTrace();
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(applicationResources.getString("app.name")
                + " "
                + applicationResources.getString("app.version"));
        alert.setHeaderText(applicationResources.getString("about.alert.header"));
        VBox vBox = new VBox();
        Label label = new Label(applicationResources.getString("about.alert.info"));
        Hyperlink hyperlink = new Hyperlink(applicationResources.getString("about.alert.info.url"));
        hyperlink.setOnAction((event) -> hostServices.showDocument(hyperlink.getText()));
        vBox.getChildren().addAll(label, hyperlink);
        alert.getDialogPane().contentProperty().set(vBox);
        alert.showAndWait();
    }

    private void addRunningPlanEntry(@NotNull RunningPlanEntry entry) {
        // if the running entry is new, add it to the list
        if (!runningPlanEntries.contains(entry)) {
            // add to the running entry list of running plan
            runningPlanEntries.add(entry);
            // add to table object list
            runningPlanEntryTableObjects.add(new RunningPlanEntryTableObject(entry));
        } else {
            // the entry can be updated - update the table object

        }
        // add context menu to table view
        if (runningPlanEntryTableObjects.size() == 1) {
            runningPlanEntryTableView.setContextMenu(tableViewContextMenu);
        }
        // refresh the table view
        runningPlanEntryTableView.getItems().clear();
        runningPlanEntryTableView.getItems().addAll(runningPlanEntryTableObjects);
    }

    private void removeRunningEntryForIndex(int index) {
        if (index < runningPlanEntries.size()) {
            // remove the entry from both lists
            runningPlanEntries.remove(index);
            runningPlanEntryTableObjects.remove(index);
            // refresh the table view
            runningPlanEntryTableView.getItems().clear();
            runningPlanEntryTableView.getItems().addAll(runningPlanEntryTableObjects);
        }
        if (runningPlanEntryTableObjects.size() == 0) {
            runningPlanEntryTableView.setContextMenu(null);
        }
    }

    private void importJSONFromFile() {
        // open system file dialog
        // user prefs for last directory
        String directoryPathString;
        if (useLastDirectory && !lastDirectoryPath.isEmpty()) {
            directoryPathString = lastDirectoryPath;
        } else {
            try {
                directoryPathString = System.getProperty("user.home");
            } catch (SecurityException exception) {
                directoryPathString = "/"; // can be used on linux, macOS and Windows
            }
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(directoryPathString));
        fileChooser.setSelectedExtensionFilter(Global.TEMPLATE_FILE_EXTENSION_FILTER);
        File jsonFile = fileChooser.showOpenDialog(mainSplitPane.getScene().getWindow());
        if (!jsonFile.exists() || jsonFile.isDirectory() || !jsonFile.canRead()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(applicationResources.getString("app.name")
                    + " "
                    + applicationResources.getString("app.version"));
            alert.setHeaderText(applicationResources.getString("alert.import.failed"));
            alert.setContentText(applicationResources.getString("alert.import.wrong.file.info"));
            alert.showAndWait();
            return;
        } else {
            // remember the last used directory
            saveLastUsedDirectory(jsonFile);
        }
        try {
            // load the plan from json
            TemplateLoader templateLoader = new TemplateLoader(sportsLibrary);
            runningPlan = templateLoader.loadRunningPlanFromJSON(jsonFile);
            runningPlanEntries = runningPlan.getEntries();
            // update the table object list
            runningPlanEntryTableObjects.clear();
            for (RunningPlanEntry entry: runningPlanEntries) {
                runningPlanEntryTableObjects.add(new RunningPlanEntryTableObject(entry));
            }
            // refresh the table view
            runningPlanEntryTableView.getItems().clear();
            runningPlanEntryTableView.getItems().addAll(runningPlanEntryTableObjects);
            // enable editing and saving the running plan
            canEdited();
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(applicationResources.getString("app.name")
                    + " "
                    + applicationResources.getString("app.version"));
            alert.setHeaderText(applicationResources.getString("alert.import.failed"));
            alert.showAndWait();
            if (sportsLibrary.isDebugMode()) {
                sportsLibrary.debug(exception, "Import from JSON failed.");
            }
        }
    }

    private void exportToJSONFile() {
        if (runningPlan != null) {
            // get all running entries from editor
            //runningPlan.setEntries(editorViewController.getRunningPlanEntries());
            // set the home as initial directory
            // user prefs for last directory
            String directoryPathString;
            if (useLastDirectory && !lastDirectoryPath.isEmpty()) {
                directoryPathString = lastDirectoryPath;
            } else {
                try {
                    directoryPathString = System.getProperty("user.home");
                } catch (SecurityException exception) {
                    directoryPathString = "/"; // can be used on linux, macOS and Windows
                }
            }
            // get the file name from running plan name, removing empty spaces
            String fileName = runningPlan.getName().replaceAll("\\s","");
            if (fileName.isEmpty()) {
                applicationResources.getString("export.file.name");
            }
            // get the export directory with file chooser dialog
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(directoryPathString));
            fileChooser.setSelectedExtensionFilter(Global.TEMPLATE_FILE_EXTENSION_FILTER);
            fileChooser.setInitialFileName(fileName + Global.TEMPLATE_FILE_EXTENSION);
            File jsonFile = fileChooser.showSaveDialog(mainSplitPane.getScene().getWindow());
            // remember last used directory
            saveLastUsedDirectory(jsonFile);
            try {
                TemplateLoader templateLoader = new TemplateLoader(sportsLibrary);
                templateLoader.exportRunningPlanToJSON(runningPlan, jsonFile);
            } catch (SportsLibraryException exception) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(applicationResources.getString("app.name")
                        + " "
                        + applicationResources.getString("app.version"));
                alert.setHeaderText(applicationResources.getString("alert.export.failed"));
                alert.showAndWait();
                if (sportsLibrary.isDebugMode()) {
                    sportsLibrary.debug(exception, "Export to JSON failed.");
                }
            }
        }
    }

    // clean all data
    private void resetRunningPlan() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(applicationResources.getString("app.name")
                + " "
                + applicationResources.getString("app.version"));
        alert.setHeaderText(applicationResources.getString("alert.runningplan.overwrite"));
        ButtonType okButton = new ButtonType(applicationResources
                .getString("action.yes"), ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType(applicationResources
                .getString("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButton);
        alert.showAndWait().ifPresent(type -> {
            if (type == okButton) {
                runningPlan = null;
                runningPlanEntry = null;
                runningPlanEntries.clear();
                runningPlanEntryTableObjects.clear();
                runningPlanEntryTableView.getItems().clear();
            } else {
                alert.close();
            }
        });
    }

    private boolean continueOperation() {
        AtomicBoolean doAction = new AtomicBoolean(false);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(applicationResources.getString("app.name")
                + " "
                + applicationResources.getString("app.version"));
        alert.setHeaderText(applicationResources.getString("alert.runningplan.overwrite"));
        ButtonType okButton = new ButtonType(applicationResources
                .getString("action.yes"), ButtonBar.ButtonData.YES);
        ButtonType cancelButton = new ButtonType(applicationResources
                .getString("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButton);
        alert.showAndWait().ifPresent(type -> {
            if (type == okButton) {
                doAction.set(true);
            } else {
                alert.close();
            }
        });
        return doAction.get();
    }

    // enable / disable editing
    private void canEdited() {
        boolean isEditable = runningPlan != null;
        menuItemEditRunningPlan.setDisable(isEditable);
        menuItemSave.setDisable(isEditable);
        if (!runningPlanEntryTableObjects.isEmpty()) {
            runningPlanEntryTableView.setContextMenu(tableViewContextMenu);
        } else {
            runningPlanEntryTableView.setContextMenu(null);
        }
    }

    private void loadUserPreferences() {
        try {
            userPreferences = Preferences.userRoot().node(RunningPlanBuilder.class.getName());
            debugMode = userPreferences.getBoolean(Global.UserPreferencesKeys.USE_DEBUG_MODE, false);
            useLastDirectory = userPreferences.getBoolean(Global.UserPreferencesKeys.USE_LAST_DIRECTORY, true);
            lastDirectoryPath = userPreferences.get(Global.UserPreferencesKeys.LAST_DIRECTORY, "");
        } catch (SecurityException exception) {
            debugMode = false;
            useLastDirectory = true;
            lastDirectoryPath = "";
            if (sportsLibrary.isDebugMode()) {
                sportsLibrary.debug(exception, "Error while loading user preferences.");
            }
        }
    }

    private void saveLastUsedDirectory(@NotNull File jsonFile) {
        lastDirectoryPath = jsonFile.getParent();
        userPreferences.put(Global.UserPreferencesKeys.LAST_DIRECTORY, lastDirectoryPath);
    }
}
