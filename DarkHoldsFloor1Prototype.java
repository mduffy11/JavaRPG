
import java.util.*;

/**
 * Dark Holds - Floor 1 Prototype
 * --------------------------------
 * Refactored version with the hybrid item architecture folded into the prototype.
 *
 * Design goals:
 * - Keep current prototype functionality the same for playtesting.
 * - Reorganize the file into clear sections for game flow, room generation,
 *   combat, player/room data, and item architecture.
 * - Integrate richer item types without forcing a full combat or room rewrite.
 */
public class DarkHoldsFloor1Prototype {

    public static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

/* =====================================================
   SECTION 1 - GAME FLOW AND MAIN LOOP
   ===================================================== */

class Game {
    private static final String FLOOR_ONE_IRON_KEY_ID = "locked_hall_iron_key";

    private Player player;
    private Map<String, Room> rooms;
    private final Random random;
    private final BattleManager battleManager;
    private boolean running;

    public Game() {
        this.random = new Random();
        this.battleManager = new BattleManager();
    }

    public void start() {
        introStory();
        setupGame();
        gameLoop();
    }

    /**
     * Show the opening story and create the player.
     */
    private void introStory() {
        System.out.println("========================================");
        System.out.println("           DARK HOLDS");
        System.out.println("      FLOOR 1 PROTOTYPE BUILD");
        System.out.println("========================================");
        System.out.println("You were once an ordinary villager with no great battle experience.");
        System.out.println("While traveling near ancient ruins, you stepped on a hidden teleportation trap.");
        System.out.println("The world twisted around you, and now you wake in a buried labyrinth.");
        System.out.println("Somewhere below, a giant serpent coils around stolen treasure.");
        System.out.println("If you want to live, you must explore, find a key, and push deeper.");
        System.out.println();
        System.out.print("Enter your hero's name: ");
        String name = DarkHoldsFloor1Prototype.input.nextLine().trim();

        if (name.isEmpty()) {
            name = "Hero";
        }

        player = new Player(name);
    }

    /**
     * Build the floor, distribute guaranteed loot, and place the player in the safe room.
     */
    private void setupGame() {
        rooms = WorldBuilder.createFloorOne(random);
        distributeGuaranteedLoot();
        player.setCurrentRoom("safe");
        player.setPreviousRoom("safe");
        running = true;
    }

    /**
     * Place the guaranteed key, torch, potions, and relic into valid reward rooms.
     *
     * The floor-one key is now stored as a real key object on the player, but it remains
     * outside the normal inventory menu so current prototype behavior stays familiar.
     */
    private void distributeGuaranteedLoot() {
        List<Room> keyEligibleRooms = new ArrayList<>();
        List<Room> torchEligibleRooms = new ArrayList<>();
        List<Room> potionEligibleRooms = new ArrayList<>();

        for (Room room : rooms.values()) {
            if (room.isKeyEligible()) {
                keyEligibleRooms.add(room);
            }

            if (room.isPreLock() && !room.getId().equals("safe")) {
                torchEligibleRooms.add(room);
            }

            if (room.isPreLock()) {
                potionEligibleRooms.add(room);
            }
        }

        Room keyRoom = keyEligibleRooms.get(random.nextInt(keyEligibleRooms.size()));
        keyRoom.setHasKeyReward(true);

        Room torchRoom = torchEligibleRooms.get(random.nextInt(torchEligibleRooms.size()));
        torchRoom.addRewardItem(DarkHoldsItems.torch());

        Collections.shuffle(potionEligibleRooms, random);
        int potionCount = Math.min(3, potionEligibleRooms.size());

        for (int i = 0; i < potionCount; i++) {
            potionEligibleRooms.get(i).addRewardItem(DarkHoldsItems.smallPotion());
        }

        Room hoard = rooms.get("snakeHoard");
        hoard.addRewardItem(DarkHoldsItems.ancientRelic());
        hoard.addGold(randomBetween(25, 50));
    }

    /**
     * Run the main room loop until the player dies, wins, or exits the prototype.
     */
    private void gameLoop() {
        while (running) {
            if (!player.isAlive()) {
                gameOver("Your health reached zero.");
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
            System.out.println(current.getDisplayDescription());
            showPlayerStatus();

            if (current.hasEnemy() && !current.getEnemy().isDefeated()) {
                boolean beginsBattle = promptBeforeBattle(current);

                if (!beginsBattle) {
                    player.setCurrentRoom(player.getPreviousRoom());
                    continue;
                }

                BattleResult result = battleManager.handleBattle(player, current);

                if (result == BattleResult.DIED) {
                    gameOver("You died in battle against " + current.getEnemy().getName() + ".");
                    return;
                }

                if (result == BattleResult.ESCAPED) {
                    continue;
                }
            }

            revealRoomRewards(current);

            if (current.getId().equals("prototypeExit")) {
                victory();
                return;
            }

            handleRoomChoices(current);
        }
    }

    /**
     * Show the player's current stats.
     *
     * Attack and Defense now flow through the richer item architecture, so passive gear
     * would be reflected automatically if later added to the player's inventory.
     */
    private void showPlayerStatus() {
        System.out.println();
        System.out.println("Player: " + player.getName());
        System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp());
        System.out.println("Attack: " + player.getAttack());
        System.out.println("Defense: " + player.getDefense());
        System.out.println("Level: " + player.getLevel() + "   XP: " + player.getXp());
        System.out.println("Gold: " + player.getGold());
        System.out.println("Has Key: " + player.hasAnyKey());
    }

    /**
     * Reveal gold, key rewards, and item rewards after a room is safe.
     */
    private void revealRoomRewards(Room room) {
        if (room.isRewardsCollected()) {
            return;
        }

        if (room.hasEnemy() && !room.getEnemy().isDefeated()) {
            return;
        }

        boolean foundSomething = false;

        if (room.getGoldReward() > 0) {
            System.out.println();
            System.out.println("You find " + room.getGoldReward() + " gold.");
            player.addGold(room.getGoldReward());
            foundSomething = true;
        }

        if (room.hasKeyReward()) {
            System.out.println();
            System.out.println("You discover an Iron Key hidden here.");
            player.addKey(DarkHoldsItems.floorOneIronKey());
            foundSomething = true;
        }

        if (!room.getRewardItems().isEmpty()) {
            System.out.println();
            for (Item item : room.getRewardItems()) {
                System.out.println("You found: " + item.getName());
                player.addItem(item);
            }
            foundSomething = true;
        }

        if (foundSomething) {
            room.setRewardsCollected(true);
        }
    }

    /**
     * Ask whether the player wants to begin a battle or step back before it starts.
     */
    private boolean promptBeforeBattle(Room room) {
        while (true) {
            System.out.println();
            System.out.println("A " + room.getEnemy().getName() + " is here.");
            System.out.println("1. Fight");
            System.out.println("2. Back away");
            System.out.print("Choose: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("1")) {
                return true;
            }

            if (answer.equals("2")) {
                System.out.println("You back away before the fight begins.");
                return false;
            }

            System.out.println("Invalid choice.");
        }
    }

