//@@author A0121597B
package procrastinate.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import procrastinate.task.Task;
import procrastinate.ui.UI.ScreenView;

import java.util.List;

public class CenterPaneController {

    // ================================================================================
    // Message strings
    // ================================================================================

    private static final String LOCATION_CENTER_SCREEN_LAYOUT = "views/CenterScreen.fxml";

    private static final String MESSAGE_UNABLE_RECOGNISE_SCREEN_TYPE = "Unable to recognise ScreenType";

    private static final String SELECTOR_SCROLL_BAR = ".scroll-bar";
    private static final String SELECTOR_SCROLL_PANE = "#scrollPane";

    // ================================================================================
    // Animation time values
    // ================================================================================

    private static final double OPACITY_FULL = 1;
    private static final double OPACITY_ZERO = 0;

    // Time values are in milliseconds
    private static final double TIME_HELP_SCREEN_FADEIN = 150;
    private static final double TIME_HELP_SCREEN_FADEOUT = 200;

    private static final double TIME_SPLASH_SCREEN_FADE = 4000;
    private static final double TIME_SPLASH_SCREEN_FULL_OPACITY = 3000;
    private static final double TIME_SPLASH_SCREEN_INTERRUPT = 2700;

    // ================================================================================
    // Class variables
    // ================================================================================

    private static double xOffset, yOffset;

 // Changed to protected for testing purposes.
    protected CenterScreen currentScreenView;
    protected ImageOverlay currentOverlay;

    private Timeline splashScreenTimeline;

    private Node mainScreenNode;
    private Node doneScreenNode;
    private Node searchScreenNode;
    private Node summaryScreenNode;

    private Node helpOverlayNode;
    private Node splashOverlayNode;

    private ImageOverlay helpOverlay;
    private ImageOverlay splashOverlay;

    private MainScreen mainScreen;
    private SearchScreen searchScreen;
    private SummaryScreen summaryScreen;

    private DoneScreen doneScreen;

    private StackPane centerStackPane;

    private boolean isInitial = true;

    // ================================================================================
    // CenterPaneController methods
    // ================================================================================

    // New CenterPaneController should only contain one screen node at all times, excluding the overlay nodes.
    protected CenterPaneController(StackPane centerStackPane) {
        this.centerStackPane = centerStackPane;
        createScreens();
        createOverlays();
        setToSummaryScreen();
    }

    protected void updateScreen(List<Task> taskList, ScreenView screenView) {
        if (isInitial) {
            summaryScreen.updateTaskList(taskList);
            mainScreen.updateTaskList(taskList);
            isInitial = false;
            return;
        }

        switch (screenView) {

            case SCREEN_DONE: {
                if (currentScreenView == doneScreen) {
                    doneScreen.updateTaskList(taskList);
                    break;
                } else {
                    startScreenSwitchSequence(taskList, doneScreenNode, doneScreen);
                    break;
                }
            }

            case SCREEN_MAIN: {
                if (currentScreenView == mainScreen) {
                    mainScreen.updateTaskList(taskList);
                    break;
                } else if (currentScreenView == summaryScreen) {
                    removeSummaryScreen(taskList);
                    break;
                } else {
                    startScreenSwitchSequence(taskList, mainScreenNode, mainScreen);
                    break;
                }
            }

            case SCREEN_SEARCH: {
                if (currentScreenView == searchScreen) {
                    searchScreen.updateTaskList(taskList);
                    break;
                } else {
                    startScreenSwitchSequence(taskList, searchScreenNode, searchScreen);
                    break;
                }
            }

            default:
                System.out.println(MESSAGE_UNABLE_RECOGNISE_SCREEN_TYPE);
                break;
        }
    }

    /**
     * Starts the fade out transition that lasts for 0.5 seconds if the stack contains it
     * and it is the current overlay screen.
     */
    protected void hideHelpOverlay() {
        if (currentOverlay != helpOverlay || !centerStackPane.getChildren().contains(helpOverlayNode)) {
            return;
        }
        FadeTransition helpOverlayFadeOut = getFadeOutTransition(TIME_HELP_SCREEN_FADEOUT, helpOverlayNode);
        helpOverlayFadeOut.setOnFinished(e -> {
            centerStackPane.getChildren().remove(helpOverlayNode);
            currentOverlay = null;
        });
        helpOverlayFadeOut.play();
    }

