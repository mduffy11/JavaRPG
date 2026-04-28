import java.util.*;

/**
 * Dark Holds - Eight-Pillar Modular Build
 * ----------------------------------------------
 * The Core Grimoire. Orchestrates the game loop and managers.
 */
public class DarkHolds {
    public static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

class Game {
    private static final String FLOOR_ONE_IRON_KEY_ID = "locked_hall_iron_key";

    private Player player;
    private Map<String, Room> rooms;
    private final Random random;
    private final BattleManager battleManager;
    private final PuzzleManager puzzleManager;
    private boolean running;

    public Game() {
        this.random = new Random();
        this.battleManager = new BattleManager();
        this.puzzleManager = new PuzzleManager();
    }

    public void start() {
        introStory();
        setupGame();
        gameLoop();
    }

    private void introStory() {
        System.out.println("========================================");
        System.out.println("           DARK HOLDS");
        System.out.println("     EIGHT-PILLAR ARCHITECTURE");
        System.out.println("========================================");
        System.out.println("You were once an ordinary villager with no great battle experience.");
        System.out.println("While traveling near ancient ruins, you stepped on a hidden teleportation trap.");
        System.out.println("The world twisted around you, and now you wake in a buried labyrinth.");
        System.out.println("Somewhere below, a giant serpent coils around stolen treasure.");
        System.out.println("If you want to live, you must explore, find a key, and push deeper.");
        System.out.println();
        System.out.print("Enter your hero's name: ");
        String name = DarkHolds.input.nextLine().trim();

        if (name.isEmpty()) {
            name = "Hero";
        }

        player = new Player(name);
    }

    private void setupGame() {
        rooms = new LinkedHashMap<>();
        
        List<Room> f1Rooms = WorldBuilder.createFloorOne(random);
        List<Room> f2Rooms = WorldBuilder.createFloorTwo(random);
        List<Room> f3Rooms = WorldBuilder.createFloorThree(random);
        
        Room f1Stairs = f1Rooms.get(f1Rooms.size() - 1);
        f1Stairs.addNeighbor(f2Rooms.get(0).getId());
        
        Room f2Stairs = f2Rooms.get(f2Rooms.size() - 1);
        f2Stairs.addNeighbor(f3Rooms.get(0).getId());

        for (Room r : f1Rooms) { rooms.put(r.getId(), r); }
        for (Room r : f2Rooms) { rooms.put(r.getId(), r); }
        for (Room r : f3Rooms) { rooms.put(r.getId(), r); }

        distributeGuaranteedLoot();
        player.setCurrentRoom("f1safe");
        player.setPreviousRoom("f1safe");
        running = true;
    }

    private void distributeGuaranteedLoot() {
        List<Room> keyEligibleRooms = new ArrayList<>();
        List<Room> torchEligibleRooms = new ArrayList<>();
        List<Room> potionEligibleRooms = new ArrayList<>();

        for (Room room : rooms.values()) {
            if (room.isKeyEligible() && room.getId().startsWith("f1")) { keyEligibleRooms.add(room); }
            if (room.isPreLock() && !room.getId().equals("f1safe")) { torchEligibleRooms.add(room); }
            if (room.isPreLock()) { potionEligibleRooms.add(room); }
        }

        if (!keyEligibleRooms.isEmpty()) {
            Room keyRoom = keyEligibleRooms.get(random.nextInt(keyEligibleRooms.size()));
            keyRoom.setHasKeyReward(true);
        }

        if (!torchEligibleRooms.isEmpty()) {
            Room torchRoom = torchEligibleRooms.get(random.nextInt(torchEligibleRooms.size()));
            torchRoom.addRewardItem(Items.torch());
        }

        Collections.shuffle(potionEligibleRooms, random);
        int potionCount = Math.min(3, potionEligibleRooms.size());
        for (int i = 0; i < potionCount; i++) {
            potionEligibleRooms.get(i).addRewardItem(Items.smallPotion());
        }

        Room hoard = rooms.get("f1snakeBoss");
        if (hoard != null) {
            hoard.addRewardItem(Items.ancientRelic());
            hoard.addGold(randomBetween(25, 50));
        }
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
            
            if (current.hasKeyReward() && !current.isRewardsCollected()) {
                System.out.println("\n>> You spot an Iron Key resting somewhere in this room! <<");
            }
            
            showPlayerStatus();

            if (current.isPuzzleRoom() && !current.isPuzzleSolved() && !current.hasEnemy()) {
                String id = current.getId();
                if (id.equals("f1p_weighingScales")) puzzleManager.handleScalesPuzzle(current, player);
                else if (id.equals("f1p_brokenFork")) puzzleManager.handleBrokenForkPuzzle(current, player);
                else if (id.equals("f1p_shrine")) puzzleManager.handleShrinePuzzle(current, player);
                else if (id.equals("f2p_totem")) puzzleManager.handleTotemPuzzle(current, player);
                else if (id.equals("f2p_weepingstatue")) puzzleManager.handleWeepingStatue(current, player, random);
                else if (id.equals("f2p_campsite")) puzzleManager.handleCampsite(current, player);
                else if (id.equals("f2p_wishwell")) puzzleManager.handleWishWell(current, player, random);
                else if (id.equals("f2p_obsidian_obelisk")) puzzleManager.handleObsidianObelisk(current, player);
                else if (id.equals("f3p_silentlibrary")) puzzleManager.handleSilentLibrary(current, player, random);
                else if (id.equals("f3p_colorpillars")) puzzleManager.handleColorPillars(current, player);
                else if (id.equals("f3p_hourglass")) puzzleManager.handleHourglassPuzzle(current, player);
                else if (id.equals("f3p_luckschance")) puzzleManager.handleLucksChance(current, player, random);
                else if (id.equals("f3p_lakeoftruth")) puzzleManager.handleLakeOfTruth(current, player, random);
                else if (id.equals("f3p_starmap_room")) puzzleManager.handleStarMapRoom(current, player);
                else if (id.equals("f3p_crimson_altar")) puzzleManager.handleCrimsonAltar(current, player);
                else if (id.equals("f3p_abandoned_lab")) puzzleManager.handleAbandonedLab(current, player, random);

                if (!player.isAlive()) continue;
            }

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
                if (result == BattleResult.ESCAPED) continue;
            }

