package arena3.core;

import javax.swing.*;
import java.awt.*;
import arena3.core.GameState.GameType;
import arena3.core.GameState.GameConfig;
import arena3.core.GameState.GameResult;
import arena3.menu.MainMenuScreen;
import arena3.menu.PlayerSetupScreen;
import arena3.snake.SnakeGame;
import arena3.ludo.LudoGame;
import arena3.chess.ChessGame;
import arena3.ui.GameOverScreen;
import arena3.ui.ThemeConstants;

public class GameLauncher extends JPanel {
    private static GameLauncher instance;
    private final CardLayout cardLayout;
    private final JFrame frame;

    private MainMenuScreen mainMenu;
    private PlayerSetupScreen playerSetup;
    private GameOverScreen gameOver;
    private JPanel currentGamePanel;

    public GameLauncher(JFrame frame) {
        instance = this;
        this.frame = frame;
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(ThemeConstants.BG_DARK);

        mainMenu = new MainMenuScreen();
        playerSetup = new PlayerSetupScreen();
        gameOver = new GameOverScreen();

        add(mainMenu, "menu");
        add(playerSetup, "setup");
        add(gameOver, "gameover");

        // Placeholder for game panels
        JPanel blank = new JPanel();
        blank.setBackground(ThemeConstants.BG_DARK);
        add(blank, "snake");
        add(new JPanel() {{ setBackground(ThemeConstants.BG_DARK); }}, "ludo");
        add(new JPanel() {{ setBackground(ThemeConstants.BG_DARK); }}, "chess");

        showScreen("menu");
    }

    public static GameLauncher getInstance() { return instance; }
    public JFrame getFrame() { return frame; }

    public void showScreen(String name) {
        cardLayout.show(this, name);
    }

    public void showMainMenu() {
        mainMenu.onShow();
        showScreen("menu");
    }

    public void showPlayerSetup(GameType type) {
        GameState.currentGame = type;
        playerSetup.configure(type);
        showScreen("setup");
    }

    public void startGame(GameConfig config) {
        GameState.currentConfig = config;
        GameState.currentGame = config.gameType;

        // Remove old game panel if exists
        if (currentGamePanel != null) {
            remove(currentGamePanel);
        }

        String screenName;
        switch (config.gameType) {
            case SNAKE:
                currentGamePanel = new SnakeGame(config);
                screenName = "snake_game";
                break;
            case LUDO:
                currentGamePanel = new LudoGame(config);
                screenName = "ludo_game";
                break;
            case CHESS:
                currentGamePanel = new ChessGame(config);
                screenName = "chess_game";
                break;
            default:
                return;
        }

        add(currentGamePanel, screenName);
        cardLayout.show(this, screenName);
        currentGamePanel.requestFocusInWindow();
    }

    public void showGameOver(GameResult result) {
        gameOver.setResult(result);
        showScreen("gameover");
    }
}
