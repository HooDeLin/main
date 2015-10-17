package procrastinate.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.io.IOException;

public abstract class CenterScreen extends VBox {

    private CenterPaneController parentController;
    private Node screen;

    protected CenterScreen(String filePath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(filePath));
        loader.setController(this); // Required due to different package declaration from Main
        try {
            screen = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setParentController(CenterPaneController centerScreenController) {
        this.parentController = centerScreenController;
    }

    protected Node getScreen() {
        return this.screen;
    }
}