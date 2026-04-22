import java.util.*;

/**
 * Dark Holds - Floor 1, 2 & 3 Prototype (External Banks Build)
 * ----------------------------------------------
 * Uses the exact uploaded item bank unchanged and a separate bestiary bank so the
 * main prototype can stay focused on game flow, room structure, combat flow,
 * player state, and room state.
 */
public class DarkHoldsFloor1PrototypeMasterBuild {

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

    private void introStory() {
        System.out.println("========================================");
        System.out.println("           DARK HOLDS");
        System.out.println("   FLOOR 1, 2 & 3 PROTOTYPE MODULAR BUILD");
        System.out.println("========================================");
        System.out.println("You were once an ordinary villager with no great battle experience.");
        System.out.println("While traveling near ancient ruins, you stepped on a hidden teleportation trap.");
        System.out.println("The world twisted around you, and now you wake in a buried labyrinth.");
        System.out.println("Somewhere below, a giant serpent coils around stolen treasure.");
        System.out.println("If you want to live, you must explore, find a key, and push deeper.");
        System.out.println();
        System.out.print("Enter your hero's name: ");
        String name = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (name.isEmpty()) {
            name = "Hero";
        }

        player = new Player(name);
    }

    private void setupGame() {
        rooms = WorldBuilder.createFloorOne(random);
        
        // Generate Floor 2 and Floor 3
        List<Room> f2Rooms = WorldBuilder.createFloorTwo(random);
        List<Room> f3Rooms = WorldBuilder.createFloorThree(random);
        
        // Create the connecting stairs room (F1 to F2)
        Room stairs1 = new Room("f1_stairs", "Stairs to Floor 2",
                "A spiral stone staircase leading down into the darkness of the second floor. The heavy stone door shuts and locks firmly behind you.",
                false, false);
        
        // Link F1 boss to the stairs, then stairs to the first F2 room (One-way)
        Room f1Boss = rooms.get("f1snakeBoss");
        f1Boss.setClearedDescription("The serpent lair is still and quiet now. A heavy stone door has opened, revealing a staircase.");
        f1Boss.addNeighbor(stairs1.getId());
        stairs1.addNeighbor(f2Rooms.get(0).getId());

        // Create the connecting stairs room (F2 to F3)
        Room stairs2 = new Room("f2_stairs", "Stairs to Floor 3",
                "A deep, echoing stairwell descends further into the dark holds. The path crumbles behind you, sealing the way back.",
                false, false);
        
        // Link F2 boss to the stairs, then stairs to the first F3 room (One-way)
        Room f2Boss = f2Rooms.get(f2Rooms.size() - 1); // f2_golem_rune is the last room
        f2Boss.addNeighbor(stairs2.getId());
        stairs2.addNeighbor(f3Rooms.get(0).getId());

        // Add the new rooms to the map
        rooms.put(stairs1.getId(), stairs1);
        for (Room r : f2Rooms) {
            rooms.put(r.getId(), r);
        }
        rooms.put(stairs2.getId(), stairs2);
        for (Room r : f3Rooms) {
            rooms.put(r.getId(), r);
        }

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
            if (room.isKeyEligible() && room.getId().startsWith("f1")) {
                keyEligibleRooms.add(room);
            }

            if (room.isPreLock() && !room.getId().equals("f1safe")) {
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

        Room hoard = rooms.get("f1snakeBoss");
        hoard.addRewardItem(DarkHoldsItems.ancientRelic());
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
            
            if (current.hasKeyReward() && !current.isRewardsCollected()) {
                System.out.println();
                System.out.println(">> You spot an Iron Key resting somewhere in this room! <<");
            }
            
            showPlayerStatus();

            if (current.isPuzzleRoom() && !current.isPuzzleSolved() && !current.hasEnemy()) {
                if (current.getId().equals("f1p_weighingScales")) {
                    handleScalesPuzzle(current);
                } else if (current.getId().equals("f1p_brokenFork")) {
                    handleBrokenForkPuzzle(current);
                } else if (current.getId().equals("f1p_shrine")) {
                    handleShrinePuzzle(current);
                } else if (current.getId().equals("f2p_totem")) {
                    handleTotemPuzzle(current);
                } else if (current.getId().equals("f2p_weepingstatue")) {
                    handleWeepingStatue(current);
                } else if (current.getId().equals("f2p_campsite")) {
                    handleCampsite(current);
                } else if (current.getId().equals("f2p_wishwell")) {
                    handleWishWell(current);
                } else if (current.getId().equals("f3p_silentlibrary")) {
                    handleSilentLibrary(current);
                } else if (current.getId().equals("f3p_colorpillars")) {
                    handleColorPillars(current);
                }

                if (!player.isAlive()) {
                    continue;
                }
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

                if (result == BattleResult.ESCAPED) {
                    continue;
                }
            }

            revealRoomRewards(current);

            // Victory is triggered at the end of floor 3
            if (current.getId().equals("f3_end")) {
                victory();
                return;
            }

            handleRoomChoices(current);
        }
    }

