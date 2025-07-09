package com.fadlan.fireporter

import atlantafx.base.theme.PrimerLight
import com.fadlan.fireporter.controller.FireporterController
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.Ktor
import com.fadlan.fireporter.repository.*
import com.fadlan.fireporter.service.AttachmentService
import com.fadlan.fireporter.service.DataCollectorService
import com.fadlan.fireporter.service.JasperReportService
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.viewmodel.FireporterViewModel
import io.ktor.client.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

class FireporterApp : Application() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(FireporterApp::class.java, *args)
        }

        lateinit var koin: Koin
    }

    override fun init() {
        super.init()

        val appModule = module {
            single<HttpClient> { Ktor.client }
            single<DateRangeResolver> { DateRangeResolver }
            single<CredentialProvider> { CredentialProvider }

            single {
                FxProgressTracker()
            }

            single {
                JasperReportService()
            }

            single {
                AttachmentService(
                    get<FxProgressTracker>(),
                    get<HttpClient>(),
                    get<CredentialProvider>(),
                )
            }

            single {
                AttachmentRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>(),
                )
            }

            single {
                TransactionRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>(),
                    get<AttachmentRepository>()
                )
            }

            single {
                InsightRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>()
                )
            }

            single {
                ChartRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>()
                )
            }

            single {
                AccountRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>()
                )
            }

            single {
                SummaryRepository(
                    get<HttpClient>(),
                    get<CredentialProvider>(),
                    get<AccountRepository>(),
                )
            }

            factory {
                DataCollectorService(
                    get<FxProgressTracker>(),
                    get<AccountRepository>(),
                    get<ChartRepository>(),
                    get<SummaryRepository>(),
                    get<InsightRepository>(),
                    get<TransactionRepository>(),
                    get<AttachmentService>(),
                    get<HttpClient>(),
                    get<CredentialProvider>()
                )
            }

            factory {
                FireporterViewModel(
                    get<HttpClient>(),
                    get<CredentialProvider>(),
                    get<FxProgressTracker>(),
                    get<DateRangeResolver>(),
                    get<DataCollectorService>(),
                    get<JasperReportService>()
                )
            }

            factory {
                FireporterController(
                    get<FireporterViewModel>(),
                    get<FxProgressTracker>()
                )
            }
        }
        val koinApp = startKoin() {
            printLogger()
            modules(appModule)
        }
        koin = koinApp.koin
    }

    override fun start(stage: Stage) {
        setUserAgentStylesheet(PrimerLight().userAgentStylesheet)

        val fxmlLoader = FXMLLoader(FireporterApp::class.java.getResource("fireporter.fxml"))
        fxmlLoader.setControllerFactory { clazz -> koin.get(clazz.kotlin) }

        val scene = Scene(fxmlLoader.load(), 560.0, 625.0)
        val controller = fxmlLoader.getController<FireporterController>()
        controller.setStage(stage)

        scene.fill = Color.TRANSPARENT
        scene.root.effect = DropShadow(1.00, 4.00, 4.00, Color(0.0, 0.0, 0.0, 0.1))
        stage.initStyle(StageStyle.TRANSPARENT)

        stage.title = "Fireporter: Firefly III Report Generator"
        stage.icons.add(Image(FireporterApp::class.java.getResourceAsStream("fireporter-icon.png")))
        stage.scene = scene

        stage.show()
    }
}