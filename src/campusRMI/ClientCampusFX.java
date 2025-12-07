package campusRMI;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.Node;
import java.time.LocalDate;

public class ClientCampusFX extends Application {
    // Référence RMI
    private IGestionCampus gestion;

    // TCP pour les notifications
    private Socket tcpSocket;
    private PrintWriter tcpOut;
    private BufferedReader tcpIn;
    private boolean tcpRunning = true;
    private Thread tcpListenerThread;

    // Données
    private ObservableList<Chambre> chambresList = FXCollections.observableArrayList();
    private ObservableList<Client> clientsList = FXCollections.observableArrayList();
    private ObservableList<Reservation> reservationsList = FXCollections.observableArrayList();
    private ObservableList<NotificationItem> notificationsList = FXCollections.observableArrayList();

    // Composants UI
    private BorderPane root;
    private VBox sidebar;
    private StackPane contentArea;
    private StackPane notificationPane;

    // Éléments de navigation
    private Button btnChambres, btnClients, btnReservations, btnCarte, btnNotifications;
    private Label lblUserStatus;
    private Circle connectionIndicator;

    // Pages
    private Node chambresPage, clientsPage, reservationsPage, cartePage, notificationsPage;

    // Données en temps réel
    private int totalChambres = 0;
    private int totalClients = 0;
    private int totalReservations = 0;
    private double revenusTotal = 0.0;

    // Labels pour les statistiques
    private Label statChambresValue, statClientsValue, statReservationsValue;

    // Labels pour les mini-statistiques des chambres
    private Label miniStatTotalValue, miniStatDispoValue, miniStatOccupeesValue, miniStatReserveesValue, miniStatMaintenanceValue;

    // Composants de recherche
    private TableView<Chambre> tableChambres;
    private TableView<Client> tableClients;
    private TableView<Reservation> tableReservations;

    // Carte
    private GridPane campusMap = new GridPane();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialisation RMI
            initializeRMI();

            // Créer la structure principale
            root = new BorderPane();
            root.getStyleClass().add("root-pane");

            // Créer les différentes sections
            createHeader();
            createSidebar();
            createContentArea();
            createNotificationPane();

            // Créer les pages
            createChambresPage();
            createClientsPage();
            createReservationsPage();
            createCartePage();
            createNotificationsPage();

            // Afficher la page chambres par défaut
            showPage(chambresPage);
            btnChambres.getStyleClass().add("active");

            // Configurer la scène
            Scene scene = new Scene(root, 1600, 900);
            try {
                scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            } catch (Exception e) {
                System.err.println("CSS non chargé: " + e.getMessage());
            }

            primaryStage.setTitle("Campus RMI Management Suite v3.0");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Animation d'entrée
            fadeInContent();

            // Charger les données
            loadInitialData();