    private void handleScalesPuzzle(Room room) {
        System.out.println();
        System.out.println("The scale waits for an offering to balance the path.");
        System.out.println("1. Offer 20 gold");
        System.out.println("2. Offer an item from your inventory");
        System.out.println("3. Ignore it and try to walk past");
        System.out.print("Choose: ");

        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            if (player.getGold() >= 20) {
                player.addGold(-20);
                System.out.println("You place 20 gold on the scale. It balances perfectly! The path is safe.");
                room.setPuzzleSolved(true);
            } else {
                System.out.println("You don't have enough gold! The scales tip violently, triggering a trap door!");
                room.setEnemy(DarkHoldsBestiary.goblin());
                room.setPuzzleSolved(true); 
            }
        } else if (answer.equals("2")) {
            if (player.getInventory().isEmpty()) {
                System.out.println("You have no items to offer! The scales tip violently, triggering a trap door!");
                room.setEnemy(DarkHoldsBestiary.goblin());
                room.setPuzzleSolved(true); 
            } else {
                System.out.println("Choose an item to offer:");
                for (int i = 0; i < player.getInventory().size(); i++) {
                    System.out.println((i + 1) + ". " + player.getInventory().get(i).getName());
                }
                System.out.print("Choose item number: ");

                try {
                    int index = Integer.parseInt(DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim()) - 1;
                    if (index >= 0 && index < player.getInventory().size()) {
                        System.out.println("You place the " + player.getInventory().get(index).getName() + " on the scale.");
                        player.removeItem(index);
                        System.out.println("It balances perfectly! The path is safe.");
                        room.setPuzzleSolved(true); 
                    } else {
                        System.out.println("Invalid choice! You hesitate too long and the mechanism snaps, triggering an ambush!");
                        room.setEnemy(DarkHoldsBestiary.goblin());
                        room.setPuzzleSolved(true);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice! You hesitate too long and the mechanism snaps, triggering an ambush!");
                    room.setEnemy(DarkHoldsBestiary.goblin());
                    room.setPuzzleSolved(true);
                }
            }
        } else {
            System.out.println("You try to sneak past, but the floor plates shift beneath your feet. An ambush is sprung!");
            room.setEnemy(DarkHoldsBestiary.goblin());
            room.setPuzzleSolved(true);
        }
    }

