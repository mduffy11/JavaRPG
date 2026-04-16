
import java.util.*;

/**
 * Dark Holds - Floor 1 Prototype
 * --------------------------------
 * This prototype rebuilds the first floor around a semi-random room deck,
 * classic alternating turns, and the Giant Snake as the first floor boss.
 *
 * Built for team discussion and fast playtesting.
 */
public class DarkHoldsFloor1Prototype {

    public static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
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

    private void setupGame() {
        rooms = WorldBuilder.createFloorOne(random);
        distributeGuaranteedLoot();
        player.setCurrentRoom("safe");
        player.setPreviousRoom("safe");
        running = true;
    }

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
        torchRoom.addRewardItem(new Item("Torch", ItemType.WEAPON, 0));

        Collections.shuffle(potionEligibleRooms, random);
        int potionCount = Math.min(3, potionEligibleRooms.size());

        for (int i = 0; i < potionCount; i++) {
            potionEligibleRooms.get(i).addRewardItem(new Item("Small Potion", ItemType.HEALING, 15));
        }

        Room hoard = rooms.get("snakeHoard");
        hoard.addRewardItem(new Item("Ancient Relic", ItemType.SPECIAL, 0));
        hoard.addGold(randomBetween(25, 50));
    }

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
            player.setHasKey(true);
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

    private void handleRoomChoices(Room current) {
        while (true) {
            System.out.println();

            List<String> neighborIds = getVisibleNeighborIds(current);
            for (int i = 0; i < neighborIds.size(); i++) {
                Room nextRoom = rooms.get(neighborIds.get(i));
                String text = "Go to " + nextRoom.getName();

                if (current.getId().equals("safe") && nextRoom.getId().equals("lockedHall") && !player.hasKey()) {
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

                if (current.getId().equals("safe") && nextRoom.getId().equals("lockedHall") && !player.hasKey()) {
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

    private List<String> getVisibleNeighborIds(Room current) {
        List<String> visible = new ArrayList<>();

        for (String neighborId : current.getNeighborIds()) {
            if (current.getId().equals("snakeBoss") && neighborId.equals("snakeHoard")
                    && current.hasEnemy() && !current.getEnemy().isDefeated()) {
                continue;
            }

            visible.add(neighborId);
        }

        return visible;
    }

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

                if (item.getType() == ItemType.HEALING) {
                    player.heal(item.getPower());
                    System.out.println("You use " + item.getName() + " and restore " + item.getPower() + " HP.");
                    player.removeItem(index);
                    continue;
                }

                System.out.println(item.getName() + " cannot be used outside battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }

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

    private void gameOver(String message) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("GAME OVER");
        System.out.println("========================================");
        System.out.println(message);
        running = false;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

class WorldBuilder {

    public static Map<String, Room> createFloorOne(Random random) {
        Map<String, Room> rooms = new LinkedHashMap<>();

        Room safeRoom = new Room("safe", "Safe Room",
                "You wake in a cold stone chamber with only a rough stick beside you. " +
                "A heavy locked hallway gate stands to one side, and the rest of the floor branches into darkness.",
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

    private static void connect(Room a, Room b) {
        a.addNeighbor(b.getId());
        b.addNeighbor(a.getId());
    }

    private static int randomBetween(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static int randomGold(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min + 1);
    }
}

class BattleManager {
    private static final int PLAYER_ATTACK_ACCURACY = 95;
    private static final int PLAYER_TORCH_ACCURACY = 80;
    private static final int RUN_SUCCESS_RATE = 80;
    private static final int CONSTRICT_DAMAGE = 2;

    private final Random random;

    public BattleManager() {
        this.random = new Random();
    }

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

    private PlayerAction promptPlayerAction(Player player) {
        while (true) {
            Map<String, PlayerAction> options = new LinkedHashMap<>();
            int optionNumber = 1;

            System.out.println(optionNumber + ". Stick Attack");
            options.put(String.valueOf(optionNumber++), PlayerAction.ATTACK);

            if (player.hasItem("Torch")) {
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

    private boolean playerBasicAttack(Player player, Enemy enemy, boolean sandInEyes) {
        int accuracy = PLAYER_ATTACK_ACCURACY;

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

        if (enemy.getType() == EnemyType.SLIME) {
            damage = 2;
        } else {
            damage = calculatePlayerDamage(player.getAttack(), 2, enemy.getDefense());
        }

        enemy.takeDamage(damage);
        System.out.println("You use Stick Attack and deal " + damage + " damage.");
        return false;
    }

    private boolean playerTorchAttack(Player player, Enemy enemy, boolean sandInEyes) {
        int accuracy = PLAYER_TORCH_ACCURACY;

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

        if (enemy.getType() == EnemyType.SLIME) {
            damage = Math.max(10, calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense()) + 6);
        } else {
            damage = calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense());
        }

        enemy.takeDamage(damage);
        System.out.println("You lash out with the Torch and deal " + damage + " damage.");
        return false;
    }

    private BattleTurnResult openActionMenu() {
        while (true) {
            System.out.println("Action:");
            System.out.println("1. Dodge");
            System.out.println("0. Return");
            System.out.print("Choose action: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("1")) {
                System.out.println("You prepare to dodge the next attack.");
                return new BattleTurnResult(true, false, false, true);
            }

            if (answer.equals("0")) {
                return new BattleTurnResult(false, false, false, false);
            }

            System.out.println("Invalid choice.");
        }
    }

    private BattleTurnResult useItemInBattle(Player player, Enemy enemy, boolean sandInEyes) {
        while (true) {
            System.out.println("Inventory:");

            if (player.getInventory().isEmpty()) {
                System.out.println("You have no items.");
                return new BattleTurnResult(false, sandInEyes, false, false);
            }

            for (int i = 0; i < player.getInventory().size(); i++) {
                System.out.println((i + 1) + ". " + player.getInventory().get(i));
            }

            System.out.println("0. Cancel");
            System.out.print("Choose item number: ");
            String answer = DarkHoldsFloor1Prototype.input.nextLine().trim();

            if (answer.equals("0")) {
                return new BattleTurnResult(false, sandInEyes, false, false);
            }

            try {
                int index = Integer.parseInt(answer) - 1;

                if (index < 0 || index >= player.getInventory().size()) {
                    System.out.println("Invalid item choice.");
                    continue;
                }

                Item item = player.getInventory().get(index);

                if (item.getType() == ItemType.HEALING) {
                    player.heal(item.getPower());
                    System.out.println("You use " + item.getName() + " and restore " + item.getPower() + " HP.");
                    player.removeItem(index);
                    return new BattleTurnResult(true, sandInEyes, false, false);
                }

                if (item.getName().equalsIgnoreCase("Stick")) {
                    boolean updatedSandInEyes = playerBasicAttack(player, enemy, sandInEyes);
                    return new BattleTurnResult(true, updatedSandInEyes, true, false);
                }

                if (item.getName().equalsIgnoreCase("Torch")) {
                    boolean updatedSandInEyes = playerTorchAttack(player, enemy, sandInEyes);
                    return new BattleTurnResult(true, updatedSandInEyes, true, false);
                }

                System.out.println(item.getName() + " cannot be used in battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid item choice.");
            }
        }
    }

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

    private int calculatePlayerDamage(int playerAttack, int movePower, int enemyDefense) {
        return Math.max(1, movePower + playerAttack - enemyDefense);
    }

    private int calculateEnemyDamage(Enemy enemy, EnemyMove move, Player player) {
        if (move.getEffect() == MoveEffect.POCKET_SAND) {
            return 1;
        }

        return Math.max(1, move.getPower() + enemy.getAttack() - player.getDefense());
    }
}

class Player {
    private static final int[] XP_THRESHOLDS = {0, 18, 42, 75};
    private static final int[] HP_BY_LEVEL = {50, 53, 57, 61};
    private static final int[] ATTACK_BY_LEVEL = {8, 9, 10, 11};
    private static final int[] DEFENSE_BY_LEVEL = {2, 2, 3, 3};

    private final String name;
    private int hp;
    private int maxHp;
    private int attack;
    private int defense;
    private int level;
    private int xp;
    private int gold;
    private boolean hasKey;
    private String currentRoom;
    private String previousRoom;
    private final ArrayList<Item> inventory;

    public Player(String name) {
        this.name = name;
        this.level = 1;
        this.xp = 0;
        this.gold = 0;
        this.hasKey = false;
        this.maxHp = HP_BY_LEVEL[0];
        this.hp = maxHp;
        this.attack = ATTACK_BY_LEVEL[0];
        this.defense = DEFENSE_BY_LEVEL[0];
        this.inventory = new ArrayList<>();
        this.inventory.add(new Item("Stick", ItemType.WEAPON, 0));
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
        while (level < 4 && xp >= XP_THRESHOLDS[level]) {
            level++;
            maxHp = HP_BY_LEVEL[level - 1];
            attack = ATTACK_BY_LEVEL[level - 1];
            defense = DEFENSE_BY_LEVEL[level - 1];
            hp = maxHp;
            System.out.println("You reached level " + level + "!");
            System.out.println("Your strength rises.");
        }
    }

    public void addGold(int amount) {
        gold += amount;
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

    public String getPreviousRoom() {
        return previousRoom;
    }

    public void setPreviousRoom(String previousRoom) {
        this.previousRoom = previousRoom;
    }

    public ArrayList<Item> getInventory() {
        return inventory;
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

    public String getDescription() {
        return description;
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

        for (EnemyMove move : moves) {
            this.moves.add(move);
        }
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

    public BattleTurnResult(boolean turnConsumed, boolean sandInEyes, boolean consumeSandBlindness, boolean dodgeActive) {
        this.turnConsumed = turnConsumed;
        this.sandInEyes = sandInEyes;
        this.consumeSandBlindness = consumeSandBlindness;
        this.dodgeActive = dodgeActive;
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
}