    /**
     * Fast-forwards the fade animation if user starts typing, which will remove the entire
     * node from the stack once it has finished fading.
     */
    protected void hideSplashOverlay() {
        if (currentOverlay == splashOverlay && centerStackPane.getChildren().contains(splashOverlayNode)) {
            Duration interruptTime = Duration.millis(TIME_SPLASH_SCREEN_INTERRUPT);
            // Only fast forward the timeline if the current time of the animation is smaller than the given
            // interrupt time. Else, just wait for the animation to end.
            if (splashScreenTimeline.getCurrentTime().lessThan(interruptTime)) {
                splashScreenTimeline.jumpTo(Duration.millis(TIME_SPLASH_SCREEN_INTERRUPT));
            }
            splashScreenTimeline.jumpTo(Duration.millis(TIME_SPLASH_SCREEN_FADE));
        }
    }

    /**
     * Creates a splash screen that maintains full opacity for 2 seconds before completely fading out in 1 second
     * or until the user starts to type.
     */
    protected void showSplashOverlay() {
        currentOverlay = splashOverlay;
        centerStackPane.getChildren().add(splashOverlayNode);

        buildSplashScreenAnimation();
        splashScreenTimeline.play();
    }

    protected void showHelpOverlay() {
        if (currentOverlay == helpOverlay || centerStackPane.getChildren().contains(helpOverlay)) {
            return;
        }
        currentOverlay = helpOverlay;
        centerStackPane.getChildren().add(helpOverlayNode);
        helpOverlayNode.toFront();

        FadeTransition helpOverlayFadeIn = getFadeInTransition(TIME_HELP_SCREEN_FADEIN, helpOverlayNode);
        helpOverlayFadeIn.play();
    }

    protected void nextHelpPage() {
        if (currentOverlay != helpOverlay) {
            return;
        }
        ((HelpOverlay) helpOverlay).nextPage();
    }

    // Methods below for scrolling current screen with key input. Scroll bar value is incremented/decremented twice
    // to enable the user scroll faster
    protected void scrollUpCurrentScreen() {
        ScrollPane currScrollPane = ((ScrollPane)(currentScreenView.getNode().lookup(SELECTOR_SCROLL_PANE)));
        ScrollBar currScrollBar = (ScrollBar) currScrollPane.lookup(SELECTOR_SCROLL_BAR);
        currScrollBar.decrement();
        currScrollBar.decrement();
    }

    protected void scrollDownCurrentScreen() {
        ScrollPane currScrollPane = ((ScrollPane)(currentScreenView.getNode().lookup(SELECTOR_SCROLL_PANE)));
        ScrollBar currScrollBar = (ScrollBar) currScrollPane.lookup(SELECTOR_SCROLL_BAR);
        currScrollBar.increment();
        currScrollBar.increment();
    }

    // ================================================================================
    // Utility methods
    // ================================================================================

    protected void receiveSearchStringAndPassToSearchScreen(String searchString) {
        searchScreen.updateSearchStringLabel(searchString);
    }

    private void startScreenSwitchSequence(List<Task> taskList, Node nodeToSwitchIn, CenterScreen screenToSwitchIn) {
        SequentialTransition screenSwitchSequence;
        screenSwitchSequence = currentScreenView.getScreenSwitchOutSequence();
        screenSwitchSequence.setOnFinished(e -> {
            centerStackPane.getChildren().clear();
            centerStackPane.getChildren().add(nodeToSwitchIn);
            screenToSwitchIn.getScreenSwitchInSequence().play();
            screenToSwitchIn.updateTaskList(taskList);
            currentScreenView = screenToSwitchIn;
        });
        screenSwitchSequence.play();
    }

