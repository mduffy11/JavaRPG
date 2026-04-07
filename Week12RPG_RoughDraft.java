import java.util.*;

/**
 * Week 12 RPG Project Rough Draft
 * --------------------------------
 * This is a revised rough-draft prototype for a terminal-based Java RPG.
 *
 * Goals of this revision:
 * - correct Java comment syntax so the program compiles
 * - split some of the oversized Game logic into helper classes
 * - fix a few gameplay flow issues so the prototype is actually playable
 */
public class Week12RPG_RoughDraft {

    public static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private Player player;
    private Map<String, Room> rooms;
    private boolean running;
    private final BattleManager battleManager;
    private final MerchantManager merchantManager;

    public Game() {
        this.battleManager = new BattleManager();
        this.merchantManager = new MerchantManager();
    }

    public void start() {
        introStory();
        setupGame();
        gameLoop();
    }

    private void introStory() {
        // Story text is still a placeholder and can be rewritten later.
        System.out.println("========================================");
        System.out.println("      ECHOES BELOW THE SEAL");
        System.out.println("========================================");
        System.out.println("You were once an ordinary villager with no great battle experience.");
        System.out.println("While traveling near ancient ruins, you stepped on a hidden teleportation trap.");
        System.out.println("In an instant, the world twisted around you.");
        System.out.println("Now you wake inside a strange underground realm full of locked doors,");
        System.out.println("monsters, merchants, riddles, and a final sealed route to the surface.");
        System.out.println("If you want to live, you must go deeper before you can escape.");
        System.out.println();
        System.out.print("Enter your hero's name: ");
        String name = Week12RPG_RoughDraft.input.nextLine().trim();
        if (name.isEmpty()) {
            name = "Hero";
        }
        player = new Player(name);
    }

    private void setupGame() {
        rooms = WorldBuilder.createWorld();
        running = true;
        player.setCurrentRoom("safe");
        player.addItem(new Item("Oil Urn", ItemType.SPECIAL, 0, 1, 0));
    }

    private void gameLoop() {
        while (running) {
            if (!player.isAlive()) {
                gameOver("Your health reached zero. - Gameover -");
                return;
            }

            Room current = rooms.get(player.getCurrentRoom());
            if (current == null) {
                gameOver("The world data is broken. Current room was not found.");
                return;
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println(current.getName());
            System.out.println("========================================");
            System.out.println(current.getDescription());
            showPlayerStatus();

            if (current.getId().equals("exit")) {
                victory();
                return;
            }

            if (current.getId().equals("merchant")) {
                merchantManager.openMerchant(player);
                continue;
            }

            if (current.hasPuzzle() && !current.isPuzzleCleared()) {
                handlePuzzle(current);
                if (!running) {
                    return;
                }
            }

            if (current.hasTreasure() && !current.isTreasureTaken()) {
                collectTreasure(current);
            }

            if (current.hasEnemy() && !current.getEnemy().isDefeated()) {
                boolean survived = battleManager.handleBattle(player, current);
                if (!survived) {
                    gameOver("You died from " + current.getEnemy().getName() + ". - Gameover -");
                    return;
                }
            }

            if (current.getId().equals("safe")) {
                handleSafeRoomChoices(current);
            } else {
                handleRoomChoices(current);
            }
        }
    }

    private void showPlayerStatus() {
        System.out.println();
        System.out.println("Player: " + player.getName());
        System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp());
        System.out.println("Attack: " + player.getAttack());
        System.out.println("Defense: " + player.getDefense());
        System.out.println("Level: " + player.getLevel() + "   XP: " + player.getXp());
        System.out.println("Gold: " + player.getGold());
        System.out.println("Has Key: " + player.hasKey());
    }

