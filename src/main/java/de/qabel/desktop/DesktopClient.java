package de.qabel.desktop;

import com.airhacks.afterburner.injection.Injector;
import de.qabel.core.config.Account;
import de.qabel.core.config.Persistence;
import de.qabel.core.config.SQLitePersistence;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.desktop.config.ClientConfiguration;
import de.qabel.desktop.config.factory.ClientConfigurationFactory;
import de.qabel.desktop.config.factory.DropUrlGenerator;
import de.qabel.desktop.repository.AccountRepository;
import de.qabel.desktop.repository.ClientConfigurationRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.persistence.PersistenceAccountRepository;
import de.qabel.desktop.repository.persistence.PersistenceClientConfigurationRepository;
import de.qabel.desktop.repository.persistence.PersistenceIdentityRepository;
import de.qabel.desktop.ui.LayoutView;
import de.qabel.desktop.ui.accounting.login.LoginView;
import de.qabel.desktop.ui.inject.RecursiveInjectionInstanceSupplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;

import java.net.URISyntaxException;
import java.util.*;


public class DesktopClient extends Application {
	private static final String TITLE = "Qabel Desktop Client";

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		setUserAgentStylesheet(STYLESHEET_MODENA);

		final Map<String, Object> customProperties = new HashMap<>();
		ClientConfiguration config = initDiContainer(customProperties);

		Scene scene;
		if (!config.hasAccount()) {
			scene = new Scene(new LoginView().getView(), 370, 530);
			config.addObserver((o, arg) -> {
				if (arg instanceof Account) {
					Scene layoutScene = new Scene(new LayoutView().getView(), 800, 600, true, SceneAntialiasing.BALANCED);
					Platform.runLater(() -> primaryStage.setScene(layoutScene));
				}
			});
		} else {
			scene = new Scene(new LayoutView().getView(), 800, 600, true, SceneAntialiasing.BALANCED);
		}

		primaryStage.setScene(scene);

		primaryStage.setTitle(TITLE);
		primaryStage.show();
	}

	private ClientConfiguration initDiContainer(Map<String, Object> customProperties) throws QblInvalidEncryptionKeyException, URISyntaxException {
		Persistence<String> persistence = new SQLitePersistence("db.sqlite", "qabel".toCharArray(), 65536);
		customProperties.put("persistence", persistence);
		customProperties.put("dropUrlGenerator", new DropUrlGenerator("http://localhost:5000"));
		PersistenceIdentityRepository identityRepository = new PersistenceIdentityRepository(persistence);
		customProperties.put("identityRepository", identityRepository);
		PersistenceAccountRepository accountRepository = new PersistenceAccountRepository(persistence);
		customProperties.put("accountRepository", accountRepository);
		ClientConfiguration clientConfig = getClientConfiguration(persistence, identityRepository, accountRepository);
		customProperties.put("clientConfiguration", clientConfig);

		Injector.setConfigurationSource(customProperties::get);
		Injector.setInstanceSupplier(new RecursiveInjectionInstanceSupplier(customProperties));
		return clientConfig;
	}

	private ClientConfiguration getClientConfiguration(Persistence<String> persistence, IdentityRepository identityRepository, AccountRepository accountRepository) {
		ClientConfigurationRepository repo = new PersistenceClientConfigurationRepository(
				persistence,
				new ClientConfigurationFactory(),
				identityRepository,
				accountRepository
		);
		final ClientConfiguration config = repo.load();
		config.addObserver((o, arg) -> {
			repo.save(config);
		});
		return config;
	}

	public static Account getBoxAccount() {
		return new Account("http://localhost:9696", "testuser", "testuser");
	}

}
