import java.util.*;

/**
 * Dark Holds - The Grand Design
 * ----------------------------------------------
 */
public class DarkHolds {

    public static final Scanner input = new Scanner(System.in);

    // PURPOSE: The primary summoning point of the application. Bootstraps the Game object.
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}

// [TAG: SECTION_1 ] - Core Game Loop & Flow
/* =========================================================================================
   This section houses the central Game Engine. It manages the Player and World composition,
   routing all player inputs to the appropriate managers.
   ========================================================================================= */

/**
 * The Game class is the master controller (GameEngine).
 * It holds the core while-loop, managing the flow of time and routing state changes.
 */
class Game {
    private static final String FLOOR_ONE_IRON_KEY_ID = "locked_hall_iron_key";

    private Player player;
    private Map<String, Room> rooms;
    private final Random random;
    private final BattleManager battleManager;
    private final PuzzleManager puzzleManager;
    private final MerchantManager merchantManager;
    private boolean running;

    // PURPOSE: Initializes the subordinate managers that handle combat, puzzles, and trade.
    public Game() {
        this.random = new Random();
        this.battleManager = new BattleManager();
        this.puzzleManager = new PuzzleManager();
        this.merchantManager = new MerchantManager();
    }

    // PURPOSE: Begins the game's execution sequence.
    public void start() {
        introStory();
        setupGame();
        gameLoop();
    }

    // PURPOSE: Collects the player's True Name and establishes the narrative premise.
    private void introStory() {
        System.out.println("========================================");
        System.out.println("           DARK HOLDS");
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

    // PURPOSE: Bootstraps the crypt. Assembles rooms from the WorldBuilder and populates loot.
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
        
        List<Room> possibleCatRooms = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (!r.isMerchantRoom() && !r.getId().equals("f3_necro_altar")) {
                possibleCatRooms.add(r);
            }
        }
        if (!possibleCatRooms.isEmpty()) {
            Room catRoom = possibleCatRooms.get(random.nextInt(possibleCatRooms.size()));
            catRoom.setHasCat(true);
        }