    private void handleSafeRoomChoices(Room current) {
        System.out.println();
        for (RoomChoice choice : current.getChoices()) {
            System.out.println(choice.getCode() + ". " + choice.getText());
        }
        System.out.print("Choose an action: ");
        String answer = Week12RPG_RoughDraft.input.nextLine().trim();

        if (answer.equals("4") && !player.hasKey()) {
            System.out.println("The iron door is locked. You need a key.");
            return;
        }

        moveToChoice(current, answer);
    }

    private void handleRoomChoices(Room current) {
        System.out.println();
        for (RoomChoice choice : current.getChoices()) {
            System.out.println(choice.getCode() + ". " + choice.getText());
        }
        System.out.print("Choose an action: ");
        String answer = Week12RPG_RoughDraft.input.nextLine().trim();
        moveToChoice(current, answer);
    }

    private void moveToChoice(Room current, String answer) {
        for (RoomChoice choice : current.getChoices()) {
            if (choice.getCode().equals(answer)) {
                player.setCurrentRoom(choice.getNextRoomId());
                return;
            }
        }
        System.out.println("Invalid choice.");
    }

    private void collectTreasure(Room room) {
        System.out.println();
        System.out.println("You found treasure: " + room.getTreasure().getName());
        System.out.println("Sell value: " + room.getTreasure().getSellPrice() + " gold");
        player.addTreasure(room.getTreasure());
        room.setTreasureTaken(true);

        if (room.hasKeyReward()) {
            player.setHasKey(true);
            System.out.println("You also found an Iron Key.");
        }
    }

    private void handlePuzzle(Room room) {
        PuzzleEncounter puzzle = room.getPuzzle();
        System.out.println();
        System.out.println("PUZZLE ENCOUNTER");
        System.out.println(puzzle.getPrompt());
        System.out.println("Type your full answer exactly. This is not multiple choice.");
        System.out.print("Answer: ");
        String response = Week12RPG_RoughDraft.input.nextLine().trim().toLowerCase();

        if (response.equals(puzzle.getCorrectAnswer())) {
            System.out.println("The path opens. You answered correctly.");
            room.setPuzzleCleared(true);
            player.addXp(15);
            player.checkLevelUp();

            if (!player.hasItem("Small Potion")) {
                player.addItem(new Item("Small Potion", ItemType.HEALING, 15, 0, 0));
                System.out.println("You received a Small Potion.");
            }
        } else {
            gameOver(puzzle.getFailureMessage());
        }
    }

    private void victory() {
        // Victory text is still a placeholder and can be expanded later.
        System.out.println();
        System.out.println("========================================");
        System.out.println("VICTORY");
        System.out.println("========================================");
        System.out.println("You escaped the underground realm.");
        System.out.println("The seal chamber is behind you, and the night sky stretches above.");
        System.out.println("For now, you survived.");
        System.out.println();
        System.out.println("Rough-draft note: this is where New Game+ could be offered.");
        System.out.println("Example later feature: keep stats, scale enemies, unlock hidden doors.");
        running = false;
    }

    private void gameOver(String message) {
        System.out.println();
        System.out.println("========================================");
        System.out.println(message);
        System.out.println("========================================");
        System.out.println("Rough-draft note: this can later connect to restart / New Game+ menus.");
        running = false;
    }
}