    /**
     * Show available room exits and the overworld inventory menu.
     *
     * Locked-hall checking now looks for a specific key object instead of a player boolean,
     * while preserving the same visible prototype behavior.
     */
    private void handleRoomChoices(Room current) {
        while (true) {
            System.out.println();

            List<String> neighborIds = getVisibleNeighborIds(current);
            for (int i = 0; i < neighborIds.size(); i++) {
                Room nextRoom = rooms.get(neighborIds.get(i));
                String text = "Go to " + nextRoom.getName();

                if (current.getId().equals("safe")
                        && nextRoom.getId().equals("lockedHall")
                        && !player.hasKey(FLOOR_ONE_IRON_KEY_ID)) {
                    text = "Try the locked hallway (locked)";
                }

                System.out.println((i + 1) + ". " + text);
            }

            System.out.println("I. Inventory");
            System.out.print("Choose an action: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equalsIgnoreCase("I")) {
                openInventoryMenu();
                continue;
            }

            try {
                int choice = Integer.parseInt(answer);

                if (choice < 1 || choice > neighborIds.size()) {
                    System.out.println("Invalid choice.");
                    continue;
                }

                Room nextRoom = rooms.get(neighborIds.get(choice - 1));

                if (current.getId().equals("safe")
                        && nextRoom.getId().equals("lockedHall")
                        && !player.hasKey(FLOOR_ONE_IRON_KEY_ID)) {
                    System.out.println("The heavy hallway gate is locked. You need a key.");
                    continue;
                }

                player.setPreviousRoom(player.getCurrentRoom());
                player.setCurrentRoom(nextRoom.getId());
                return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }

    /**
     * Hide the boss-hoard connection until the floor boss is defeated.
     */
    private List<String> getVisibleNeighborIds(Room current) {
        List<String> visible = new ArrayList<>();

        for (String neighborId : current.getNeighborIds()) {
            if (current.getId().equals("snakeBoss")
                    && neighborId.equals("snakeHoard")
                    && current.hasEnemy()
                    && !current.getEnemy().isDefeated()) {
                continue;
            }

            visible.add(neighborId);
        }

        return visible;
    }

    /**
     * Run the overworld inventory menu.
     *
     * Outside battle, only items marked for outside use can activate. Passive items and
     * combat-only items simply explain that they cannot be used here.
     */
    private void openInventoryMenu() {
        while (true) {
            System.out.println();
            System.out.println("INVENTORY");

            if (player.getInventory().isEmpty()) {
                System.out.println("You are carrying nothing.");
                System.out.println("0. Leave inventory");
                System.out.print("Choose: ");
                String emptyChoice = DarkHoldsFloor1Prototype.input.nextLine().trim();

                if (emptyChoice.equals("0")) {
                    return;
                }

                continue;
            }

            for (int i = 0; i < player.getInventory().size(); i++) {
                System.out.println((i + 1) + ". " + player.getInventory().get(i));
            }

            System.out.println("0. Leave inventory");
            System.out.print("Choose an item to use, or 0 to leave: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("0")) {
                return;
            }

            try {
                int index = Integer.parseInt(answer) - 1;

                if (index < 0 || index >= player.getInventory().size()) {
                    System.out.println("Invalid item choice.");
                    continue;
                }

                Item item = player.getInventory().get(index);

                if (item instanceof GameItem gameItem && gameItem.canUseOutsideBattle()) {
                    ItemContext context = new ItemContext();
                    context.setInBattle(false);
                    context.setCurrentRoomId(player.getCurrentRoom());
                    context.setPreviousRoomId(player.getPreviousRoom());

                    boolean removeAfterUse = gameItem.use(player, context);

                    if (removeAfterUse) {
                        player.removeItem(index);
                    }

                    continue;
                }

                System.out.println(item.getName() + " cannot be used outside battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }

    /**
     * Print the victory message and stop the loop.
     */
    private void victory() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("FLOOR 1 CLEARED");
        System.out.println("========================================");
        System.out.println("The giant snake lies still behind you.");
        System.out.println("A deeper stair yawns open below the hoard.");
        System.out.println("For this prototype build, your descent ends here.");
        running = false;
    }

    /**
     * Print the game over message and stop the loop.
     */
    private void gameOver(String message) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("GAME OVER");
        System.out.println("========================================");
        System.out.println(message);
        running = false;
    }

    /**
     * Roll a number inside an inclusive range.
     */
    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

/* =====================================================
   SECTION 2 - WORLD GENERATION AND ROOM LAYOUT
   ===================================================== */

class WorldBuilder {

    /**
     * Build the full first floor, including the main route, optional rooms, and all room links.
     */
    public static Map<String, Room> createFloorOne(Random random) {
        Map<String, Room> rooms = new LinkedHashMap<>();

        Room safeRoom = new Room("safe", "Safe Room",
                "You wake in a cold stone chamber with only a rough stick beside you. "
                        + "A heavy locked hallway gate stands to one side, and the rest of the floor branches into darkness.",
                true, true);

        Room brokenFork = new Room("brokenFork", "Broken Fork",
                "The passage splits here around cracked masonry. Dirty tracks, torn cloth, and scattered debris suggest multiple creatures pass through this junction.",
                true, true);

        Room batRoost = new Room("batRoost", "Bat Roost",
                "The ceiling above is black with restless shapes. Thin squeaks echo between the stones.",
                true, true);
        batRoost.setEnemy(createBatEnemy());
        batRoost.setClearedDescription("The roost is still and quiet now. Only a few drifting feathers remain.");

        Room goblinOne = new Room("goblinOne", "Goblin Trail - Outer Den",
                "The smell of rot and old smoke thickens. You spot a goblin scavenger lurking among pilfered junk.",
                true, true);
        goblinOne.setEnemy(createGoblinEnemy());
        goblinOne.setClearedDescription("The outer den is quiet now. The scavenger that haunted it is gone.");
        goblinOne.addGold(randomBetween(random, 2, 6));

        Room goblinTwo = createGoblinMiddleRoom(random);

        Room goblinThree = new Room("goblinThree", "Goblin Trail - Hidden Cache",
                "The trail ends in a cramped nook full of stolen odds and ends. A tougher goblin guards the prize.",
                true, true);
        goblinThree.setEnemy(createGoblinEnemy());
        goblinThree.setClearedDescription("The hidden cache lies quiet now, stripped of anything worth taking.");
        goblinThree.addGold(randomBetween(random, 8, 20));

        Room lockedHall = new Room("lockedHall", "Locked Hallway",
                "With the key in hand, you force the rusted mechanism open. A narrow hall slopes toward something older and more dangerous.",
                false, false);

        Room shrine = new Room("shrine", "Serpent Shrine",
                "A shrine of coiled stone and old offerings lies beyond the hall. Something vast has shed its skin here more than once.",
                false, false);

        Room snakeBoss = new Room("snakeBoss", "Serpent Lair",
                "A giant snake rises from the treasure mound, its eyes fixed on you. This beast rules the floor.",
                false, false);
        snakeBoss.setEnemy(createSnakeEnemy());
        snakeBoss.setClearedDescription("The serpent lair is still and quiet now. The giant snake no longer rules this floor.");

        Room snakeHoard = new Room("snakeHoard", "Snake Hoard",
                "Gold glints among bones and torn packs. Past the hoard, a stair curls deeper into the dark.",
                false, false);

        Room prototypeExit = new Room("prototypeExit", "Deeper Stair",
                "A deeper stair waits below. The first floor is behind you.",
                false, false);

        rooms.put(safeRoom.getId(), safeRoom);
        rooms.put(brokenFork.getId(), brokenFork);
        rooms.put(batRoost.getId(), batRoost);
        rooms.put(goblinOne.getId(), goblinOne);
        rooms.put(goblinTwo.getId(), goblinTwo);
        rooms.put(goblinThree.getId(), goblinThree);
        rooms.put(lockedHall.getId(), lockedHall);
        rooms.put(shrine.getId(), shrine);
        rooms.put(snakeBoss.getId(), snakeBoss);
        rooms.put(snakeHoard.getId(), snakeHoard);
        rooms.put(prototypeExit.getId(), prototypeExit);

        Room optionalOne = createOptionalRoom(random, "optionalOne");
        Room optionalTwo = createOptionalRoom(random, "optionalTwo");
        rooms.put(optionalOne.getId(), optionalOne);
        rooms.put(optionalTwo.getId(), optionalTwo);

        connect(safeRoom, brokenFork);
        connect(safeRoom, lockedHall);
        connect(brokenFork, batRoost);
        connect(brokenFork, goblinOne);
        connect(goblinOne, goblinTwo);
        connect(goblinTwo, goblinThree);
        connect(lockedHall, shrine);
        connect(shrine, snakeBoss);
        connect(snakeBoss, snakeHoard);
        connect(snakeHoard, prototypeExit);

        List<Room> anchorPool = new ArrayList<>();
        anchorPool.add(safeRoom);
        anchorPool.add(brokenFork);
        anchorPool.add(batRoost);
        anchorPool.add(lockedHall);

        Collections.shuffle(anchorPool, random);
        Room firstAnchor = anchorPool.get(0);
        Room secondAnchor = anchorPool.get(1);

        connect(firstAnchor, optionalOne);
        connect(secondAnchor, optionalTwo);

        if (!firstAnchor.isPreLock()) {
            optionalOne.setPreLock(false);
            optionalOne.setKeyEligible(false);
        }

        if (!secondAnchor.isPreLock()) {
            optionalTwo.setPreLock(false);
            optionalTwo.setKeyEligible(false);
        }

        return rooms;
    }

    /**
     * Build the variable middle goblin room from a weighted random selection.
     */
    private static Room createGoblinMiddleRoom(Random random) {
        int roll = random.nextInt(10);

        if (roll <= 2) {
            Room room = new Room("goblinTwo", "Goblin Trail - Empty Cache",
                    "A looted side chamber lies here with only scattered coins left behind.",
                    true, true);
            room.addGold(randomBetween(random, 1, 30));
            return room;
        }

        if (roll <= 4) {
            Room room = new Room("goblinTwo", "Goblin Trail - Bat Pocket",
                    "A hollow in the stone opens overhead. Bats pour down from the dark when you step inside.",
                    true, true);
            room.setEnemy(createBatEnemy());
            return room;
        }

        if (roll <= 6) {
            Room room = new Room("goblinTwo", "Goblin Trail - Forgotten Treasure",
                    "Something gleams in the dust here. No enemy claims it at the moment.",
                    true, true);
            room.addGold(randomBetween(random, 10, 24));
            return room;
        }

        if (roll <= 8) {
            Room room = new Room("goblinTwo", "Goblin Trail - Wolf Den",
                    "A lean cave wolf has made a nest here from old cloth and bones.",
                    true, true);
            room.setEnemy(createWolfEnemy());
            room.addGold(randomBetween(random, 4, 10));
            return room;
        }

        Room room = new Room("goblinTwo", "Goblin Trail - Damp Tunnel",
                "A wet side cut opens into a slick pocket where something gelatinous shivers in the dark.",
                true, true);
        room.setEnemy(createSlimeEnemy());
        room.addGold(randomBetween(random, 15, 35));
        return room;
    }

    /**
     * Build an optional room from a weighted list of encounter templates.
     */
    private static Room createOptionalRoom(Random random, String id) {
        List<Integer> weightedPool = Arrays.asList(0, 0, 1, 1, 2, 3);
        int pick = weightedPool.get(random.nextInt(weightedPool.size()));

        if (pick == 0) {
            Room room = new Room(id, "Optional Bat Room",
                    "A cramped side room thrums with leathery wings and old guano.",
                    true, true);
            room.setEnemy(createBatEnemy());
            room.addGold(randomBetween(random, 1, 6));
            return room;
        }

        if (pick == 1) {
            Room room = new Room(id, "Optional Goblin Room",
                    "A lone goblin rummages through a pile of cracked bowls and bent tools.",
                    true, true);
            room.setEnemy(createGoblinEnemy());
            room.addGold(randomBetween(random, 3, 8));
            return room;
        }

        if (pick == 2) {
            Room room = new Room(id, "Optional Slime Room",
                    "This chamber is slick with foul moisture. A slime waits here, patient and hungry.",
                    true, true);
            room.setEnemy(createSlimeEnemy());
            room.addGold(randomBetween(random, 15, 35));
            return room;
        }

        Room room = new Room(id, "Optional Wolf Den",
                "A gaunt wolf growls low from the shadow of a collapsed arch.",
                true, true);
        room.setEnemy(createWolfEnemy());
        room.addGold(randomBetween(random, 5, 10));
        return room;
    }

    /**
     * Create a goblin enemy and its moves.
     */
    private static Enemy createGoblinEnemy() {
        return new Enemy(
                EnemyType.GOBLIN,
                "Goblin",
                20,
                5,
                2,
                10,
                randomGold(4, 8),
                new EnemyMove("Knife Stab", 2, 78, MoveEffect.NONE, 0),
                new EnemyMove("Wild Swing", 4, 55, MoveEffect.NONE, 0),
                new EnemyMove("Pocket Sand", 0, 50, MoveEffect.POCKET_SAND, 50)
        );
    }

    /**
     * Create a bat enemy and its moves.
     */
    private static Enemy createBatEnemy() {
        return new Enemy(
                EnemyType.BAT,
                "Bats",
                14,
                4,
                1,
                8,
                randomGold(1, 4),
                new EnemyMove("Bite", 1, 80, MoveEffect.NONE, 0),
                new EnemyMove("Screech", 2, 72, MoveEffect.NONE, 0)
        );
    }

    /**
     * Create a wolf enemy and its moves.
     */
    private static Enemy createWolfEnemy() {
        return new Enemy(
                EnemyType.WOLF,
                "Wolf",
                24,
                6,
                2,
                12,
                randomGold(4, 7),
                new EnemyMove("Bite", 2, 78, MoveEffect.NONE, 0),
                new EnemyMove("Pounce", 5, 52, MoveEffect.NONE, 0)
        );
    }

    /**
     * Create a slime enemy and its moves.
     */
    private static Enemy createSlimeEnemy() {
        return new Enemy(
                EnemyType.SLIME,
                "Slime",
                42,
                4,
                5,
                16,
                randomGold(8, 15),
                new EnemyMove("Slam", 2, 74, MoveEffect.NONE, 0),
                new EnemyMove("Acid Splash", 3, 68, MoveEffect.NONE, 0)
        );
    }

    /**
     * Create the floor boss and its moves.
     */
    private static Enemy createSnakeEnemy() {
        return new Enemy(
                EnemyType.SNAKE,
                "Giant Snake",
                34,
                8,
                3,
                28,
                randomGold(15, 25),
                new EnemyMove("Bite", 3, 76, MoveEffect.NONE, 0),
                new EnemyMove("Tail Lash", 5, 52, MoveEffect.NONE, 0),
                new EnemyMove("Constrict", 2, 60, MoveEffect.CONSTRICT, 0)
        );
    }

    /**
     * Link two rooms in both directions.
     */
    private static void connect(Room a, Room b) {
        a.addNeighbor(b.getId());
        b.addNeighbor(a.getId());
    }

    /**
     * Roll a number inside an inclusive range.
     */
    private static int randomBetween(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Roll an enemy gold reward inside an inclusive range.
     */
    private static int randomGold(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min + 1);
    }
}

/* =====================================================
   SECTION 3 - COMBAT FLOW, ENEMY DATA, AND TURN RESULTS
   ===================================================== */

class BattleManager {
    private static final int RUN_SUCCESS_RATE = 80;
    private static final int CONSTRICT_DAMAGE = 2;

    private final Random random;

    public BattleManager() {
        this.random = new Random();
    }

    /**
     * Run a full battle until the player wins, dies, or escapes.
     */
    public BattleResult handleBattle(Player player, Room room) {
        Enemy enemy = room.getEnemy();
        boolean sandInEyes = false;
        int constrictStage = 0;

        System.out.println();
        System.out.println("A battle begins against " + enemy.getName() + "!");

        while (player.isAlive() && enemy.isAlive()) {
            System.out.println();
            System.out.println(player.getName() + " HP: " + player.getHp() + "/" + player.getMaxHp());
            System.out.println(enemy.getName() + " HP: " + enemy.getHp() + "/" + enemy.getMaxHp());

            if (constrictStage > 0) {
                if (constrictStage == 1) {
                    System.out.println("The giant snake tightens its coils around you!");
                    player.takeDamage(CONSTRICT_DAMAGE);
                    System.out.println("You lose your turn and take " + CONSTRICT_DAMAGE + " damage.");
                    constrictStage = 2;
                    continue;
                }

                if (constrictStage == 2 || constrictStage == 3) {
                    boolean continues = random.nextBoolean();

                    if (continues) {
                        System.out.println("The constriction continues!");
                        player.takeDamage(CONSTRICT_DAMAGE);
                        System.out.println("You lose your turn and take " + CONSTRICT_DAMAGE + " damage.");

                        if (constrictStage == 3) {
                            constrictStage = 0;
                            System.out.println("The giant snake finally releases you.");
                        } else {
                            constrictStage++;
                        }

                        continue;
                    }

                    constrictStage = 0;
                    System.out.println("You wrench yourself free of the giant snake's coils!");
                }
            }

            boolean actionResolved = false;
            boolean dodgeActive = false;

            while (!actionResolved) {
                PlayerAction action = promptPlayerAction(player);

                if (action == PlayerAction.ATTACK) {
                    sandInEyes = playerBasicAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.TORCH) {
                    sandInEyes = playerTorchAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.ACTION) {
                    BattleTurnResult result = openActionMenu();
                    actionResolved = result.isTurnConsumed();
                    dodgeActive = result.isDodgeActive();
                } else if (action == PlayerAction.INVENTORY) {
                    BattleTurnResult result = useItemInBattle(player, enemy, sandInEyes);

                    if (result.didConsumeSandBlindness()) {
                        sandInEyes = result.isSandInEyes();
                    }

                    actionResolved = result.isTurnConsumed();

                    if (result.isForcedEscape()) {
                        return BattleResult.ESCAPED;
                    }
                } else if (action == PlayerAction.RUN) {
                    boolean escaped = tryRun(player);
                    actionResolved = true;

                    if (escaped) {
                        return BattleResult.ESCAPED;
                    }
                }
            }

            if (!enemy.isAlive()) {
                break;
            }

            EnemyMove move = enemy.chooseMove(random);
            System.out.println(enemy.getName() + " uses " + move.getName() + "!");

            if (dodgeActive) {
                System.out.println("You managed to dodge the attack!");
                continue;
            }

            int hitRoll = random.nextInt(100) + 1;

            if (hitRoll > move.getAccuracy()) {
                System.out.println(enemy.getName() + " misses.");
                continue;
            }

            int damage = calculateEnemyDamage(enemy, move, player);
            player.takeDamage(damage);
            System.out.println(enemy.getName() + " hits you for " + damage + " damage.");

            if (move.getEffect() == MoveEffect.POCKET_SAND) {
                int effectRoll = random.nextInt(100) + 1;

                if (effectRoll <= move.getEffectChance()) {
                    sandInEyes = true;
                    System.out.println("You have sand in your eyes! It's making it hard to see!");
                } else {
                    System.out.println("The goblin's filthy trick fails to blind you.");
                }
            }

            if (move.getEffect() == MoveEffect.CONSTRICT && constrictStage == 0) {
                constrictStage = 1;
                System.out.println("The giant snake wraps around you!");
            }
        }

        if (!player.isAlive()) {
            return BattleResult.DIED;
        }

        enemy.setDefeated(true);
        System.out.println("You defeated " + enemy.getName() + "!");
        System.out.println("You gained " + enemy.getXpReward() + " XP and " + enemy.getGoldReward() + " gold.");
        player.addXp(enemy.getXpReward());
        player.addGold(enemy.getGoldReward());
        player.checkLevelUp();

        return BattleResult.WON;
    }

    /**
     * Show the current battle menu.
     *
     * This still preserves the prototype's current action list:
     * Stick Attack, Torch when carried, Action, Inventory, and Run.
     */
    private PlayerAction promptPlayerAction(Player player) {
        while (true) {
            Map<String, PlayerAction> options = new LinkedHashMap<>();
            int optionNumber = 1;

            System.out.println(optionNumber + ". Stick Attack");
            options.put(String.valueOf(optionNumber++), PlayerAction.ATTACK);

            if (DarkHoldsItems.playerHasTorch(player)) {
                System.out.println(optionNumber + ". Torch");
                options.put(String.valueOf(optionNumber++), PlayerAction.TORCH);
            }

            System.out.println(optionNumber + ". Action");
            options.put(String.valueOf(optionNumber++), PlayerAction.ACTION);

            System.out.println(optionNumber + ". Inventory");
            options.put(String.valueOf(optionNumber++), PlayerAction.INVENTORY);

            System.out.println(optionNumber + ". Run");
            options.put(String.valueOf(optionNumber), PlayerAction.RUN);

            System.out.print("Choose action: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            PlayerAction chosen = options.get(answer);
            if (chosen != null) {
                return chosen;
            }

            System.out.println("Invalid choice.");
        }
    }

    /**
     * Resolve the current stick attack without changing its existing prototype behavior.
     */
    private boolean playerBasicAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem stick = DarkHoldsItems.findWeapon(player, "Stick");
        int accuracy = stick != null ? stick.getAccuracy() : 95;

        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You blink through the sand in your eyes and swing blindly!");
        }

        int hitRoll = random.nextInt(100) + 1;

        if (hitRoll > accuracy) {
            System.out.println("Your Stick Attack misses.");
            return false;
        }

        int damage;

        if (stick != null) {
            damage = stick.getDamageAgainst(enemy, player);
        } else if (enemy.getType() == EnemyType.SLIME) {
            damage = 2;
        } else {
            damage = calculatePlayerDamage(player.getAttack(), 2, enemy.getDefense());
        }

        enemy.takeDamage(damage);
        System.out.println("You use Stick Attack and deal " + damage + " damage.");
        return false;
    }

    /**
     * Resolve the current torch attack without changing its existing prototype behavior.
     */
    private boolean playerTorchAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem torch = DarkHoldsItems.findWeapon(player, "Torch");
        int accuracy = torch != null ? torch.getAccuracy() : 80;

        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You try to line up the torch through burning, gritty eyes!");
        }

        int hitRoll = random.nextInt(100) + 1;

        if (hitRoll > accuracy) {
            System.out.println("Your Torch attack misses.");
            return false;
        }

        int damage;

        if (torch != null) {
            damage = torch.getDamageAgainst(enemy, player);
        } else if (enemy.getType() == EnemyType.SLIME) {
            damage = Math.max(10, calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense()) + 6);
        } else {
            damage = calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense());
        }

        enemy.takeDamage(damage);
        System.out.println("You lash out with the Torch and deal " + damage + " damage.");
        return false;
    }

