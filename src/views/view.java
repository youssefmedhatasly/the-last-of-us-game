package views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.Game;
import exceptions.InvalidTargetException;
import exceptions.MovementException;
import exceptions.NoAvailableResourcesException;
import exceptions.NotEnoughActionsException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.characters.Character;
import model.characters.Direction;
import model.characters.Explorer;
import model.characters.Fighter;
import model.characters.Hero;
import model.characters.Medic;
import model.characters.Zombie;
import model.collectibles.Supply;
import model.collectibles.Vaccine;
import model.world.Cell;
import model.world.CharacterCell;
import model.world.CollectibleCell;

public class view extends Application {

	private static final int SIZE = 15;
	private static final int CELL = 38;
	private static final int ICON = 30;
	private static final double WIDTH = 1240, HEIGHT = 780;

	private Stage stage;
	private Image imgFighter, imgMedic, imgExplorer, imgZombie, imgSupply, imgVaccine;

	private Hero selectedHero;
	private boolean targetMode = false;
	private boolean gameEnded = false;
	private int turn = 1;

	private StackPane rootStack;
	private GridPane mapGrid;
	private Button[][] cells;
	private Label statusTurn, statusHeroes, statusVaccines, statusZombies, modeHint;
	private ImageView heroPortrait;
	private Label heroName, heroType, heroHpText, heroDmg, heroSupplies, heroVaccines, heroSpecial;
	private ProgressBar heroHpBar;
	private HBox actionPips;
	private Label targetName, targetHpText, targetRange;
	private ProgressBar targetHpBar;
	private ImageView targetPortrait;
	private VBox teamList, logList;
	private ToggleButton targetBtn;

	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		imgFighter = new Image("views/fighter.jpg");
		imgMedic = new Image("views/medic.jpg");
		imgExplorer = new Image("views/exploer.jpg");
		imgZombie = new Image("views/smallzombie.jpg");
		imgSupply = new Image("views/smallsupply.jpg");
		imgVaccine = new Image("views/supplyvacine.jpg");
		try {
			stage.getIcons().add(new Image("views/icon.png"));
		} catch (Exception ignored) {
		}
		stage.setTitle("The Last of Us");
		showHeroSelect();
		stage.show();
	}

	private void resetGameState() {
		Game.heroes.clear();
		Game.zombies.clear();
		Game.availableHeroes.clear();
		try {
			Game.loadHeroes("Heroes.csv");
		} catch (Exception e) {
			System.err.println("Heroes.csv could not be found: " + e.getMessage());
		}
		selectedHero = null;
		targetMode = false;
		gameEnded = false;
		turn = 1;
	}

	private void setScene(Region root) {
		Scene scene = new Scene(root, WIDTH, HEIGHT);
		scene.getStylesheets().add("views/application.css");
		stage.setScene(scene);
		stage.setMinWidth(900);
		stage.setMinHeight(600);
		stage.centerOnScreen();
	}

	private void showHeroSelect() {
		resetGameState();

		Label title = new Label("THE LAST OF US");
		title.getStyleClass().add("title");
		Label subtitle = new Label("Choose your survivor");
		subtitle.getStyleClass().add("subtitle");

		int maxHp = 1, maxAct = 1, maxDmg = 1;
		for (Hero h : Game.availableHeroes) {
			maxHp = Math.max(maxHp, h.getMaxHp());
			maxAct = Math.max(maxAct, h.getMaxActions());
			maxDmg = Math.max(maxDmg, h.getAttackDmg());
		}

		Button startBtn = new Button("START GAME");
		startBtn.getStyleClass().add("primary-button");
		startBtn.setDisable(true);

		ToggleGroup group = new ToggleGroup();
		FlowPane cards = new FlowPane(16, 16);
		cards.setAlignment(Pos.CENTER);
		cards.setPrefWrapLength(4 * 260 + 3 * 16);
		for (Hero h : new ArrayList<>(Game.availableHeroes)) {
			ToggleButton card = heroCard(h, maxHp, maxAct, maxDmg);
			card.setToggleGroup(group);
			card.setUserData(h);
			card.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2)
					startGame(h);
			});
			cards.getChildren().add(card);
		}
		group.selectedToggleProperty().addListener((obs, o, n) -> startBtn.setDisable(n == null));
		startBtn.setOnAction(e -> {
			Toggle t = group.getSelectedToggle();
			if (t != null)
				startGame((Hero) t.getUserData());
		});

		Label goal = new Label("Goal: find all 5 vaccines and use them to cure zombies until you have 5 heroes.");
		goal.getStyleClass().add("goal");

		VBox root = new VBox(14, title, subtitle, cards, goal, startBtn);
		VBox.setMargin(cards, new Insets(10, 0, 6, 0));
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(24));
		root.getStyleClass().add("menu-root");
		setScene(root);
	}

	private ToggleButton heroCard(Hero h, int maxHp, int maxAct, int maxDmg) {
		ImageView iv = roundedImage(imageFor(h), 52, 10);
		Label name = new Label(h.getName());
		name.getStyleClass().add("card-name");
		Label type = typeBadge(h);
		VBox nameBox = new VBox(5, name, type);
		nameBox.setAlignment(Pos.CENTER_LEFT);
		HBox header = new HBox(12, iv, nameBox);
		header.setAlignment(Pos.CENTER_LEFT);

		VBox stats = new VBox(5,
				statRow("HP", h.getMaxHp(), maxHp, "bar-hp"),
				statRow("ACT", h.getMaxActions(), maxAct, "bar-act"),
				statRow("DMG", h.getAttackDmg(), maxDmg, "bar-dmg"));

		Label ability = new Label(abilityText(h));
		ability.getStyleClass().add("ability");
		ability.setWrapText(true);
		ability.setPrefWidth(228);
		ability.setMaxWidth(228);
		ability.setMinHeight(32);

		VBox content = new VBox(10, header, stats, ability);
		content.setPrefWidth(228);
		content.setMaxWidth(228);

		ToggleButton card = new ToggleButton();
		card.setGraphic(content);
		card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		card.getStyleClass().add("hero-card");
		card.setPrefWidth(260);
		card.setPrefHeight(206);
		card.setMinHeight(206);
		card.setMaxHeight(206);
		card.setFocusTraversable(false);
		return card;
	}

	private HBox statRow(String label, int value, int max, String barClass) {
		Label l = new Label(label);
		l.getStyleClass().add("stat-label");
		l.setMinWidth(32);
		ProgressBar bar = new ProgressBar((double) value / max);
		bar.getStyleClass().add(barClass);
		bar.setPrefWidth(140);
		bar.setPrefHeight(8);
		Label v = new Label(String.valueOf(value));
		v.getStyleClass().add("stat-value");
		HBox row = new HBox(8, l, bar, v);
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	private void startGame(Hero h) {
		Game.startGame(h);
		selectedHero = h;
		buildGameScreen();
		log("Welcome, " + h.getName() + ". Find the vaccines and stay alive.", "good");
		refresh();
	}

	private void buildGameScreen() {
		mapGrid = new GridPane();
		mapGrid.setHgap(3);
		mapGrid.setVgap(3);
		mapGrid.getStyleClass().add("map");
		cells = new Button[SIZE][SIZE];
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				final int fx = x, fy = y;
				Button b = new Button();
				b.setMinSize(CELL, CELL);
				b.setPrefSize(CELL, CELL);
				b.setMaxSize(CELL, CELL);
				b.setFocusTraversable(false);
				b.setOnMouseClicked(e -> onCellClicked(fx, fy, e));
				cells[x][y] = b;
				// model row 0 is drawn at the bottom
				mapGrid.add(b, y, SIZE - 1 - x);
			}
		}

		statusTurn = statusChip("status-turn");
		statusHeroes = statusChip("status-heroes");
		statusVaccines = statusChip("status-vaccines");
		statusZombies = statusChip("status-zombies");
		HBox statusBar = new HBox(8, statusTurn, statusHeroes, statusVaccines, statusZombies);
		statusBar.setAlignment(Pos.CENTER_LEFT);

		modeHint = new Label();
		modeHint.getStyleClass().add("mode-hint");
		modeHint.setVisible(false);

		VBox mapPanel = new VBox(10, statusBar, mapGrid, legend(), modeHint);
		mapPanel.setAlignment(Pos.TOP_CENTER);

		VBox colA = new VBox(12, heroPanel(), targetPanel(), teamPanel());
		colA.setPrefWidth(270);
		colA.setMinWidth(250);

		VBox logPanel = logPanel();
		VBox.setVgrow(logPanel, Priority.ALWAYS);
		VBox colB = new VBox(12, actionsPanel(), movePanel(), logPanel);
		colB.setPrefWidth(270);
		colB.setMinWidth(250);

		HBox content = new HBox(18, mapPanel, colA, colB);
		content.setPadding(new Insets(18));
		content.setAlignment(Pos.TOP_CENTER);

		BorderPane base = new BorderPane(content);
		base.getStyleClass().add("game-root");
		rootStack = new StackPane(base);
		setScene(rootStack);
		stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
	}

	private Label statusChip(String cls) {
		Label l = new Label();
		l.getStyleClass().addAll("status-chip", cls);
		return l;
	}

	private HBox legend() {
		HBox box = new HBox(14,
				legendItem(imgFighter, "Fighter"), legendItem(imgMedic, "Medic"),
				legendItem(imgExplorer, "Explorer"), legendItem(imgZombie, "Zombie"),
				legendItem(imgSupply, "Supply"), legendItem(imgVaccine, "Vaccine"));
		Region fog = new Region();
		fog.getStyleClass().add("legend-fog");
		fog.setMinSize(16, 16);
		Label fogL = new Label("Unexplored");
		fogL.getStyleClass().add("legend-label");
		HBox fogItem = new HBox(5, fog, fogL);
		fogItem.setAlignment(Pos.CENTER_LEFT);
		box.getChildren().add(fogItem);
		box.setAlignment(Pos.CENTER);
		return box;
	}

	private HBox legendItem(Image img, String text) {
		Label l = new Label(text);
		l.getStyleClass().add("legend-label");
		HBox item = new HBox(5, roundedImage(img, 16, 4), l);
		item.setAlignment(Pos.CENTER_LEFT);
		return item;
	}

	private VBox panel(String title, Node... children) {
		Label t = new Label(title);
		t.getStyleClass().add("panel-title");
		VBox p = new VBox(8);
		p.getChildren().add(t);
		p.getChildren().addAll(children);
		p.getStyleClass().add("panel");
		return p;
	}

	private VBox heroPanel() {
		heroPortrait = new ImageView();
		heroPortrait.setFitWidth(56);
		heroPortrait.setFitHeight(56);
		heroPortrait.setSmooth(true);
		Rectangle clip = new Rectangle(56, 56);
		clip.setArcWidth(14);
		clip.setArcHeight(14);
		heroPortrait.setClip(clip);

		heroName = new Label();
		heroName.getStyleClass().add("hero-name");
		heroType = new Label();
		heroSpecial = new Label("SPECIAL ACTIVE");
		heroSpecial.getStyleClass().add("special-badge");
		HBox badges = new HBox(6, heroType, heroSpecial);
		VBox nameBox = new VBox(5, heroName, badges);
		HBox header = new HBox(12, heroPortrait, nameBox);
		header.setAlignment(Pos.CENTER_LEFT);

		heroHpBar = new ProgressBar(1);
		heroHpBar.setMaxWidth(Double.MAX_VALUE);
		heroHpBar.setPrefHeight(12);
		heroHpBar.getStyleClass().add("bar-hp");
		heroHpText = new Label();
		heroHpText.getStyleClass().add("stat-value");
		HBox hpRow = labeledRow("HP", heroHpBar, heroHpText);

		actionPips = new HBox(4);
		actionPips.setAlignment(Pos.CENTER_LEFT);
		HBox actRow = labeledRow("ACT", actionPips, null);

		heroDmg = inventoryLabel();
		heroSupplies = inventoryLabel();
		heroVaccines = inventoryLabel();
		HBox inv = new HBox(14,
				iconWith(null, heroDmg, "inv-dmg"),
				iconWith(imgSupply, heroSupplies, null),
				iconWith(imgVaccine, heroVaccines, null));
		inv.setAlignment(Pos.CENTER_LEFT);

		return panel("SELECTED HERO", header, hpRow, actRow, inv);
	}

	private VBox targetPanel() {
		targetPortrait = new ImageView();
		targetPortrait.setFitWidth(36);
		targetPortrait.setFitHeight(36);
		Rectangle clip = new Rectangle(36, 36);
		clip.setArcWidth(10);
		clip.setArcHeight(10);
		targetPortrait.setClip(clip);
		targetName = new Label();
		targetName.getStyleClass().add("target-name");
		targetRange = new Label();
		VBox nb = new VBox(3, targetName, targetRange);
		HBox header = new HBox(10, targetPortrait, nb);
		header.setAlignment(Pos.CENTER_LEFT);

		targetHpBar = new ProgressBar(1);
		targetHpBar.setMaxWidth(Double.MAX_VALUE);
		targetHpBar.setPrefHeight(8);
		targetHpText = new Label();
		targetHpText.getStyleClass().add("stat-value");
		HBox hp = labeledRow("HP", targetHpBar, targetHpText);
		return panel("TARGET", header, hp);
	}

	private VBox teamPanel() {
		teamList = new VBox(6);
		return panel("YOUR TEAM", teamList);
	}

	private VBox actionsPanel() {
		Button attack = actionButton("Attack", "F", "btn-attack", this::doAttack);
		Button cure = actionButton("Cure", "C", "btn-cure", this::doCure);
		Button special = actionButton("Special", "Q", "btn-special", this::doSpecial);
		targetBtn = new ToggleButton();
		targetBtn.setGraphic(buttonContent("Target", "T"));
		targetBtn.getStyleClass().addAll("action-btn", "btn-target");
		targetBtn.setFocusTraversable(false);
		targetBtn.setMaxWidth(Double.MAX_VALUE);
		targetBtn.setOnAction(e -> setTargetMode(targetBtn.isSelected()));

		GridPane g = new GridPane();
		g.setHgap(8);
		g.setVgap(8);
		g.add(attack, 0, 0);
		g.add(cure, 1, 0);
		g.add(special, 0, 1);
		g.add(targetBtn, 1, 1);
		for (Node n : g.getChildren())
			GridPane.setHgrow(n, Priority.ALWAYS);

		Button endTurn = actionButton("End Turn", "E", "btn-end", this::doEndTurn);
		endTurn.setMaxWidth(Double.MAX_VALUE);
		return panel("ACTIONS", g, endTurn);
	}

	private VBox movePanel() {
		Button up = arrowButton(0, Direction.UP);
		Button left = arrowButton(270, Direction.LEFT);
		Button right = arrowButton(90, Direction.RIGHT);
		Button down = arrowButton(180, Direction.DOWN);
		GridPane pad = new GridPane();
		pad.setHgap(6);
		pad.setVgap(6);
		pad.add(up, 1, 0);
		pad.add(left, 0, 1);
		pad.add(right, 2, 1);
		pad.add(down, 1, 2);
		pad.setAlignment(Pos.CENTER);

		Label keys = new Label("Arrow keys / WASD");
		keys.getStyleClass().add("hint");
		VBox box = new VBox(6, pad, keys);
		box.setAlignment(Pos.CENTER);
		return panel("MOVE", box);
	}

	private VBox logPanel() {
		logList = new VBox(6);
		VBox p = panel("EVENT LOG", logList);
		p.setMinHeight(120);
		return p;
	}

	private HBox labeledRow(String label, Node middle, Label right) {
		Label l = new Label(label);
		l.getStyleClass().add("stat-label");
		l.setMinWidth(32);
		HBox row = new HBox(8, l, middle);
		HBox.setHgrow(middle, Priority.ALWAYS);
		if (right != null) {
			right.setMinWidth(62);
			right.setAlignment(Pos.CENTER_RIGHT);
			row.getChildren().add(right);
		}
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	private Label inventoryLabel() {
		Label l = new Label();
		l.getStyleClass().add("inv-value");
		return l;
	}

	private HBox iconWith(Image img, Label label, String cls) {
		HBox b = new HBox(5);
		if (img != null) {
			b.getChildren().add(roundedImage(img, 20, 5));
		} else {
			Label sword = new Label("DMG");
			sword.getStyleClass().add(cls);
			b.getChildren().add(sword);
		}
		b.getChildren().add(label);
		b.setAlignment(Pos.CENTER_LEFT);
		return b;
	}

	private Button actionButton(String text, String key, String cls, Runnable action) {
		Button b = new Button();
		b.setGraphic(buttonContent(text, key));
		b.getStyleClass().addAll("action-btn", cls);
		b.setFocusTraversable(false);
		b.setMaxWidth(Double.MAX_VALUE);
		b.setOnAction(e -> action.run());
		return b;
	}

	private HBox buttonContent(String text, String key) {
		Label t = new Label(text);
		t.getStyleClass().add("btn-text");
		Label k = new Label(key);
		k.getStyleClass().add("key-cap");
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox h = new HBox(6, t, spacer, k);
		h.setAlignment(Pos.CENTER_LEFT);
		h.setMinWidth(90);
		return h;
	}

	private Button arrowButton(double rotate, Direction d) {
		Polygon tri = new Polygon(0, 12, 7, 0, 14, 12);
		tri.getStyleClass().add("arrow");
		tri.setRotate(rotate);
		Button b = new Button();
		b.setGraphic(tri);
		b.getStyleClass().add("arrow-btn");
		b.setFocusTraversable(false);
		b.setOnAction(e -> doMove(d));
		return b;
	}

	private ImageView roundedImage(Image img, double size, double arc) {
		ImageView iv = new ImageView(img);
		iv.setFitWidth(size);
		iv.setFitHeight(size);
		iv.setSmooth(true);
		Rectangle clip = new Rectangle(size, size);
		clip.setArcWidth(arc);
		clip.setArcHeight(arc);
		iv.setClip(clip);
		return iv;
	}

	private Label typeBadge(Hero h) {
		Label type = new Label(typeName(h).toUpperCase());
		type.getStyleClass().addAll("type-badge", "type-" + typeName(h).toLowerCase());
		return type;
	}

	private Image imageFor(Character c) {
		if (c instanceof Fighter)
			return imgFighter;
		if (c instanceof Medic)
			return imgMedic;
		if (c instanceof Explorer)
			return imgExplorer;
		return imgZombie;
	}

	private String typeName(Hero h) {
		if (h instanceof Fighter)
			return "Fighter";
		if (h instanceof Medic)
			return "Medic";
		return "Explorer";
	}

	private String abilityText(Hero h) {
		if (h instanceof Fighter)
			return "Special: attacks cost no actions for the rest of the turn.";
		if (h instanceof Medic)
			return "Special: fully heal an adjacent hero (or yourself).";
		return "Special: reveal the entire map for this turn.";
	}

	private void refresh() {
		renderMap();
		renderSidebar();
		renderStatus();
	}

	private void renderMap() {
		Character target = selectedHero == null ? null : selectedHero.getTarget();
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				Button b = cells[x][y];
				Cell cell = Game.map[x][y];
				b.getStyleClass().setAll("button", "cell");
				b.setGraphic(null);
				b.setTooltip(null);

				if (!cell.isVisible()) {
					b.getStyleClass().add("fog");
					continue;
				}
				b.getStyleClass().add("ground");

				if (cell instanceof CharacterCell && ((CharacterCell) cell).getCharacter() != null) {
					Character c = ((CharacterCell) cell).getCharacter();
					b.setGraphic(characterGraphic(c));
					b.getStyleClass().add(c instanceof Hero ? "hero" : "zombie");
					if (c == selectedHero)
						b.getStyleClass().add("selected");
					if (c == target)
						b.getStyleClass().add("targeted");
					if (c instanceof Zombie && selectedHero != null && isAdjacent(selectedHero, c))
						b.getStyleClass().add("in-range");
					b.setTooltip(tooltip(describe(c)));
				} else if (cell instanceof CollectibleCell) {
					boolean vaccine = ((CollectibleCell) cell).getCollectible() instanceof Vaccine;
					b.setGraphic(roundedImage(vaccine ? imgVaccine : imgSupply, ICON, 8));
					b.getStyleClass().add(vaccine ? "vaccine" : "supply");
					b.setTooltip(tooltip(vaccine ? "Vaccine - walk here to pick it up" : "Supply - walk here to pick it up"));
				}
			}
		}
		if (targetMode)
			mapGrid.getStyleClass().add("targeting");
		else
			mapGrid.getStyleClass().remove("targeting");
	}

	private Node characterGraphic(Character c) {
		ImageView iv = roundedImage(imageFor(c), ICON, 8);
		double ratio = Math.max(0, Math.min(1, (double) c.getCurrentHp() / c.getMaxHp()));
		Region track = new Region();
		track.getStyleClass().add("mini-track");
		track.setMinSize(ICON, 4);
		track.setMaxSize(ICON, 4);
		Region fill = new Region();
		fill.getStyleClass().add(c instanceof Hero ? "mini-fill-hero" : "mini-fill-zombie");
		fill.setMinSize(ICON * ratio, 4);
		fill.setMaxSize(ICON * ratio, 4);
		StackPane bar = new StackPane(track, fill);
		StackPane.setAlignment(fill, Pos.CENTER_LEFT);
		bar.setMaxSize(ICON, 4);
		StackPane g = new StackPane(iv, bar);
		StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
		StackPane.setMargin(bar, new Insets(0, 0, -3, 0));
		return g;
	}

	private Tooltip tooltip(String text) {
		Tooltip t = new Tooltip(text);
		t.setShowDelay(Duration.millis(150));
		return t;
	}

	private String describe(Character c) {
		StringBuilder sb = new StringBuilder(c.getName());
		if (c instanceof Hero) {
			Hero h = (Hero) c;
			sb.append(" (").append(typeName(h)).append(")");
			sb.append("\nHP ").append(h.getCurrentHp()).append(" / ").append(h.getMaxHp());
			sb.append("\nActions ").append(h.getActionsAvailable()).append(" / ").append(h.getMaxActions());
			sb.append("\nDamage ").append(h.getAttackDmg());
			sb.append("\nSupplies ").append(h.getSupplyInventory().size())
					.append("   Vaccines ").append(h.getVaccineInventory().size());
			sb.append("\n\nClick to select  -  right-click to target");
		} else {
			sb.append("\nHP ").append(c.getCurrentHp()).append(" / ").append(c.getMaxHp());
			sb.append("\nDamage ").append(c.getAttackDmg());
			sb.append("\n\nClick to target");
		}
		return sb.toString();
	}

	private void renderSidebar() {
		Hero h = selectedHero;
		if (h == null) {
			heroPortrait.setImage(null);
			heroName.setText("No hero selected");
			heroType.setText("");
			heroType.getStyleClass().setAll("label");
			heroSpecial.setVisible(false);
			heroHpBar.setProgress(0);
			heroHpText.setText("-");
			actionPips.getChildren().clear();
			heroDmg.setText("-");
			heroSupplies.setText("-");
			heroVaccines.setText("-");
		} else {
			heroPortrait.setImage(imageFor(h));
			heroName.setText(h.getName());
			heroType.setText(typeName(h).toUpperCase());
			heroType.getStyleClass().setAll("label", "type-badge", "type-" + typeName(h).toLowerCase());
			heroSpecial.setVisible(h.isSpecialAction() && h instanceof Fighter);
			double ratio = (double) h.getCurrentHp() / h.getMaxHp();
			heroHpBar.setProgress(ratio);
			setHpColor(heroHpBar, ratio);
			heroHpText.setText(h.getCurrentHp() + " / " + h.getMaxHp());
			actionPips.getChildren().clear();
			for (int i = 0; i < h.getMaxActions(); i++) {
				Region pip = new Region();
				pip.getStyleClass().add(i < h.getActionsAvailable() ? "pip" : "pip-used");
				pip.setMinSize(16, 10);
				pip.setPrefSize(16, 10);
				actionPips.getChildren().add(pip);
			}
			heroDmg.setText(String.valueOf(h.getAttackDmg()));
			heroSupplies.setText("x " + h.getSupplyInventory().size());
			heroVaccines.setText("x " + h.getVaccineInventory().size());
		}

		Character t = h == null ? null : h.getTarget();
		if (t == null || t.getCurrentHp() <= 0) {
			targetPortrait.setImage(null);
			targetName.setText("No target");
			targetRange.setText("Click a zombie on the map");
			targetRange.getStyleClass().setAll("label", "hint");
			targetHpBar.setProgress(0);
			targetHpText.setText("-");
		} else {
			targetPortrait.setImage(imageFor(t));
			targetName.setText(t.getName());
			boolean adj = isAdjacent(h, t);
			targetRange.setText(adj ? "In range" : "Out of range - move closer");
			targetRange.getStyleClass().setAll("label", adj ? "range-ok" : "range-bad");
			double ratio = (double) t.getCurrentHp() / t.getMaxHp();
			targetHpBar.setProgress(ratio);
			targetHpBar.getStyleClass().setAll("progress-bar", t instanceof Zombie ? "bar-zombie" : "bar-hp");
			targetHpText.setText(t.getCurrentHp() + " / " + t.getMaxHp());
		}

		teamList.getChildren().clear();
		for (Hero member : Game.heroes) {
			ImageView iv = roundedImage(imageFor(member), 26, 6);
			Label n = new Label(member.getName());
			n.getStyleClass().add("team-name");
			ProgressBar bar = new ProgressBar((double) member.getCurrentHp() / member.getMaxHp());
			setHpColor(bar, bar.getProgress());
			bar.setPrefWidth(70);
			bar.setPrefHeight(6);
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);
			HBox row = new HBox(8, iv, n, spacer, bar);
			row.setAlignment(Pos.CENTER_LEFT);
			row.getStyleClass().add("team-row");
			if (member == selectedHero)
				row.getStyleClass().add("team-row-selected");
			row.setOnMouseClicked(e -> {
				selectedHero = member;
				refresh();
			});
			teamList.getChildren().add(row);
		}
		Label count = new Label(Game.heroes.size() + " / 5 heroes needed to win");
		count.getStyleClass().add("hint");
		teamList.getChildren().add(count);
	}

	private void setHpColor(ProgressBar bar, double ratio) {
		bar.getStyleClass().setAll("progress-bar", "bar-hp");
		if (ratio <= 0.3)
			bar.getStyleClass().add("low");
		else if (ratio <= 0.6)
			bar.getStyleClass().add("mid");
	}

	private void renderStatus() {
		statusTurn.setText("TURN " + turn);
		statusHeroes.setText("Heroes " + Game.heroes.size() + "/5");
		statusVaccines.setText("Vaccines left " + vaccinesRemaining());
		statusZombies.setText("Zombies " + Game.zombies.size());
	}

	private int vaccinesRemaining() {
		int n = 0;
		for (int i = 0; i < SIZE; i++)
			for (int j = 0; j < SIZE; j++)
				if (Game.map[i][j] instanceof CollectibleCell
						&& ((CollectibleCell) Game.map[i][j]).getCollectible() instanceof Vaccine)
					n++;
		for (Hero h : Game.heroes)
			n += h.getVaccineInventory().size();
		return n;
	}

	private boolean isAdjacent(Character a, Character b) {
		if (a == null || b == null || a.getLocation() == null || b.getLocation() == null)
			return false;
		return Math.abs(a.getLocation().x - b.getLocation().x) <= 1
				&& Math.abs(a.getLocation().y - b.getLocation().y) <= 1;
	}

	private void log(String msg, String kind) {
		Label l = new Label(msg);
		l.setWrapText(true);
		l.getStyleClass().addAll("log-entry", "log-" + kind);
		logList.getChildren().add(0, l);
		while (logList.getChildren().size() > 7)
			logList.getChildren().remove(logList.getChildren().size() - 1);
		for (int i = 0; i < logList.getChildren().size(); i++)
			logList.getChildren().get(i).setOpacity(1.0 - i * 0.11);
	}

	private void onKey(KeyEvent e) {
		if (gameEnded)
			return;
		switch (e.getCode()) {
		case UP: case W: doMove(Direction.UP); break;
		case DOWN: case S: doMove(Direction.DOWN); break;
		case LEFT: case A: doMove(Direction.LEFT); break;
		case RIGHT: case D: doMove(Direction.RIGHT); break;
		case F: doAttack(); break;
		case C: doCure(); break;
		case Q: doSpecial(); break;
		case E: doEndTurn(); break;
		case T: setTargetMode(!targetMode); break;
		case ESCAPE: setTargetMode(false); break;
		case TAB: cycleHero(); break;
		default: return;
		}
		e.consume();
	}

	private void cycleHero() {
		if (Game.heroes.isEmpty())
			return;
		int i = Game.heroes.indexOf(selectedHero);
		selectedHero = Game.heroes.get((i + 1) % Game.heroes.size());
		refresh();
	}

	private void setTargetMode(boolean on) {
		targetMode = on;
		if (targetBtn != null)
			targetBtn.setSelected(on);
		modeHint.setText("TARGET MODE - click any character (heroes too, for Medic heals). Esc to cancel.");
		modeHint.setVisible(on);
		renderMap();
	}

	private void onCellClicked(int x, int y, MouseEvent e) {
		if (gameEnded)
			return;
		Cell cell = Game.map[x][y];
		if (!cell.isVisible())
			return;
		Character c = cell instanceof CharacterCell ? ((CharacterCell) cell).getCharacter() : null;
		if (c == null) {
			setTargetMode(false);
			return;
		}
		boolean wantsTarget = targetMode || e.getButton() == MouseButton.SECONDARY || c instanceof Zombie;
		if (!wantsTarget && c instanceof Hero) {
			selectedHero = (Hero) c;
		} else if (selectedHero != null) {
			selectedHero.setTarget(c);
			log("Target set: " + c.getName(), "info");
		}
		setTargetMode(false);
		refresh();
	}

	private boolean ready() {
		if (gameEnded)
			return false;
		if (selectedHero == null) {
			log("Select a hero first.", "warn");
			return false;
		}
		return true;
	}

	private void doMove(Direction d) {
		if (!ready())
			return;
		Hero h = selectedHero;
		int hp = h.getCurrentHp(), sup = h.getSupplyInventory().size(), vac = h.getVaccineInventory().size();
		try {
			h.move(d);
			if (h.getCurrentHp() < hp)
				log(h.getName() + " stepped on a trap! -" + (hp - h.getCurrentHp()) + " HP", "bad");
			if (h.getSupplyInventory().size() > sup)
				log(h.getName() + " picked up a supply.", "good");
			if (h.getVaccineInventory().size() > vac)
				log(h.getName() + " picked up a VACCINE!", "good");
		} catch (MovementException | NotEnoughActionsException ex) {
			log(ex.getMessage(), "warn");
		}
		afterAction();
	}

	private void doAttack() {
		if (!ready())
			return;
		Hero h = selectedHero;
		Character t = h.getTarget();
		int hp = h.getCurrentHp();
		int thp = t == null ? 0 : t.getCurrentHp();
		try {
			h.attack();
			log(h.getName() + " hit " + t.getName() + " for " + (thp - t.getCurrentHp())
					+ (h.getCurrentHp() < hp ? " and took " + (hp - h.getCurrentHp()) + " back" : ""), "info");
			if (t.getCurrentHp() <= 0) {
				log(t.getName() + " was killed. Another one rises somewhere...", "good");
				h.setTarget(null);
			}
		} catch (NotEnoughActionsException | InvalidTargetException ex) {
			log(ex.getMessage(), "warn");
		}
		afterAction();
	}

	private void doCure() {
		if (!ready())
			return;
		Hero h = selectedHero;
		int before = Game.heroes.size();
		try {
			h.cure();
			if (Game.heroes.size() > before) {
				Hero joined = Game.heroes.get(Game.heroes.size() - 1);
				log("Cured! " + joined.getName() + " (" + typeName(joined) + ") joined your team.", "good");
			}
			h.setTarget(null);
		} catch (NoAvailableResourcesException | InvalidTargetException | NotEnoughActionsException ex) {
			log(ex.getMessage(), "warn");
		}
		afterAction();
	}

	private void doSpecial() {
		if (!ready())
			return;
		Hero h = selectedHero;
		if (h instanceof Medic && h.getTarget() == null) {
			log("Medic: target a hero to heal first (right-click it or press T).", "warn");
			return;
		}
		try {
			h.useSpecial();
			if (h instanceof Fighter)
				log(h.getName() + " is enraged: attacks are free this turn!", "good");
			else if (h instanceof Medic)
				log(h.getName() + " healed " + h.getTarget().getName() + " to full HP.", "good");
			else
				log(h.getName() + " scouted the area: whole map revealed this turn.", "good");
		} catch (NoAvailableResourcesException | InvalidTargetException ex) {
			log(ex.getMessage(), "warn");
		}
		afterAction();
	}

	private void doEndTurn() {
		if (gameEnded)
			return;
		Map<Hero, Integer> hpBefore = new HashMap<>();
		List<Hero> before = new ArrayList<>(Game.heroes);
		for (Hero h : before)
			hpBefore.put(h, h.getCurrentHp());
		Game.endTurn();
		turn++;
		log("--- Turn " + turn + " ---", "turn");
		for (Hero h : before) {
			int lost = hpBefore.get(h) - h.getCurrentHp();
			if (!Game.heroes.contains(h))
				log(h.getName() + " was killed by zombies!", "bad");
			else if (lost > 0)
				log("Zombies attacked " + h.getName() + ": -" + lost + " HP", "bad");
		}
		afterAction();
	}

	private void afterAction() {
		if (selectedHero != null && !Game.heroes.contains(selectedHero)) {
			log(selectedHero.getName() + " has fallen.", "bad");
			selectedHero = Game.heroes.isEmpty() ? null : Game.heroes.get(0);
		}
		if (selectedHero != null && selectedHero.getTarget() != null
				&& selectedHero.getTarget().getCurrentHp() <= 0)
			selectedHero.setTarget(null);
		refresh();
		if (Game.checkWin())
			showEndScreen(true);
		else if (Game.checkGameOver())
			showEndScreen(false);
	}

	private void showEndScreen(boolean won) {
		gameEnded = true;
		setTargetMode(false);
		Label title = new Label(won ? "YOU SURVIVED" : "GAME OVER");
		title.getStyleClass().addAll("end-title", won ? "end-win" : "end-lose");
		String reason = won ? "All vaccines used and your team of " + Game.heroes.size() + " is safe."
				: Game.heroes.isEmpty() ? "Every hero has fallen." : "The vaccines are gone, but your team is too small.";
		Label sub = new Label(reason);
		sub.getStyleClass().add("end-sub");
		Label stats = new Label("Turns survived: " + turn + "     Heroes: " + Game.heroes.size());
		stats.getStyleClass().add("end-stats");

		Button again = new Button("PLAY AGAIN");
		again.getStyleClass().add("primary-button");
		again.setOnAction(e -> showHeroSelect());
		Button quit = new Button("QUIT");
		quit.getStyleClass().add("secondary-button");
		quit.setOnAction(e -> Platform.exit());
		HBox buttons = new HBox(14, again, quit);
		buttons.setAlignment(Pos.CENTER);

		VBox card = new VBox(16, title, sub, stats, buttons);
		card.setAlignment(Pos.CENTER);
		card.getStyleClass().add("end-card");
		card.setMaxSize(560, 300);

		StackPane overlay = new StackPane(card);
		overlay.getStyleClass().add("overlay");
		rootStack.getChildren().add(overlay);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