class WorldBuilder {
    public static Map<String, Room> createWorld() {
        Map<String, Room> rooms = new HashMap<>();

        // Room descriptions are still examples and can be rewritten later.
        Room safeRoom = new Room("safe", "Safe Room",
                "A quiet stone chamber lit by blue torches. Four doors stand before you.");
        Room goblinNest = new Room("goblin", "Goblin Nest",
                "You smell rot and hear cackling. A goblin leaps from the shadows.");
        Room hallOfDoors = new Room("hall", "Hall of Doors",
                "A larger room opens before you. Three more doors branch deeper into the cave.");
        Room merchantRoom = new Room("merchant", "Merchant Alcove",
                "A hooded merchant sits beside a lantern and a pile of strange goods.");
        Room lockedRoute = new Room("lockedRoute", "Locked Route",
                "A heavy iron door opens into a corridor leading toward the seal chamber.");
        Room crystalRoom = new Room("crystal", "Crystal Vault",
                "Glittering crystals line the walls. Some can be taken.");
        Room snakeRiddle = new Room("snake", "Serpent Shrine",
                "A giant stone serpent blocks the way and demands an answer.");
        Room trollGate = new Room("troll", "Troll Gate",
                "A troll guards a reinforced passage deeper below.");
        Room finalDoor = new Room("finalDoor", "Final Route",
                "You stand before the last door. Beyond it lies the way out... or death.");
        Room finalBossRoom = new Room("boss", "Seal Chamber",
                "A cursed guardian rises before the broken seal. This is the final battle.");
        Room exitRoom = new Room("exit", "Surface Exit",
                "Cold night air reaches your face. The underground realm is finally behind you.");

        safeRoom.addChoice("1", "Go to the Goblin Nest", "goblin");
        safeRoom.addChoice("2", "Go to the Hall of Doors", "hall");
        safeRoom.addChoice("3", "Visit the Merchant", "merchant");
        safeRoom.addChoice("4", "Try the locked iron door", "lockedRoute");

        hallOfDoors.addChoice("1", "Enter the Crystal Vault", "crystal");
        hallOfDoors.addChoice("2", "Enter the Serpent Shrine", "snake");
        hallOfDoors.addChoice("3", "Go back to the Safe Room", "safe");

        goblinNest.addChoice("1", "Go back to the Safe Room", "safe");
        merchantRoom.addChoice("1", "Go back to the Safe Room", "safe");
        crystalRoom.addChoice("1", "Go back to the Hall of Doors", "hall");
        snakeRiddle.addChoice("1", "Go back to the Hall of Doors", "hall");

        lockedRoute.addChoice("1", "Approach the Troll Gate", "troll");
        lockedRoute.addChoice("2", "Go back to the Safe Room", "safe");

        trollGate.addChoice("1", "Continue to the Final Route", "finalDoor");
        trollGate.addChoice("2", "Go back to the Locked Route", "lockedRoute");

        finalDoor.addChoice("1", "Open the last door", "boss");
        finalDoor.addChoice("2", "Retreat to the Troll Gate", "troll");

        finalBossRoom.addChoice("1", "Step toward the exit", "exit");

        // Enemy and reward data are still basic and can be expanded later.
        goblinNest.setEnemy(new Enemy("Goblin", 22, 5, 2, 12, 10, "quick slash", true));
        goblinNest.setConsumableReward(new Item("Small Potion", ItemType.HEALING, 15, 0, 0));
        goblinNest.setRetreatRoomId("safe");

        crystalRoom.setTreasure(new Treasure("Rare Crystal", Rarity.RARE, 20));
        crystalRoom.setKeyReward(true);

        trollGate.setEnemy(new Enemy("Cave Troll", 36, 9, 4, 25, 18, "heavy club swing", true));
        trollGate.setRetreatRoomId("lockedRoute");

        finalBossRoom.setEnemy(new Enemy("Seal Guardian", 55, 11, 5, 80, 30, "cursed blade", false));

        snakeRiddle.setPuzzle(new PuzzleEncounter(
                "The serpent speaks: 'I have cities, but no houses; forests, but no trees; rivers, but no water. What am I?'",
                "map",
                "Swallowed by Snake for wrong answer - Gameover -"
        ));

        rooms.put("safe", safeRoom);
        rooms.put("goblin", goblinNest);
        rooms.put("hall", hallOfDoors);
        rooms.put("merchant", merchantRoom);
        rooms.put("lockedRoute", lockedRoute);
        rooms.put("crystal", crystalRoom);
        rooms.put("snake", snakeRiddle);
        rooms.put("troll", trollGate);
        rooms.put("finalDoor", finalDoor);
        rooms.put("boss", finalBossRoom);
        rooms.put("exit", exitRoom);

        return rooms;
    }
}