    /**
     * Open the action submenu. Right now it only contains Dodge.
     */
    private BattleTurnResult openActionMenu() {
        while (true) {
            System.out.println("Action:");
            System.out.println("1. Dodge");
            System.out.println("0. Return");
            System.out.print("Choose action: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("1")) {
                System.out.println("You prepare to dodge the next attack.");
                return new BattleTurnResult(true, false, false, true, false);
            }

            if (answer.equals("0")) {
                return new BattleTurnResult(false, false, false, false, false);
            }

            System.out.println("Invalid choice.");
        }
    }

    /**
     * Open the battle inventory menu.
     *
     * Stick and Torch still duplicate their battle actions from inventory.
     * Other battle-usable game items now route through the richer item architecture.
     * Invalid choices do not consume the player's turn.
     */
    private BattleTurnResult useItemInBattle(Player player, Enemy enemy, boolean sandInEyes) {
        while (true) {
            System.out.println("Inventory:");

            if (player.getInventory().isEmpty()) {
                System.out.println("You have no items.");
                return new BattleTurnResult(false, sandInEyes, false, false, false);
            }

            for (int i = 0; i < player.getInventory().size(); i++) {
                System.out.println((i + 1) + ". " + player.getInventory().get(i));
            }

            System.out.println("0. Cancel");
            System.out.print("Choose item number: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("0")) {
                return new BattleTurnResult(false, sandInEyes, false, false, false);
            }

            try {
                int index = Integer.parseInt(answer) - 1;

                if (index < 0 || index >= player.getInventory().size()) {
                    System.out.println("Invalid item choice.");
                    continue;
                }

                Item item = player.getInventory().get(index);

                if (item.getName().equalsIgnoreCase("Stick")) {
                    boolean updatedSandInEyes = playerBasicAttack(player, enemy, sandInEyes);
                    return new BattleTurnResult(true, updatedSandInEyes, true, false, false);
                }

                if (item.getName().equalsIgnoreCase("Torch")) {
                    boolean updatedSandInEyes = playerTorchAttack(player, enemy, sandInEyes);
                    return new BattleTurnResult(true, updatedSandInEyes, true, false, false);
                }

                if (item instanceof GameItem gameItem && gameItem.canUseInBattle()) {
                    ItemContext context = new ItemContext();
                    context.setInBattle(true);
                    context.setCurrentEnemy(enemy);
                    context.setCurrentRoomId(player.getCurrentRoom());
                    context.setPreviousRoomId(player.getPreviousRoom());

                    boolean removeAfterUse = gameItem.use(player, context);

                    if (removeAfterUse) {
                        player.removeItem(index);
                    }

                    if (context.isEscapedByItem()) {
                        return new BattleTurnResult(true, sandInEyes, false, false, true);
                    }

                    return new BattleTurnResult(true, sandInEyes, false, false, false);
                }

                System.out.println(item.getName() + " cannot be used in battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid item choice.");
            }
        }
    }

