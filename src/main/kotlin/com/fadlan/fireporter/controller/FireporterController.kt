package com.fadlan.fireporter.controller

import com.fadlan.fireporter.FireporterApp
import com.fadlan.fireporter.model.Theme
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.utils.IconizedAlert
import com.fadlan.fireporter.utils.getProperty
import com.fadlan.fireporter.viewmodel.FireporterViewModel
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.awt.Color
import java.time.YearMonth

class FireporterController(
    private val viewModel: FireporterViewModel,
    private val progressTracker: FxProgressTracker,
    private val logger: Logger
) {
    private val controllerScope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null
    private val icon: Image = Image(FireporterApp::class.java.getResourceAsStream("fireporter-icon.png"))
    private val version: String = getProperty("config/app.properties", "app.version")

    @FXML
    private lateinit var versionLabel: Label

    @FXML
    fun initialize() {
        setupYearComboBox()
        setupPeriodUpdateListener()
        updatePeriodOptions(currentYear)

        hostProtocolComboBox.items.setAll(listOf("http://", "https://"))
        hostProtocolComboBox.selectionModel.select(0)

        registerThemes()
        activeTheme = themeHashMap.getValue("plainThemeBtn")

        progressBar.progressProperty().bind(progressTracker.progressProperty)
        statusLabel.textProperty().bind(progressTracker.messageProperty)

        hostTextField.text = "localhost"
        versionLabel.text = "v$version"
        logger.info("JavaFX controller initiated.")
    }

    private var x: Double = 0.0
    private var y: Double = 0.0
    private lateinit var stage: Stage
    private val themeHashMap = HashMap<String, Theme>()
    private lateinit var activeTheme: Theme

    private val currentYearMonth = YearMonth.now()
    private val currentYear = currentYearMonth.year
    private val currentMonth = currentYearMonth.monthValue

    private val monthNames = listOf(
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )

    @FXML
    private lateinit var vBoxBase: VBox

    @FXML
    private lateinit var closeBtn: Button

    fun setStage(stage: Stage) {
        this.stage = stage
    }

    @FXML
    private fun onCloseBtnClick() {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Exit Fireporter"
            headerText = "Proceed to exit?"
            buttonTypes.setAll(ButtonType.YES, ButtonType.CANCEL)
        }

        val alertStage = alert.dialogPane.scene.window as Stage
        alertStage.icons.add(icon)

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.YES) {
            stage.close()
            logger.info("Application stopped.")
        }
    }

    @FXML
    private fun onMousePressed(event: MouseEvent) {
        x = event.sceneX
        y = event.sceneY
    }

    @FXML
    private fun onMouseDragged(event: MouseEvent) {
        stage.x = event.screenX - x
        stage.y = event.screenY - y
    }

    @FXML
    private lateinit var aboutBtn: Button

    @FXML
    private fun aboutBtnOnAction() {
        val alert = IconizedAlert(
            Alert.AlertType.INFORMATION,
            "About Fireporter",
            "Fireporter: Firefly III PDF Report Generator",
            "Fireporter Version $version\nCompatible with Firefly III v6.x.x\nGithub: vFadlan011/fireporter",
        )

        val iconView = ImageView(icon)
        iconView.fitWidth = 48.0
        iconView.fitHeight = 48.0

        alert.graphic = iconView
        alert.showAndWait()
    }

    @FXML
    private lateinit var hostTextField: TextField

    @FXML
    private lateinit var tokenTextArea: TextArea

    @FXML
    private lateinit var plainThemeBtn: Button
    @FXML
    private lateinit var redThemeBtn: Button
    @FXML
    private lateinit var purpleThemeBtn: Button
    @FXML
    private lateinit var blueThemeBtn: Button
    @FXML
    private lateinit var cyanThemeBtn: Button
    @FXML
    private lateinit var yellowThemeBtn: Button
    @FXML
    private lateinit var greenThemeBtn: Button

    @FXML
    private lateinit var periodComboBox: ComboBox<String>

    @FXML
    private lateinit var yearComboBox: ComboBox<Int>

    private fun setupYearComboBox() {
        yearComboBox.items.setAll(currentYear - 1, currentYear)
        yearComboBox.selectionModel.select(1)
    }

    private fun setupPeriodUpdateListener() {
        yearComboBox.valueProperty().addListener { _, _, selectedYear ->
            updatePeriodOptions(selectedYear)
        }
    }

    private fun updatePeriodOptions(selectedYear: Int) {
        val options = if (selectedYear < currentYear) {
            getFullYearPeriods()
        } else {
            getCurrentYearPeriods()
        }

        periodComboBox.items.setAll(FXCollections.observableArrayList(options))
        periodComboBox.selectionModel.select(currentMonth - 1)
        yearComboBox.selectionModel.select(selectedYear)
    }

    private fun getFullYearPeriods(): List<String> {
        return buildList {
            addAll(monthNames)
            addAll(listOf("Q1", "Q2", "Q3", "H1", "H2", "All Year"))
        }
    }

    private fun getCurrentYearPeriods(): List<String> {
        return buildList {
            addAll(monthNames.take(currentMonth))

            if (currentMonth > 4) add("Q1")  // Jan–Apr
            if (currentMonth > 8) add("Q2")  // May–Aug
            if (currentMonth > 9) add("Q3")  // Sep–Dec
            if (currentMonth > 6) add("H1")  // Jan–Jun

            add("All Year")
        }
    }

    private fun registerThemes() {
        themeHashMap["redThemeBtn"] = Theme("red", redThemeBtn, Color(229, 19, 57), Color(110, 3, 21))
        themeHashMap["yellowThemeBtn"] = Theme("yellow", yellowThemeBtn, Color(237, 165, 21), Color(120, 61, 2))
        themeHashMap["greenThemeBtn"] = Theme("green", greenThemeBtn, Color(36, 171, 71), Color(18, 64, 30))
        themeHashMap["cyanThemeBtn"] = Theme("cyan", cyanThemeBtn, Color(6, 207, 211), Color(2, 83, 84))
        themeHashMap["blueThemeBtn"] = Theme("blue", blueThemeBtn, Color(23, 116, 255), Color(16, 40, 112))
        themeHashMap["purpleThemeBtn"] = Theme("purple", purpleThemeBtn, Color(179, 92, 226), Color(63, 34, 79))
        themeHashMap["plainThemeBtn"] = Theme("plain", plainThemeBtn, Color(28, 55, 115), Color(16, 30, 69))
    }

    @FXML
    private fun themeButtonOnClick(event: ActionEvent) {
        val clicked = event.source as? Button ?: return
        activeTheme.button.text = ""
        activeTheme = themeHashMap.getValue(clicked.id)
        activeTheme.button.text = "X"
    }

    private fun disableAllThemeButtons() {
        for (value in themeHashMap.values) {
            value.button.isDisable = true
        }
    }

    private fun enableAllThemeButtons() {
        for (value in themeHashMap.values) {
            value.button.isDisable = false
        }
    }

    @FXML
    private lateinit var generateBtn: Button

    @FXML
    private lateinit var cancelBtn: Button

    @FXML
    private lateinit var progressBar: ProgressBar

    @FXML
    private lateinit var statusLabel: Label

    @FXML
    private lateinit var hostProtocolComboBox: ComboBox<String>

    @FXML
    private lateinit var includeAttachmentsCheckBox: CheckBox

    @FXML
    private fun generateBtnOnAction() {
        logger.info("Generating PDF for period ${periodComboBox.selectionModel.selectedItem} ${yearComboBox.selectionModel.selectedItem}...")
        val host = hostProtocolComboBox.selectionModel.selectedItemProperty().value + hostTextField.text
        val token = tokenTextArea.text

        disableAllThemeButtons()
        includeAttachmentsCheckBox.isDisable = true
        cancelBtn.isDisable = false

        progressTracker.reset()
        job = controllerScope.launch {
            if (viewModel.testConnection(host, token)) {
                viewModel.generate(periodComboBox, yearComboBox, activeTheme, includeAttachmentsCheckBox)
            }

            includeAttachmentsCheckBox.isDisable = false
            enableAllThemeButtons()
            cancelBtn.isDisable = true
        }
    }

    @FXML
    private fun cancelBtnOnAction() {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Cancel Report"
            headerText = "Are you sure?"
            buttonTypes.setAll(ButtonType.YES, ButtonType.CANCEL)
        }
        val alertStage = alert.dialogPane.scene.window as Stage
        alertStage.icons.add(icon)

        val result = alert.showAndWait()

        if (result.isPresent && result.get() == ButtonType.YES) {
            job?.cancel()
            cancelBtn.isDisable = true
            enableAllThemeButtons()


            IconizedAlert(
                Alert.AlertType.INFORMATION,
                "Process Canceled",
                "Process Canceled",
                "You can close this pop-up"
            ).showAndWait()
            logger.info("PDF Generation process cancelled.")
        }
    }

    @FXML
    private lateinit var testConnectionBtn: Label

    @FXML
    private fun testConnectionOnClick() {
        val host = hostProtocolComboBox.selectionModel.selectedItemProperty().value + hostTextField.text
        val token = tokenTextArea.text

        controllerScope.launch {
            if(viewModel.testConnection(host, token)) {
                IconizedAlert(
                    Alert.AlertType.INFORMATION,
                    "Connection Success",
                    "Connection Success",
                    "Successfully connected to Firefly III installation. You can close this pop-up"
                ).showAndWait()
            }
        }
    }
}