class BattleManager {
    public boolean handleBattle(Player player, Room room) {
        Enemy enemy = room.getEnemy();
        System.out.println();
        System.out.println("A battle begins against " + enemy.getName() + "!");

        if (!enemy.getName().equals("Seal Guardian") && room.getRetreatRoomId() != null) {
            System.out.println("1. Fight");
            System.out.println("2. Escape to previous route");
            System.out.print("Choose: ");
            String startChoice = Week12RPG_RoughDraft.input.nextLine().trim();
            if (startChoice.equals("2")) {
                System.out.println("You escaped.");
                player.setCurrentRoom(room.getRetreatRoomId());
                return true;
            }
        }

        boolean oilEffect = false;

        while (player.isAlive() && enemy.isAlive()) {
            System.out.println();
            System.out.println(player.getName() + " HP: " + player.getHp() + "/" + player.getMaxHp());
            System.out.println(enemy.getName() + " HP: " + enemy.getHp() + "/" + enemy.getMaxHp());

            String telegraphedAttack = enemy.chooseAttack();
            System.out.println(enemy.getName() + " prepares: " + telegraphedAttack);

            System.out.println("1. Attack");
            System.out.println("2. Dodge");
            System.out.println("3. Defend");
            System.out.println("4. Item");
            System.out.print("Choose action: ");
            String action = Week12RPG_RoughDraft.input.nextLine().trim();

            boolean defended = false;
            boolean dodged = false;

            switch (action) {
                case "1":
                    int damage = Math.max(1, player.getAttack() - enemy.getDefense());
                    enemy.takeDamage(damage);
                    System.out.println("You attack and deal " + damage + " damage.");
                    break;
                case "2":
                    dodged = true;
                    System.out.println("You prepare to dodge.");
                    break;
                case "3":
                    defended = true;
                    System.out.println("You brace for the attack.");
                    break;
                case "4":
                    oilEffect = useItemInBattle(player);
                    break;
                default:
                    System.out.println("Invalid action. You hesitate and lose momentum.");
            }

            if (!enemy.isAlive()) {
                break;
            }

            if (oilEffect) {
                System.out.println(enemy.getName() + " slips and loses its turn!");
                oilEffect = false;
                continue;
            }

            int enemyDamage = enemy.getAttack();

            // Attack-pattern logic is still a placeholder and can be expanded later.
            if (telegraphedAttack.toLowerCase().contains("club") && dodged) {
                System.out.println("You dodged the heavy club swing.");
                continue;
            }

            if (telegraphedAttack.toLowerCase().contains("fist") && dodged) {
                System.out.println("The fist attack is too fast to dodge completely.");
            }

            if (defended) {
                enemyDamage = Math.max(1, enemyDamage - player.getDefense() - 3);
            } else {
                enemyDamage = Math.max(1, enemyDamage - player.getDefense());
            }

            player.takeDamage(enemyDamage);
            System.out.println(enemy.getName() + " hits you for " + enemyDamage + " damage.");
        }

        if (!player.isAlive()) {
            return false;
        }

        enemy.setDefeated(true);
        System.out.println("You defeated " + enemy.getName() + "!");
        System.out.println("You gained " + enemy.getXpReward() + " XP and " + enemy.getGoldReward() + " gold.");
        player.addXp(enemy.getXpReward());
        player.addGold(enemy.getGoldReward());
        player.checkLevelUp();

        if (room.getConsumableReward() != null) {
            System.out.println("You found: " + room.getConsumableReward().getName());
            player.addItem(room.getConsumableReward());
        }

        return true;
    }

