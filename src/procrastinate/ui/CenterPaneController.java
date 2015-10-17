package procrastinate.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;

public class CenterPaneController {

    protected static final int SCREEN_MAIN = 1;
    protected static final int SCREEN_SEARCH = 2;
    protected static final int SCREEN_HELP = 3;  // Maybe should use arraylist of these integers/string to reference the integers?

    private static final String LOCATION_MAIN_SCREEN_LAYOUT = "MainScreen.fxml";
    private static final String LOCATION_HELP_SCREEN_LAYOUT = "HelpScreen.fxml";
    private static final String LOCATION_SEARCH_SCREEN_LAYOUT = "SearchScreen.fxml";

    private HashMap<Integer, Node> controlledScreens;   // CHANGE TO SWITCH
    /**
     * NEED TO CHANGE HASHMAP TO SWITCH,
     * set up subcontrollers for each screen.
     *
     * or
     * SPLIT OUT SCREENS TO LIKE DISPLAY, HELP ETC.
     */

    private Node mainScreen;
    private Node searchScreen;
    private Node helpScreen;

    private StackPane centerStackPane;
    private Node currentScreen;

//    protected CenterPaneController() {
//        this.controlledScreens = new HashMap<Integer, Node>();
//        this.centerStackPane = new StackPane();
//
//        createScreens();
//        centerStackPane.getChildren().add(mainScreen);
//    }

    protected CenterPaneController(StackPane centerStackPane) {
        this.controlledScreens = new HashMap<Integer, Node>();
        this.centerStackPane = centerStackPane;

//        createScreens();
//        centerStackPane.getChildren().add(mainScreen);
        initialiseScreens();
        currentScreen = mainScreen;
        mainScreen.setOpacity(1);
    }

    protected void changeScreen(int screenKey) {
        Node screen = controlledScreens.get(screenKey);
        setScreen(screen);
    }

    protected void initialiseScreens() {
        ArrayList<Node> screensList = createScreens();
        centerStackPane.getChildren().addAll(screensList);
    }

    private void mapScreen(int screenKey, Node newScreen) {
        controlledScreens.put(screenKey, newScreen);
    }

    private void setScreen(Node screen) {
        currentScreen.setOpacity(0);
        screen.toFront();
        screen.setOpacity(1);
    }

    private ArrayList<Node> createScreens() {
        ArrayList<Node> screensList = new ArrayList<>();

        // Main Screen setup
        screensList.add(createMainScreen());

        // Help Screen setup
        screensList.add(createHelpScreen());

        // Search screen setup

        return screensList;
    }

    private Node createHelpScreen() {
        this.helpScreen = new HelpScreen(LOCATION_HELP_SCREEN_LAYOUT).getScreen();
        helpScreen.setOpacity(0);
        mapScreen(SCREEN_HELP, helpScreen);
        return helpScreen;
    }

    private Node createMainScreen() {
        this.mainScreen = new MainScreen(LOCATION_MAIN_SCREEN_LAYOUT).getScreen();
//        mainScreen.resize(centerStackPane.getWidth(), centerStackPane.getHeight());
        mainScreen.setOpacity(0);
        mapScreen(SCREEN_MAIN, mainScreen);
        return mainScreen;
    }
}