    /**
     * Attempt to escape the current battle and return to the previous room.
     */
    private boolean tryRun(Player player) {
        int roll = random.nextInt(100) + 1;

        if (roll <= RUN_SUCCESS_RATE) {
            System.out.println("You escape back to the previous room.");
            player.setCurrentRoom(player.getPreviousRoom());
            return true;
        }

        System.out.println("You fail to escape!");
        return false;
    }

    /**
     * Calculate normal player damage from attack stat, move power, and enemy defense.
     */
    private int calculatePlayerDamage(int playerAttack, int movePower, int enemyDefense) {
        return Math.max(1, movePower + playerAttack - enemyDefense);
    }

    /**
     * Calculate enemy damage, including the special Pocket Sand exception.
     */
    private int calculateEnemyDamage(Enemy enemy, EnemyMove move, Player player) {
        if (move.getEffect() == MoveEffect.POCKET_SAND) {
            return 1;
        }

        return Math.max(1, move.getPower() + enemy.getAttack() - player.getDefense());
    }
}

class Enemy {
    private final EnemyType type;
    private final String name;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int xpReward;
    private final int goldReward;
    private final ArrayList<EnemyMove> moves;
    private int hp;
    private boolean defeated;

    public Enemy(EnemyType type, String name, int hp, int attack, int defense, int xpReward, int goldReward,
                 EnemyMove... moves) {
        this.type = type;
        this.name = name;
        this.maxHp = hp;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.xpReward = xpReward;
        this.goldReward = goldReward;
        this.defeated = false;
        this.moves = new ArrayList<>();

        Collections.addAll(this.moves, moves);
    }