    private void handleBrokenForkPuzzle(Room room) {
        System.out.println();
        System.out.println("The cracked masonry features three stone bricks that can be pushed in.");
        System.out.println("1. Push the left brick");
        System.out.println("2. Push the center brick");
        System.out.println("3. Push the right brick");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("2")) {
            System.out.println("A heavy stone slides away, solving the puzzle!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden dart shoots from the wall!");
            player.takeDamage(5);
            System.out.println("You take 5 damage and step back.");
        }
    }

    private void handleShrinePuzzle(Room room) {
        System.out.println();
        System.out.println("The riddle on the altar reads: 'I speak without a mouth and hear without ears. I have nobody, but I come alive with wind. What am I?'");
        System.out.println("1. A ghost");
        System.out.println("2. An echo");
        System.out.println("3. A snake");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("2")) {
            System.out.println("The shrine hums in approval. The puzzle is solved!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden mechanism strikes you!");
            player.takeDamage(5);
            System.out.println("You take 5 damage and the riddle resets.");
        }
    }
    
    private void handleTotemPuzzle(Room room) {
        System.out.println();
        System.out.println("1. Feed the Wolf");
        System.out.println("2. Feed the Bat");
        System.out.println("3. Feed the Snake");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("2")) {
            System.out.println("The portcullis grinds open, solving the puzzle.");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden spring trap shoots a rusty spear!");
            player.takeDamage(8);
            System.out.println("You take 8 damage.");
        }
    }
    
    private void handleWeepingStatue(Room room) {
        System.out.println();
        System.out.println("1. Drink the water");
        System.out.println("2. Ignore it");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            if (random.nextBoolean()) {
                player.increaseBaseAttack(2);
                System.out.println("The oily water burns your throat, but you feel a surge of power! Attack permanently increased by 2.");
            } else {
                player.takeDamage(5);
                System.out.println("The oily water is foul! You take 5 damage.");
            }
            room.setPuzzleSolved(true);
        } else {
            System.out.println("You wisely decide to ignore the strange water.");
            room.setPuzzleSolved(true);
        }
    }

    private void handleCampsite(Room room) {
        System.out.println();
        System.out.println("1. Rest by the cold firepit");
        System.out.println("2. Keep moving");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            System.out.println("You lay out the dusty bedroll and rest for a while, regaining 20 HP.");
            player.heal(20);
        } else {
            System.out.println("You decide not to rest.");
        }
        room.setPuzzleSolved(true);
    }

    private void handleWishWell(Room room) {
        System.out.println();
        System.out.println("1. Toss in 5 gold");
        System.out.println("2. Leave it alone");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            if (player.getGold() >= 5) {
                player.addGold(-5);
                System.out.println("You toss 5 gold into the dark well. It vanishes into the depths...");
                if (random.nextBoolean()) {
                    System.out.println("A small vial inexplicably floats to the surface! You found a Small Potion.");
                    player.addItem(DarkHoldsItems.smallPotion());
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

    private void handleSilentLibrary(Room room) {
        System.out.println();
        System.out.println("1. Search the rotting shelves");
        System.out.println("2. Walk past them");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            System.out.println("You brush away the thick dust and find an intact magical scroll!");
            player.addItem(DarkHoldsItems.scrollOfFireball());
        } else {
            System.out.println("You leave the ancient books undisturbed in their silence.");
        }
        room.setPuzzleSolved(true);
    }

    private void handleColorPillars(Room room) {
        System.out.println();
        System.out.println("1. Touch the Red pillar");
        System.out.println("2. Touch the Blue pillar");
        System.out.println("3. Touch the Yellow pillar");
        System.out.print("Choose: ");
        String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

        if (answer.equals("1")) {
            System.out.println("The red pillar sinks into the floor, and the door clicks open. The path is clear!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("Incorrect! A hidden dart shoots out from the wall!");
            player.takeDamage(5);
            System.out.println("You take 5 damage.");
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
        System.out.println("Has Key: " + player.hasAnyKey());
    }

    private void revealRoomRewards(Room room) {
        if (room.isRewardsCollected()) {
            return;
        }

        if (room.hasEnemy() && !room.getEnemy().isDefeated()) {
            return;
        }

        if (room.isPuzzleRoom() && !room.isPuzzleSolved()) {
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

    private boolean promptBeforeBattle(Room room) {
        while (true) {
            System.out.println();
            System.out.println("A " + room.getEnemy().getName() + " is here.");
            System.out.println("1. Fight");
            System.out.println("2. Back away");
            
            System.out.print("Choose: ");
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

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

                if (current.getId().equals("f1safe")
                        && nextRoom.getId().equals("f1lockedHall")
                        && !player.hasKey(FLOOR_ONE_IRON_KEY_ID)) {
                    text = "Try the locked hallway (locked)";
                }

                System.out.println((i + 1) + ". " + text);
            }

            System.out.println("I. Inventory");
            System.out.print("Choose an action: ");
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

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

                if (current.getId().equals("f1safe")
                        && nextRoom.getId().equals("f1lockedHall")
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

    private List<String> getVisibleNeighborIds(Room current) {
        List<String> visible = new ArrayList<>();

        for (String neighborId : current.getNeighborIds()) {
            if (current.getId().equals("f1snakeBoss")
                    && (neighborId.equals("f1_snakeHoard") || neighborId.equals("f1_stairs") || neighborId.startsWith("f2"))
                    && current.hasEnemy()
                    && !current.getEnemy().isDefeated()) {
                continue;
            }

            if (current.getId().equals("f2_golem_rune")
                    && (neighborId.equals("f2_stairs") || neighborId.startsWith("f3"))
                    && current.hasEnemy()
                    && !current.getEnemy().isDefeated()) {
                continue;
            }
            
            if (current.getId().equals("f3_necro_altar")
                    && (neighborId.equals("f3_end"))
                    && current.hasEnemy()
                    && !current.getEnemy().isDefeated()) {
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
                String emptyChoice = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

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
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

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

    private void victory() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("FLOOR 3 CLEARED");
        System.out.println("========================================");
        System.out.println("You have vanquished the Necromancer and cleared the third floor.");
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

/* =====================================================
   SECTION 2 - WORLD GENERATION AND ROOM LAYOUT
   ===================================================== */

class WorldBuilder {

    public static Map<String, Room> createFloorOne(Random random) {
        Map<String, Room> rooms = new LinkedHashMap<>();

        List<Room> puzzlePool = new ArrayList<>();
        puzzlePool.add(new Room("f1p_brokenFork", "Broken Fork",
                "The passage splits here around cracked masonry. An ancient puzzle locks the path.",
                true, true));
        puzzlePool.add(new Room("f1p_shrine", "Serpent Shrine",
                "A shrine of coiled stone and old offerings. A riddle is carved into the altar.",
                true, true));
        Room weighingScales = new Room("f1p_weighingScales", "Room of Scales",
                "A massive bronze scale dominates the center of the room. One side is weighed down by a stone heart; the other side sits empty, waiting for an offering.",
                true, true);
        puzzlePool.add(weighingScales);

        List<Room> fightPool = new ArrayList<>();
        Room batRoom = new Room("f1f_batRoost", "Bat Roost",
                "The ceiling above is black with restless shapes. Thin squeaks echo between the stones.",
                true, true);
        batRoom.setEnemy(DarkHoldsBestiary.bat());
        batRoom.setClearedDescription("The roost is still and quiet now. Only a few drifting feathers remain.");
        fightPool.add(batRoom);

        Room goblinRoom = new Room("f1f_goblinOne", "Goblin Trail",
                "The smell of rot and old smoke thickens. You spot a goblin scavenger lurking among pilfered junk.",
                true, true);
        goblinRoom.setEnemy(DarkHoldsBestiary.goblin());
        goblinRoom.setClearedDescription("The goblin trail falls quiet now. The scavenger that haunted it is gone.");
        fightPool.add(goblinRoom);

        Room slimeRoom = new Room("f1f_slimeRoom", "Damp Tunnel",
                "A wet side cut opens into a slick pocket where something gelatinous shivers in the dark.",
                true, true);
        slimeRoom.setEnemy(DarkHoldsBestiary.slime());
        slimeRoom.setClearedDescription("The damp tunnel is calmer now, though the stones still glisten with foul residue.");
        fightPool.add(slimeRoom);
        
        Room chimneyShaft = new Room("f1f_chimneyShaft", "Crumbling Chimney",
                "A vertical shaft lets in a sliver of moonlight. A swarm of bats uses this as a highway to the surface, and you've just stepped directly into their flight path.",
                true, true);
        chimneyShaft.setEnemy(DarkHoldsBestiary.bat());
        fightPool.add(chimneyShaft);

        Room batRoots = new Room("f1f_bat_roots", "Hanging Roots", 
                "Thick, dead tree roots punch through the ceiling here. Clinging to the roots like grim fruit are dozens of sleeping bats. One opens its red eyes.", 
                true, true);
        batRoots.setEnemy(DarkHoldsBestiary.bat());
        fightPool.add(batRoots);

        Room wolfDen = new Room("f1f_wolf_den", "Beast's Den", 
                "The floor is covered in matted fur and the smell of wet dog is overpowering. A massive wolf blocks the exit, growling low in its throat.", 
                true, true);
        wolfDen.setEnemy(DarkHoldsBestiary.wolf());
        fightPool.add(wolfDen);

        Room wolfTunnel = new Room("f1f_wolf_tunnel", "Howling Tunnel", 
                "The wind drafts through this tunnel perfectly, creating a low, mournful howl. A real howl answers back as a wolf lunges from the shadows.", 
                true, true);
        wolfTunnel.setEnemy(DarkHoldsBestiary.wolf());
        fightPool.add(wolfTunnel);

        List<Room> generalPool = new ArrayList<>();
        generalPool.add(new Room("f1_cache", "Empty Cache",
                "A looted side chamber lies here with only scattered coins left behind.",
                true, true));
        generalPool.add(new Room("f1_snakeHoard", "Snake Hoard",
                "Gold glints among bones and torn packs.",
                true, true));
        Room fallenAdventurer = new Room("f1_fallenAdventurer", "Fallen Adventurer",
                "A skeleton slumps against the wall, clutching a broken weapon. Their armor is torn to ribbons by something incredibly large.",
                true, true);
        generalPool.add(fallenAdventurer);

        Room r1 = new Room("f1safe", "Safe Room",
                "You wake in a cold stone chamber with only a rough stick beside you. "
                        + "A heavy locked hallway gate stands to one side, and the rest of the floor branches into darkness.",
                true, true);

        Room r2 = puzzlePool.remove(random.nextInt(puzzlePool.size()));
        Room r3 = fightPool.remove(random.nextInt(fightPool.size()));

        Room r4 = new Room("f1lockedHall", "Locked Hallway",
                "With the key in hand, you force the rusted mechanism open. A narrow hall slopes toward something older and more dangerous.",
                false, false);

        List<Room> remainingPool = new ArrayList<>();
        remainingPool.addAll(puzzlePool);
        remainingPool.addAll(fightPool);
        remainingPool.addAll(generalPool);

        Room r5 = remainingPool.remove(random.nextInt(remainingPool.size()));
        r5.setPreLock(false);
        r5.setKeyEligible(false);

        Room r6 = new Room("f1snakeBoss", "Serpent Lair",
                "A giant snake rises from the treasure mound, its eyes fixed on you. This beast rules the floor.",
                false, false);
        r6.setEnemy(DarkHoldsBestiary.giantSnake());
        r6.setClearedDescription("The serpent lair is still and quiet now. The giant snake no longer rules this floor.");

        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);
        rooms.put(r4.getId(), r4);
        rooms.put(r5.getId(), r5);
        rooms.put(r6.getId(), r6);

        connect(r1, r2);
        connect(r2, r3);
        connect(r1, r4);
        connect(r4, r5);
        connect(r5, r6);

        return rooms;
    }

    public static List<Room> createFloorTwo(Random random) {
        List<Room> f2Rooms = new ArrayList<>();

        Room totem = new Room("f2p_totem", "Totem of Gruum",
                "A crude wooden totem pole stands before a locked portcullis. It has three animal faces carved into it: a Wolf, a Bat, and a Snake. A blood-stained prompt reads: 'Feed the beast that sees without eyes to open the path.'",
                false, false);

        Room altar = new Room("f2_shamansAltar", "The Shaman's Altar",
                "A makeshift altar of stacked stones is covered in melted wax and glowing green embers. The Orc shaman who built it is long gone, but they left behind a pristine glass flask filled with a glowing red liquid.",
                false, false);
        altar.addRewardItem(DarkHoldsItems.bigPotion());

        Room statue = new Room("f2p_weepingstatue", "Weeping Statue",
                "A towering marble statue of a hooded figure stands in the corner. Dark, oily water streaks down its face like tears, pooling at its stone feet.",
                false, false);

        Room armory = new Room("f2_desecratedArmory", "Desecrated Armory",
                "Shattered shields and bent swords litter the floor. It looks torn through here and there is nothing of real value. However, searching through the wreckage reveals a small, untouched lockbox.",
                false, false);
        if (random.nextBoolean()) {
            armory.addRewardItem(DarkHoldsItems.smallPotion());
        } else {
            armory.addGold(randomGold(7, 20));
        }

        Room slimePool = new Room("f2f_slime_pool", "Acidic Pool", 
                "A bubbling, green puddle takes up most of the floor. Suddenly, the puddle rises upward, forming into a massive, translucent blob.", 
                false, false);
        slimePool.setEnemy(DarkHoldsBestiary.slime());

        Room slimeCeiling = new Room("f2f_slime_ceiling", "The Dripping Ceiling", 
                "You hear a wet plop behind you. You look up to see the ceiling is coated in a vibrating slime that suddenly drops onto the floor to engage you.", 
                false, false);
        slimeCeiling.setEnemy(DarkHoldsBestiary.slime());

        Room skelCrypt = new Room("f2f_skel_crypt", "Forgotten Crypt", 
                "Stone sarcophagi line the walls. The lid of one slides off with a horrible grinding noise, and a skeleton steps out, raising a rusted sword.", 
                false, false);
        skelCrypt.setEnemy(DarkHoldsBestiary.skeleton());

        Room skelGrave = new Room("f2f_skel_grave", "The Mass Grave", 
                "The floor here isn't stone—it's tightly packed earth, churning and shifting. A skeletal hand bursts from the dirt, followed by the rest of the undead warrior.", 
                false, false);
        skelGrave.setEnemy(DarkHoldsBestiary.skeleton());

        Room campsite = new Room("f2p_campsite", "Abandoned Campsite", 
                "A dusty bedroll and a cold firepit sit in the corner. It looks like whoever was here left in a hurry, but it is quiet and safe now.", 
                false, false);

        Room wishWell = new Room("f2p_wishwell", "The Wishing Well", 
                "A deep stone well sits in the center of the room. You can hear the faint sound of running water far below, and see the glimmer of coins at the bottom.", 
                false, false);

        f2Rooms.add(totem);
        f2Rooms.add(altar);
        f2Rooms.add(statue);
        f2Rooms.add(armory);
        f2Rooms.add(slimePool);
        f2Rooms.add(slimeCeiling);
        f2Rooms.add(skelCrypt);
        f2Rooms.add(skelGrave);
        f2Rooms.add(campsite);
        f2Rooms.add(wishWell);

        // Shuffle the core F2 rooms
        Collections.shuffle(f2Rooms, random);

        // Floor 2 Boss
        Room golemBoss = new Room("f2_golem_rune", "Runestone Chamber", 
                "A Golem made entirely of carved runestones stands guard in front of a locked iron door. As you step forward, the runes glow red.", 
                false, false);
        golemBoss.setEnemy(DarkHoldsBestiary.bat()); // Bat used as placeholder for missing Golem
        golemBoss.setClearedDescription("The Golem crumbles into a pile of inert stones. The heavy iron door has opened, revealing stairs downwards.");
        f2Rooms.add(golemBoss);

        // Connect them linearly
        for (int i = 0; i < f2Rooms.size() - 1; i++) {
            connect(f2Rooms.get(i), f2Rooms.get(i + 1));
        }

        return f2Rooms;
    }

    public static List<Room> createFloorThree(Random random) {
        List<Room> f3Rooms = new ArrayList<>();

        Room silentLibrary = new Room("f3p_silentlibrary", "Silent Library", 
                "Rows of rotting bookshelves line this chamber. The dust is so thick it absorbs all sound, making the room eerily silent.", 
                false, false);
        
        Room colorPillars = new Room("f3p_colorpillars", "Color Pillars", 
                "Three pillars stand before a locked door. One is painted red, one blue, and one yellow. A plaque reads: 'The color of the lifeblood opens the way.'", 
                false, false);

        Room bonePit = new Room("f3f_bonePit", "The Bone Pit",
                "You step onto a floor entirely covered in massive, gnawed bones. A hulking Ogre slowly turns towards you.",
                false, false);
        bonePit.setEnemy(DarkHoldsBestiary.bat()); 

        f3Rooms.add(silentLibrary);
        f3Rooms.add(colorPillars);
        f3Rooms.add(bonePit);
        Collections.shuffle(f3Rooms, random);

        // Floor 3 Boss
        Room necroBoss = new Room("f3_necro_altar", "Profane Altar", 
                "Black candles burn with purple flames. A Necromancer in tattered robes is chanting in a language that hurts your ears.", 
                false, false);
        necroBoss.setEnemy(DarkHoldsBestiary.necromancer());
        necroBoss.setClearedDescription("The purple flames extinguish instantly as the Necromancer falls. The dark magic holding this room has vanished.");
        f3Rooms.add(necroBoss);

        // End of prototype game state
        Room f3End = new Room("f3_end", "The Final Descent", 
                "You step past the profane altar, having conquered the Dark Holds prototype...", 
                false, false);
        f3Rooms.add(f3End);

        for (int i = 0; i < f3Rooms.size() - 1; i++) {
            connect(f3Rooms.get(i), f3Rooms.get(i + 1));
        }

        return f3Rooms;
    }

    public static void connect(Room a, Room b) {
        a.addNeighbor(b.getId());
        b.addNeighbor(a.getId());
    }
    
    private static int randomGold(int min, int max) {
        Random random = new Random();
        return min + random.nextInt(max - min + 1);
    }
}

class BattleManager {
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
            boolean parryActive = false;

            while (!actionResolved) {
                PlayerAction action = promptPlayerAction(player);

                if (action == PlayerAction.ATTACK) {
                    sandInEyes = playerBasicAttack(player, enemy, sandInEyes);
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

                    if (result.didConsumeSandBlindness()) {
                        sandInEyes = result.isSandInEyes();
                    }

                    actionResolved = result.isTurnConsumed();

                    if (result.isForcedEscape()) {
                        return BattleResult.ESCAPED;
                    }
                } else if (action == PlayerAction.RUN) {
                    boolean escaped = tryRun(player);
                    actionResolved = true; // Consumes turn whether successful or not

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

            if (parryActive) {
                if (move.isParryable()) {
                    int reflectedDamage = calculateEnemyDamage(enemy, move, player);
                    enemy.takeDamage(reflectedDamage);
                    System.out.println("Parry successful! You redirect the attack and deal "
                            + reflectedDamage + " damage back to " + enemy.getName() + ".");

                    if (!enemy.isAlive()) {
                        continue;
                    }

                    continue;
                } else {
                    System.out.println("You try to parry, but " + move.getName() + " cannot be parried!");
                }
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

            if (move.getEffect() == MoveEffect.SELF_HEAL) {
                int healAmount = move.getEffectPower();
                enemy.heal(healAmount);
                System.out.println(enemy.getName() + " restores " + healAmount + " HP.");
            }

            if (move.getEffect() == MoveEffect.SOUL_DRAIN) {
                int healAmount = move.getEffectPower();
                enemy.heal(healAmount);
                System.out.println(enemy.getName() + " steals vitality and restores " + healAmount + " HP.");
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
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

            PlayerAction chosen = options.get(answer);
            if (chosen != null) {
                return chosen;
            }

            System.out.println("Invalid choice.");
        }
    }

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

    private BattleTurnResult openActionMenu(Player player) {
        while (true) {
            System.out.println("Action:");
            System.out.println("1. Dodge");

            boolean canParry = playerCanParry(player);
            if (canParry) {
                System.out.println("2. Parry");
            }

            System.out.println("0. Return");
            System.out.print("Choose action: ");
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

            if (answer.equals("1")) {
                System.out.println("You prepare to dodge the next attack.");
                return new BattleTurnResult(true, false, false, true, false, false);
            }

            if (canParry && answer.equals("2")) {
                System.out.println("You brace yourself to parry the next physical attack.");
                return new BattleTurnResult(true, false, false, false, false, true);
            }

            if (answer.equals("0")) {
                return new BattleTurnResult(false, false, false, false, false, false);
            }

            System.out.println("Invalid choice.");
        }
    }

    private boolean playerCanParry(Player player) {
        for (Item item : player.getInventory()) {
            if (item instanceof GameItem gameItem && gameItem.grantsParry()) {
                return true;
            }
        }
        return false;
    }

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

            System.out.println("0. Cancel");
            System.out.print("Choose item number: ");
            String answer = DarkHoldsFloor1PrototypeMasterBuild.input.nextLine().trim();

            if (answer.equals("0")) {
                return new BattleTurnResult(false, sandInEyes, false, false, false, false);
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
                    return new BattleTurnResult(true, updatedSandInEyes, true, false, false, false);
                }

                if (item.getName().equalsIgnoreCase("Torch")) {
                    boolean updatedSandInEyes = playerTorchAttack(player, enemy, sandInEyes);
                    return new BattleTurnResult(true, updatedSandInEyes, true, false, false, false);
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
                        return new BattleTurnResult(true, sandInEyes, false, false, true, false);
                    }

                    return new BattleTurnResult(true, sandInEyes, false, false, false, false);
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
    private final boolean parryActive;

    public BattleTurnResult(boolean turnConsumed, boolean sandInEyes, boolean consumeSandBlindness,
                            boolean dodgeActive, boolean forcedEscape, boolean parryActive) {
        this.turnConsumed = turnConsumed;
        this.sandInEyes = sandInEyes;
        this.consumeSandBlindness = consumeSandBlindness;
        this.dodgeActive = dodgeActive;
        this.forcedEscape = forcedEscape;
        this.parryActive = parryActive;
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

    public boolean isParryActive() {
        return parryActive;
    }
}

/* =====================================================
   SECTION 4 - PLAYER STATE AND ROOM DATA
   ===================================================== */

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

    public void increaseBaseAttack(int amount) {
        this.baseAttack += amount;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void addXp(int amount) {
        xp += amount;
    }

    public void checkLevelUp() {
        while (level < HP_BY_LEVEL.length && xp >= XP_THRESHOLDS[level]) {
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

    public int getAttack() {
        return baseAttack + DarkHoldsItems.getPassiveAttackBonus(this);
    }

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
    private final boolean puzzleRoom;
    private boolean puzzleSolved;

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
        
        if (isPuzzleRoom() && puzzleSolved) {
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

    public boolean isPuzzleRoom() {
        return puzzleRoom;
    }

    public boolean isPuzzleSolved() {
        return puzzleSolved;
    }

    public void setPuzzleSolved(boolean puzzleSolved) {
        this.puzzleSolved = puzzleSolved;
    }
}

/* =====================================================
   SECTION 5 - SHARED ITEM MODEL
   ===================================================== */

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

/* =====================================================
   SECTION 6 - ITEM BANK (MERGED FROM rpgitem.java)
   ===================================================== */

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
            if (item instanceof WeaponItem weapon && item.getName().equalsIgnoreCase(weaponName)) {
                return weapon;
            }
        }

        return null;
    }
}

/* =====================================================
   ITEM CONTEXT
   ===================================================== */

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

/* =====================================================
   BASE ITEM CLASSES
   ===================================================== */

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

    @Override
    public String toString() {
        return getName() + " - " + description;
    }
}

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

/* =====================================================
   POTIONS
   ===================================================== */

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

/* =====================================================
   SCROLLS
   ===================================================== */

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

/* =====================================================
   WEAPONS
   ===================================================== */

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

/* =====================================================
   PASSIVE GEAR
   ===================================================== */

class ArmorItem extends PassiveGearItem {
    public ArmorItem() {
        super("Armor", "Passive +6 Defense while carried.", 0, 6, false);
    }
}

class ShieldItem extends PassiveGearItem {
    public ShieldItem() {
        super("Shield", "Passive +3 Defense while carried. Also supports future Parry work.", 0, 3, true);
    }

    public boolean enablesParry() {
        return true;
    }

    public String getBattleActionName() {
        return "Parry";
    }

    public int parryDamage(String attackType, boolean hitLands,
                           int enemyAttack, int movePower, int playerDefense) {
        if (!hitLands || attackType == null) {
            return 0;
        }

        if (attackType.equalsIgnoreCase("heavy")) {
            return Math.max(1, enemyAttack + movePower - playerDefense);
        }

        return 0;
    }

    public String getParryResultMessage(String attackType, boolean hitLands, int reflectedDamage) {
        if (!hitLands) {
            return "The enemy attack missed, so parry was not needed.";
        }

        if (attackType == null) {
            return "Parry failed.";
        }

        if (attackType.equalsIgnoreCase("heavy") && reflectedDamage > 0) {
            return "Parry successful! You redirect the heavy attack and deal "
                    + reflectedDamage + " damage back to the enemy.";
        }

        return "Parry failed. Light attacks cannot be parried.";
    }
}

/* =====================================================
   KEYS / SPECIAL ITEMS
   ===================================================== */

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

/* =====================================================
   SECTION 7 - BESTIARY BANK (MERGED FROM rpgbestiary.java)
   ===================================================== */

final class DarkHoldsBestiary {
    private static final Random random = new Random();

    private DarkHoldsBestiary() {
    }

    /* =====================================================
       TIER 1 ENEMIES
       ===================================================== */

    public static Enemy bat() {
        return new Enemy(
                EnemyType.BAT,
                "Bats",
                14,
                4,
                1,
                7,
                randomGold(1, 4),
                new EnemyMove("Bite", 1, 84, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wing Buffet", 2, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Screech", 1, 78, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy goblin() {
        return new Enemy(
                EnemyType.GOBLIN,
                "Goblin",
                20,
                5,
                2,
                10,
                randomGold(4, 8),
                new EnemyMove("Knife Stab", 2, 80, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Wild Swing", 4, 58, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pocket Sand", 0, 52, MoveType.STATUS, false, MoveEffect.POCKET_SAND, 50, 0)
        );
    }

    public static Enemy wolf() {
        return new Enemy(
                EnemyType.WOLF,
                "Wolf",
                24,
                6,
                2,
                13,
                randomGold(4, 7),
                new EnemyMove("Snap Bite", 2, 82, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Lunge", 4, 68, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Pounce", 5, 56, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0)
        );
    }

    /* =====================================================
       TIER 2 ENEMIES
       ===================================================== */

    public static Enemy slime() {
        return new Enemy(
                EnemyType.SLIME,
                "Slime",
                42,
                4,
                5,
                20,
                randomGold(8, 15),
                new EnemyMove("Slam", 2, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Body Press", 4, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Acid Splash", 3, 70, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy skeleton() {
        return new Enemy(
                EnemyType.SKELETON,
                "Skeleton",
                30,
                7,
                4,
                17,
                randomGold(6, 10),
                new EnemyMove("Rusty Slash", 3, 78, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Shield Bash", 5, 62, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Bone Rattle", 1, 74, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy orc() {
        return new Enemy(
                EnemyType.ORC,
                "Orc",
                34,
                8,
                3,
                19,
                randomGold(8, 14),
                new EnemyMove("Axe Chop", 4, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Cleaver Drop", 6, 58, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("War Cry", 2, 80, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    /* =====================================================
       TIER 3 ENEMIES
       ===================================================== */

    public static Enemy ogre() {
        return new Enemy(
                EnemyType.OGRE,
                "Ogre",
                52,
                11,
                4,
                30,
                randomGold(12, 20),
                new EnemyMove("Club Smash", 7, 60, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Backhand", 4, 74, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Ground Stomp", 3, 82, MoveType.STATUS, false, MoveEffect.NONE, 0, 0)
        );
    }

    public static Enemy caveTroll() {
        return new Enemy(
                EnemyType.CAVE_TROLL,
                "Cave Troll",
                64,
                12,
                6,
                36,
                randomGold(15, 24),
                new EnemyMove("Maul", 7, 66, MoveType.HEAVY, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Crushing Grab", 5, 62, MoveType.GRAPPLE, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Regenerate", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 8)
        );
    }

    /* =====================================================
       BOSSES
       ===================================================== */

    public static Enemy giantSnake() {
        return new Enemy(
                EnemyType.SNAKE,
                "Giant Snake",
                34,
                8,
                3,
                42,
                randomGold(15, 25),
                new EnemyMove("Bite", 3, 78, MoveType.LIGHT, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Tail Lash", 5, 54, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Constrict", 2, 60, MoveType.GRAPPLE, false, MoveEffect.CONSTRICT, 100, 0)
        );
    }

    public static Enemy necromancer() {
        return new Enemy(
                EnemyType.NECROMANCER,
                "Necromancer",
                86,
                13,
                7,
                80,
                randomGold(40, 60),
                new EnemyMove("Bone Spear", 7, 84, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0),
                new EnemyMove("Grave Lash", 5, 76, MoveType.NORMAL, true, MoveEffect.NONE, 0, 0),
                new EnemyMove("Soul Drain", 5, 72, MoveType.MAGIC, false, MoveEffect.SOUL_DRAIN, 100, 5),
                new EnemyMove("Raise Dead", 0, 100, MoveType.STATUS, false, MoveEffect.SELF_HEAL, 100, 10),
                new EnemyMove("Black Flame", 8, 64, MoveType.MAGIC, false, MoveEffect.NONE, 0, 0)
        );
    }

    private static int randomGold(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}


/* =====================================================
   ENEMY MODEL
   ===================================================== */

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

    public void heal(int amount) {
        hp += amount;

        if (hp > maxHp) {
            hp = maxHp;
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
    private final MoveType moveType;
    private final boolean parryable;
    private final MoveEffect effect;
    private final int effectChance;
    private final int effectPower;

    public EnemyMove(String name, int power, int accuracy, MoveType moveType, boolean parryable,
                     MoveEffect effect, int effectChance, int effectPower) {
        this.name = name;
        this.power = power;
        this.accuracy = accuracy;
        this.moveType = moveType;
        this.parryable = parryable;
        this.effect = effect;
        this.effectChance = effectChance;
        this.effectPower = effectPower;
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

    public MoveType getMoveType() {
        return moveType;
    }

    public boolean isParryable() {
        return parryable;
    }

    public MoveEffect getEffect() {
        return effect;
    }

    public int getEffectChance() {
        return effectChance;
    }

    public int getEffectPower() {
        return effectPower;
    }
}


/* =====================================================
   ENEMY ENUMS
   ===================================================== */

enum EnemyType {
    BAT,
    GOBLIN,
    WOLF,
    SLIME,
    SKELETON,
    ORC,
    OGRE,
    CAVE_TROLL,
    SNAKE,
    NECROMANCER
}

enum MoveType {
    LIGHT,
    NORMAL,
    HEAVY,
    GRAPPLE,
    STATUS,
    MAGIC
}

enum MoveEffect {
    NONE,
    POCKET_SAND,
    CONSTRICT,
    SELF_HEAL,
    SOUL_DRAIN
}