            revealRoomRewards(current);

            if (current.getId().equals("f3_end")) {
                victory();
                return;
            }

            handleRoomChoices(current);
        }
    }

    private void showPlayerStatus() {
        System.out.println("\nPlayer: " + player.getName());
        System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp());
        System.out.println("Attack: " + player.getAttack());
        System.out.println("Defense: " + player.getDefense());
        System.out.println("Level: " + player.getLevel() + "   XP: " + player.getXp());
        System.out.println("Gold: " + player.getGold());
        System.out.println("Has Key: " + player.hasAnyKey());
    }

    private void revealRoomRewards(Room room) {
        if (room.isRewardsCollected() || (room.hasEnemy() && !room.getEnemy().isDefeated()) || (room.isPuzzleRoom() && !room.isPuzzleSolved())) return;

        boolean foundSomething = false;
        if (room.getGoldReward() > 0) {
            System.out.println("\nYou find " + room.getGoldReward() + " gold.");
            player.addGold(room.getGoldReward());
            foundSomething = true;
        }

        if (room.hasKeyReward()) {
            System.out.println("\nYou discover an Iron Key hidden here.");
            player.addKey(Items.floorOneIronKey());
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

        if (foundSomething) room.setRewardsCollected(true);
    }

    private boolean promptBeforeBattle(Room room) {
        while (true) {
            System.out.println("\nA " + room.getEnemy().getName() + " is here.");
            System.out.println("1. Fight\n2. Back away\nChoose: ");
            String answer = DarkHolds.input.nextLine().trim();

            if (answer.equals("1")) return true;
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

                if (current.getId().equals("f1safe") && nextRoom.getId().equals("f1lockedHall") && !player.hasKey(FLOOR_ONE_IRON_KEY_ID)) {
                    text = "Try the locked hallway (locked)";
                }
                System.out.println((i + 1) + ". " + text);
            }

            System.out.println("I. Inventory\nChoose an action: ");
            String answer = DarkHolds.input.nextLine().trim();

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
                if (current.getId().equals("f1safe") && nextRoom.getId().equals("f1lockedHall") && !player.hasKey(FLOOR_ONE_IRON_KEY_ID)) {
                    System.out.println("The heavy hallway gate is locked. You need a key.");
                    continue;
                }

                if (!nextRoom.getNeighborIds().contains(current.getId())) player.setPreviousRoom(nextRoom.getId());
                else player.setPreviousRoom(player.getCurrentRoom());

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
            if (current.getId().equals("f1snakeBoss") && (neighborId.equals("f1_snakeHoard") || neighborId.equals("f1_stairs") || neighborId.startsWith("f2")) && current.hasEnemy() && !current.getEnemy().isDefeated()) continue;
            if (current.getId().equals("f2_golem_rune") && (neighborId.equals("f2_stairs") || neighborId.startsWith("f3")) && current.hasEnemy() && !current.getEnemy().isDefeated()) continue;
            if (current.getId().equals("f3_necro_altar") && (neighborId.equals("f3_end")) && current.hasEnemy() && !current.getEnemy().isDefeated()) continue;
            visible.add(neighborId);
        }
        return visible;
    }

    private void openInventoryMenu() {
        while (true) {
            System.out.println("\nINVENTORY");
            if (player.getInventory().isEmpty()) {
                System.out.println("You are carrying nothing.\n0. Leave inventory\nChoose: ");
                if (DarkHolds.input.nextLine().trim().equals("0")) return;
                continue;
            }

            for (int i = 0; i < player.getInventory().size(); i++) {
                System.out.println((i + 1) + ". " + player.getInventory().get(i));
            }
            System.out.print("0. Leave inventory\nChoose an item to use, or 0 to leave: ");
            String answer = DarkHolds.input.nextLine().trim();
            if (answer.equals("0")) return;

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

                    if (gameItem.use(player, context)) player.removeItem(index);
                    continue;
                }
                System.out.println(item.getName() + " cannot be used outside battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void victory() {
        System.out.println("\n========================================\nFLOOR 3 CLEARED\n========================================");
        System.out.println("You have vanquished the Necromancer and cleared the third floor.");
        System.out.println("For this prototype build, your descent ends here.");
        running = false;
    }

    private void gameOver(String message) {
        System.out.println("\n========================================\nGAME OVER\n========================================");
        System.out.println(message);
        running = false;
    }

    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}