        player.setCurrentRoom("f1safe");
        player.setPreviousRoom("f1safe");
        running = true;
    }

    // PURPOSE: Ensures vital items (keys, torches) are seeded into eligible rooms before play begins.
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
        }
    }

    // PURPOSE: The eternal heartbeat of the application. Displays state, evaluates encounters, and waits for inputs.
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
            
            if (current.getId().equals("f3_dragon_antechamber")) {
                if (player.hasItem("Ancient Relic")) {
                    System.out.println("A heavy stone door sits in the shadows. It is sealed tight, but in its center is an odd, jagged indentation that perfectly matches the Ancient Relic in your pack.");
                } else {
                    System.out.println("A heavy stone door sits in the shadows. It is sealed tight by ancient magic. There is a strange indentation in the center, but you have nothing that fits it.");
                }
            } else {
                System.out.println(current.getDisplayDescription());
            }

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

            if (current.hasCat() && !current.isCatResolved()) {
                System.out.println();
                ImageGallery.reveal("Cat");
                if (current.hasEnemy() && current.getEnemy().isDefeated()) {
                    System.out.println("With the room pacified, a small, fluffy dungeon cat emerges from hiding.");
                } else {
                    System.out.println("You spot a small, fluffy dungeon cat sitting in the corner.");
                }
                
                while (true) {
                    System.out.print("Do you want to pick it up? (Y/N): ");
                    String catAns = DarkHolds.input.nextLine().trim().toUpperCase();
                    if (catAns.equals("Y")) {
                        System.out.println("The cat seems happy to have found you.");
                        player.addItem(Items.cat());
                        current.setCatResolved(true);
                        break;
                    } else if (catAns.equals("N")) {
                        System.out.println("The cat is frightened and runs off into the darkness.");
                        current.setCatResolved(true);
                        break;
                    } else {
                        System.out.println("Invalid choice.");
                    }
                }
            }

            revealRoomRewards(current);

            // PROFESSOR REQUIREMENT: Win Condition check routed to victory()
            if (current.getId().equals("f3_end")) {
                victory();
                return;
            }

            handleRoomChoices(current);
        }
    }

    // PURPOSE: Prints current health, stats, level, and gold to the console.
    private void showPlayerStatus() {
        System.out.println("\nPlayer: " + player.getName());
        System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp());
        
        int atkBonus = Items.getPassiveAttackBonus(player);
        int baseAtk = player.getAttack() - atkBonus;
        if (atkBonus > 0) System.out.println("Attack: " + baseAtk + " (+" + atkBonus + ")");
        else System.out.println("Attack: " + baseAtk);
        
        int defBonus = Items.getPassiveDefenseBonus(player);
        int baseDef = player.getDefense() - defBonus;
        if (defBonus > 0) System.out.println("Defense: " + baseDef + " (+" + defBonus + ")");
        else System.out.println("Defense: " + baseDef);
        
        System.out.println("Level: " + player.getLevel() + "   XP: " + player.getXp());
        System.out.println("Gold: " + player.getGold());
    }

    // PURPOSE: Extracts items and gold from the room and adds them to the player's inventory.
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
            ImageGallery.reveal("Iron Key");
            player.addItem(Items.floorOneIronKey()); 
            foundSomething = true;
        }

        if (!room.getRewardItems().isEmpty()) {
            System.out.println();
            for (Item item : room.getRewardItems()) {
                System.out.println("You found: " + item.getName());
                ImageGallery.reveal(item.getName());
                player.addItem(item);
            }
            foundSomething = true;
        }

        if (foundSomething) room.setRewardsCollected(true);
    }

    // PURPOSE: Pauses flow to ask the player if they wish to engage the room's enemy or flee.
    private boolean promptBeforeBattle(Room room) {
        while (true) {
            System.out.println();
            ImageGallery.reveal(room.getEnemy().getName()); 
            System.out.println("A " + room.getEnemy().getName() + " is here.");
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

    // PURPOSE: Manages movement between rooms.
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
                
                if (current.getId().equals("f3_dragon_antechamber") && nextRoom.getId().equals("f3_dragon_lair")) {
                    text = player.hasItem("Ancient Relic") ? "Open the sealed stone door with the Relic" : "Try the sealed stone door (Locked)";
                }

                System.out.println((i + 1) + ". " + text);
            }

            System.out.println("I. Inventory");
            if (current.isMerchantRoom()) {
                System.out.println("M. Trade with the Goblin Merchant");
            }
            System.out.print("Choose an action: ");
            String answer = DarkHolds.input.nextLine().trim();

            if (answer.equalsIgnoreCase("I")) {
                openInventoryMenu();
                continue;
            }
            
            if (answer.equalsIgnoreCase("M") && current.isMerchantRoom()) {
                merchantManager.handleMerchant(player, current);
                continue;
            }

            // PROFESSOR REQUIREMENT: Exception Handling preventing crash on bad input.
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
                
                if (current.getId().equals("f3_dragon_antechamber") && nextRoom.getId().equals("f3_dragon_lair")) {
                    if (!player.hasItem("Ancient Relic")) {
                        System.out.println("The heavy stone door refuses to budge. It is magically sealed.");
                        continue;
                    } else {
                        System.out.println("\n*** The heavy stone door grinds open, consuming the Ancient Relic in the process! ***\n");
                        for (int j = 0; j < player.getInventory().size(); j++) {
                            if (player.getInventory().get(j).getName().equals("Ancient Relic")) {
                                player.removeItem(j);
                                break;
                            }
                        }
                    }
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

    // PURPOSE: Ensures locked boss rooms block paths until cleared.
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

    // PURPOSE: Displays held items and allows use of implements adhering to the Usable interface outside of combat.
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

            // PROFESSOR REQUIREMENT: Exception handling for incorrect menu inputs.
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

    // PROFESSOR REQUIREMENT: Defined Win Condition.
    // PURPOSE: Resolves the game state successfully and displays one of multiple possible endings based on items held.
    private void victory() {
        System.out.println("\n========================================");
        System.out.println("FLOOR 3 CLEARED");
        System.out.println("========================================");
        
        System.out.print("The Necromancer is defeated. There is an option to Grab the staff. Do you grab it? (Y/N): ");
        String staffAns = DarkHolds.input.nextLine().trim().toUpperCase();
        System.out.println();
        
        if (staffAns.equals("Y")) {
            System.out.println("As the Necromancer falls, you pick up their staff. A cold, dark power surges through your veins. You do not leave the crypt. Instead, you sit upon the profane altar, gazing into the shadows, chanting, patiently awaiting the next foolish adventurer.");
            System.out.println("\nThe Corrupted Successor");
            System.out.println("Ending 2 of 4");
        } else if (player.hasItem("Dragon Egg")) {
            System.out.println("The egg hatches in your arms, and the baby dragon imprints on you. You leave the dungeon not as a wealthy adventurer, but as a very tired, highly protective single parent.");
            System.out.println("\nThe Parent");
            System.out.println("Ending 4 of 4");
        } else if (player.hasItem("Cat")) {
            System.out.println("You use your dungeon loot to buy a small, quiet cottage at the edge of the woods. You hang up your sword forever, spending your days napping by a warm hearth with your purring dungeon cat.");
            System.out.println("\nThe Cozy Retirement");
            System.out.println("Ending 3 of 4");
        } else {
            System.out.println("You have vanquished the Necromancer and cleared the third floor. The heavy crypt doors groan open, revealing the blinding light of the surface world. You step out, lungs filling with fresh air, leaving the dark holds behind you. You return to your village, sell your weapons, and buy a modest plot of land. You spend the rest of your days quietly growing produce, perfectly content to never see another goblin again.");
            System.out.println("\nThe Farmer");
            System.out.println("Ending 1 of 4");
        }
        
        running = false;
    }

    // PROFESSOR REQUIREMENT: Defined Lose Condition.
    // PURPOSE: Resolves the game state negatively upon player death.
    private void gameOver(String message) {
        System.out.println("\n========================================\nGAME OVER\n========================================");
        System.out.println(message);
        running = false;
    }

    // PURPOSE: Utility method to fetch random integers.
    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

// [TAG: SECTION_2 ] - Master of Traps (PuzzleManager)
/* =========================================================================================
   This class encapsulates all custom logic for puzzle rooms, separating complex 
   if/else riddle chains from the main game loop to maintain clean architecture.
   ========================================================================================= */

/**
 * Handles all logic for non-combat interactive events (puzzles, shrines, traps).
 */
class PuzzleManager {
    private final Random random = new Random();

    // PURPOSE: Shuffles options so players cannot memorize numeric answers across playthroughs.
    private String getShuffledChoice(String... options) {
        List<String> list = Arrays.asList(options);
        Collections.shuffle(list, random);
        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ". " + list.get(i));
        }
        System.out.print("Choose: ");
        String input = DarkHolds.input.nextLine().trim();
        try {
            int choice = Integer.parseInt(input);
            if (choice > 0 && choice <= list.size()) {
                return list.get(choice - 1);
            }
        } catch (NumberFormatException e) {
        }
        return "";
    }

    // PURPOSE: Floor 1 Puzzle - Scale balancing logic.
    public void handleScalesPuzzle(Room room, Player player) {
        System.out.println("\nThe scale waits for an offering to balance the path.");
        String answer = getShuffledChoice("Offer 20 gold", "Offer an item from your inventory", "Ignore it and try to walk past");

        if (answer.equals("Offer 20 gold")) {
            if (player.getGold() >= 20) {
                player.addGold(-20);
                System.out.println("You place 20 gold on the scale. It balances perfectly! The path is safe.");
                room.setPuzzleSolved(true);
            } else {
                System.out.println("You don't have enough gold! The scales tip violently, triggering a trap door!");
                room.setEnemy(Bestiary.goblin());
                room.setPuzzleSolved(true); 
            }
        } else if (answer.equals("Offer an item from your inventory")) {
            if (player.getInventory().isEmpty()) {
                System.out.println("You have no items to offer! The scales tip violently, triggering a trap door!");
                room.setEnemy(Bestiary.goblin());
                room.setPuzzleSolved(true); 
            } else {
                boolean itemAccepted = false;
                while (!itemAccepted) {
                    System.out.println("\nChoose an item to offer:");
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        System.out.println((i + 1) + ". " + player.getInventory().get(i).getName());
                    }
                    System.out.print("0. Cancel and step back\nChoose item number: ");

                    try {
                        int choice = Integer.parseInt(DarkHolds.input.nextLine().trim());
                        if (choice == 0) {
                            System.out.println("You reconsider and step back from the scale.");
                            break; 
                        }
                        
                        int index = choice - 1;
                        if (index >= 0 && index < player.getInventory().size()) {
                            Item offeredItem = player.getInventory().get(index);
                            if (offeredItem.getName().equalsIgnoreCase("Stick")) {
                                System.out.println("You place the Stick on the scale. The stick is too light!\nYou must choose a different item.");
                            } else {
                                System.out.println("You place the " + offeredItem.getName() + " on the scale.");
                                player.removeItem(index);
                                System.out.println("It balances perfectly! The path is safe.");
                                room.setPuzzleSolved(true); 
                                itemAccepted = true; 
                            }
                        } else {
                            System.out.println("Invalid choice! You hesitate too long and the mechanism snaps, triggering an ambush!");
                            room.setEnemy(Bestiary.goblin());
                            room.setPuzzleSolved(true);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid choice! You hesitate too long and the mechanism snaps, triggering an ambush!");
                        room.setEnemy(Bestiary.goblin());
                        room.setPuzzleSolved(true);
                        break;
                    }
                }
            }
        } else {
            System.out.println("You try to sneak past, but the floor plates shift beneath your feet. An ambush is sprung!");
            room.setEnemy(Bestiary.goblin());
            room.setPuzzleSolved(true);
        }
    }

    // PURPOSE: Floor 1 Puzzle - Masonry brick logic.
    public void handleBrokenForkPuzzle(Room room, Player player) {
        System.out.println("\nThe cracked masonry features three stone bricks that can be pushed in.");
        String answer = getShuffledChoice("Push the left brick", "Push the center brick", "Push the right brick");
        if (answer.equals("Push the center brick")) {
            System.out.println("A heavy stone slides away, solving the puzzle!");
            room.setPuzzleSolved(true);
        } else {
            int damage = random.nextInt(3) + 3; // 3 to 5
            System.out.println("A hidden dart shoots from the wall!");
            player.takeDamage(damage);
            System.out.println("You take " + damage + " damage and step back.");
        }
    }

    // PURPOSE: Floor 1 Puzzle - Text riddle.
    public void handleShrinePuzzle(Room room, Player player) {
        System.out.println("\nThe riddle on the altar reads: 'I speak without a mouth and hear without ears. I have nobody, but I come alive with wind. What am I?'");
        String answer = getShuffledChoice("A ghost", "An echo", "A snake");
        if (answer.equals("An echo")) {
            System.out.println("The shrine hums in approval. The puzzle is solved!");
            room.setPuzzleSolved(true);
        } else {
            int damage = random.nextInt(2) + 4; // 4 to 5
            System.out.println("A hidden mechanism strikes you!");
            player.takeDamage(damage);
            System.out.println("You take " + damage + " damage and the riddle resets.");
        }
    }
    
    // PURPOSE: Floor 2 Puzzle - Portcullis lock mechanism.
    public void handleTotemPuzzle(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Feed the Wolf", "Feed the Bat", "Feed the Snake");
        if (answer.equals("Feed the Bat")) {
            System.out.println("The portcullis grinds open, solving the puzzle.");
            room.setPuzzleSolved(true);
        } else {
            int damage = random.nextInt(3) + 6; // 6 to 8
            System.out.println("A hidden spring trap shoots a rusty spear!");
            player.takeDamage(damage);
            System.out.println("You take " + damage + " damage.");
        }
    }
    
    // PURPOSE: Floor 2 Puzzle - Random stat buff or damage.
    public void handleWeepingStatue(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Drink the water", "Ignore it");
        if (answer.equals("Drink the water")) {
            if (rand.nextBoolean()) {
                player.increaseBaseAttack(2);
                System.out.println("The oily water burns your throat, but you feel a surge of power! Attack permanently increased by 2.");
            } else {
                int damage = rand.nextInt(5) + 1; // 1 to 5
                player.takeDamage(damage);
                System.out.println("The oily water is foul! You take " + damage + " damage.");
            }
            room.setPuzzleSolved(true);
        } else {
            System.out.println("You wisely decide to ignore the strange water.");
            room.setPuzzleSolved(true);
        }
    }

    // PURPOSE: Floor 2 Event - Free heal.
    public void handleCampsite(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Rest by the cold firepit", "Keep moving");
        if (answer.equals("Rest by the cold firepit")) {
            System.out.println("You lay out the dusty bedroll and rest for a while, regaining 20 HP.");
            player.heal(20);
        } else {
            System.out.println("You decide not to rest.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 2 Event - Gamble gold for item.
    public void handleWishWell(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Toss in 5 gold", "Leave it alone");
        if (answer.equals("Toss in 5 gold")) {
            if (player.getGold() >= 5) {
                player.addGold(-5);
                System.out.println("You toss 5 gold into the dark well. It vanishes into the depths...");
                if (rand.nextBoolean()) {
                    System.out.println("A small vial inexplicably floats to the surface! You found a Small Potion.");
                    player.addItem(Items.smallPotion());
                } else {
                    System.out.println("You wait, but nothing happens. The gold is gone.");
                }
                room.setPuzzleSolved(true);
            } else {
                System.out.println("You don't have enough gold!");
            }
        } else {
            System.out.println("You step away from the edge of the well.");
            room.setPuzzleSolved(true);
        }
    }

    // PURPOSE: Floor 2 Event - Free item.
    public void handleObsidianObelisk(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Touch the humming obelisk", "Leave it alone");
        if (answer.equals("Touch the humming obelisk")) {
            System.out.println("As you lay your hand on the smooth, black stone, a vial materializes at its base!\nYou received a Small Potion.");
            player.addItem(Items.smallPotion());
        } else {
            System.out.println("You decide it's best not to touch strange magical artifacts.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Puzzle - Loot generation.
    public void handleSilentLibrary(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Search the rotting shelves", "Walk past them");
        if (answer.equals("Search the rotting shelves")) {
            int randomOutcome = rand.nextInt(5);
            if (randomOutcome < 3) {
                System.out.println("You brush away the thick dust and find an intact magical scroll!");
                int scrollType = rand.nextInt(3);
                if (scrollType == 0) {
                    player.addItem(Items.scrollOfFireball());
                } else if (scrollType == 1) {
                    player.addItem(Items.scrollOfLifeSteal());
                } else {
                    player.addItem(Items.scrollOfLevelUp());
                }
            } else if (randomOutcome == 3) {
                System.out.println("Reading the books you were able to determine that a necromancer is guarding the exit here.");
            } else {
                System.out.println("Even after reading the books you were unable to determine what they meant.");
            }
        } else {
            System.out.println("You leave the ancient books undisturbed in their silence.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Puzzle - Deduction test.
    public void handleColorPillars(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Touch the Red pillar", "Touch the Blue pillar", "Touch the Yellow pillar");
        if (answer.equals("Touch the Red pillar")) {
            System.out.println("The red pillar sinks into the floor, and the door clicks open. The path is clear!");
            room.setPuzzleSolved(true);
        } else {
            int damage = random.nextInt(3) + 5; // 5 to 7
            System.out.println("Incorrect! A hidden dart shoots out from the wall!");
            player.takeDamage(damage);
            System.out.println("You take " + damage + " damage.");
        }
    }

    // PURPOSE: Floor 3 Puzzle - Patience test.
    public void handleHourglassPuzzle(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Pull the lever and try to escape!", "Do nothing and wait it out.");
        if (answer.equals("Do nothing and wait it out.")) {
            System.out.println("You remain calm. The sand reaches your waist, then suddenly drains away. The door unlocks!");
        } else {
            int damage = random.nextInt(4) + 10; // 10 to 13
            System.out.println("Panicking, you pull the lever! A trapdoor opens above, dropping heavy rocks on you!");
            player.takeDamage(damage);
            System.out.println("You take " + damage + " damage before the door finally opens.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Puzzle - Gambler's chest sequence.
    public void handleLucksChance(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Open Chest 1", "Open Chest 2", "Open Chest 3");
        if (answer.isEmpty()) {
            System.out.println("You hesitate. The chests magically seal themselves.");
            room.setPuzzleSolved(true);
            return;
        }
        
        int outcome = rand.nextInt(3); 
        if (outcome == 0) {
            System.out.println("The chest suddenly unhinges its jaw! It's a Mimic!");
            room.setEnemy(Bestiary.mimic());
            
            List<Item> highTier = List.of(Items.bigPotion(), Items.greatSword(), Items.armor(), Items.scrollOfFireball());
            room.addRewardItem(highTier.get(rand.nextInt(highTier.size())));
        } else if (outcome == 1) {
            System.out.println("You found 30 gold inside!");
            player.addGold(30);
        } else {
            System.out.println("You found a greater health potion inside!");
            player.addItem(Items.bigPotion()); 
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Event - Stat buff vs damage gamble.
    public void handleLakeOfTruth(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Take the Silver Dagger", "Take the Gold Dagger", "Decline both");
        
        if (answer.equals("Take the Silver Dagger")) {
            int damage = rand.nextInt(7) + 1;
            System.out.println("As you grab the Silver Dagger, it burns with freezing cold and shatters! You take " + damage + " damage.");
            player.takeDamage(damage);
        } else if (answer.equals("Take the Gold Dagger")) {
            int damage = rand.nextInt(6) + 4;
            System.out.println("As you grab the Gold Dagger, it sears your flesh and melts away! You take " + damage + " damage.");
            player.takeDamage(damage);
        } else if (answer.equals("Decline both")) {
            System.out.println("You decline both daggers. The pool glows warmly, and you feel a surge of inner strength!");
            player.increaseBaseAttack(2);
            System.out.println("Your base strength permanently increases by 2.");
        } else {
            System.out.println("You hesitate, and the daggers sink back into the pool. You missed your chance.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Event - Free stat buff.
    public void handleStarMapRoom(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Wait and observe the stars", "Move on");
        if (answer.equals("Wait and observe the stars")) {
            System.out.println("You sit quietly and study the alien constellations above.\nA strange, cosmic insight fills your mind, permanently increasing your attack by 1!");
            player.increaseBaseAttack(1);
        } else {
            System.out.println("You glance at the glowing ceiling but decide not to linger.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Event - Blood sacrifice for stats.
    public void handleCrimsonAltar(Room room, Player player) {
        System.out.println();
        String answer = getShuffledChoice("Sacrifice your blood (lose 12 HP)", "Walk away");
        if (answer.equals("Sacrifice your blood (lose 12 HP)")) {
            System.out.println("You cut your palm and let your blood drip onto the pristine marble.");
            player.takeDamage(12);
            System.out.println("You lose 12 HP, but feel a dark power surge through your veins! Attack increased by 3.");
            player.increaseBaseAttack(3);
        } else {
            System.out.println("You decide the toll is too high and step away from the altar.");
        }
        room.setPuzzleSolved(true);
    }

    // PURPOSE: Floor 3 Event - Gamble potion.
    public void handleAbandonedLab(Room room, Player player, Random rand) {
        System.out.println();
        String answer = getShuffledChoice("Drink the bubbling pink liquid", "Leave it alone");
        if (answer.equals("Drink the bubbling pink liquid")) {
            if (rand.nextBoolean()) {
                int healAmount = 7 + rand.nextInt(6);
                System.out.println("The liquid is sweet and invigorating! You regain " + healAmount + " HP.");
                player.heal(healAmount);
            } else {
                int damageAmount = 5 + rand.nextInt(5);
                System.out.println("The liquid burns your throat! It's poison! You take " + damageAmount + " damage.");
                player.takeDamage(damageAmount);
            }
        } else {
            System.out.println("You decide not to drink random alchemical liquids.");
        }
        room.setPuzzleSolved(true);
    }
}

// [TAG: SECTION_3 ] - Arena of Blood (BattleManager)
/* =========================================================================================
   Decoupled combat logic. Calculates damage, handles turns, and resolves the use of 
   potions, parrying, dodging, running, and special enemy attacks (constrict, sand).
   ========================================================================================= */

/**
 * Manages the turn-based combat system between the Player and an Enemy object.
 */
class BattleManager {
    private static final int RUN_SUCCESS_RATE = 80;
    private static final int CONSTRICT_DAMAGE = 2;
    private final Random random;

    // PURPOSE: Instantiates the BattleManager and its RNG core.
    public BattleManager() {
        this.random = new Random();
    }

    // PURPOSE: Drives the combat loop until either the Player or Enemy HP reaches 0, or the player escapes.
    public BattleResult handleBattle(Player player, Room room) {
        Enemy enemy = room.getEnemy();
        boolean sandInEyes = false;
        int constrictStage = 0;

        System.out.println("\nA battle begins against " + enemy.getName() + "!");

        while (player.isAlive() && enemy.isAlive()) {
            System.out.println("\n" + player.getName() + " HP: " + player.getHp() + "/" + player.getMaxHp());
            System.out.println(enemy.getName() + " HP: " + enemy.getHp() + "/" + enemy.getMaxHp());

            if (constrictStage > 0) {
                if (constrictStage == 1) {
                    System.out.println("The giant snake tightens its coils around you!");
                    player.takeDamage(CONSTRICT_DAMAGE);
                    System.out.println("You lose your turn and take " + CONSTRICT_DAMAGE + " damage.");
                    constrictStage = 2;
                    continue;
                }
                if (constrictStage >= 2) {
                    if (random.nextBoolean()) {
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
            boolean parryActive = false;

            while (!actionResolved) {
                PlayerAction action = promptPlayerAction(player);

                if (action == PlayerAction.ATTACK) {
                    sandInEyes = playerBasicAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.SWORD) {
                    sandInEyes = playerSwordAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.GREAT_SWORD) {
                    sandInEyes = playerGreatSwordAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.TORCH) {
                    sandInEyes = playerTorchAttack(player, enemy, sandInEyes);
                    actionResolved = true;
                } else if (action == PlayerAction.ACTION) {
                    BattleTurnResult result = openActionMenu(player);
                    actionResolved = result.isTurnConsumed();
                    dodgeActive = result.isDodgeActive();
                    parryActive = result.isParryActive();
                } else if (action == PlayerAction.INVENTORY) {
                    BattleTurnResult result = useItemInBattle(player, enemy, sandInEyes);
                    if (result.didConsumeSandBlindness()) sandInEyes = result.isSandInEyes();
                    actionResolved = result.isTurnConsumed();
                    if (result.isForcedEscape()) return BattleResult.ESCAPED;
                } else if (action == PlayerAction.RUN) {
                    boolean escaped = tryRun(player);
                    actionResolved = !player.getCurrentRoom().equals(player.getPreviousRoom()); 
                    if (escaped) return BattleResult.ESCAPED;
                }
            }

            if (!enemy.isAlive()) break;
            if (!actionResolved) continue;

            EnemyMove move = enemy.chooseMove(random);
            System.out.println(enemy.getName() + " uses " + move.getName() + "!");

            if (dodgeActive) {
                System.out.println("You managed to dodge the attack!");
                continue;
            }

            if (parryActive) {
                if (move.isParryable()) {
                    int reflectedDamage = calculateEnemyDamage(enemy, move, player);
                    enemy.takeDamage(reflectedDamage);
                    System.out.println("Parry successful! You redirect the attack and deal " + reflectedDamage + " damage back to " + enemy.getName() + ".");
                    continue;
                } else {
                    System.out.println("You try to parry, but " + move.getName() + " cannot be parried!");
                }
            }

            if (random.nextInt(100) + 1 > move.getAccuracy()) {
                System.out.println(enemy.getName() + " misses.");
                continue;
            }

            int damage = calculateEnemyDamage(enemy, move, player);
            player.takeDamage(damage);
            System.out.println(enemy.getName() + " hits you for " + damage + " damage.");

            if (move.getEffect() == MoveEffect.POCKET_SAND) {
                if (random.nextInt(100) + 1 <= move.getEffectChance()) {
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
            if (move.getEffect() == MoveEffect.SELF_HEAL || move.getEffect() == MoveEffect.SOUL_DRAIN) {
                int healAmount = move.getEffectPower();
                enemy.heal(healAmount);
                System.out.println(enemy.getName() + " restores " + healAmount + " HP.");
            }
        }

        if (!player.isAlive()) return BattleResult.DIED;

        enemy.setDefeated(true);
        System.out.println("You defeated " + enemy.getName() + "!\nYou gained " + enemy.getXpReward() + " XP and " + enemy.getGoldReward() + " gold.");
        player.addXp(enemy.getXpReward());
        player.addGold(enemy.getGoldReward());
        player.checkLevelUp();

        return BattleResult.WON;
    }

    // PURPOSE: Gathers numeric input from the player to determine their combat action.
    private PlayerAction promptPlayerAction(Player player) {
        while (true) {
            Map<String, PlayerAction> options = new LinkedHashMap<>();
            int optionNumber = 1;
            System.out.println(optionNumber + ". Stick Attack");
            options.put(String.valueOf(optionNumber++), PlayerAction.ATTACK);

            if (Items.findWeapon(player, "Sword") != null) {
                System.out.println(optionNumber + ". Sword Slash");
                options.put(String.valueOf(optionNumber++), PlayerAction.SWORD);
            }
            if (Items.findWeapon(player, "Great Sword") != null) {
                System.out.println(optionNumber + ". Great Sword Slash");
                options.put(String.valueOf(optionNumber++), PlayerAction.GREAT_SWORD);
            }
            if (Items.playerHasTorch(player)) {
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
            PlayerAction chosen = options.get(DarkHolds.input.nextLine().trim());
            if (chosen != null) return chosen;
            System.out.println("Invalid choice.");
        }
    }

    // PURPOSE: Executes a low-accuracy, low-damage stick attack.
    private boolean playerBasicAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem stick = Items.findWeapon(player, "Stick");
        int accuracy = stick != null ? stick.getAccuracy() : 95;
        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You blink through the sand in your eyes and swing blindly!");
        }

        if (random.nextInt(100) + 1 > accuracy) {
            System.out.println("Your Stick Attack misses.");
            return false;
        }

        int damage = stick != null ? stick.getDamageAgainst(enemy, player) : (enemy.getType() == EnemyType.SLIME ? 2 : calculatePlayerDamage(player.getAttack(), 2, enemy.getDefense()));
        enemy.takeDamage(damage);
        System.out.println("You use Stick Attack and deal " + damage + " damage.");
        return false;
    }

    // PURPOSE: Executes a standard sword attack.
    private boolean playerSwordAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem sword = Items.findWeapon(player, "Sword");
        int accuracy = sword != null ? sword.getAccuracy() : 90;
        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You blink through the sand in your eyes and swing your blade blindly!");
        }

        if (random.nextInt(100) + 1 > accuracy) {
            System.out.println("Your Sword Slash misses.");
            return false;
        }

        int damage = sword != null ? sword.getDamageAgainst(enemy, player) : calculatePlayerDamage(player.getAttack(), 8, enemy.getDefense());
        enemy.takeDamage(damage);
        System.out.println("You use Sword Slash and deal " + damage + " damage.");
        return false;
    }

    // PURPOSE: Executes a high-damage but level-dependent accuracy attack.
    private boolean playerGreatSwordAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem weapon = Items.findWeapon(player, "Great Sword");
        int accuracy = 60;
        if (weapon instanceof GreatSwordItem gSword) {
            accuracy = gSword.getAccuracy(player);
        }
        
        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You blink through the sand in your eyes and heave the massive blade blindly!");
        }

        if (random.nextInt(100) + 1 > accuracy) {
            System.out.println("Your Great Sword Slash misses.");
            return false;
        }

        int damage = weapon != null ? weapon.getDamageAgainst(enemy, player) : calculatePlayerDamage(player.getAttack(), 12, enemy.getDefense());
        enemy.takeDamage(damage);
        System.out.println("You use Great Sword Slash and deal " + damage + " damage.");
        return false;
    }

    // PURPOSE: Executes a torch attack that deals elemental bonus damage to slime.
    private boolean playerTorchAttack(Player player, Enemy enemy, boolean sandInEyes) {
        WeaponItem torch = Items.findWeapon(player, "Torch");
        int accuracy = torch != null ? torch.getAccuracy() : 80;
        if (sandInEyes) {
            accuracy = Math.max(1, accuracy / 2);
            System.out.println("You try to line up the torch through burning, gritty eyes!");
        }

        if (random.nextInt(100) + 1 > accuracy) {
            System.out.println("Your Torch attack misses.");
            return false;
        }

        int damage;
        if (enemy.getType() == EnemyType.SKELETON || enemy.getType() == EnemyType.GOLEM) {
            damage = 2; 
            System.out.println("The torch flame merely licks the stone and bone, finding no purchase!");
        } else if (torch != null) {
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

    // PURPOSE: Prompts the player to Dodge or Parry (if a shield is held).
    private BattleTurnResult openActionMenu(Player player) {
        while (true) {
            System.out.println("Action:\n1. Dodge");
            boolean canParry = playerCanParry(player);
            if (canParry) System.out.println("2. Parry");
            System.out.print("0. Return\nChoose action: ");
            String answer = DarkHolds.input.nextLine().trim();

            if (answer.equals("1")) {
                System.out.println("You prepare to dodge the next attack.");
                return new BattleTurnResult(true, false, false, true, false, false);
            }
            if (canParry && answer.equals("2")) {
                System.out.println("You brace yourself to parry the next physical attack.");
                return new BattleTurnResult(true, false, false, false, false, true);
            }
            if (answer.equals("0")) return new BattleTurnResult(false, false, false, false, false, false);
            System.out.println("Invalid choice.");
        }
    }

    // PURPOSE: Helper method to determine if any inventory item provides the parry skill.
    private boolean playerCanParry(Player player) {
        for (Item item : player.getInventory()) {
            if (item instanceof GameItem gameItem && gameItem.grantsParry()) return true;
        }
        return false;
    }

    // PURPOSE: Processes items chosen from the inventory during a combat encounter. Uses the Usable interface.
    private BattleTurnResult useItemInBattle(Player player, Enemy enemy, boolean sandInEyes) {
        while (true) {
            System.out.println("Inventory:");
            if (player.getInventory().isEmpty()) {
                System.out.println("You have no items.");
                return new BattleTurnResult(false, sandInEyes, false, false, false, false);
            }

            for (int i = 0; i < player.getInventory().size(); i++) {
                System.out.println((i + 1) + ". " + player.getInventory().get(i));
            }
            System.out.print("0. Cancel\nChoose item number: ");
            String answer = DarkHolds.input.nextLine().trim();
            if (answer.equals("0")) return new BattleTurnResult(false, sandInEyes, false, false, false, false);

            try {
                int index = Integer.parseInt(answer) - 1;
                if (index < 0 || index >= player.getInventory().size()) {
                    System.out.println("Invalid item choice.");
                    continue;
                }

                Item item = player.getInventory().get(index);
                if (item.getName().equalsIgnoreCase("Stick")) return new BattleTurnResult(true, playerBasicAttack(player, enemy, sandInEyes), true, false, false, false);
                if (item.getName().equalsIgnoreCase("Sword")) return new BattleTurnResult(true, playerSwordAttack(player, enemy, sandInEyes), true, false, false, false);
                if (item.getName().equalsIgnoreCase("Great Sword")) return new BattleTurnResult(true, playerGreatSwordAttack(player, enemy, sandInEyes), true, false, false, false);
                if (item.getName().equalsIgnoreCase("Torch")) return new BattleTurnResult(true, playerTorchAttack(player, enemy, sandInEyes), true, false, false, false);

                if (item instanceof GameItem gameItem && gameItem.grantsParry()) {
                    System.out.println("You raise your " + item.getName() + ", bracing to parry the next physical attack!");
                    return new BattleTurnResult(true, sandInEyes, false, false, false, true);
                }

                if (item instanceof GameItem gameItem && gameItem.canUseInBattle()) {
                    ItemContext context = new ItemContext();
                    context.setInBattle(true);
                    context.setCurrentEnemy(enemy);
                    context.setCurrentRoomId(player.getCurrentRoom());
                    context.setPreviousRoomId(player.getPreviousRoom());

                    if (gameItem.use(player, context)) player.removeItem(index);
                    if (context.isEscapedByItem()) return new BattleTurnResult(true, sandInEyes, false, false, true, false);
                    return new BattleTurnResult(true, sandInEyes, false, false, false, false);
                }
                System.out.println(item.getName() + " cannot be used in battle right now.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid item choice.");
            }
        }
    }

    // PURPOSE: Executes probability logic to see if the player flees the current room.
    private boolean tryRun(Player player) {
        if (player.getCurrentRoom().equals(player.getPreviousRoom())) {
            System.out.println("The way behind you is blocked! You cannot escape!");
            return false;
        }
        if (random.nextInt(100) + 1 <= RUN_SUCCESS_RATE) {
            System.out.println("You escape back to the previous room.");
            player.setCurrentRoom(player.getPreviousRoom());
            return true;
        }
        System.out.println("You fail to escape!");
        return false;
    }

    // PURPOSE: Calculates standard player attack damage against enemy defense.
    private int calculatePlayerDamage(int playerAttack, int movePower, int enemyDefense) {
        return Math.max(1, movePower + playerAttack - enemyDefense);
    }

    // PURPOSE: Calculates standard enemy attack damage against player defense.
    private int calculateEnemyDamage(Enemy enemy, EnemyMove move, Player player) {
        if (move.getEffect() == MoveEffect.POCKET_SAND) return 1;
        return Math.max(1, move.getPower() + enemy.getAttack() - player.getDefense());
    }
}

enum BattleResult { WON, ESCAPED, DIED }
enum PlayerAction { ATTACK, SWORD, GREAT_SWORD, TORCH, ACTION, INVENTORY, RUN }

/**
 * Encapsulates the specific outcomes of a single combat turn.
 */
class BattleTurnResult {
    private final boolean turnConsumed, sandInEyes, consumeSandBlindness, dodgeActive, forcedEscape, parryActive;
    public BattleTurnResult(boolean turnConsumed, boolean sandInEyes, boolean consumeSandBlindness, boolean dodgeActive, boolean forcedEscape, boolean parryActive) {
        this.turnConsumed = turnConsumed;
        this.sandInEyes = sandInEyes;
        this.consumeSandBlindness = consumeSandBlindness;
        this.dodgeActive = dodgeActive;
        this.forcedEscape = forcedEscape;
        this.parryActive = parryActive;
    }
    public boolean isTurnConsumed() { return turnConsumed; }
    public boolean isSandInEyes() { return sandInEyes; }
    public boolean didConsumeSandBlindness() { return consumeSandBlindness; }
    public boolean isDodgeActive() { return dodgeActive; }
    public boolean isForcedEscape() { return forcedEscape; }
    public boolean isParryActive() { return parryActive; }
}

// [TAG: SECTION_4 ] - Architect's Blueprint (WorldBuilder)
/* =========================================================================================
   Builds the map on startup. Provides procedural generation for replayability by 
   randomly assembling rooms and injecting them into the World's map structure.
   ========================================================================================= */

/**
 * A static utility class responsible for generating the dungeon layout, seeding enemies,
 * and linking room edges to construct the navigation graph.
 */
class WorldBuilder {

    // PURPOSE: Assembles the rooms and events for the first floor.
    public static List<Room> createFloorOne(Random random) {
        List<Room> f1Rooms = new ArrayList<>();
        int numRooms = random.nextInt(7) + 7; 

        Room safeRoom = new Room("f1safe", "Safe Room", "You wake in a cold stone chamber with only a rough stick beside you. A heavy locked hallway gate stands to one side, and the rest of the floor branches into darkness.", true, true);
        safeRoom.setClearedDescription(safeRoom.getDisplayDescription()); // Keep original description

        List<Room> f1Pool = new ArrayList<>();
        
        Room brokenFork = new Room("f1p_brokenFork", "Broken Fork", "The passage splits here around cracked masonry. An ancient puzzle locks the path.", true, true);
        brokenFork.setClearedDescription("The cracked masonry has been shifted, solving the puzzle.");
        f1Pool.add(brokenFork);
        
        Room shrine = new Room("f1p_shrine", "Serpent Shrine", "A shrine of coiled stone and old offerings. A riddle is carved into the altar.", true, true);
        shrine.setClearedDescription("The serpent shrine is silent, its riddle answered.");
        f1Pool.add(shrine);
        
        Room scales = new Room("f1p_weighingScales", "Room of Scales", "A massive bronze scale dominates the center of the room. One side is weighed down by a stone heart; the other side sits empty, waiting for an offering.", true, true);
        scales.setClearedDescription("The bronze scale rests perfectly balanced, allowing safe passage.");
        f1Pool.add(scales);
        
        Room batRoom = new Room("f1f_batRoost", "Bat Roost", "The ceiling above is black with restless shapes. Thin squeaks echo between the stones.", true, true);
        batRoom.setEnemy(Bestiary.bat());
        batRoom.setClearedDescription("The roost is quiet now, the aggressive bats defeated.");
        f1Pool.add(batRoom);

        Room goblinRoom = new Room("f1f_goblinOne", "Goblin Trail", "The smell of rot and old smoke thickens. You spot a goblin scavenger lurking among pilfered items.", true, true);
        goblinRoom.setEnemy(Bestiary.goblin());
        goblinRoom.setClearedDescription("The goblin's corpse lies among the pilfered items.");
        f1Pool.add(goblinRoom);

        Room slimeRoom = new Room("f1f_slimeRoom", "Damp Tunnel", "A wet side cut opens into a slick pocket where something gelatinous shivers in the dark.", true, true);
        slimeRoom.setEnemy(Bestiary.slime());
        slimeRoom.setClearedDescription("Only a puddle of inert ooze remains where the slime once shivered.");
        f1Pool.add(slimeRoom);
        
        Room chimneyShaft = new Room("f1f_chimneyShaft", "Crumbling Chimney", "A vertical shaft lets in a sliver of moonlight. A swarm of bats uses this as a highway to the surface.", true, true);
        chimneyShaft.setEnemy(Bestiary.bat());
        chimneyShaft.setClearedDescription("The shaft is peaceful, the bat swarm having dispersed.");
        f1Pool.add(chimneyShaft);

        Room batRoots = new Room("f1f_bat_roots", "Hanging Roots", "Thick, dead tree roots punch through the ceiling here. Dozens of sleeping bats cling to them.", true, true);
        batRoots.setEnemy(Bestiary.bat());
        batRoots.setClearedDescription("The dead tree roots hang empty; the bats have been slain.");
        f1Pool.add(batRoots);

        Room wolfDen = new Room("f1f_wolf_den", "Beast's Den", "The floor is covered in matted fur. A massive wolf blocks the exit, growling.", true, true);
        wolfDen.setEnemy(Bestiary.wolf());
        wolfDen.setClearedDescription("The massive wolf lies still on the matted fur.");
        f1Pool.add(wolfDen);

        Room wolfTunnel = new Room("f1f_wolf_tunnel", "Howling Tunnel", "A real howl answers the draft as a wolf lunges from the shadows.", true, true);
        wolfTunnel.setEnemy(Bestiary.wolf());
        wolfTunnel.setClearedDescription("The draft howls through the tunnel, but the beast is no more.");
        f1Pool.add(wolfTunnel);

        f1Pool.add(new Room("f1_cache", "Empty Cache", "A looted side chamber lies here with only scattered coins left behind.", true, true));
        f1Pool.add(new Room("f1_snakeHoard", "Snake Hoard", "Gold glints among bones and torn packs.", true, true));
        f1Pool.add(new Room("f1_fallenAdventurer", "Fallen Adventurer", "A skeleton slumps against the wall, clutching a broken weapon.", true, true));

        Collections.shuffle(f1Pool, random);
        f1Rooms.add(safeRoom);
        f1Rooms.addAll(f1Pool.subList(0, numRooms - 4)); 

        Room lockedHall = new Room("f1lockedHall", "Locked Hallway", "With the key in hand, you force the rusted mechanism open.", false, false);
        lockedHall.setClearedDescription(lockedHall.getDisplayDescription());
        f1Rooms.add(lockedHall);

        Room f1Boss = new Room("f1snakeBoss", "Serpent Lair", "A giant snake rises from the treasure mound, its eyes fixed on you.", false, false);
        f1Boss.setEnemy(Bestiary.giantSnake());
        f1Boss.setClearedDescription("The giant snake's massive lifeless coils rest upon the plundered hoard.");
        f1Rooms.add(f1Boss);

        Room stairs1 = new Room("f1_stairs", "Stairs to Floor 2", "A spiral stone staircase ascending into the darkness.", false, false);
        f1Rooms.add(stairs1);

        for (int i = 0; i < f1Rooms.size() - 2; i++) connect(f1Rooms.get(i), f1Rooms.get(i + 1));
        f1Rooms.get(f1Rooms.size() - 2).addNeighbor(f1Rooms.get(f1Rooms.size() - 1).getId());

        return f1Rooms;
    }

    // PURPOSE: Assembles the rooms and events for the second floor, including the wandering merchant.
    public static List<Room> createFloorTwo(Random random) {
        List<Room> f2Rooms = new ArrayList<>();
        int numRooms = random.nextInt(8) + 8; 

        List<Room> f2Pool = new ArrayList<>();
        
        Room totem = new Room("f2p_totem", "Totem of Gruum", "A crude wooden totem pole stands before a locked portcullis.", false, false);
        totem.setClearedDescription("The wooden totem stands silently; the portcullis remains open.");
        f2Pool.add(totem);
        
        Room statue = new Room("f2p_weepingstatue", "Weeping Statue", "A towering marble statue of a hooded figure weeps oily water.", false, false);
        statue.setClearedDescription("The marble statue continues to weep, but you have already interacted with its waters.");
        f2Pool.add(statue);
        
        Room campsite = new Room("f2p_campsite", "Abandoned Campsite", "A dusty bedroll and a cold firepit sit in the corner.", false, false);
        campsite.setClearedDescription("The campsite is abandoned once more, the firepit cold.");
        f2Pool.add(campsite);
        
        Room wishwell = new Room("f2p_wishwell", "The Wishing Well", "A deep stone well sits in the center of the room.", false, false);
        wishwell.setClearedDescription("The dark well sits quietly, its depths keeping their secrets.");
        f2Pool.add(wishwell);
        
        Room obelisk = new Room("f2p_obsidian_obelisk", "Obsidian Obelisk", "A perfectly smooth, black stone spire stands in the center.", false, false);
        obelisk.setClearedDescription("The black stone spire continues to hum faintly, its gift already given.");
        f2Pool.add(obelisk);

        Room altar = new Room("f2_shamansAltar", "The Shaman's Altar", "A makeshift altar of stacked stones is covered in melted wax.", false, false);
        altar.addRewardItem(Items.bigPotion());
        f2Pool.add(altar);

        Room armory = new Room("f2_desecratedArmory", "Desecrated Armory", "Shattered shields and bent swords litter the floor.", false, false);
        int armoryLoot = random.nextInt(3);
        if (armoryLoot == 0) armory.addRewardItem(Items.smallPotion());
        else if (armoryLoot == 1) armory.addRewardItem(Items.decayedArmor());
        else armory.addGold(randomGold(7, 20, random));
        f2Pool.add(armory);

        Room slimePool = new Room("f2f_slime_pool", "Acidic Pool", "A bubbling puddle rises upward, forming into a massive blob.", false, false);
        slimePool.setEnemy(Bestiary.slime());
        slimePool.setClearedDescription("The bubbling puddle has dissolved into harmless water.");
        f2Pool.add(slimePool);

        Room slimeCeiling = new Room("f2f_slime_ceiling", "The Dripping Ceiling", "The ceiling is coated in a vibrating slime that suddenly drops.", false, false);
        slimeCeiling.setEnemy(Bestiary.slime());
        slimeCeiling.setClearedDescription("The ceiling still drips, but the slime has been defeated.");
        f2Pool.add(slimeCeiling);

        Room skelCrypt = new Room("f2f_skel_crypt", "Forgotten Crypt", "A skeleton steps out of a sarcophagus, raising a rusted sword.", false, false);
        skelCrypt.setEnemy(Bestiary.skeleton());
        skelCrypt.setClearedDescription("The skeletal warrior is nothing more than scattered bones before its sarcophagus.");
        f2Pool.add(skelCrypt);

        Room skelGrave = new Room("f2f_skel_grave", "The Mass Grave", "A skeletal hand burst from the dirt, followed by the warrior.", false, false);
        skelGrave.setEnemy(Bestiary.skeleton());
        skelGrave.setClearedDescription("The dirt is disturbed where the skeleton emerged, but the undead is vanquished.");
        f2Pool.add(skelGrave);

        Room fightingPit = new Room("f2f_fighting_pit", "The Fighting Pit", "A battle-crazed Orc leaps down from the spectator ledge.", false, false);
        fightingPit.setEnemy(Bestiary.orc());
        fightingPit.setClearedDescription("The Orc lies defeated in the bloodstained pit.");
        f2Pool.add(fightingPit);

        Room raidersBarricade = new Room("f2f_raiders_barricade", "Raider's Barricade", "A scarred Orc barks an alarm as you approach the chokepoint.", false, false);
        raidersBarricade.setEnemy(Bestiary.orc());
        raidersBarricade.setClearedDescription("The barricade is clear, its Orcish defender slain.");
        f2Pool.add(raidersBarricade);

        Room alchemistSpill = new Room("f2f_alchemist_spill", "Alchemist's Spill", "Spilled alchemical reagents have coalesced into a bubbling Slime.", false, false);
        alchemistSpill.setEnemy(Bestiary.slime());
        alchemistSpill.setClearedDescription("The alchemical slime has been neutralized, leaving only a stain.");
        f2Pool.add(alchemistSpill);

        Room spoilsRoom = new Room("f2f_spoils_room", "Spoils Room", "An Orc is busy biting coins to check their worth.", false, false);
        spoilsRoom.setEnemy(Bestiary.orc());
        spoilsRoom.setClearedDescription("The Orc looter lies amidst the scattered coins.");
        f2Pool.add(spoilsRoom);

        Room merchantRoom = new Room("f2_merchant", "Wandering Merchant's Camp", "A small fire crackles in the corner. A neutral Goblin sits on a heavily burdened mule pack, smoking a pipe.", false, false);
        merchantRoom.setMerchantRoom(true);

        Collections.shuffle(f2Pool, random);
        List<Room> selectedF2 = new ArrayList<>(f2Pool.subList(0, numRooms - 3));
        selectedF2.add(merchantRoom);
        Collections.shuffle(selectedF2, random);
        f2Rooms.addAll(selectedF2);

        Room golemBoss = new Room("f2_golem_rune", "Runestone Chamber", "A Golem made entirely of carved runestones stands guard.", false, false);
        golemBoss.setEnemy(Bestiary.golem()); 
        golemBoss.setClearedDescription("The runestone Golem has crumbled into a heap of inert, carved rocks.");
        f2Rooms.add(golemBoss);

        Room stairs2 = new Room("f2_stairs", "Stairs to Floor 3", "A massive, spiraling iron staircase suspended by rusted chains over a seemingly bottomless abyss.", false, false);
        f2Rooms.add(stairs2);

        for (int i = 0; i < f2Rooms.size() - 2; i++) connect(f2Rooms.get(i), f2Rooms.get(i + 1));
        f2Rooms.get(f2Rooms.size() - 2).addNeighbor(f2Rooms.get(f2Rooms.size() - 1).getId());

        return f2Rooms;
    }

    // PURPOSE: Assembles the rooms and events for the final floor, appending the secret boss lair correctly.
    public static List<Room> createFloorThree(Random random) {
        List<Room> f3Rooms = new ArrayList<>();
        int numRooms = random.nextInt(9) + 9; 
        
        List<Room> f3Pool = new ArrayList<>();
        
        Room silentLibrary = new Room("f3p_silentlibrary", "Silent Library", "Rows of rotting bookshelves line this chamber.", false, false);
        silentLibrary.setClearedDescription("The rotting bookshelves collapse, leaving the library truly silent.");
        f3Pool.add(silentLibrary);
        
        Room colorPillars = new Room("f3p_colorpillars", "Color Pillars", "Three pillars stand before a locked door. Red, blue, and yellow.", false, false);
        colorPillars.setClearedDescription("The pillars have sunk into the floor, the mechanism permanently unlocked.");
        f3Pool.add(colorPillars);
        
        Room hourglass = new Room("f3p_hourglass", "Hourglass of Sand", "A giant hourglass flips when you enter.", false, false);
        hourglass.setClearedDescription("The giant hourglass is shattered and empty, the trial taken.");
        f3Pool.add(hourglass);
        
        Room lucksChance = new Room("f3p_luckschance", "Luck's Chance", "You encounter a room with three chests.", false, false);
        lucksChance.setClearedDescription("The chests remain open and empty, the gamble concluded.");
        f3Pool.add(lucksChance);
        
        Room lakeOfTruth = new Room("f3p_lakeoftruth", "Lake of Truth", "Two daggers rise from a pool. One silver, one gold.", false, false);
        lakeOfTruth.setClearedDescription("The pool glows warmly.");
        f3Pool.add(lakeOfTruth);
        
        Room starMapRoom = new Room("f3p_starmap_room", "Star Map Room", "The ceiling is painted with a glowing map of the night sky.", false, false);
        starMapRoom.setClearedDescription("The alien constellations glow steadily on the ceiling above.");
        f3Pool.add(starMapRoom);
        
        Room crimsonAltar = new Room("f3p_crimson_altar", "The Crimson Altar", "A pristine white marble altar sits in the center.", false, false);
        crimsonAltar.setClearedDescription("The pristine marble is stained, the altar's toll already paid.");
        f3Pool.add(crimsonAltar);
        
        Room abandonedLab = new Room("f3p_abandoned_lab", "Abandoned Laboratory", "A central cauldron bubbles with a sweet-smelling pink liquid.", false, false);
        abandonedLab.setClearedDescription("The cauldron's liquid has settled, no longer bubbling.");
        f3Pool.add(abandonedLab);
        
        f3Pool.add(new Room("f3_guardhouse", "Abandoned Guardhouse", "Weapon racks stand empty.", false, false));
        f3Pool.add(new Room("f3_statueGallery", "Statue Gallery", "A hall lined with statues of faceless knights.", false, false));
        f3Pool.add(new Room("f3_armorers_forge", "Armorer's Forge", "An unfinished breastplate rests on a table.", false, false));

        Room bonePit = new Room("f3f_bonePit", "The Bone Pit", "A hulking Ogre slowly turns towards you in a pit of bones.", false, false);
        bonePit.setEnemy(Bestiary.ogre()); 
        bonePit.setClearedDescription("The hulking Ogre rests permanently among the bones of its victims.");
        f3Pool.add(bonePit);

        Room tremblingCavern = new Room("f3f_tremblingCavern", "The Trembling Cavern", "An Ogre wearing scavenged armor plates beats its chest and charges.", false, false);
        tremblingCavern.setEnemy(Bestiary.ogre());
        tremblingCavern.setClearedDescription("The armored Ogre has fallen, leaving the cavern still.");
        f3Pool.add(tremblingCavern);

        Room trollsHoard = new Room("f3f_trollsHoard", "Troll's Hoard", "The Troll protecting a pile of shiny trash roars fiercely.", false, false);
        trollsHoard.setEnemy(Bestiary.caveTroll());
        trollsHoard.setClearedDescription("The Cave Troll is dead, its shiny trash left unguarded.");
        f3Pool.add(trollsHoard);

        Room echoingChasm = new Room("f3f_echoingChasm", "Echoing Chasm", "A Cave Troll hangs from the stalactites above.", false, false);
        echoingChasm.setEnemy(Bestiary.caveTroll());
        echoingChasm.setClearedDescription("The chasm echoes only with your footsteps; the Troll has been slain.");
        f3Pool.add(echoingChasm);

        Room cryptKings = new Room("f3_crypt_kings", "Crypt of the Forgotten Kings", "Ornate sarcophagi rest in alcoves.", false, false);
        cryptKings.addGold(randomGold(15, 25, random));
        f3Pool.add(cryptKings);

        Room merchantRoom = new Room("f3_merchant", "Wandering Merchant's Camp", "The Goblin Merchant has set up his camp here. He recognizes you and nods, adjusting his wares.", false, false);
        merchantRoom.setMerchantRoom(true);

        Room antechamber = new Room("f3_dragon_antechamber", "The Whispering Antechamber", "The air here is unnaturally hot.", false, false);
        antechamber.addGold(randomGold(10, 30, random));
        f3Pool.add(antechamber);

        Collections.shuffle(f3Pool, random);
        List<Room> selectedF3 = new ArrayList<>(f3Pool.subList(0, numRooms - 3));
        selectedF3.add(merchantRoom);
        Collections.shuffle(selectedF3, random);
        f3Rooms.addAll(selectedF3);

        Room dragonLair = new Room("f3_dragon_lair", "The Dragon's Lair", "A massive cavern scorched black by ancient fire.", false, false);
        dragonLair.setEnemy(Bestiary.dragon());
        dragonLair.addRewardItem(Items.dragonScale());
        dragonLair.addRewardItem(Items.dragonEgg()); 
        dragonLair.setClearedDescription("The massive cavern is scorched black, the mighty Dragon's corpse resting eternally in its center.");
        
        connect(antechamber, dragonLair);

        Room necroBoss = new Room("f3_necro_altar", "Profane Altar", "A Necromancer in tattered robes is chanting.", false, false);
        necroBoss.setEnemy(Bestiary.necromancer());
        necroBoss.setClearedDescription("The Necromancer lies defeated, their dark chanting silenced forever.");
        f3Rooms.add(necroBoss);

        Room f3End = new Room("f3_end", "The Final Descent", "", false, false);
        f3Rooms.add(f3End);

        for (int i = 0; i < f3Rooms.size() - 2; i++) connect(f3Rooms.get(i), f3Rooms.get(i + 1));
        f3Rooms.get(f3Rooms.size() - 2).addNeighbor(f3Rooms.get(f3Rooms.size() - 1).getId());

        f3Rooms.add(dragonLair);

        return f3Rooms;
    }

    // PURPOSE: Utility to bidirectionally link two Room nodes.
    public static void connect(Room a, Room b) {
        a.addNeighbor(b.getId());
        b.addNeighbor(a.getId());
    }
    
    // PURPOSE: Utility to generate a random gold amount for chest loot.
    private static int randomGold(int min, int max, Random random) {
        return min + random.nextInt(max - min + 1);
    }
}

// [TAG: SECTION_5 ] - Vessel of Flesh (Player State)
/* =========================================================================================
   Holds the state data for the user. Manages inventory array, level progression,
   and core stat scaling as the game loops.
   ========================================================================================= */

/**
 * The Player class represents the human-controlled entity.
 * It tracks HP, stats, gold, level, and stores items via Aggregation.
 */
class Player {
    private static final int[] XP_THRESHOLDS = {0, 12, 26, 42, 60, 82, 108, 138, 172, 210, 252, 298, 348};
    private static final int[] HP_BY_LEVEL = {50, 54, 58, 62, 66, 70, 74, 79, 84, 88, 92, 96, 100};
    private static final int[] ATTACK_BY_LEVEL = {8, 8, 9, 9, 10, 10, 11, 12, 12, 13, 14, 15, 16};
    private static final int[] DEFENSE_BY_LEVEL = {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6};

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

    // PURPOSE: Constructs a level 1 player, granting them a stick and their initial stat arrays.
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
        this.inventory.add(Items.stick());
    }

    public void takeDamage(int amount) { hp = Math.max(0, hp - amount); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public void increaseBaseAttack(int amount) { this.baseAttack += amount; }
    public boolean isAlive() { return hp > 0; }
    public void addXp(int amount) { xp += amount; }

    // PURPOSE: Scans XP thresholds. If met, increments level and scales max stats accordingly.
    public void checkLevelUp() {
        while (level < HP_BY_LEVEL.length && xp >= XP_THRESHOLDS[level]) {
            level++;
            maxHp = HP_BY_LEVEL[level - 1];
            baseAttack = ATTACK_BY_LEVEL[level - 1];
            baseDefense = DEFENSE_BY_LEVEL[level - 1];
            hp = maxHp;
            System.out.println("You reached level " + level + "!\nYour strength rises.");
        }
    }

    public void addGold(int amount) { gold += amount; }
    public void addItem(Item item) { inventory.add(item); }
    public void removeItem(int index) { inventory.remove(index); }
    
    // PURPOSE: Boolean check to see if an item by string name exists within the inventory array.
    public boolean hasItem(String itemName) {
        for (Item item : inventory) { if (item.getName().equalsIgnoreCase(itemName)) return true; }
        return false;
    }

    // PURPOSE: Boolean check to see if *any* object instanced as KeyItem exists in the inventory.
    public boolean hasAnyKey() { 
        for (Item item : inventory) { if (item instanceof KeyItem) return true; }
        return false; 
    }
    
    // PURPOSE: Boolean check to see if a specific key by keyId exists in the inventory.
    public boolean hasKey(String keyId) {
        for (Item item : inventory) { 
            if (item instanceof KeyItem key && key.getKeyId().equalsIgnoreCase(keyId)) return true; 
        }
        return false;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return baseAttack + Items.getPassiveAttackBonus(this); }
    public int getDefense() { return baseDefense + Items.getPassiveDefenseBonus(this); }
    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getGold() { return gold; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }
    public String getPreviousRoom() { return previousRoom; }
    public void setPreviousRoom(String previousRoom) { this.previousRoom = previousRoom; }
    public ArrayList<Item> getInventory() { return inventory; }
}

// [TAG: SECTION_6 ] - Vessel of Stone (Room State)
/* =========================================================================================
   The map node class. Holds all states relating to a physical location, including 
   uncollected loot, untriggered puzzles, and undefeated enemies.
   ========================================================================================= */

/**
 * A Room object acts as a node in the navigation map. It uses Composition to 
 * hold an Enemy object and Aggregation to hold Lists of Items.
 */
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
    private final boolean puzzleRoom;
    private boolean puzzleSolved;
    private boolean merchantRoom;
    private boolean hasCat;
    private boolean catResolved;

    // PURPOSE: Constructs a baseline room and sets its identity flags for the WorldBuilder.
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
        this.puzzleRoom = id.contains("p_");
        this.puzzleSolved = false;
        this.merchantRoom = false;
        this.hasCat = false;
        this.catResolved = false;
    }

    public void addNeighbor(String neighborId) { if (!neighborIds.contains(neighborId)) neighborIds.add(neighborId); }
    public void addRewardItem(Item item) { rewardItems.add(item); }
    public void addGold(int amount) { goldReward += amount; }
    public boolean hasEnemy() { return enemy != null; }

    public String getId() { return id; }
    public String getName() { return name; }
    
    // PURPOSE: Dynamically alters the descriptive text if the room's threat/puzzle has been resolved.
    public String getDisplayDescription() { return hasBeenCleared() ? clearedDescription : description; }
    public void setClearedDescription(String clearedDescription) { this.clearedDescription = clearedDescription; }

    // PURPOSE: Master flag check to see if a room's obstacle (enemy, puzzle, or loot) has been neutralized.
    public boolean hasBeenCleared() {
        if (hasEnemy() && enemy.isDefeated()) return true;
        if (isPuzzleRoom() && puzzleSolved) return true;
        return rewardsCollected;
    }

    public ArrayList<String> getNeighborIds() { return neighborIds; }
    public Enemy getEnemy() { return enemy; }
    public void setEnemy(Enemy enemy) { this.enemy = enemy; }
    public int getGoldReward() { return goldReward; }
    public boolean hasKeyReward() { return hasKeyReward; }
    public void setHasKeyReward(boolean hasKeyReward) { this.hasKeyReward = hasKeyReward; }
    public ArrayList<Item> getRewardItems() { return rewardItems; }
    public boolean isRewardsCollected() { return rewardsCollected; }
    public void setRewardsCollected(boolean rewardsCollected) { this.rewardsCollected = rewardsCollected; }
    public boolean isPreLock() { return preLock; }
    public void setPreLock(boolean preLock) { this.preLock = preLock; }
    public boolean isKeyEligible() { return keyEligible; }
    public void setKeyEligible(boolean keyEligible) { this.keyEligible = keyEligible; }
    public boolean isPuzzleRoom() { return puzzleRoom; }
    public boolean isPuzzleSolved() { return puzzleSolved; }
    public void setPuzzleSolved(boolean puzzleSolved) { this.puzzleSolved = puzzleSolved; }
    public boolean isMerchantRoom() { return merchantRoom; }
    public void setMerchantRoom(boolean merchantRoom) { this.merchantRoom = merchantRoom; }
    public boolean hasCat() { return hasCat; }
    public void setHasCat(boolean hasCat) { this.hasCat = hasCat; }
    public boolean isCatResolved() { return catResolved; }
    public void setCatResolved(boolean catResolved) { this.catResolved = catResolved; }
}

// [TAG: SECTION_7 ] - Master Reliquary (Items)
/* =========================================================================================
   This architecture dictates what every item IS (Item -> GameItem) and what it DOES (Usable).
   ========================================================================================= */

/**
 * A static factory containing instantiation methods for every physical item in the game.
 */
final class Items {

    private Items() {}

    public static StickItem stick() { return new StickItem(); }
    public static SwordItem sword() { return new SwordItem(); }
    public static GreatSwordItem greatSword() { return new GreatSwordItem(); }
    public static TorchItem torch() { return new TorchItem(); }
    public static DragonEggItem dragonEgg() { return new DragonEggItem(); }
    public static SmallPotionItem smallPotion() { return new SmallPotionItem(); }
    public static BigPotionItem bigPotion() { return new BigPotionItem(); }
    public static ScrollOfEscapeItem scrollOfEscape() { return new ScrollOfEscapeItem(); }
    public static ScrollOfFireballItem scrollOfFireball() { return new ScrollOfFireballItem(); }
    public static ScrollOfStealthItem scrollOfStealth() { return new ScrollOfStealthItem(); }
    public static ScrollOfLifeStealItem scrollOfLifeSteal() { return new ScrollOfLifeStealItem(); }
    public static ScrollOfLevelUpItem scrollOfLevelUp() { return new ScrollOfLevelUpItem(); }
    public static DecayedArmorItem decayedArmor() { return new DecayedArmorItem(); }
    public static ArmorItem armor() { return new ArmorItem(); }
    public static ShieldItem shield() { return new ShieldItem(); }
    public static DragonScaleShieldItem dragonScale() { return new DragonScaleShieldItem(); }
    public static KeyItem floorOneIronKey() { return new KeyItem("locked_hall_iron_key", "Iron Key"); }
    public static KeyItem ancientRelic() { return new KeyItem("dragon_lair_secret_door", "Ancient Relic"); }
    public static CatItem cat() { return new CatItem(); }

    // PURPOSE: Sums up all Attack bonuses from equipped gear.
    public static int getPassiveAttackBonus(Player player) {
        int total = 0;
        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) total += gear.getPassiveAttackBonus();
        }
        return total;
    }

    // PURPOSE: Evaluates the highest shielding gear and armor worn by the player, summing them.
    public static int getPassiveDefenseBonus(Player player) {
        int highestArmor = 0;
        int highestShield = 0;
        
        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) {
                if (gear.grantsParry()) { 
                    if (gear.getPassiveDefenseBonus() > highestShield) {
                        highestShield = gear.getPassiveDefenseBonus();
                    }
                } else { 
                    if (gear.getPassiveDefenseBonus() > highestArmor) {
                        highestArmor = gear.getPassiveDefenseBonus();
                    }
                }
            }
        }
        return highestArmor + highestShield;
    }

    // PURPOSE: Short-hand check if player holds a specific item for dark room vision.
    public static boolean playerHasTorch(Player player) { return player.hasItem("Torch"); }

    // PURPOSE: Returns a reference to a weapon if it exists in the player's pack.
    public static WeaponItem findWeapon(Player player, String weaponName) {
        for (Item item : player.getInventory()) {
            if (item instanceof WeaponItem weapon && item.getName().equalsIgnoreCase(weaponName)) return weapon;
        }
        return null;
    }
}

/**
 * The base physical item class. Purely informational representation of a thing.
 */
class Item {
    private final String name;
    private final ItemType type;
    private final int power;
    
    // PURPOSE: Constructs a generic item.
    public Item(String name, ItemType type, int power) { this.name = name; this.type = type; this.power = power; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getPower() { return power; }
    @Override public String toString() { return name + " (" + type + ")"; }
}

enum ItemType { HEALING, WEAPON, SPECIAL }

/**
 * An envelope class to pass contextual data to an item's use() method.
 */
class ItemContext {
    private boolean inBattle;
    private Enemy currentEnemy;
    private String previousRoomId, currentRoomId;
    private boolean stealthActive, battleEnded, escapedByItem, encounterBypassed;

    public boolean isInBattle() { return inBattle; }
    public void setInBattle(boolean inBattle) { this.inBattle = inBattle; }
    public Enemy getCurrentEnemy() { return currentEnemy; }
    public void setCurrentEnemy(Enemy currentEnemy) { this.currentEnemy = currentEnemy; }
    public String getPreviousRoomId() { return previousRoomId; }
    public void setPreviousRoomId(String previousRoomId) { this.previousRoomId = previousRoomId; }
    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }
    public boolean isStealthActive() { return stealthActive; }
    public void setStealthActive(boolean stealthActive) { this.stealthActive = stealthActive; }
    public boolean isBattleEnded() { return battleEnded; }
    public void setBattleEnded(boolean battleEnded) { this.battleEnded = battleEnded; }
    public boolean isEscapedByItem() { return escapedByItem; }
    public void setEscapedByItem(boolean escapedByItem) { this.escapedByItem = escapedByItem; }
    public boolean isEncounterBypassed() { return encounterBypassed; }
    public void setEncounterBypassed(boolean encounterBypassed) { this.encounterBypassed = encounterBypassed; }
}

// PROFESSOR REQUIREMENT: Interface Implementation
/**
 * Interface that formalizes the contract of usability.
 * Separates the behavior of 'using' a thing from the 'physical matter' of an Item.
 */
interface Usable {
    boolean canUseInBattle();
    boolean canUseOutsideBattle();
    boolean use(Player player, ItemContext context);
}

/**
 * An abstract GameItem dictating an object is physically present in the game world, 
 * possessing a description, and adhering to the Usable behavior contract.
 */
abstract class GameItem extends Item implements Usable {
    private final String description;
    
    // PURPOSE: Constructs the base GameItem.
    public GameItem(String name, ItemType type, int power, String description) { super(name, type, power); this.description = description; }
    public String getDescription() { return description; }
    
    // PURPOSE: The default behavior for these methods dictates inertness unless overridden.
    public boolean canUseInBattle() { return false; }
    public boolean canUseOutsideBattle() { return false; }
    public boolean isPassive() { return false; }
    public boolean use(Player player, ItemContext context) { System.out.println(getName() + " has no active use."); return false; }
    public int getPassiveAttackBonus() { return 0; }
    public int getPassiveDefenseBonus() { return 0; }
    public boolean grantsParry() { return false; }
    @Override public String toString() { return getName() + " - " + description; }
}

/**
 * Abstract sub-class for offensive armaments. Overrides specific damage/accuracy algorithms.
 */
abstract class WeaponItem extends GameItem {
    private final int accuracy;
    private final String attackName;
    
    // PURPOSE: Pre-constructs the type to WEAPON for the hierarchy.
    public WeaponItem(String name, int power, int accuracy, String attackName, String description) {
        super(name, ItemType.WEAPON, power, description);
        this.accuracy = accuracy;
        this.attackName = attackName;
    }
    public int getAccuracy() { return accuracy; }
    public String getAttackName() { return attackName; }
    
    // PURPOSE: Uses the specific weapon's stats against an enemy's defense rating to establish damage.
    public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy == null) return Math.max(1, getPower());
        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
    public boolean allowsDarkRoomEntry() { return false; }
}

/**
 * Abstract sub-class for worn items. These are inert/un-usable, providing passive boons.
 */
abstract class PassiveGearItem extends GameItem {
    private final int passiveAttackBonus;
    private final int passiveDefenseBonus;
    private final boolean parryEnabled;
    
    // PURPOSE: Constructors setting up static defensive blocks.
    public PassiveGearItem(String name, String description, int passiveAttackBonus, int passiveDefenseBonus, boolean parryEnabled) {
        super(name, ItemType.SPECIAL, 0, description);
        this.passiveAttackBonus = passiveAttackBonus;
        this.passiveDefenseBonus = passiveDefenseBonus;
        this.parryEnabled = parryEnabled;
    }
    @Override public boolean isPassive() { return true; }
    @Override public int getPassiveAttackBonus() { return passiveAttackBonus; }
    @Override public int getPassiveDefenseBonus() { return passiveDefenseBonus; }
    @Override public boolean grantsParry() { return parryEnabled; }
}

// ----------------- CONCRETE ITEM CLASSES (Usable override instances) ----------------- //

class SmallPotionItem extends GameItem {
    public SmallPotionItem() { super("Small Potion", ItemType.HEALING, 15, "Heals 15 HP. Can be used in or out of battle."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean canUseOutsideBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        int before = player.getHp();
        player.heal(15);
        System.out.println("You use " + getName() + " and restore " + (player.getHp() - before) + " HP.");
        return true;
    }
}

class BigPotionItem extends GameItem {
    public BigPotionItem() { super("Big Potion", ItemType.HEALING, 40, "Heals 40 HP. Can be used in or out of battle."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean canUseOutsideBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        int before = player.getHp();
        player.heal(40);
        System.out.println("Used Big Potion. Restored " + (player.getHp() - before) + " HP.");
        return true;
    }
}

class ScrollOfEscapeItem extends GameItem {
    public ScrollOfEscapeItem() { super("Scroll of Escape", ItemType.SPECIAL, 0, "Use during battle to escape back to the previous room."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        if (context == null || !context.isInBattle()) { System.out.println("Scroll of Escape can only be used during battle."); return false; }
        if (context.getPreviousRoomId() == null) { System.out.println("No previous room exists."); return false; }
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
    public ScrollOfFireballItem() { super("Scroll of Fireball", ItemType.SPECIAL, 25, "Deals 25 damage to the current enemy during battle."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        if (context == null || !context.isInBattle() || context.getCurrentEnemy() == null) { System.out.println("Scroll of Fireball can only be used during battle."); return false; }
        context.getCurrentEnemy().takeDamage(25);
        System.out.println("Fireball hits " + context.getCurrentEnemy().getName() + " for 25 damage.");
        return true;
    }
}

class ScrollOfStealthItem extends GameItem {
    public ScrollOfStealthItem() { super("Scroll of Stealth", ItemType.SPECIAL, 0, "Use before battle to sneak past the next non-boss fight."); }
    @Override public boolean canUseOutsideBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        if (context == null) { System.out.println("Stealth cannot be applied without room context."); return false; }
        if (context.isInBattle()) { System.out.println("Scroll of Stealth must be used before battle starts."); return false; }
        if (context.getCurrentEnemy() != null && context.getCurrentEnemy().getType() == EnemyType.SNAKE) { System.out.println("Scroll of Stealth cannot bypass a boss encounter."); return false; }
        context.setStealthActive(true);
        context.setEncounterBypassed(true);
        System.out.println("Stealth activated. You may bypass the next non-boss encounter.");
        return true;
    }
}

class ScrollOfLifeStealItem extends GameItem {
    public ScrollOfLifeStealItem() { super("Scroll of Life Steal", ItemType.SPECIAL, 15, "Deals 15 damage to the current enemy and restores 15 HP to the hero during battle."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        if (context == null || !context.isInBattle() || context.getCurrentEnemy() == null) { System.out.println("Scroll of Life Steal can only be used during battle."); return false; }
        Enemy enemy = context.getCurrentEnemy();
        int enemyHpBefore = enemy.getHp();
        enemy.takeDamage(15);
        int playerHpBefore = player.getHp();
        player.heal(15);
        System.out.println("Dark energy drains " + (enemyHpBefore - enemy.getHp()) + " HP from " + enemy.getName() + " and restores " + (player.getHp() - playerHpBefore) + " HP to the hero.");
        return true;
    }
}

class ScrollOfLevelUpItem extends GameItem {
    public ScrollOfLevelUpItem() { super("Scroll of Level Up", ItemType.SPECIAL, 70, "The hero reads the scroll and gains 70 experience points immediately."); }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean canUseOutsideBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        player.addXp(70);
        player.checkLevelUp();
        System.out.println("The hero read the scroll and gained 70 experience points.");
        return true;
    }
}

class DragonEggItem extends GameItem {
    public DragonEggItem() {
        super("Dragon Egg", ItemType.SPECIAL, 0, "Real dragon treasure, can be sold with extreme value.");
    }
    @Override public boolean canUseInBattle() { return false; }
    @Override public boolean canUseOutsideBattle() { return false; }
}

class StickItem extends WeaponItem {
    public StickItem() { super("Stick", 2, 95, "Stick Attack", "Wooden Stick, hardly can be called a weapon. Weak but reliable."); }
    @Override public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy != null && enemy.getType() == EnemyType.SLIME) return 2;
        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
}

class SwordItem extends WeaponItem {
    public SwordItem() { super("Sword", 8, 90, "Sword Slash", "Hilt & iron blade, every knight's best argument."); }
}

class GreatSwordItem extends WeaponItem {
    public GreatSwordItem() { super("Great Sword", 12, 60, "Great Sword Slash", "A very heavy sword yet more effective. Accuracy scales with level."); }
    @Override public int getAccuracy() { return 60; }
    public int getAccuracy(Player player) {
        if (player == null) return 60;
        int level = player.getLevel();
        if (level < 5) return 60;
        if (level <= 10) return 80;
        return 90;
    }
}

class TorchItem extends WeaponItem {
    public TorchItem() { super("Torch", 4, 80, "Torch", "Weapon and utility item. Strong against slime and useful for dark rooms."); }
    @Override public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy != null && enemy.getType() == EnemyType.SLIME) return Math.max(10, getPower() + player.getAttack() - enemy.getDefense() + 6);
        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
    @Override public boolean allowsDarkRoomEntry() { return true; }
}

class DecayedArmorItem extends PassiveGearItem {
    public DecayedArmorItem() { super("Decayed Armor", "Worn-out armor that still offers some protection. Passive +3 Defense while carried.", 0, 3, false); }
}

class ArmorItem extends PassiveGearItem {
    public ArmorItem() { super("Armor", "Covers most vital body parts, knight's best defense. Passive +6 Defense while carried.", 0, 6, false); }
}

class ShieldItem extends PassiveGearItem {
    public ShieldItem() { super("Shield", "Round, one hand small shield. Passive +3 Defense while carried. Allows to Parry some attacks.", 0, 3, true); }
}

class DragonScaleShieldItem extends PassiveGearItem {
    public DragonScaleShieldItem() { super("Dragon Scale", "A dragon scale from defeated dragon can be used as shield. Passive +8 Defense while carried. Allows to Parry some attacks.", 0, 8, true); }
}

class KeyItem extends GameItem {
    private final String keyId;
    public KeyItem(String keyId, String displayName) {
        super(displayName, ItemType.SPECIAL, 0, "Used automatically when checking locked doors. Key ID: " + keyId);
        this.keyId = keyId;
    }
    public String getKeyId() { return keyId; }
}

class CatItem extends GameItem {
    public CatItem() {
        super("Cat", ItemType.SPECIAL, 4, "A fluffy dungeon cat. It meows softly.");
    }
    @Override public boolean canUseInBattle() { return true; }
    @Override public boolean canUseOutsideBattle() { return true; }
    @Override public boolean use(Player player, ItemContext context) {
        if (context.isInBattle() && context.getCurrentEnemy() != null) {
            System.out.println("You toss the Cat at the " + context.getCurrentEnemy().getName() + "!");
            context.getCurrentEnemy().takeDamage(4);
            System.out.println("The Cat viciously scratches the enemy for 4 damage... and then runs away into the darkness!");
            context.setEscapedByItem(false); 
            return true; 
        } else {
            ImageGallery.reveal("Cat");
            System.out.println("You pet the Cat. It purrs loudly. You feel a tiny bit better. (+1 HP)");
            player.heal(1);
            return false; 
        }
    }
}

// [TAG: SECTION_8 ] - Monster Factory (Bestiary)
/* =========================================================================================
   Generates Enemy objects, detailing their moves, health, defenses, and rewards.
   ========================================================================================= */

/**
 * A static factory containing all enemy definitions to keep the WorldBuilder clean.
 */
final class Bestiary {
    private static final Random random = new Random();

    private Bestiary() {}

    // PURPOSE: Constructors for individual enemy types providing scaling challenges.
    public static Enemy bat() {
        return new Enemy(EnemyType.BAT, "Bats", 20, 4, 1, 7, randomGold(2, 8),
                new EnemyMove("Bite", 1, 84, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wing Buffet", 2, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Screech", 1, 78, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy goblin() {
        return new Enemy(EnemyType.GOBLIN, "Goblin", 30, 5, 2, 10, randomGold(8, 16),
                new EnemyMove("Knife Stab", 2, 80, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wild Swing", 4, 58, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pocket Sand", 0, 52, MoveType.STATUS, false, MoveEffect.POCKET_SAND, 50, 0));
    }

    public static Enemy wolf() {
        return new Enemy(EnemyType.WOLF, "Wolf", 35, 6, 2, 13, randomGold(8, 14),
                new EnemyMove("Snap Bite", 2, 82, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Lunge", 4, 68, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pounce", 5, 56, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0));
    }

    public static Enemy slime() {
        return new Enemy(EnemyType.SLIME, "Slime", 60, 4, 5, 20, randomGold(20, 50),
                new EnemyMove("Slam", 2, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Body Press", 4, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Acid Splash", 3, 70, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy skeleton() {
        return new Enemy(EnemyType.SKELETON, "Skeleton", 45, 7, 4, 17, randomGold(12, 20),
                new EnemyMove("Rusty Slash", 3, 78, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Shield Bash", 5, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Bone Rattle", 1, 74, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy orc() {
        return new Enemy(EnemyType.ORC, "Orc", 50, 8, 3, 19, randomGold(16, 28),
                new EnemyMove("Axe Chop", 4, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Cleaver Drop", 6, 58, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("War Cry", 2, 80, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy ogre() {
        return new Enemy(EnemyType.OGRE, "Ogre", 75, 11, 4, 30, randomGold(24, 40),
                new EnemyMove("Club Smash", 7, 60, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Backhand", 4, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Ground Stomp", 3, 82, MoveType.STATUS, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy caveTroll() {
        return new Enemy(EnemyType.CAVE_TROLL, "Cave Troll", 90, 12, 6, 36, randomGold(30, 48),
                new EnemyMove("Maul", 7, 66, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Crushing Grab", 5, 62, MoveType.GRAPPLE, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Regenerate", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 8));
    }

    public static Enemy giantSnake() {
        return new Enemy(EnemyType.SNAKE, "Giant Snake", 55, 8, 3, 42, randomGold(30, 50),
                new EnemyMove("Bite", 3, 78, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tail Lash", 5, 54, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Constrict", 2, 60, MoveType.GRAPPLE, false, MoveEffect.CONSTRICT, 100, 0));
    }

    public static Enemy necromancer() {
        return new Enemy(EnemyType.NECROMANCER, "Necromancer", 120, 13, 7, 80, 0,
                new EnemyMove("Bone Spear", 7, 84, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Grave Lash", 5, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Soul Drain", 5, 72, MoveType.MAGIC, false, MoveEffect.SOUL_DRAIN, 100, 5),
                new EnemyMove("Raise Dead", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 10),
                new EnemyMove("Black Flame", 8, 64, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy golem() {
        return new Enemy(EnemyType.GOLEM, "Golem", 80, 12, 8, 40, randomGold(30, 50),
                new EnemyMove("Granite Fist", 8, 75, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Earthquake", 4, 90, MoveType.NORMAL, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy mimic() {
        return new Enemy(EnemyType.MIMIC, "Mimic", 60, 14, 2, 45, randomGold(50, 80),
                new EnemyMove("Chomp", 10, 85, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tongue Lash", 6, 70, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0));
    }

    public static Enemy dragon() {
        return new Enemy(EnemyType.DRAGON, "Dragon", 150, 20, 5, 100, 100,
                new EnemyMove("Inferno Breath", 15, 45, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tail Sweep", 10, 50, MoveType.HEAVY, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Claw", 5, 70, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0));
    }

    // PURPOSE: Localizes the gold dropping logic for monsters.
    private static int randomGold(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

/**
 * Concrete representation of an active threat in a room. Manages health and turn options.
 */
class Enemy {
    private final EnemyType type;
    private final String name;
    private final int maxHp, attack, defense, xpReward, goldReward;
    private final ArrayList<EnemyMove> moves;
    private int hp;
    private boolean defeated;

    // PURPOSE: Constructs an enemy and populates its internal array of possible attacks.
    public Enemy(EnemyType type, String name, int hp, int attack, int defense, int xpReward, int goldReward, EnemyMove... moves) {
        this.type = type; this.name = name; this.maxHp = hp; this.hp = hp;
        this.attack = attack; this.defense = defense; this.xpReward = xpReward; this.goldReward = goldReward;
        this.defeated = false;
        this.moves = new ArrayList<>();
        Collections.addAll(this.moves, moves);
    }

    public EnemyMove chooseMove(Random random) { return moves.get(random.nextInt(moves.size())); }
    public void takeDamage(int amount) { hp = Math.max(0, hp - amount); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public boolean isAlive() { return hp > 0; }

    public EnemyType getType() { return type; }
    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getXpReward() { return xpReward; }
    public int getGoldReward() { return goldReward; }
    public boolean isDefeated() { return defeated; }
    public void setDefeated(boolean defeated) { this.defeated = defeated; }
}

/**
 * The blueprint for an attack utilized by an enemy. Holds logic for damage type and side-effects.
 */
class EnemyMove {
    private final String name;
    private final int power, accuracy, effectChance, effectPower;
    private final MoveType moveType;
    private final boolean parryable;
    private final MoveEffect effect;

    // PURPOSE: Constructs an enemy move, defining if it triggers a status effect or is blockable.
    public EnemyMove(String name, int power, int accuracy, MoveType moveType, boolean parryable, MoveEffect effect, int effectChance, int effectPower) {
        this.name = name; this.power = power; this.accuracy = accuracy;
        this.moveType = moveType; this.parryable = parryable;
        this.effect = effect; this.effectChance = effectChance; this.effectPower = effectPower;
    }

    public String getName() { return name; }
    public int getPower() { return power; }
    public int getAccuracy() { return accuracy; }
    public MoveType getMoveType() { return moveType; }
    public boolean isParryable() { return parryable; }
    public MoveEffect getEffect() { return effect; }
    public int getEffectChance() { return effectChance; }
    public int getEffectPower() { return effectPower; }
}

enum EnemyType { BAT, GOBLIN, WOLF, SLIME, SKELETON, ORC, OGRE, CAVE_TROLL, SNAKE, NECROMANCER, GOLEM, MIMIC, DRAGON }
enum MoveType { LIGHT, NORMAL, HEAVY, GRAPPLE, STATUS, MAGIC }
enum MoveEffect { NONE, POCKET_SAND, CONSTRICT, SELF_HEAL, SOUL_DRAIN }

// [TAG: SECTION_9 ] - Broker of Souls (MerchantManager)
/* =========================================================================================
   Handles logic for the in-game economy, allowing the player to convert gold to items 
   and vice versa, tracking depleted stock.
   ========================================================================================= */

/**
 * Handles interactions with the Goblin Merchant, processing transactions and inventory management.
 */
class MerchantManager {
    // The Merchant's memory of unique items sold
    private final Set<String> depletedStock = new HashSet<>();

    // PURPOSE: Serves as the primary entry point for opening the merchant interaction sequence.
    public void handleMerchant(Player player, Room room) {
        System.out.println("\n--- THE WANDERING BAZAAR ---");
        ImageGallery.reveal("Goblin"); 
        System.out.println("The Goblin grins, revealing a mouth of jagged, golden teeth.");
        System.out.println("\"Ah, a traveler! I have many shiny things... Or perhaps you have shiny things for me?\"");
        
        while (true) {
            System.out.println("\nGold: " + player.getGold());
            System.out.println("1. Buy");
            System.out.println("2. Sell");
            System.out.println("0. Leave");
            System.out.print("Choose: ");
            String answer = DarkHolds.input.nextLine().trim();
            
            if (answer.equals("1")) {
                buyMenu(player, room);
            } else if (answer.equals("2")) {
                sellMenu(player);
            } else if (answer.equals("0")) {
                System.out.println("\"Safe travels in the dark, friend! Come back if you survive!\"");
                break;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    // PURPOSE: Spawns items dynamically and processes gold subtractions.
    private void buyMenu(Player player, Room room) {
        boolean isFloor3 = room.getId().startsWith("f3");
        
        while (true) {
            List<GameItem> wares = new ArrayList<>();
            List<Integer> prices = new ArrayList<>();
            
            wares.add((GameItem) Items.smallPotion()); prices.add(20);
            wares.add((GameItem) Items.torch());       prices.add(15);
            wares.add((GameItem) Items.scrollOfEscape()); prices.add(30);
            
            if (!depletedStock.contains("Sword")) { wares.add((GameItem) Items.sword()); prices.add(40); }
            if (!depletedStock.contains("Shield")) { wares.add((GameItem) Items.shield()); prices.add(50); }
            
            if (isFloor3) {
                wares.add((GameItem) Items.bigPotion());   prices.add(50);
                wares.add((GameItem) Items.scrollOfFireball()); prices.add(60);
                if (!depletedStock.contains("Great Sword")) { wares.add((GameItem) Items.greatSword()); prices.add(85); }
                if (!depletedStock.contains("Armor")) { wares.add((GameItem) Items.armor()); prices.add(100); }
            }
            
            System.out.println("\n--- BUY ---");
            System.out.println("Your Gold: " + player.getGold());
            for (int i = 0; i < wares.size(); i++) {
                System.out.println((i + 1) + ". " + wares.get(i).getName() + " - " + prices.get(i) + "g (" + wares.get(i).getDescription() + ")");
            }
            System.out.println("0. Back");
            System.out.print("Choose item to buy: ");
            String answer = DarkHolds.input.nextLine().trim();
            if (answer.equals("0")) break;
            
            // PROFESSOR REQUIREMENT: Exception Handling preventing crash on bad merchant input.
            try {
                int choice = Integer.parseInt(answer) - 1;
                if (choice >= 0 && choice < wares.size()) {
                    int cost = prices.get(choice);
                    if (player.getGold() >= cost) {
                        GameItem boughtItem = wares.get(choice);
                        player.addGold(-cost);
                        player.addItem(boughtItem);
                        System.out.println("You bought the " + boughtItem.getName() + " for " + cost + " gold.");
                        
                        if (boughtItem instanceof WeaponItem || boughtItem instanceof PassiveGearItem) {
                            depletedStock.add(boughtItem.getName());
                        }
                    } else {
                        System.out.println("\"You're too poor for that one!\"");
                    }
                } else {
                    System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    // PURPOSE: Calculates raw value of player inventory items and processes gold addition/item deletion.
    private void sellMenu(Player player) {
        while (true) {
            System.out.println("\n--- SELL ---");
            System.out.println("Your Gold: " + player.getGold());
            if (player.getInventory().isEmpty()) {
                System.out.println("You have nothing to sell.");
                return;
            }
            
            for (int i = 0; i < player.getInventory().size(); i++) {
                Item item = player.getInventory().get(i);
                int value = getSellValue(item);
                if (value > 0) {
                    System.out.println((i + 1) + ". " + item.getName() + " - Offers " + value + "g");
                } else {
                    System.out.println((i + 1) + ". " + item.getName() + " - Refused");
                }
            }
            System.out.println("0. Back");
            System.out.print("Choose item to sell: ");
            String answer = DarkHolds.input.nextLine().trim();
            if (answer.equals("0")) break;
            
            // PROFESSOR REQUIREMENT: Exception Handling preventing crash on bad merchant input.
            try {
                int choice = Integer.parseInt(answer) - 1;
                if (choice >= 0 && choice < player.getInventory().size()) {
                    Item item = player.getInventory().get(choice);
                    if (item.getName().equalsIgnoreCase("Stick")) {
                        System.out.println("\"That's a pretty nice stick, but no, I don't want to buy it.\"");
                    } else if (item.getName().equalsIgnoreCase("Cat")) {
                        System.out.println("\"Hmm... that looks tasty,\" the Goblin mutters, drooling slightly.");
                        player.addGold(10);
                        player.removeItem(choice);
                        System.out.println("You sold the Cat for 10 gold. You monster.");
                    } else if (item instanceof KeyItem) {
                        System.out.println("\"I don't touch keys. I'm a merchant, not a jailer!\"");
                    } else {
                        int value = getSellValue(item);
                        if (value > 0) {
                            player.addGold(value);
                            player.removeItem(choice);
                            System.out.println("You sold the " + item.getName() + " for " + value + " gold.");
                        }
                    }
                } else {
                    System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
            }
        }
    }
    
    // PURPOSE: The hardcoded appraisal values for every item type.
    private int getSellValue(Item item) {
        String name = item.getName().toLowerCase();
        if (name.contains("stick") || item instanceof KeyItem) return 0;
        if (name.contains("cat")) return 10;
        if (name.contains("torch") || name.contains("small potion") || name.contains("decayed armor")) return 10;
        if (name.contains("sword") && !name.contains("great")) return 20;
        if (name.contains("shield") && !name.contains("dragon")) return 20;
        if (name.contains("big potion")) return 25;
        if (name.contains("scroll")) return 30;
        if (name.contains("great sword") || name.contains("armor") || name.contains("dragon scale")) return 30;
        if (name.contains("ancient relic")) return 50;
        return 5; 
    }
}

// [TAG: SECTION_10 ] - Phantom Gallery (ImageGallery)
/* =========================================================================================
   Stores massive multiline ASCII strings separated from the main code. 
   Outputs terminal art via static lookups to preserve class cleanliness.
   ========================================================================================= */

/**
 * A utility database to serve ASCII graphical elements when an entity's True Name is invoked.
 */
class ImageGallery {
    private static final Map<String, String> gallery = new HashMap<>();

    static {
        gallery.put("Cat", """
 ,_     _
 |\\\\_,-~/
 / _  _ |    ,--.
(  @  @ )   / ,-'
 \\\\  _T_/-._( (
 /         `. \\\\
|          _  \\\\ |
 \\\\ \\\\ ,  /      |
  || |-_\\\\__   /
 ((_/`(____,-'
""");

        gallery.put("Goblin", """
          ,      ,
      /(.-""-.)\\
 |\\  \\/     \\/   /|
 | \\ / =.  .= \\  / |
  \\ \\    o\\/o  \\/  /
   \\_, '-/  \\-' ,_/
     /   \\__/   \\
     \\ \\__/\\__/ /
   ___\\ \\|--|/ /___
 /`    \\      /    `\\
/       '----'       \\
""");

        gallery.put("Cave Troll", """
                  .::::::::..
                .::''''''''''::.
               .::  XXXXXXXX  ::.
              .::  [  o  o  ]  ::.
              :::.  \\  --  /  .:::
              '::::. '----' .::::'
              ___/::::::::::::::\\___
          .-'  |     ######     |  '-.
         /    / \\    ######    / \\  \\
        |    |   \\   ######   /   |    |
        |    |    \\  ######  /    |    |
        |    |     \\ ###### /     |    |
        |    |      \\######/      |    |
         \\    \\      '----'      /    /
          '-._ \\       ||||      / _.-'
               \\ \\     ||||     / /
                \\ \\    ||||    / /
                 \\_\\  /####\\  /_/
                 [###|      |###]
                 '---'      '---'
""");

        gallery.put("Skeleton", """
               :.                
              :#*=:              
      :=.      +-#.              
      -%=.  ..-=#+.==:           
       =-  -+#%%#+#+:=-          
        *-.  #@%=++++*=          
        ..   %@#++#+:+.          
             +*=@-+-:=           
             .:.%..  =.          
             -@%%#@* :           
             :%@%#%=  .:         
            -##-:::+: .-:        
            :-#    := .%=        
            +.=     +  =+        
           --       =.  ::       
          -=:       -* -.=.      
          +-        .* #- *-+    
         .+          # =@#*+     
         =-          #  -:       
       ..#.           :@#:       
       :** .:=-        
      :##=                       
      .+=   
""");

        gallery.put("Bats", """
         /\\                 /\\
        / \\'._  (\\_/)   _.'/ \\
       /_.''._'--('.')--'_.''._\\
       | \\_ / `;=/ " \\=;` \\ _/ |
        \\/ `\\__|`\\___/`|__/`\\/
                \\(/|\\)/
""");

        gallery.put("Ogre", """
            .=-::.              
          .+%+:.=*: ..          
        :-#@@%*+=#=-:..         
        :*=#@@%**#%+-.::...      
       .+%%%%@@@%#=::**---::     
     .==%*@%++*%+:::+@@+-*--=    
     -*=*%%  ##***+-+*@==%+=+    
     :++*=.  #%#+****##* .+-=    
    .+*=    .%@%%%%@@*=*-=*--:   
   .+*+*-   =@@@%##%%++#*-+*** 
           .=%@@:    #%%         
        :+*##*###=--+%%%%+: 
""");

        gallery.put("Orc", """
                  .--:           
              .::-+++::          
                =+#%#@#+         
               .+#%@@@@@*:       
              =#@@@@@.@%+=-.     
             :#%%%%@.%#%%%%#.    
             -%@@@@@%#@@@@@%%.   
              *@@%@@%%@@@@@@@+   
              :%#*@##@@@@*%@%#=  
               ##*##@@@@:  #@#*- 
               #@%##%%@@=  #%%%- 
              .#@@@@@@@@#  -@@@. 
    -:...     #@@.@%%@..@. -@.=  
   :@@@@%*+++*@...@%%%@@@=.#..=  
   +@#=   :  =@...@@@@...##@@@##%
 :*@@=-+*=. :%@..@@@%%%@@@+**.   
 -*+#++:.   #@...@@@@@@@@@.      
           =@.....@%.@..@@+      
           #.....@*#@.@@@@@.     
           #....@-=#@%%@...-     
           %....# +%#*@@...@-    
           +....#     #.....@    
           :@..@:     .*......   
           .@...        %....    
          :%....        .*..@.   
         =#%@%* #..=   
         -=-:              #..%:  
                          +%%%#. 
                          .#@%#.
""");

        gallery.put("Slime", """
         _________
        /           \\
       /   I    I     \\
      |               |
      |               |
       \\   \\____/    /
        \\___________/
""");

        gallery.put("Necromancer", """
                    ____ 
                  .'* *.'
               __/_*_*(_
              / _______ \\
             _\\_)/___\\(_/_
            / _((\\- -/))_ \\
            \\ \\())(-)(()/ /
             ' \\(((()))/ '
            / ' \\)).))/ ' \\
           / _ \\ - | - /_  \\
          (   ( .;''';. .'  )
          _\\\"__ /    )\\ __\"/_
            \\/  \\   ' /  \\/
             .'  '...' ' )
              / /  |  \\ \\
             / .   .   . \\
            /   .     .   \\
           /   /   |   \\   \\
         .'   /    b    '.  '.
     _.-'    /     Bb     '-. '-._ 
 _.-'       |      BBb       '-.  '-. 
(___________\\____.dBBBb.________)____)
""");

        gallery.put("Giant Snake", """
            ____
           /  __\\
           | /
           | |
    _______| |
   /  _______|
  /  /
  \\  \\__
   \\____\\
""");

        gallery.put("Iron Key", """
O--====--++--
           ||
""");
        gallery.put("Ancient Relic", gallery.get("Iron Key"));

        gallery.put("Small Potion", """
        _|_
       |___|
      / ~ ~ \\
     |___*___|
      \\_____/
""");
        gallery.put("Big Potion", gallery.get("Small Potion"));

        gallery.put("Scroll of Escape", """
         _________
        /        /
       /        /
      / '' ''  /
     /        /
    /________/
""");

        gallery.put("Stick", """
          |
          | /
          |/
       \\ |
        \\|
          |
""");

        gallery.put("Sword", """
          /\\
         /  \\
         ||||
         ||||
         ||||
         ||||
         ||||
         ||||
        /____\\
          ||
          ||
""");
        gallery.put("Great Sword", gallery.get("Sword"));

        gallery.put("Armor", """
                .=*=       
       --+#%@@@*+=+#*.     
     -*%@%%%%%#*:..-*#=    
    -%###*#####*++-=+*#-   
    *%###*+++#%%##*#*--*:  
   -%@#%#*++#*+*##%@#++##: 
  .%@.%##**%*++**%@@@#%##*:
  -@@@@%##@*==##%@@+.#@%#%%
  #@@@###%%**+%%%@@-  +@%%@
 =@@@% =@@%%%#%@@@@:  -#==+
 %%@@= =@@@@@%@@@@@.  -*==#
 %@* *@@@@@%@@@@@-  -%*#+
 %@#  :@%@%%#####*#+  :%#@:
 %%-  #@%@@#**####%%- :@%# 
 @#: .@@%@@@#*###%%%#*#%%= 
 **. -@@%%%@#++++***#+-%#: 
     =%@@=.%@@%@@@@%*-+#-  
           .:::..    .:. 
""");
        gallery.put("Decayed Armor", gallery.get("Armor"));

        gallery.put("Shield", """
        _______
       /       \\
      /         \\
     |   _____   |
     |  |     |  |
     |  |_____|  |
      \\         /
       \\_______/
""");
        gallery.put("Dragon Scale Shield", gallery.get("Shield"));

        gallery.put("Torch", """
          ( )
         ( _ )
          \\_/
           |
           |
           |
           |
""");

        gallery.put("Scroll of Fireball", """
         _________
        /        /
       / ( )    /
      / (   )  /
     /  \\_/  /
    /________/
""");

        String blankScroll = """
         _________
        /        /
       /        /
      /        /
     /        /
    /________/
""";
        gallery.put("Scroll of Stealth", blankScroll);
        gallery.put("Scroll of Life Steal", blankScroll);
        gallery.put("Scroll of Level Up", blankScroll);
        
        gallery.put("Golem", """
               :.                 
            :+%*+::- ::.          
       .+*+*@@@*-*@*%+=:.        
       %%%%#%%###=-:-##+-:.      
      -@@@@@##+*#+=-:+@%#+=      
     =@@%@.@@@%**#*++@.%+-.=..   
   :+@@@@@.%%@@%#%#=*@=@@#*+*:   
  :%*#@@..* #.@%#%%@=  @@@*-:.. 
  %%#*%%@.: +@@@%*+@*-. +@%*#*+- 
 .@@%#%@@= =%#%@@%*%#*+ :#%*.-+* 
  :@%*%%+ .@%*+:   #@%#- *#@@#.   
   =%@%*: :@%*+* :@@%#=.  .      
          -@%%#%  .@.@%*:        
          :@#%%* +@@#=.        
        .++*+:-* #%=-+:.       
        ++=#=--=   +@*:*#=: 
""");

        gallery.put("Mimic", """
                                              
                                              
                                              
      ..:-----::-++*#+-::-=+:                  
    .=***=#@%##%%%%%%@%%%@@%##+-               
   .+###**-%@####%#**###%#%%@@#%-              
   +##****++%@%**++#-+#-*:+#+%##+              
  :*#*****=-*%%%=+==*:+:*+#*+*#**:             
  .*####*=+%@@@@#:%:+**%@@@@@%##@-      :::    
   :+#*++#%@.@@#-=%@%...@@@@@%##:       +:=+.  
    -**%@%*%@@#*%.....@@@%####+=.       .+-##  
    :*@@@#**%@@.@#...@@@@%%####**=       .**%- 
    .-***+==--+#*++*+%@@@@@@@@%##%*:      :#%* 
      +#%#****-*@@###=%*#=##*@@%*%**+=+*+=+*%@. 
     =*###***:*@@@%%@%%@%@@@@@@@%: :=**###%*.  
     +*#****+:#@%%@@@@@@@@@@@@@@@:      ..     
     =*###**+:%@%%%%%@@@%@@@@@@@@:
     +*####*+:%@%%%@@@%##%@@@@@@@=
    .+#####*+-@@@@@@@#***#%@@@@@#*.
    =***++==-=@@@@@@%#*+*#%@..@@@@@@@%*=.
     ...:::::+**++++=====---::::...
""");

        gallery.put("Dragon", """
                                
                      *++++=-:  
    .:-=+***.        :#*#*++=-. 
 :-++++*+*+*+:.::.-*%*+=*-:.   
      ----+=:-*=+#%@#*-.::=     
         :.    .+#@%#.          
                -.*++#-         
                  .   :-:.      
                                
""");
    }

    // PURPOSE: Accesses the internal dictionary to print the relevant ASCII art to the console.
    public static void reveal(String trueName) {
        if (gallery.containsKey(trueName)) {
            System.out.println(gallery.get(trueName));
        }
    }
}