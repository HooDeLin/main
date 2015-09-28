package procrastinate;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class Logic implements Initializable {

    // ================================================================================
    // Message Strings
    // ================================================================================
    private static final String DEBUG_VIEW_LOADED = "View is now loaded!";
    private static final String STATUS_READY = "Ready!";
    private static final String STATUS_PREVIEW_COMMAND = "Preview command: ";
    private static final String FEEDBACK_ADD_DREAM = "Adding dream: ";
    
    private static FileHandler file;

    // ================================================================================
    // Class Variables
    // ================================================================================
    private StringProperty userInput = new SimpleStringProperty();
    private StringProperty statusLabelText = new SimpleStringProperty();

    // ================================================================================
    // FXML Field Variables
    // ================================================================================
    @FXML private Label statusLabel;
    @FXML private TextField userInputField;
    @FXML private BorderPane borderPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Utilities.printDebug(DEBUG_VIEW_LOADED);
        attachHandlersAndListeners();
        initBinding();
        setStatus(STATUS_READY);
        file = new FileHandler();
    }

    private String executeCommand(String userCommand) {

        Command command = Parser.parse(userCommand);

        switch (command.getType()) {

            case ADD_DREAM:
            	String desc = command.getDescription();
            	file.writeToFile(desc);
                return FEEDBACK_ADD_DREAM + desc;

            case EXIT:
                System.exit(0);

            default:
                throw new Error("Error with parser: unknown command type returned");

        }
    }

    private void initBinding() {
        userInput.bindBidirectional(userInputField.textProperty());
        statusLabelText.bindBidirectional(statusLabel.textProperty());
    }

    private void attachHandlersAndListeners() {
        userInputField.setOnKeyReleased(createKeyReleaseHandler());
        userInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                setStatus(STATUS_READY);
            } else {
                setStatus(STATUS_PREVIEW_COMMAND + newValue);
            }
        });
    }

    private EventHandler<KeyEvent> createKeyReleaseHandler() {
        return (KeyEvent keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                if (!getInput().isEmpty()) {
                    String userCommand = getInput();
                    clearInput();

                    String feedback = executeCommand(userCommand);
                    setStatus(feedback);
                } else {
                    setStatus(STATUS_READY);
                }
            }
        };
    }

    // ================================================================================
    // UI utility methods
    // ================================================================================

    private void clearInput() {
        userInputField.clear();
    }

    private String getInput() {
        return userInput.get();
    }

    private void setStatus(String status) {
        statusLabelText.set(status);
    }

}