    public EnemyMove chooseMove(Random random) {
        return moves.get(random.nextInt(moves.size()));
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

    public EnemyType getType() {
        return type;
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

class EnemyMove {
    private final String name;
    private final int power;
    private final int accuracy;
    private final MoveEffect effect;
    private final int effectChance;

    public EnemyMove(String name, int power, int accuracy, MoveEffect effect, int effectChance) {
        this.name = name;
        this.power = power;
        this.accuracy = accuracy;
        this.effect = effect;
        this.effectChance = effectChance;
    }

    public String getName() {
        return name;
    }

    public int getPower() {
        return power;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public MoveEffect getEffect() {
        return effect;
    }

    public int getEffectChance() {
        return effectChance;
    }
}

enum EnemyType {
    GOBLIN,
    BAT,
    WOLF,
    SLIME,
    SNAKE
}

enum MoveEffect {
    NONE,
    POCKET_SAND,
    CONSTRICT
}

enum BattleResult {
    WON,
    ESCAPED,
    DIED
}

enum PlayerAction {
    ATTACK,
    TORCH,
    ACTION,
    INVENTORY,
    RUN
}

class BattleTurnResult {
    private final boolean turnConsumed;
    private final boolean sandInEyes;
    private final boolean consumeSandBlindness;
    private final boolean dodgeActive;
    private final boolean forcedEscape;

    public BattleTurnResult(boolean turnConsumed, boolean sandInEyes, boolean consumeSandBlindness,
                            boolean dodgeActive, boolean forcedEscape) {
        this.turnConsumed = turnConsumed;
        this.sandInEyes = sandInEyes;
        this.consumeSandBlindness = consumeSandBlindness;
        this.dodgeActive = dodgeActive;
        this.forcedEscape = forcedEscape;
    }

    public boolean isTurnConsumed() {
        return turnConsumed;
    }

    public boolean isSandInEyes() {
        return sandInEyes;
    }

    public boolean didConsumeSandBlindness() {
        return consumeSandBlindness;
    }

    public boolean isDodgeActive() {
        return dodgeActive;
    }

    public boolean isForcedEscape() {
        return forcedEscape;
    }
}

/* =====================================================
   SECTION 4 - PLAYER STATE AND ROOM DATA
   ===================================================== */

class Player {
    private static final int[] XP_THRESHOLDS = {0, 18, 42, 75};
    private static final int[] HP_BY_LEVEL = {50, 53, 57, 61};
    private static final int[] ATTACK_BY_LEVEL = {8, 9, 10, 11};
    private static final int[] DEFENSE_BY_LEVEL = {2, 2, 3, 3};

    private final String name;
    private int hp;
    private int maxHp;
    private int baseAttack;
    private int baseDefense;
    private int level;
    private int xp;
    private int gold;
    private String currentRoom;
    private String previousRoom;
    private final ArrayList<Item> inventory;
    private final ArrayList<KeyItem> keys;

    public Player(String name) {
        this.name = name;
        this.level = 1;
        this.xp = 0;
        this.gold = 0;
        this.maxHp = HP_BY_LEVEL[0];
        this.hp = maxHp;
        this.baseAttack = ATTACK_BY_LEVEL[0];
        this.baseDefense = DEFENSE_BY_LEVEL[0];
        this.inventory = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.inventory.add(DarkHoldsItems.stick());
    }

    /**
     * Reduce HP but never below zero.
     */
    public void takeDamage(int amount) {
        hp -= amount;

        if (hp < 0) {
            hp = 0;
        }
    }

    /**
     * Restore HP but never above max HP.
     */
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

    /**
     * Apply level-up thresholds and refresh the player's base stats.
     */
    public void checkLevelUp() {
        while (level < 4 && xp >= XP_THRESHOLDS[level]) {
            level++;
            maxHp = HP_BY_LEVEL[level - 1];
            baseAttack = ATTACK_BY_LEVEL[level - 1];
            baseDefense = DEFENSE_BY_LEVEL[level - 1];
            hp = maxHp;
            System.out.println("You reached level " + level + "!");
            System.out.println("Your strength rises.");
        }
    }

    public void addGold(int amount) {
        gold += amount;
    }

    /**
     * Add a normal item to the player's carried inventory.
     */
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

    /**
     * Add a key object to the player's key collection.
     *
     * Keys are modeled as real items now, but are stored separately from the main
     * inventory so the current prototype menus remain unchanged.
     */
    public void addKey(KeyItem key) {
        keys.add(key);
    }

    public boolean hasAnyKey() {
        return !keys.isEmpty();
    }

    public boolean hasKey(String keyId) {
        for (KeyItem key : keys) {
            if (key.getKeyId().equalsIgnoreCase(keyId)) {
                return true;
            }
        }

        return false;
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

    /**
     * Return attack with any passive item bonuses included.
     */
    public int getAttack() {
        return baseAttack + DarkHoldsItems.getPassiveAttackBonus(this);
    }

    /**
     * Return defense with any passive item bonuses included.
     */
    public int getDefense() {
        return baseDefense + DarkHoldsItems.getPassiveDefenseBonus(this);
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

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public String getPreviousRoom() {
        return previousRoom;
    }

    public void setPreviousRoom(String previousRoom) {
        this.previousRoom = previousRoom;
    }

    public ArrayList<Item> getInventory() {
        return inventory;
    }

    public ArrayList<KeyItem> getKeys() {
        return keys;
    }
}

class Room {
    private final String id;
    private final String name;
    private final String description;
    private String clearedDescription;
    private final ArrayList<String> neighborIds;
    private final ArrayList<Item> rewardItems;
    private Enemy enemy;
    private int goldReward;
    private boolean hasKeyReward;
    private boolean rewardsCollected;
    private boolean preLock;
    private boolean keyEligible;

    public Room(String id, String name, String description, boolean preLock, boolean keyEligible) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.clearedDescription = "This room is still and quiet now.";
        this.preLock = preLock;
        this.keyEligible = keyEligible;
        this.neighborIds = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
        this.goldReward = 0;
        this.hasKeyReward = false;
        this.rewardsCollected = false;
    }

    public void addNeighbor(String neighborId) {
        if (!neighborIds.contains(neighborId)) {
            neighborIds.add(neighborId);
        }
    }

    public void addRewardItem(Item item) {
        rewardItems.add(item);
    }

    public void addGold(int amount) {
        goldReward += amount;
    }

    public boolean hasEnemy() {
        return enemy != null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayDescription() {
        if (hasBeenCleared()) {
            return clearedDescription;
        }

        return description;
    }

    public void setClearedDescription(String clearedDescription) {
        this.clearedDescription = clearedDescription;
    }

    public boolean hasBeenCleared() {
        if (hasEnemy() && enemy.isDefeated()) {
            return true;
        }

        return rewardsCollected;
    }

    public ArrayList<String> getNeighborIds() {
        return neighborIds;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public boolean hasKeyReward() {
        return hasKeyReward;
    }

    public void setHasKeyReward(boolean hasKeyReward) {
        this.hasKeyReward = hasKeyReward;
    }

    public ArrayList<Item> getRewardItems() {
        return rewardItems;
    }

    public boolean isRewardsCollected() {
        return rewardsCollected;
    }

    public void setRewardsCollected(boolean rewardsCollected) {
        this.rewardsCollected = rewardsCollected;
    }

    public boolean isPreLock() {
        return preLock;
    }

    public void setPreLock(boolean preLock) {
        this.preLock = preLock;
    }

    public boolean isKeyEligible() {
        return keyEligible;
    }

    public void setKeyEligible(boolean keyEligible) {
        this.keyEligible = keyEligible;
    }
}

/* =====================================================
   SECTION 5 - ITEM ARCHITECTURE, ITEM HELPERS, AND ITEM TYPES
   ===================================================== */

/**
 * Central item helper and factory class.
 *
 * This keeps the prototype's simple inventory model while still allowing a few richer
 * item subclasses for weapons, consumables, passive gear, keys, and future utility items.
 */
final class DarkHoldsItems {

    private DarkHoldsItems() {
    }

    public static StickItem stick() {
        return new StickItem();
    }

    public static SwordItem sword() {
        return new SwordItem();
    }

    public static TorchItem torch() {
        return new TorchItem();
    }

    public static SmallPotionItem smallPotion() {
        return new SmallPotionItem();
    }

    public static BigPotionItem bigPotion() {
        return new BigPotionItem();
    }

    public static ScrollOfEscapeItem scrollOfEscape() {
        return new ScrollOfEscapeItem();
    }

    public static ScrollOfFireballItem scrollOfFireball() {
        return new ScrollOfFireballItem();
    }

    public static ScrollOfStealthItem scrollOfStealth() {
        return new ScrollOfStealthItem();
    }

    public static ArmorItem armor() {
        return new ArmorItem();
    }

    public static ShieldItem shield() {
        return new ShieldItem();
    }

    public static KeyItem floorOneIronKey() {
        return new KeyItem("locked_hall_iron_key", "Iron Key");
    }

    public static AncientRelicItem ancientRelic() {
        return new AncientRelicItem();
    }

    public static BookOfSpellsItem bookOfSpells() {
        return new BookOfSpellsItem();
    }

    public static int getPassiveAttackBonus(Player player) {
        int total = 0;

        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) {
                total += gear.getPassiveAttackBonus();
            }
        }

        return total;
    }

    public static int getPassiveDefenseBonus(Player player) {
        int total = 0;

        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) {
                total += gear.getPassiveDefenseBonus();
            }
        }

        return total;
    }

    public static boolean playerHasTorch(Player player) {
        return player.hasItem("Torch");
    }

    public static WeaponItem findWeapon(Player player, String weaponName) {
        for (Item item : player.getInventory()) {
            if (item instanceof WeaponItem weapon && weapon.getName().equalsIgnoreCase(weaponName)) {
                return weapon;
            }
        }

        return null;
    }
}

/**
 * Context object passed into active item uses.
 *
 * The current prototype only needs a little context, but this leaves room for later battle,
 * stealth, and room utility without rewriting the base item model again.
 */
class ItemContext {
    private boolean inBattle;
    private Enemy currentEnemy;
    private String previousRoomId;
    private String currentRoomId;
    private boolean stealthActive;
    private boolean battleEnded;
    private boolean escapedByItem;
    private boolean encounterBypassed;

    public boolean isInBattle() {
        return inBattle;
    }

    public void setInBattle(boolean inBattle) {
        this.inBattle = inBattle;
    }

    public Enemy getCurrentEnemy() {
        return currentEnemy;
    }

    public void setCurrentEnemy(Enemy currentEnemy) {
        this.currentEnemy = currentEnemy;
    }

    public String getPreviousRoomId() {
        return previousRoomId;
    }

    public void setPreviousRoomId(String previousRoomId) {
        this.previousRoomId = previousRoomId;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public boolean isStealthActive() {
        return stealthActive;
    }

    public void setStealthActive(boolean stealthActive) {
        this.stealthActive = stealthActive;
    }

    public boolean isBattleEnded() {
        return battleEnded;
    }

    public void setBattleEnded(boolean battleEnded) {
        this.battleEnded = battleEnded;
    }

    public boolean isEscapedByItem() {
        return escapedByItem;
    }

    public void setEscapedByItem(boolean escapedByItem) {
        this.escapedByItem = escapedByItem;
    }

    public boolean isEncounterBypassed() {
        return encounterBypassed;
    }

    public void setEncounterBypassed(boolean encounterBypassed) {
        this.encounterBypassed = encounterBypassed;
    }
}

class Item {
    private final String name;
    private final ItemType type;
    private final int power;

    public Item(String name, ItemType type, int power) {
        this.name = name;
        this.type = type;
        this.power = power;
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

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

enum ItemType {
    HEALING,
    WEAPON,
    SPECIAL
}

/**
 * Base subclass for richer item behavior.
 *
 * Only items that truly need extra logic use this layer. The rest can remain simple Item
 * objects, so the prototype stays lightweight.
 */
abstract class GameItem extends Item {
    private final String description;

    public GameItem(String name, ItemType type, int power, String description) {
        super(name, type, power);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canUseInBattle() {
        return false;
    }

    public boolean canUseOutsideBattle() {
        return false;
    }

    public boolean isPassive() {
        return false;
    }

    /**
     * Return true when the item should be removed after a successful use.
     */
    public boolean use(Player player, ItemContext context) {
        System.out.println(getName() + " has no active use.");
        return false;
    }

    public int getPassiveAttackBonus() {
        return 0;
    }

    public int getPassiveDefenseBonus() {
        return 0;
    }

    public boolean grantsParry() {
        return false;
    }
}

/**
 * Rich weapon item used for attack menu actions and inventory attack duplication.
 */
abstract class WeaponItem extends GameItem {
    private final int accuracy;
    private final String attackName;

    public WeaponItem(String name, int power, int accuracy, String attackName, String description) {
        super(name, ItemType.WEAPON, power, description);
        this.accuracy = accuracy;
        this.attackName = attackName;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public String getAttackName() {
        return attackName;
    }

    public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy == null) {
            return Math.max(1, getPower());
        }

        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }

    public boolean allowsDarkRoomEntry() {
        return false;
    }
}

/**
 * Passive gear grants stat bonuses simply by being carried.
 */
abstract class PassiveGearItem extends GameItem {
    private final int passiveAttackBonus;
    private final int passiveDefenseBonus;
    private final boolean parryEnabled;

    public PassiveGearItem(String name, String description,
                           int passiveAttackBonus, int passiveDefenseBonus, boolean parryEnabled) {
        super(name, ItemType.SPECIAL, 0, description);
        this.passiveAttackBonus = passiveAttackBonus;
        this.passiveDefenseBonus = passiveDefenseBonus;
        this.parryEnabled = parryEnabled;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public int getPassiveAttackBonus() {
        return passiveAttackBonus;
    }

    @Override
    public int getPassiveDefenseBonus() {
        return passiveDefenseBonus;
    }

    @Override
    public boolean grantsParry() {
        return parryEnabled;
    }
}

/* ---------------------
   ITEM SUBSECTION - POTIONS
   --------------------- */

class SmallPotionItem extends GameItem {
    private static final int HEAL_AMOUNT = 15;

    public SmallPotionItem() {
        super("Small Potion", ItemType.HEALING, HEAL_AMOUNT,
                "Heals 15 HP. Can be used in or out of battle.");
    }

    @Override
    public boolean canUseInBattle() {
        return true;
    }

    @Override
    public boolean canUseOutsideBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        int before = player.getHp();
        player.heal(HEAL_AMOUNT);
        int healed = player.getHp() - before;
        System.out.println("You use " + getName() + " and restore " + healed + " HP.");
        return true;
    }
}

class BigPotionItem extends GameItem {
    private static final int HEAL_AMOUNT = 40;

    public BigPotionItem() {
        super("Big Potion", ItemType.HEALING, HEAL_AMOUNT,
                "Heals 40 HP. Can be used in or out of battle.");
    }

    @Override
    public boolean canUseInBattle() {
        return true;
    }

    @Override
    public boolean canUseOutsideBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        int before = player.getHp();
        player.heal(HEAL_AMOUNT);
        int healed = player.getHp() - before;
        System.out.println("Used Big Potion. Restored " + healed + " HP.");
        return true;
    }
}

/* ---------------------
   ITEM SUBSECTION - SCROLLS
   --------------------- */

class ScrollOfEscapeItem extends GameItem {
    public ScrollOfEscapeItem() {
        super("Scroll of Escape", ItemType.SPECIAL, 0,
                "Use during battle to escape back to the previous room.");
    }

    @Override
    public boolean canUseInBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        if (context == null || !context.isInBattle()) {
            System.out.println("Scroll of Escape can only be used during battle.");
            return false;
        }

        if (context.getPreviousRoomId() == null) {
            System.out.println("No previous room exists.");
            return false;
        }

        player.setCurrentRoom(context.getPreviousRoomId());
        context.setCurrentRoomId(context.getPreviousRoomId());
        context.setCurrentEnemy(null);
        context.setInBattle(false);
        context.setBattleEnded(true);
        context.setEscapedByItem(true);

        System.out.println("You used Scroll of Escape and fled to the previous room.");
        return true;
    }
}

class ScrollOfFireballItem extends GameItem {
    private static final int FIREBALL_DAMAGE = 35;

    public ScrollOfFireballItem() {
        super("Scroll of Fireball", ItemType.SPECIAL, FIREBALL_DAMAGE,
                "Deals 35 damage to the current enemy during battle.");
    }

    @Override
    public boolean canUseInBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        if (context == null || !context.isInBattle() || context.getCurrentEnemy() == null) {
            System.out.println("Scroll of Fireball can only be used during battle.");
            return false;
        }

        context.getCurrentEnemy().takeDamage(FIREBALL_DAMAGE);
        System.out.println("Fireball hits " + context.getCurrentEnemy().getName()
                + " for " + FIREBALL_DAMAGE + " damage.");
        return true;
    }
}

class ScrollOfStealthItem extends GameItem {
    public ScrollOfStealthItem() {
        super("Scroll of Stealth", ItemType.SPECIAL, 0,
                "Use before battle to sneak past the next non-boss fight.");
    }

    @Override
    public boolean canUseOutsideBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        if (context == null) {
            System.out.println("Stealth cannot be applied without room context.");
            return false;
        }

        if (context.isInBattle()) {
            System.out.println("Scroll of Stealth must be used before battle starts.");
            return false;
        }

        if (context.getCurrentEnemy() != null && context.getCurrentEnemy().getType() == EnemyType.SNAKE) {
            System.out.println("Scroll of Stealth cannot bypass a boss encounter.");
            return false;
        }

        context.setStealthActive(true);
        context.setEncounterBypassed(true);
        System.out.println("Stealth activated. You may bypass the next non-boss encounter.");
        return true;
    }
}

/* ---------------------
   ITEM SUBSECTION - WEAPONS
   --------------------- */

class StickItem extends WeaponItem {
    public StickItem() {
        super("Stick", 2, 95, "Stick Attack",
                "Default weapon. Weak but reliable.");
    }

    @Override
    public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy != null && enemy.getType() == EnemyType.SLIME) {
            return 2;
        }

        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
}

class SwordItem extends WeaponItem {
    public SwordItem() {
        super("Sword", 8, 90, "Sword Slash",
                "Stronger weapon that can later appear as its own combat action.");
    }
}

class TorchItem extends WeaponItem {
    public TorchItem() {
        super("Torch", 4, 80, "Torch",
                "Weapon and utility item. Strong against slime and useful for dark rooms later.");
    }

