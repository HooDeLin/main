package procrastinate.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;
import procrastinate.task.Task;

import java.util.List;

public class MainScreen extends CenterScreen {

    // ================================================================================
    // Message strings
    // ================================================================================

    private static final String CATEGORY_OVERDUE = "Overdue";
    private static final String CATEGORY_THIS_WEEK = "This Week";
    private static final String CATEGORY_FUTURE = "Future";
    private static final String CATEGORY_DREAMS = "Dreams";

    private static final String UI_NUMBER_SEPARATOR = ". ";

    // ================================================================================
    // Class variables
    // ================================================================================

    private Node overdueNode;
    private Node thisWeekNode;
    private Node futureNode;
    private Node dreamsNode;

    private CategoryBox overdueBox;
    private CategoryBox thisWeekBox;
    private CategoryBox futureBox;
    private CategoryBox dreamsBox;

    private VBox overdueTaskList;
    private VBox thisWeekTaskList;
    private VBox futureTaskList;
    private VBox dreamsTaskList;

    private IntegerProperty taskCount = new SimpleIntegerProperty(1);
    private StringProperty taskCountFormatted = new SimpleStringProperty();
    private StringProperty taskCountString = new SimpleStringProperty();

    // ================================================================================
    // FXML field variables
    // ================================================================================

    @FXML VBox mainVBox;

    // ================================================================================
    // MainScreen methods
    // ================================================================================

    protected MainScreen(String filePath) {
        super(filePath);
        createCategories();
        setupBinding();
        getEntriesBoxes();
    }

    protected void updateTaskList(List<Task> taskList) {
        clearTaskList();

        // Currently add all to just dreams, until a comparator for the dates is created
        for (Task task : taskList) {
            TaskEntry taskEntry = new TaskEntry(taskCountFormatted.get(), task.getDescription());
            taskCount.set(taskCount.get() + 1);
            dreamsTaskList.getChildren().add(taskEntry.getEntryDisplay());
        }
    }

    // ================================================================================
    // Utility methods
    // ================================================================================

    private void clearTaskList() {
        resetTaskCount();
        resetTaskList();
    }

    private void resetTaskCount() {
        taskCount.set(1);
    }

    private void resetTaskList() {
        overdueTaskList.getChildren().clear();
        thisWeekTaskList.getChildren().clear();
        futureTaskList.getChildren().clear();
        dreamsTaskList.getChildren().clear();
    }

    // ================================================================================
    // Init methods
    // ================================================================================

    private void createCategories() {
        // Create all the different categories(by time frame) for entries to go into
        this.overdueBox = new CategoryBox(CATEGORY_OVERDUE);
        this.thisWeekBox = new CategoryBox(CATEGORY_THIS_WEEK);
        this.futureBox = new CategoryBox(CATEGORY_FUTURE);
        this.dreamsBox = new CategoryBox(CATEGORY_DREAMS);

        this.overdueNode = overdueBox.getCategoryBox();
        this.thisWeekNode = thisWeekBox.getCategoryBox();
        this.futureNode = futureBox.getCategoryBox();
        this.dreamsNode = dreamsBox.getCategoryBox();

        // Set up for placement of boxes
        mainVBox.setPadding(new Insets(10));    // Looks a bit weird if the boxes stick right next to the border
        mainVBox.setSpacing(10);                // Spacing between boxes
        mainVBox.getChildren().addAll(overdueNode, thisWeekNode, futureNode, dreamsNode);
    }

    private void getEntriesBoxes() {
        this.overdueTaskList = overdueBox.getTaskListVBox();
        this.thisWeekTaskList = thisWeekBox.getTaskListVBox();
        this.futureTaskList = futureBox.getTaskListVBox();
        this.dreamsTaskList = dreamsBox.getTaskListVBox();
    }

    private void setupBinding() {
        taskCountString.bindBidirectional(taskCount, new NumberStringConverter());
        taskCountFormatted.bind(Bindings.concat(taskCountString).concat(UI_NUMBER_SEPARATOR));
    }
}