    private FadeTransition getFadeOutTransition(double timeInMs, Node transitingNode) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeInMs), transitingNode);
        fadeTransition.setFromValue(OPACITY_FULL);
        fadeTransition.setToValue(OPACITY_ZERO);
        fadeTransition.setInterpolator(Interpolator.EASE_OUT);
        return fadeTransition;
    }

    private FadeTransition getFadeInTransition(double timeInMs, Node transitingNode) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeInMs), transitingNode);
        fadeTransition.setFromValue(OPACITY_ZERO);
        fadeTransition.setToValue(OPACITY_FULL);
        fadeTransition.setInterpolator(Interpolator.EASE_IN);
        return fadeTransition;
    }

    private void buildSplashScreenAnimation() {
        // Set SplashScreen opacity at full for 2 seconds.
        Duration fullOpacityDuration = Duration.millis(TIME_SPLASH_SCREEN_FULL_OPACITY);
        KeyValue fullOpacityKeyValue = new KeyValue(splashOverlayNode.opacityProperty(), OPACITY_FULL);
        KeyFrame fullOpacityFrame = new KeyFrame(fullOpacityDuration, fullOpacityKeyValue);

        // Set SplashScreen to fade out completely at time = 3 seconds
        Duration zeroOpacityDuration = Duration.millis(TIME_SPLASH_SCREEN_FADE);
        KeyValue zeroOpacityKeyValue = new KeyValue(splashOverlayNode.opacityProperty(), OPACITY_ZERO);
        KeyFrame zeroOpacityFrame = new KeyFrame(zeroOpacityDuration, zeroOpacityKeyValue);

        splashScreenTimeline= new Timeline(fullOpacityFrame, zeroOpacityFrame);
        splashScreenTimeline.setOnFinished(e -> {
            centerStackPane.getChildren().remove(splashOverlayNode);
            currentOverlay = null;
        });
    }

    // ================================================================================
    // Init methods
    // ================================================================================

    private void createOverlays() {
        createHelpOverlay();
        createSplashOverlay();
    }

    /**
     * This creates and holds a list of the screens that can be easily added onto the center pane
     * @return list of screens
     */
    private void createScreens() {
        createMainScreen();
        createDoneScreen();
        createSearchScreen();
        createSummaryScreen();
    }

    private void createHelpOverlay() {
        this.helpOverlay = new HelpOverlay();
        this.helpOverlayNode = helpOverlay.getNode();
    }

    private void createSplashOverlay() {
        this.splashOverlay = new SplashOverlay();
        this.splashOverlayNode = splashOverlay.getNode();
    }

    private void createMainScreen() {
        this.mainScreen = new MainScreen(LOCATION_CENTER_SCREEN_LAYOUT);
        this.mainScreenNode = mainScreen.getNode();
        addMouseDragListeners(mainScreenNode);
    }

    private void createDoneScreen() {
        this.doneScreen = new DoneScreen(LOCATION_CENTER_SCREEN_LAYOUT);
        this.doneScreenNode = doneScreen.getNode();
        addMouseDragListeners(doneScreenNode);
    }

    private void createSearchScreen() {
        this.searchScreen = new SearchScreen(LOCATION_CENTER_SCREEN_LAYOUT);
        this.searchScreenNode = searchScreen.getNode();
        addMouseDragListeners(searchScreenNode);
    }

    private void createSummaryScreen() {
        this.summaryScreen = new SummaryScreen(LOCATION_CENTER_SCREEN_LAYOUT);
        this.summaryScreenNode = summaryScreen.getNode();
        addMouseDragListeners(summaryScreenNode);
    }

    /**
     * Hide the MainScreen below the SummaryScreen since it'll take some time to start up later on.
     */
    private void setToSummaryScreen() {
        centerStackPane.getChildren().add(mainScreenNode);
        centerStackPane.getChildren().add(summaryScreenNode);
        currentScreenView = summaryScreen;
        summaryScreenNode.toFront();
        summaryScreenNode.setOpacity(OPACITY_FULL);
    }

    private void removeSummaryScreen(List<Task> taskList) {
        centerStackPane.getChildren().remove(summaryScreenNode);
        mainScreen.updateTaskList(taskList);
        currentScreenView = mainScreen;
    }

    //@@author A0121597B-reused
    // Required since each screen node is wrapped inside a scrollPane.
    private void addMouseDragListeners(Node screenNode) {
        Node scrollPaneNode = ((ScrollPane)screenNode.lookup(SELECTOR_SCROLL_PANE)).getContent();
        scrollPaneNode.setOnMousePressed((mouseEvent) -> {
            xOffset = mouseEvent.getSceneX();
            yOffset = mouseEvent.getSceneY();
        });
        scrollPaneNode.setOnMouseDragged((mouseEvent) -> {
            centerStackPane.getScene().getWindow().setX(mouseEvent.getScreenX() - xOffset);
            centerStackPane.getScene().getWindow().setY(mouseEvent.getScreenY() - yOffset);
        });
    }

    // ================================================================================
    // Test methods
    // ================================================================================

    //@@author A0121597B generated
    protected Node getMainScreen() {
        return mainScreenNode;
    }

    protected Node getHelpOverlay() {
        return helpOverlayNode;
    }
}