    @Override
    public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy != null && enemy.getType() == EnemyType.SLIME) {
            return Math.max(10, getPower() + player.getAttack() - enemy.getDefense() + 6);
        }

        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }

    @Override
    public boolean allowsDarkRoomEntry() {
        return true;
    }
}

/* ---------------------
   ITEM SUBSECTION - PASSIVE GEAR
   --------------------- */

class ArmorItem extends PassiveGearItem {
    public ArmorItem() {
        super("Armor", "Passive +6 Defense while carried.", 0, 6, false);
    }
}

class ShieldItem extends PassiveGearItem {
    public ShieldItem() {
        super("Shield", "Passive +3 Defense while carried. Also supports future Parry work.", 0, 3, true);
    }
}

/* ---------------------
   ITEM SUBSECTION - KEYS AND SPECIALS
   --------------------- */

class KeyItem extends GameItem {
    private final String keyId;

    public KeyItem(String keyId, String displayName) {
        super(displayName, ItemType.SPECIAL, 0,
                "Used automatically when checking locked doors. Key ID: " + keyId);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}

class AncientRelicItem extends GameItem {
    public AncientRelicItem() {
        super("Ancient Relic", ItemType.SPECIAL, 0,
                "Placeholder reward item. No active function yet.");
    }
}

class BookOfSpellsItem extends GameItem {
    private final ArrayList<String> storedSpells = new ArrayList<>();

    public BookOfSpellsItem() {
        super("Book of Spells", ItemType.SPECIAL, 0,
                "Placeholder item. Planned for storing learned spells later.");
    }

    public void addSpell(String spellName) {
        storedSpells.add(spellName);
    }

    public ArrayList<String> getStoredSpells() {
        return storedSpells;
    }

    @Override
    public boolean canUseOutsideBattle() {
        return true;
    }

    @Override
    public boolean use(Player player, ItemContext context) {
        System.out.println("Book of Spells is not finished yet.");
        return false;
    }
}