            // Démarrer les mises à jour en temps réel
            startRealTimeUpdates();

        } catch (Exception e) {
            showErrorAlert("Erreur d'initialisation", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeRMI() {
        try {
            System.out.println("Connexion au serveur RMI...");
            gestion = (IGestionCampus) Naming.lookup("rmi://localhost:1099/GestionCampus");
            System.out.println("Connecté au serveur RMI avec succès");

            // Démarrer TCP
            startTCPListener();

            // Mettre à jour l'indicateur de connexion
            if (connectionIndicator != null) {
                connectionIndicator.setFill(Color.web("#00C853"));
                connectionIndicator.setEffect(new DropShadow(10, Color.web("#00C853")));
            }

        } catch (Exception e) {
            showErrorAlert("Erreur RMI", "Impossible de se connecter au serveur RMI: " + e.getMessage());
            System.exit(1);
        }
    }

    private void startTCPListener() {
        try {
            tcpSocket = new Socket("localhost", 9999);
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

            tcpListenerThread = new Thread(() -> {
                try {
                    String message;
                    while (tcpRunning && (message = tcpIn.readLine()) != null) {
                        String finalMsg = message;
                        javafx.application.Platform.runLater(() -> {
                            addNotification("TCP", finalMsg);
                        });
                    }
                } catch (IOException e) {
                    if (tcpRunning) {
                        System.err.println("TCP Error: " + e.getMessage());
                    }
                }
            });
            tcpListenerThread.setDaemon(true);
            tcpListenerThread.start();

        } catch (Exception e) {
            System.err.println("TCP Connection Failed: " + e.getMessage());
        }
    }

    private void createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 40, 15, 40));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("main-header");

        // Logo
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Circle logoCircle = new Circle(20);
        logoCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#667eea")),
                new Stop(1, Color.web("#764ba2"))));
        logoCircle.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.3)));

        Label logoText = new Label("RMI CAMPUS");
        logoText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        logoText.setTextFill(Color.WHITE);

        logoBox.getChildren().addAll(logoCircle, logoText);

        // Centre : titre
        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.CENTER);

        Label title = new Label("Gestion de Campus Distribué");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Remote Method Invocation - Architecture Client/Serveur");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subtitle.setTextFill(Color.rgb(255, 255, 255, 0.8));

        centerBox.getChildren().addAll(title, subtitle);

        // Droite : statut utilisateur
        HBox userBox = new HBox(15);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        // Indicateur de connexion
        connectionIndicator = new Circle(6);
        connectionIndicator.setFill(Color.web("#FF5252"));

        lblUserStatus = new Label("Déconnecté");
        lblUserStatus.setTextFill(Color.WHITE);
        lblUserStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        userBox.getChildren().addAll(connectionIndicator, lblUserStatus);

        // Ajouter au header
        header.getChildren().addAll(logoBox, centerBox, userBox);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        root.setTop(header);
    }

    private void createSidebar() {
        sidebar = new VBox(0);
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("sidebar");

        // En-tête sidebar
        VBox sidebarHeader = new VBox(10);
        sidebarHeader.setPadding(new Insets(30, 20, 30, 20));
        sidebarHeader.getStyleClass().add("sidebar-header");

        Label sidebarTitle = new Label("NAVIGATION");
        sidebarTitle.getStyleClass().add("sidebar-title");

        Label sidebarSubtitle = new Label("Gestion complète du campus");
        sidebarSubtitle.getStyleClass().add("sidebar-subtitle");

        sidebarHeader.getChildren().addAll(sidebarTitle, sidebarSubtitle);

        // Boutons de navigation
        VBox navButtons = new VBox(5);
        navButtons.setPadding(new Insets(10, 0, 20, 0));

        btnChambres = createNavButton("Gestion Chambres", false);
        btnClients = createNavButton("Gestion Clients", false);
        btnReservations = createNavButton("Réservations", false);
        btnCarte = createNavButton("Carte Interactive", false);
        btnNotifications = createNavButton("Notifications", false);

        navButtons.getChildren().addAll(btnChambres, btnClients,
                btnReservations, btnCarte, btnNotifications);

        // Statistiques rapides
        VBox quickStats = new VBox(15);
        quickStats.setPadding(new Insets(20));
        quickStats.getStyleClass().add("quick-stats");

        Label statsTitle = new Label("STATISTIQUES RAPIDES");
        statsTitle.getStyleClass().add("stats-title");

        // Indicateurs avec référence aux labels
        HBox chambreStat = createStatIndicator("Chambres", "#667eea");
        HBox clientStat = createStatIndicator("Clients", "#00C853");
        HBox resaStat = createStatIndicator("Réservations", "#FF9800");

        quickStats.getChildren().addAll(statsTitle, chambreStat, clientStat, resaStat);

        // Footer sidebar
        VBox sidebarFooter = new VBox(10);
        sidebarFooter.setPadding(new Insets(20));
        sidebarFooter.setAlignment(Pos.CENTER);

        Button btnLogout = new Button("Déconnexion");
        btnLogout.getStyleClass().add("logout-button");
        btnLogout.setOnAction(e -> logout());

        sidebarFooter.getChildren().add(btnLogout);

        // Assembler la sidebar
        sidebar.getChildren().addAll(sidebarHeader, navButtons, quickStats, sidebarFooter);
        VBox.setVgrow(navButtons, Priority.ALWAYS);

        root.setLeft(sidebar);
    }

    private Button createNavButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        if (active) btn.getStyleClass().add("active");

        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(15, 20, 15, 20));

        // Effet de survol
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyleClass().contains("active")) {
                btn.getStyleClass().add("hover");
            }
        });

        btn.setOnMouseExited(e -> {
            btn.getStyleClass().remove("hover");
        });

        // Action
        btn.setOnAction(e -> {
            // Retirer la classe active de tous les boutons
            for (Node node : ((VBox)btn.getParent()).getChildren()) {
                if (node instanceof Button) {
                    ((Button) node).getStyleClass().remove("active");
                }
            }
            // Ajouter la classe active au bouton cliqué
            btn.getStyleClass().add("active");

            // Afficher la page correspondante
            switch (text) {
                case "Gestion Chambres": showPage(chambresPage); break;
                case "Gestion Clients": showPage(clientsPage); break;
                case "Réservations": showPage(reservationsPage); break;
                case "Carte Interactive": showPage(cartePage); break;
                case "Notifications": showPage(notificationsPage); break;
            }
        });

        return btn;
    }

    private HBox createStatIndicator(String label, String color) {
        HBox statBox = new HBox(10);
        statBox.setAlignment(Pos.CENTER_LEFT);

        Circle indicator = new Circle(4);
        indicator.setFill(Color.web(color));

        Label statLabel = new Label(label);
        statLabel.getStyleClass().add("stat-label");

        Label statValue = new Label("0");
        statValue.getStyleClass().add("stat-value");

        // Stocker la référence pour mise à jour
        switch (label) {
            case "Chambres": statChambresValue = statValue; break;
            case "Clients": statClientsValue = statValue; break;
            case "Réservations": statReservationsValue = statValue; break;
        }

        HBox.setHgrow(statLabel, Priority.ALWAYS);
        statBox.getChildren().addAll(indicator, statLabel, statValue);

        return statBox;
    }

    private HBox createLegendItem(String text, String color) {
        HBox legendItem = new HBox(10);
        legendItem.setAlignment(Pos.CENTER_LEFT);

        Rectangle colorBox = new Rectangle(12, 12);
        colorBox.setFill(Color.web(color));
        colorBox.setArcWidth(3);
        colorBox.setArcHeight(3);

        Label legendText = new Label(text);
        legendText.getStyleClass().add("legend-text");

        legendItem.getChildren().addAll(colorBox, legendText);
        return legendItem;
    }

    private void createContentArea() {
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        root.setCenter(contentArea);
    }

    private void createNotificationPane() {
        notificationPane = new StackPane();
        notificationPane.setPadding(new Insets(10));
        notificationPane.setVisible(false);
        root.setBottom(notificationPane);
    }

    private void createChambresPage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox pageContent = new VBox(25);
        pageContent.setPadding(new Insets(30));

        // En-tête
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des Chambres");
        title.getStyleClass().add("page-title");

        Button addBtn = new Button("Ajouter une chambre");
        addBtn.getStyleClass().add("action-button");
        addBtn.setOnAction(e -> showAddChambreDialog());

        HBox.setHgrow(title, Priority.ALWAYS);
        headerBox.getChildren().addAll(title, addBtn);

        // Tableau des chambres
        tableChambres = new TableView<>();
        tableChambres.getStyleClass().add("modern-table");
        tableChambres.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Chambre, String> colNumero = new TableColumn<>("Numéro");
        colNumero.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().numero));
        colNumero.setPrefWidth(100);

        TableColumn<Chambre, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type_chambre));
        colType.setPrefWidth(120);

        TableColumn<Chambre, Integer> colCapacite = new TableColumn<>("Capacité");
        colCapacite.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().capacite).asObject());
        colCapacite.setPrefWidth(80);

        TableColumn<Chambre, Double> colPrix = new TableColumn<>("Prix/jour");
        colPrix.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().prix_par_jour).asObject());
        colPrix.setCellFactory(tc -> new TableCell<Chambre, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f D", item));
                    setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Chambre, String> colEquipements = new TableColumn<>("Équipements");
        colEquipements.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().equipements));
        colEquipements.setPrefWidth(200);

        TableColumn<Chambre, String> colDescription = new TableColumn<>("Description");
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().description));
        colDescription.setPrefWidth(200);

        TableColumn<Chambre, String> colStatut = new TableColumn<>("Statut");
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statut));
        colStatut.setCellFactory(tc -> new TableCell<Chambre, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Disponible": setStyle("-fx-text-fill: #00C853; -fx-font-weight: bold;"); break;
                        case "Occupée": setStyle("-fx-text-fill: #FF5252; -fx-font-weight: bold;"); break;
                        case "Réservée": setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); break;
                        case "Maintenance": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "En nettoyage": setStyle("-fx-text-fill: #9E9E9E; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });

        TableColumn<Chambre, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(200);
        colActions.setCellFactory(param -> new TableCell<Chambre, Void>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");

            {
                editBtn.getStyleClass().add("table-button");
                deleteBtn.getStyleClass().add("table-button-danger");

                editBtn.setOnAction(e -> {
                    Chambre chambre = getTableView().getItems().get(getIndex());
                    editChambre(chambre);
                });

                deleteBtn.setOnAction(e -> {
                    Chambre chambre = getTableView().getItems().get(getIndex());
                    deleteChambre(chambre);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });

        tableChambres.getColumns().addAll(colNumero, colType, colCapacite, colPrix,
                colEquipements, colDescription, colStatut, colActions);
        tableChambres.setItems(chambresList);

        // Statistiques rapides
        HBox quickStats = new HBox(20);

        VBox statTotal = createMiniStatCard("Total chambres", "0", "#667eea");
        VBox statDispo = createMiniStatCard("Disponibles", "0", "#00C853");
        VBox statOccupees = createMiniStatCard("Occupées", "0", "#FF5252");
        VBox statReservees = createMiniStatCard("Réservées", "0", "#2196F3");
        VBox statMaintenance = createMiniStatCard("Maintenance", "0", "#FF9800");
        VBox statNettoyage = createMiniStatCard("En nettoyage", "0", "#9E9E9E");

        // Stocker les références pour mise à jour
        miniStatTotalValue = (Label) statTotal.getChildren().get(1);
        miniStatDispoValue = (Label) statDispo.getChildren().get(1);
        miniStatOccupeesValue = (Label) statOccupees.getChildren().get(1);
        miniStatReserveesValue = (Label) statReservees.getChildren().get(1);
        miniStatMaintenanceValue = (Label) statMaintenance.getChildren().get(1);

        quickStats.getChildren().addAll(statTotal, statDispo, statOccupees, statReservees, statMaintenance, statNettoyage);

        pageContent.getChildren().addAll(headerBox, quickStats, tableChambres);
        scrollPane.setContent(pageContent);

        chambresPage = scrollPane;
    }

    private VBox createMiniStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("mini-stat-card");
        card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 3 0;");

        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("mini-stat-title");

        Label cardValue = new Label(value);
        cardValue.getStyleClass().add("mini-stat-value");
        cardValue.setStyle("-fx-text-fill: " + color + ";");

        card.getChildren().addAll(cardTitle, cardValue);
        return card;
    }

    private void createClientsPage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox pageContent = new VBox(25);
        pageContent.setPadding(new Insets(30));

        // En-tête
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des Clients");
        title.getStyleClass().add("page-title");

        Button addBtn = new Button("Ajouter un client");
        addBtn.getStyleClass().add("action-button");
        addBtn.setOnAction(e -> showAddClientDialog());

        HBox.setHgrow(title, Priority.ALWAYS);
        headerBox.getChildren().addAll(title, addBtn);

        // Tableau des clients
        tableClients = new TableView<>();
        tableClients.getStyleClass().add("modern-table");
        tableClients.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Client, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nom));
        colNom.setPrefWidth(150);

        TableColumn<Client, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email));
        colEmail.setPrefWidth(200);

        TableColumn<Client, String> colTelephone = new TableColumn<>("Téléphone");
        colTelephone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().telephone));
        colTelephone.setPrefWidth(120);

        TableColumn<Client, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().type_client));
        colType.setPrefWidth(100);

        TableColumn<Client, String> colOrganisation = new TableColumn<>("Organisation");
        colOrganisation.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().organisation));
        colOrganisation.setPrefWidth(150);

        TableColumn<Client, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(250);
        colActions.setCellFactory(param -> new TableCell<Client, Void>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button reservBtn = new Button("Nouvelle réservation");

            {
                editBtn.getStyleClass().add("table-button");
                deleteBtn.getStyleClass().add("table-button-danger");
                reservBtn.getStyleClass().add("table-button");

                editBtn.setOnAction(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    editClient(client);
                });

                deleteBtn.setOnAction(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    deleteClient(client);
                });

                reservBtn.setOnAction(e -> {
                    Client client = getTableView().getItems().get(getIndex());
                    showAddReservationDialog(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, reservBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });

        tableClients.getColumns().addAll(colNom, colEmail, colTelephone, colType, colOrganisation, colActions);
        tableClients.setItems(clientsList);

        pageContent.getChildren().addAll(headerBox, tableClients);
        scrollPane.setContent(pageContent);

        clientsPage = scrollPane;
    }

    private void createReservationsPage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox pageContent = new VBox(25);
        pageContent.setPadding(new Insets(30));

        // En-tête
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des Réservations");
        title.getStyleClass().add("page-title");

        Button addBtn = new Button("Nouvelle réservation");
        addBtn.getStyleClass().add("action-button");
        addBtn.setOnAction(e -> showAddReservationDialog(null));

        HBox.setHgrow(title, Priority.ALWAYS);
        headerBox.getChildren().addAll(title, addBtn);

        // Tableau des réservations
        tableReservations = new TableView<>();
        tableReservations.getStyleClass().add("modern-table");
        tableReservations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reservation, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(data -> {
            try {
                Client client = gestion.rechercherClient(data.getValue().client_id);
                return new SimpleStringProperty(client.nom);
            } catch (RemoteException e) {
                return new SimpleStringProperty("Inconnu");
            }
        });
        colClient.setPrefWidth(200);

        TableColumn<Reservation, String> colChambre = new TableColumn<>("Chambre");
        colChambre.setCellValueFactory(data -> {
            try {
                Chambre chambre = gestion.rechercherChambre(data.getValue().chambre_id);
                return new SimpleStringProperty(chambre.numero);
            } catch (RemoteException e) {
                return new SimpleStringProperty("Inconnue");
            }
        });
        colChambre.setPrefWidth(100);

        TableColumn<Reservation, String> colDates = new TableColumn<>("Dates");
        colDates.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().date_debut + " - " + data.getValue().date_fin));
        colDates.setPrefWidth(200);

        TableColumn<Reservation, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().total).asObject());
        colTotal.setCellFactory(tc -> new TableCell<Reservation, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f D", item));
                    setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Reservation, String> colStatut = new TableColumn<>("Statut");
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statut));
        colStatut.setCellFactory(tc -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Confirmée": setStyle("-fx-text-fill: #00C853; -fx-font-weight: bold;"); break;
                        case "En attente": setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); break;
                        case "Annulée": setStyle("-fx-text-fill: #FF5252; -fx-font-weight: bold;"); break;
                        case "Terminée": setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;"); break;
                    }
                }
            }
        });

        TableColumn<Reservation, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(200);
        colActions.setCellFactory(param -> new TableCell<Reservation, Void>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Annuler");

            {
                editBtn.getStyleClass().add("table-button");
                deleteBtn.getStyleClass().add("table-button-danger");

                editBtn.setOnAction(e -> {
                    Reservation resa = getTableView().getItems().get(getIndex());
                    editReservation(resa);
                });

                deleteBtn.setOnAction(e -> {
                    Reservation resa = getTableView().getItems().get(getIndex());
                    deleteReservation(resa);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });

        tableReservations.getColumns().addAll(colClient, colChambre, colDates, colTotal, colStatut, colActions);
        tableReservations.setItems(reservationsList);

        // Statistiques des revenus
        HBox revenueStats = new HBox(20);
        VBox revenueCard = createMiniStatCard("Revenus totaux", "0 D", "#9C27B0");
        VBox activeCard = createMiniStatCard("Réservations actives", "0", "#00C853");
        revenueStats.getChildren().addAll(revenueCard, activeCard);

        pageContent.getChildren().addAll(headerBox, revenueStats, tableReservations);
        scrollPane.setContent(pageContent);

        reservationsPage = scrollPane;
    }

    private void createCartePage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox pageContent = new VBox(25);
        pageContent.setPadding(new Insets(30));
        pageContent.setAlignment(Pos.CENTER);

        Label title = new Label("Carte Interactive du Campus");
        title.getStyleClass().add("page-title");

        // Légende
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);

        legend.getChildren().addAll(
                createLegendItem("Disponible", "#00C853"),
                createLegendItem("Occupée", "#FF5252"),
                createLegendItem("Réservée", "#2196F3"),
                createLegendItem("Maintenance", "#FF9800"),
                createLegendItem("En nettoyage", "#9E9E9E")
        );

        // Carte
        campusMap.setHgap(15);
        campusMap.setVgap(15);
        campusMap.setPadding(new Insets(20));
        campusMap.setAlignment(Pos.CENTER);

        // Boutons de contrôle
        HBox controlButtons = new HBox(15);
        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setPadding(new Insets(20, 0, 0, 0));

        Button btnRefresh = new Button("Actualiser la carte");
        btnRefresh.getStyleClass().add("action-button");
        btnRefresh.setOnAction(e -> updateCarte());

        controlButtons.getChildren().add(btnRefresh);

        pageContent.getChildren().addAll(title, legend, campusMap, controlButtons);

        scrollPane.setContent(pageContent);
        cartePage = scrollPane;
    }

    private void updateCarte() {
        campusMap.getChildren().clear();

        int col = 0;
        int row = 0;
        int maxCols = 8;

        for (Chambre chambre : chambresList) {
            VBox roomCard = createRoomCard(chambre);
            campusMap.add(roomCard, col, row);
            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createRoomCard(Chambre chambre) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("room-card");

        // Déterminer la couleur en fonction du statut
        String color;
        switch (chambre.statut) {
            case "Disponible": color = "#00C853"; break;
            case "Occupée": color = "#FF5252"; break;
            case "Réservée": color = "#2196F3"; break;
            case "Maintenance": color = "#FF9800"; break;
            case "En nettoyage": color = "#9E9E9E"; break;
            default: color = "#9E9E9E";
        }

        // Appliquer la couleur de bordure
        card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2;");

        Label roomLabel = new Label("Chambre " + chambre.numero);
        roomLabel.getStyleClass().add("room-label");

        Label typeLabel = new Label(chambre.type_chambre);
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Label statusLabel = new Label(chambre.statut);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");

        Label priceLabel = new Label(String.format("%.2f D/j", chambre.prix_par_jour));
        priceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");

        card.getChildren().addAll(roomLabel, typeLabel, statusLabel, priceLabel);

        // Tooltip avec informations détaillées
        Tooltip tooltip = new Tooltip(
                "Chambre: " + chambre.numero + "\n" +
                        "Type: " + chambre.type_chambre + "\n" +
                        "Capacité: " + chambre.capacite + " personnes\n" +
                        "Prix/jour: " + String.format("%.2f", chambre.prix_par_jour) + " D\n" +
                        "Statut: " + chambre.statut + "\n" +
                        "Équipements: " + chambre.equipements
        );
        tooltip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(card, tooltip);

        return card;
    }

    private void createNotificationsPage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox pageContent = new VBox(25);
        pageContent.setPadding(new Insets(30));

        Label title = new Label("Historique des Activités");
        title.getStyleClass().add("page-title");

        // Liste des notifications
        ListView<NotificationItem> listView = new ListView<>();
        listView.setItems(notificationsList);
        listView.setPrefHeight(600);
        listView.setCellFactory(param -> new ListCell<NotificationItem>() {
            @Override
            protected void updateItem(NotificationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox notificationBox = new VBox(5);
                    notificationBox.setPadding(new Insets(10));
                    notificationBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

                    HBox header = new HBox(10);
                    header.setAlignment(Pos.CENTER_LEFT);

                    Label typeLabel = new Label(item.getType());
                    typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + item.getColor() + ";");

                    Label timeLabel = new Label(item.getTime());
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");

                    HBox.setHgrow(timeLabel, Priority.ALWAYS);
                    header.getChildren().addAll(typeLabel, timeLabel);

                    Label messageLabel = new Label(item.getMessage());
                    messageLabel.setStyle("-fx-font-size: 13px;");
                    messageLabel.setWrapText(true);

                    notificationBox.getChildren().addAll(header, messageLabel);
                    setGraphic(notificationBox);
                }
            }
        });

        // Boutons d'action
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button clearAll = new Button("Effacer l'historique");
        clearAll.getStyleClass().add("notification-button-danger");
        clearAll.setOnAction(e -> notificationsList.clear());

        actionButtons.getChildren().addAll(clearAll);

        pageContent.getChildren().addAll(title, listView, actionButtons);

        scrollPane.setContent(pageContent);
        notificationsPage = scrollPane;
    }

    private void showPage(Node page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), page);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showAddChambreDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une nouvelle chambre");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField tfNumero = new TextField();
        tfNumero.setPromptText("Ex: A101");

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Simple", "Double", "Suite", "Dortoir");
        cbType.setPromptText("Sélectionner le type");

        TextField tfCapacite = new TextField();
        tfCapacite.setPromptText("Ex: 2");

        TextField tfPrix = new TextField();
        tfPrix.setPromptText("Ex: 150.00");

        TextArea taEquipements = new TextArea();
        taEquipements.setPromptText("Ex: TV, WiFi, Climatisation, Salle de bain");
        taEquipements.setPrefRowCount(3);

        TextArea taDescription = new TextArea();
        taDescription.setPromptText("Ex: Chambre spacieuse avec vue sur le jardin");
        taDescription.setPrefRowCount(3);

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Disponible", "Maintenance", "En nettoyage");
        cbStatut.setValue("Disponible");

        int row = 0;
        grid.add(new Label("Numéro*:"), 0, row); grid.add(tfNumero, 1, row++);
        grid.add(new Label("Type*:"), 0, row); grid.add(cbType, 1, row++);
        grid.add(new Label("Capacité*:"), 0, row); grid.add(tfCapacite, 1, row++);
        grid.add(new Label("Prix/jour*:"), 0, row); grid.add(tfPrix, 1, row++);
        grid.add(new Label("Équipements:"), 0, row); grid.add(taEquipements, 1, row++);
        grid.add(new Label("Description:"), 0, row); grid.add(taDescription, 1, row++);
        grid.add(new Label("Statut*:"), 0, row); grid.add(cbStatut, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    // Validation des champs obligatoires
                    if (tfNumero.getText().isEmpty() || cbType.getValue() == null ||
                            tfCapacite.getText().isEmpty() || tfPrix.getText().isEmpty() || cbStatut.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires (*)");
                        return null;
                    }

                    gestion.ajouterChambre(
                            tfNumero.getText(),
                            cbType.getValue(),
                            Integer.parseInt(tfCapacite.getText()),
                            Double.parseDouble(tfPrix.getText()),
                            taEquipements.getText(),
                            taDescription.getText(),
                            cbStatut.getValue()
                    );
                    addNotification("AJOUT", "Nouvelle chambre ajoutée: " + tfNumero.getText() +
                            " (Type: " + cbType.getValue() + ", Statut: " + cbStatut.getValue() + ")");
                    loadInitialData();
                    updateCarte();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Veuillez vérifier les données: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editChambre(Chambre chambre) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier la chambre");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField tfNumero = new TextField(chambre.numero);
        tfNumero.setPromptText("Numéro de chambre");

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Simple", "Double", "Suite", "Dortoir");
        cbType.setValue(chambre.type_chambre);

        TextField tfCapacite = new TextField(String.valueOf(chambre.capacite));
        tfCapacite.setPromptText("Capacité");

        TextField tfPrix = new TextField(String.valueOf(chambre.prix_par_jour));
        tfPrix.setPromptText("Prix par jour");

        TextArea taEquipements = new TextArea(chambre.equipements);
        taEquipements.setPromptText("Équipements");
        taEquipements.setPrefRowCount(3);

        TextArea taDescription = new TextArea(chambre.description);
        taDescription.setPromptText("Description");
        taDescription.setPrefRowCount(3);

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Disponible", "Occupée", "Réservée", "Maintenance", "En nettoyage");
        cbStatut.setValue(chambre.statut);

        int row = 0;
        grid.add(new Label("Numéro:"), 0, row); grid.add(tfNumero, 1, row++);
        grid.add(new Label("Type:"), 0, row); grid.add(cbType, 1, row++);
        grid.add(new Label("Capacité:"), 0, row); grid.add(tfCapacite, 1, row++);
        grid.add(new Label("Prix/jour:"), 0, row); grid.add(tfPrix, 1, row++);
        grid.add(new Label("Équipements:"), 0, row); grid.add(taEquipements, 1, row++);
        grid.add(new Label("Description:"), 0, row); grid.add(taDescription, 1, row++);
        grid.add(new Label("Statut:"), 0, row); grid.add(cbStatut, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    // Validation
                    if (tfNumero.getText().isEmpty() || cbType.getValue() == null ||
                            tfCapacite.getText().isEmpty() || tfPrix.getText().isEmpty() || cbStatut.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
                        return null;
                    }

                    gestion.modifierChambre(
                            chambre.chambre_id,
                            tfNumero.getText(),
                            cbType.getValue(),
                            Integer.parseInt(tfCapacite.getText()),
                            Double.parseDouble(tfPrix.getText()),
                            taEquipements.getText(),
                            taDescription.getText()
                    );

                    if (!chambre.statut.equals(cbStatut.getValue())) {
                        gestion.majStatutChambre(chambre.chambre_id, cbStatut.getValue());
                    }

                    addNotification("MODIFICATION", "Chambre modifiée: " + tfNumero.getText() +
                            " (Nouveau statut: " + cbStatut.getValue() + ")");
                    loadInitialData();
                    updateCarte();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteChambre(Chambre chambre) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression de chambre");
        alert.setHeaderText("Supprimer la chambre " + chambre.numero + " ?");
        alert.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestion.supprimerChambre(chambre.chambre_id);
                    addNotification("SUPPRESSION", "Chambre supprimée: " + chambre.numero +
                            " (Type: " + chambre.type_chambre + ")");
                    loadInitialData();
                    updateCarte();
                } catch (RemoteException e) {
                    showErrorAlert("Erreur", "Impossible de supprimer la chambre: " + e.getMessage());
                }
            }
        });
    }

    private void showAddClientDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un nouveau client");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField tfNom = new TextField();
        tfNom.setPromptText("Ex: Mariam Leffi");

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Ex: mariamleffi@email.com");

        TextField tfTelephone = new TextField();
        tfTelephone.setPromptText("Ex: +216 27 860 277");

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Individuel", "Entreprise", "Étudiant");
        cbType.setValue("Individuel");

        TextField tfOrganisation = new TextField();
        tfOrganisation.setPromptText("Ex: Telnet(optionnel)");

        int row = 0;
        grid.add(new Label("Nom*:"), 0, row); grid.add(tfNom, 1, row++);
        grid.add(new Label("Email*:"), 0, row); grid.add(tfEmail, 1, row++);
        grid.add(new Label("Téléphone*:"), 0, row); grid.add(tfTelephone, 1, row++);
        grid.add(new Label("Type*:"), 0, row); grid.add(cbType, 1, row++);
        grid.add(new Label("Organisation:"), 0, row); grid.add(tfOrganisation, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    // Validation
                    if (tfNom.getText().isEmpty() || tfEmail.getText().isEmpty() ||
                            tfTelephone.getText().isEmpty() || cbType.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires (*)");
                        return null;
                    }

                    gestion.creerClient(
                            tfNom.getText(),
                            tfEmail.getText(),
                            tfTelephone.getText(),
                            cbType.getValue(),
                            tfOrganisation.getText()
                    );
                    addNotification("AJOUT", "Nouveau client ajouté: " + tfNom.getText() +
                            " (Type: " + cbType.getValue() + ")");
                    loadInitialData();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editClient(Client client) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier le client");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField tfNom = new TextField(client.nom);
        TextField tfEmail = new TextField(client.email);
        TextField tfTelephone = new TextField(client.telephone);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Individuel", "Entreprise", "Étudiant");
        cbType.setValue(client.type_client);

        TextField tfOrganisation = new TextField(client.organisation != null ? client.organisation : "");

        int row = 0;
        grid.add(new Label("Nom*:"), 0, row); grid.add(tfNom, 1, row++);
        grid.add(new Label("Email*:"), 0, row); grid.add(tfEmail, 1, row++);
        grid.add(new Label("Téléphone*:"), 0, row); grid.add(tfTelephone, 1, row++);
        grid.add(new Label("Type*:"), 0, row); grid.add(cbType, 1, row++);
        grid.add(new Label("Organisation:"), 0, row); grid.add(tfOrganisation, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    // Validation
                    if (tfNom.getText().isEmpty() || tfEmail.getText().isEmpty() ||
                            tfTelephone.getText().isEmpty() || cbType.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires (*)");
                        return null;
                    }

                    gestion.modifierClient(
                            client.client_id,
                            tfNom.getText(),
                            tfEmail.getText(),
                            tfTelephone.getText(),
                            cbType.getValue(),
                            tfOrganisation.getText()
                    );
                    addNotification("MODIFICATION", "Client modifié: " + tfNom.getText() +
                            " (Nouveau type: " + cbType.getValue() + ")");
                    loadInitialData();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteClient(Client client) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression de client");
        alert.setHeaderText("Supprimer le client " + client.nom + " ?");
        alert.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestion.supprimerClient(client.client_id);
                    addNotification("SUPPRESSION", "Client supprimé: " + client.nom +
                            " (Email: " + client.email + ")");
                    loadInitialData();
                } catch (RemoteException e) {
                    showErrorAlert("Erreur", "Impossible de supprimer le client: " + e.getMessage());
                }
            }
        });
    }

    private void showAddReservationDialog(Client client) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle réservation");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // Sélection du client - afficher uniquement le nom
        ComboBox<Client> cbClient = new ComboBox<>();
        cbClient.setItems(clientsList);
        cbClient.setPromptText("Sélectionner un client");
        cbClient.setButtonCell(new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.nom);
                }
            }
        });

        cbClient.setCellFactory(lv -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.nom);
                }
            }
        });

        if (client != null) {
            cbClient.setValue(client);
        }

        // Sélection de la chambre (uniquement les disponibles)
        ComboBox<Chambre> cbChambre = new ComboBox<>();
        cbChambre.setItems(chambresList.filtered(c -> c.statut.equals("Disponible")));
        cbChambre.setPromptText("Sélectionner une chambre disponible");
        cbChambre.setButtonCell(new ListCell<Chambre>() {
            @Override
            protected void updateItem(Chambre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.numero + " - " + item.type_chambre + " (" + item.prix_par_jour + " D/j)");
                }
            }
        });

        cbChambre.setCellFactory(lv -> new ListCell<Chambre>() {
            @Override
            protected void updateItem(Chambre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.numero + " - " + item.type_chambre + " (" + item.prix_par_jour + " D/j)");
                }
            }
        });

        // Dates
        DatePicker dpDebut = new DatePicker(LocalDate.now());
        dpDebut.setPromptText("Date de début");

        DatePicker dpFin = new DatePicker(LocalDate.now().plusDays(1));
        dpFin.setPromptText("Date de fin");

        int row = 0;
        grid.add(new Label("Client*:"), 0, row); grid.add(cbClient, 1, row++);
        grid.add(new Label("Chambre*:"), 0, row); grid.add(cbChambre, 1, row++);
        grid.add(new Label("Date début*:"), 0, row); grid.add(dpDebut, 1, row++);
        grid.add(new Label("Date fin*:"), 0, row); grid.add(dpFin, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (cbClient.getValue() == null || cbChambre.getValue() == null ||
                            dpDebut.getValue() == null || dpFin.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires (*)");
                        return null;
                    }

                    gestion.creerReservation(
                            cbClient.getValue().client_id,
                            cbChambre.getValue().chambre_id,
                            dpDebut.getValue().toString(),
                            dpFin.getValue().toString()
                    );
                    addNotification("AJOUT", "Nouvelle réservation créée pour " +
                            cbClient.getValue().nom + " dans la chambre " +
                            cbChambre.getValue().numero);
                    loadInitialData();
                    updateCarte();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Erreur lors de la création: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editReservation(Reservation reservation) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Modifier la réservation");
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        // Sélection du client
        ComboBox<Client> cbClient = new ComboBox<>();
        cbClient.setItems(clientsList);
        cbClient.setButtonCell(new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.nom);
                }
            }
        });

        cbClient.setCellFactory(lv -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.nom);
                }
            }
        });

        try {
            Client client = gestion.rechercherClient(reservation.client_id);
            cbClient.setValue(client);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Sélection de la chambre
        ComboBox<Chambre> cbChambre = new ComboBox<>();
        cbChambre.setItems(chambresList);
        cbChambre.setButtonCell(new ListCell<Chambre>() {
            @Override
            protected void updateItem(Chambre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.numero + " - " + item.type_chambre);
                }
            }
        });

        cbChambre.setCellFactory(lv -> new ListCell<Chambre>() {
            @Override
            protected void updateItem(Chambre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.numero + " - " + item.type_chambre);
                }
            }
        });

        try {
            Chambre chambre = gestion.rechercherChambre(reservation.chambre_id);
            cbChambre.setValue(chambre);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Dates
        DatePicker dpDebut = new DatePicker(LocalDate.parse(reservation.date_debut));
        DatePicker dpFin = new DatePicker(LocalDate.parse(reservation.date_fin));

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("En attente", "Confirmée", "Annulée", "Terminée");
        cbStatut.setValue(reservation.statut);

        int row = 0;
        grid.add(new Label("Client*:"), 0, row); grid.add(cbClient, 1, row++);
        grid.add(new Label("Chambre*:"), 0, row); grid.add(cbChambre, 1, row++);
        grid.add(new Label("Date début*:"), 0, row); grid.add(dpDebut, 1, row++);
        grid.add(new Label("Date fin*:"), 0, row); grid.add(dpFin, 1, row++);
        grid.add(new Label("Statut*:"), 0, row); grid.add(cbStatut, 1, row++);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (cbClient.getValue() == null || cbChambre.getValue() == null ||
                            dpDebut.getValue() == null || dpFin.getValue() == null || cbStatut.getValue() == null) {
                        showErrorAlert("Erreur", "Veuillez remplir tous les champs obligatoires (*)");
                        return null;
                    }

                    gestion.modifierReservation(
                            reservation.reservation_id,
                            cbClient.getValue().client_id,
                            cbChambre.getValue().chambre_id,
                            dpDebut.getValue().toString(),
                            dpFin.getValue().toString()
                    );

                    if (!reservation.statut.equals(cbStatut.getValue())) {
                        gestion.majStatutReservation(reservation.reservation_id, cbStatut.getValue());
                    }

                    addNotification("MODIFICATION", "Réservation #" + reservation.reservation_id +
                            " modifiée (Nouveau statut: " + cbStatut.getValue() + ")");
                    loadInitialData();
                    updateCarte();
                } catch (Exception e) {
                    showErrorAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteReservation(Reservation reservation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annulation de réservation");
        alert.setHeaderText("Annuler la réservation #" + reservation.reservation_id + " ?");
        alert.setContentText("Voulez-vous vraiment annuler cette réservation ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    gestion.annulerReservation(reservation.reservation_id);
                    addNotification("SUPPRESSION", "Réservation #" + reservation.reservation_id +
                            " annulée (Client: " + reservation.client_id + ")");
                    loadInitialData();
                    updateCarte();
                } catch (RemoteException e) {
                    showErrorAlert("Erreur", "Impossible d'annuler la réservation: " + e.getMessage());
                }
            }
        });
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                tcpRunning = false;
                try {
                    if (tcpSocket != null) tcpSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private void loadInitialData() {
        try {
            // Charger les chambres
            Chambre[] chambres = gestion.listerToutesChambres();
            if (chambres != null) {
                chambresList.setAll(chambres);
                totalChambres = chambres.length;
                if (statChambresValue != null) {
                    statChambresValue.setText(String.valueOf(totalChambres));
                }

                // Calculer les statistiques des chambres
                int dispo = 0, occupees = 0, reservees = 0, maintenance = 0, nettoyage = 0;
                for (Chambre c : chambres) {
                    switch (c.statut) {
                        case "Disponible": dispo++; break;
                        case "Occupée": occupees++; break;
                        case "Réservée": reservees++; break;
                        case "Maintenance": maintenance++; break;
                        case "En nettoyage": nettoyage++; break;
                    }
                }

                // Mettre à jour les mini-statistiques
                if (miniStatTotalValue != null) miniStatTotalValue.setText(String.valueOf(totalChambres));
                if (miniStatDispoValue != null) miniStatDispoValue.setText(String.valueOf(dispo));
                if (miniStatOccupeesValue != null) miniStatOccupeesValue.setText(String.valueOf(occupees));
                if (miniStatReserveesValue != null) miniStatReserveesValue.setText(String.valueOf(reservees));
                if (miniStatMaintenanceValue != null) miniStatMaintenanceValue.setText(String.valueOf(maintenance));

                updateCarte();
            }

            // Charger les clients
            Client[] clients = gestion.listerTousClients();
            if (clients != null) {
                clientsList.setAll(clients);
                totalClients = clients.length;
                if (statClientsValue != null) {
                    statClientsValue.setText(String.valueOf(totalClients));
                }
            }

            // Charger les réservations
            Reservation[] reservations = gestion.toutesLesReservations();
            if (reservations != null) {
                reservationsList.setAll(reservations);
                totalReservations = reservations.length;
                if (statReservationsValue != null) {
                    statReservationsValue.setText(String.valueOf(totalReservations));
                }

                // Calculer les revenus
                revenusTotal = 0;
                for (Reservation r : reservations) {
                    revenusTotal += r.total;
                }
            }

            // Mettre à jour le statut
            if (connectionIndicator != null) {
                connectionIndicator.setFill(Color.web("#00C853"));
                lblUserStatus.setText("Connecté");
            }

            addNotification("SYSTÈME", "Données chargées avec succès");

        } catch (RemoteException e) {
            showErrorAlert("Erreur de chargement", e.getMessage());
        }
    }

    private void startRealTimeUpdates() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            try {
                // Mettre à jour les statistiques
                totalChambres = gestion.listerToutesChambres().length;
                totalClients = gestion.listerTousClients().length;
                totalReservations = gestion.toutesLesReservations().length;

                // Mettre à jour les labels
                if (statChambresValue != null) statChambresValue.setText(String.valueOf(totalChambres));
                if (statClientsValue != null) statClientsValue.setText(String.valueOf(totalClients));
                if (statReservationsValue != null) statReservationsValue.setText(String.valueOf(totalReservations));

                addNotification("SYSTÈME", "Données mises à jour automatiquement");

            } catch (RemoteException ex) {
                System.err.println("Erreur mise à jour: " + ex.getMessage());
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void addNotification(String type, String message) {
        String color;
        switch (type) {
            case "AJOUT": color = "#00C853"; break;
            case "MODIFICATION": color = "#2196F3"; break;
            case "SUPPRESSION": color = "#FF5252"; break;
            case "TCP": color = "#9C27B0"; break;
            case "SYSTÈME": color = "#FF9800"; break;
            default: color = "#9E9E9E";
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        notificationsList.add(0, new NotificationItem(type, message, time, color));

        if (notificationsList.size() > 100) {
            notificationsList.remove(notificationsList.size() - 1);
        }
    }

    private void fadeInContent() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        tcpRunning = false;
        try {
            if (tcpSocket != null) tcpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Application fermée");
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Classe interne pour les notifications
    class NotificationItem {
        private final String type;
        private final String message;
        private final String time;
        private final String color;

        public NotificationItem(String type, String message, String time, String color) {
            this.type = type;
            this.message = message;
            this.time = time;
            this.color = color;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getTime() { return time; }
        public String getColor() { return color; }
    }
}