    private boolean useItemInBattle(Player player) {
        if (player.getInventory().isEmpty()) {
            System.out.println("You have no items.");
            return false;
        }

        System.out.println("Inventory:");
        for (int i = 0; i < player.getInventory().size(); i++) {
            System.out.println((i + 1) + ". " + player.getInventory().get(i));
        }
        System.out.print("Choose item number: ");
        String choice = Week12RPG_RoughDraft.input.nextLine().trim();

        try {
            int index = Integer.parseInt(choice) - 1;
            Item item = player.getInventory().get(index);

            if (item.getType() == ItemType.HEALING) {
                player.heal(item.getPower());
                System.out.println("You used " + item.getName() + " and restored " + item.getPower() + " HP.");
                player.removeItem(index);
                return false;
            }

            if (item.getType() == ItemType.SPECIAL && item.getName().equalsIgnoreCase("Oil Urn")) {
                System.out.println("You throw the Oil Urn. The ground becomes slippery.");
                player.removeItem(index);
                return true;
            }

            System.out.println("That item cannot be used now.");
        } catch (Exception e) {
            System.out.println("Invalid item choice.");
        }

        return false;
    }
}

class MerchantManager {
    public void openMerchant(Player player) {
        while (true) {
            System.out.println();
            System.out.println("MERCHANT MENU");
            System.out.println("1. Sell treasure");
            System.out.println("2. Buy Small Potion (10 gold)");
            System.out.println("3. Leave merchant");
            System.out.print("Choose: ");
            String choice = Week12RPG_RoughDraft.input.nextLine().trim();

            if (choice.equals("1")) {
                if (player.getTreasures().isEmpty()) {
                    System.out.println("You have no treasure to sell.");
                } else {
                    int total = 0;
                    for (Treasure treasure : player.getTreasures()) {
                        total += treasure.getSellPrice();
                    }
                    player.addGold(total);
                    player.getTreasures().clear();
                    System.out.println("You sold all treasure for " + total + " gold.");
                }
            } else if (choice.equals("2")) {
                if (player.getGold() >= 10) {
                    player.addGold(-10);
                    player.addItem(new Item("Small Potion", ItemType.HEALING, 15, 0, 0));
                    System.out.println("You bought a Small Potion.");
                } else {
                    System.out.println("Not enough gold.");
                }
            } else if (choice.equals("3")) {
                player.setCurrentRoom("safe");
                return;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}

class Player {
    private String name;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    private int level;
    private int xp;
    private int gold;
    private boolean hasKey;
    private String currentRoom;
    private ArrayList<Item> inventory;
    private ArrayList<Treasure> treasures;

    public Player(String name) {
        this.name = name;
        this.maxHp = 50;
        this.hp = 50;
        this.attack = 8;
        this.defense = 2;
        this.level = 1;
        this.xp = 0;
        this.gold = 0;
        this.hasKey = false;
        this.inventory = new ArrayList<>();
        this.treasures = new ArrayList<>();
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) {
            hp = 0;
        }
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void addXp(int amount) {
        xp += amount;
    }

    public void checkLevelUp() {
        // Level-up rewards are still basic and can be expanded later.
        while (xp >= level * 25) {
            xp -= level * 25;
            level++;
            maxHp += 10;
            hp = maxHp;
            attack += 2;
            defense += 1;
            System.out.println("You leveled up! You are now level " + level + ".");
        }
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public void removeItem(int index) {
        inventory.remove(index);
    }

    public boolean hasItem(String itemName) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }

    public void addTreasure(Treasure treasure) {
        treasures.add(treasure);
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getGold() {
        return gold;
    }

    public boolean hasKey() {
        return hasKey;
    }

    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public ArrayList<Item> getInventory() {
        return inventory;
    }

    public ArrayList<Treasure> getTreasures() {
        return treasures;
    }
}

class Room {
    private String id;
    private String name;
    private String description;
    private ArrayList<RoomChoice> choices;
    private Enemy enemy;
    private Treasure treasure;
    private boolean treasureTaken;
    private boolean keyReward;
    private Item consumableReward;
    private PuzzleEncounter puzzle;
    private boolean puzzleCleared;
    private String retreatRoomId;

    public Room(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.choices = new ArrayList<>();
    }

    public void addChoice(String code, String text, String nextRoomId) {
        choices.add(new RoomChoice(code, text, nextRoomId));
    }

    public boolean hasEnemy() {
        return enemy != null;
    }

    public boolean hasTreasure() {
        return treasure != null;
    }

    public boolean hasPuzzle() {
        return puzzle != null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<RoomChoice> getChoices() {
        return choices;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }

    public Treasure getTreasure() {
        return treasure;
    }

    public void setTreasure(Treasure treasure) {
        this.treasure = treasure;
    }

    public boolean isTreasureTaken() {
        return treasureTaken;
    }

    public void setTreasureTaken(boolean treasureTaken) {
        this.treasureTaken = treasureTaken;
    }

    public boolean hasKeyReward() {
        return keyReward;
    }

    public void setKeyReward(boolean keyReward) {
        this.keyReward = keyReward;
    }

    public Item getConsumableReward() {
        return consumableReward;
    }

    public void setConsumableReward(Item consumableReward) {
        this.consumableReward = consumableReward;
    }

    public PuzzleEncounter getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(PuzzleEncounter puzzle) {
        this.puzzle = puzzle;
    }

    public boolean isPuzzleCleared() {
        return puzzleCleared;
    }

    public void setPuzzleCleared(boolean puzzleCleared) {
        this.puzzleCleared = puzzleCleared;
    }

    public String getRetreatRoomId() {
        return retreatRoomId;
    }

    public void setRetreatRoomId(String retreatRoomId) {
        this.retreatRoomId = retreatRoomId;
    }
}

class RoomChoice {
    private String code;
    private String text;
    private String nextRoomId;

    public RoomChoice(String code, String text, String nextRoomId) {
        this.code = code;
        this.text = text;
        this.nextRoomId = nextRoomId;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public String getNextRoomId() {
        return nextRoomId;
    }
}

class Enemy {
    private String name;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    private int xpReward;
    private int goldReward;
    private String mainAttack;
    private boolean canUseFistAttack;
    private boolean defeated;
    private Random random;

    public Enemy(String name, int hp, int attack, int defense, int xpReward,
                 int goldReward, String mainAttack, boolean canUseFistAttack) {
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.attack = attack;
        this.defense = defense;
        this.xpReward = xpReward;
        this.goldReward = goldReward;
        this.mainAttack = mainAttack;
        this.canUseFistAttack = canUseFistAttack;
        this.defeated = false;
        this.random = new Random();
    }

    public String chooseAttack() {
        if (canUseFistAttack && random.nextBoolean()) {
            return "fast fist strike";
        }
        return mainAttack;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) {
            hp = 0;
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getXpReward() {
        return xpReward;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public boolean isDefeated() {
        return defeated;
    }

    public void setDefeated(boolean defeated) {
        this.defeated = defeated;
    }
}

class PuzzleEncounter {
    private String prompt;
    private String correctAnswer;
    private String failureMessage;

    public PuzzleEncounter(String prompt, String correctAnswer, String failureMessage) {
        this.prompt = prompt;
        this.correctAnswer = correctAnswer.toLowerCase();
        this.failureMessage = failureMessage;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}

class Item {
    private String name;
    private ItemType type;
    private int power;
    private int quantity;
    private int sellValue;

    public Item(String name, ItemType type, int power, int quantity, int sellValue) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.quantity = quantity;
        this.sellValue = sellValue;
    }

    public String getName() {
        return name;
    }

    public ItemType getType() {
        return type;
    }

    public int getPower() {
        return power;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getSellValue() {
        return sellValue;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

enum ItemType {
    HEALING,
    SPECIAL,
    EQUIPMENT
}

class Treasure {
    private String name;
    private Rarity rarity;
    private int sellPrice;

    public Treasure(String name, Rarity rarity, int sellPrice) {
        this.name = name;
        this.rarity = rarity;
        this.sellPrice = sellPrice;
    }

    public String getName() {
        return name + " [" + rarity + "]";
    }

    public int getSellPrice() {
        return sellPrice;
    }
}

enum Rarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}
