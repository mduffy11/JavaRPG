import java.util.Random;

public class PuzzleManager {

    public void handleScalesPuzzle(Room room, Player player) {
        System.out.println("\nThe scale waits for an offering to balance the path.");
        System.out.println("1. Offer 20 gold\n2. Offer an item from your inventory\n3. Ignore it and try to walk past\nChoose: ");
        String answer = DarkHolds.input.nextLine().trim();

        if (answer.equals("1")) {
            if (player.getGold() >= 20) {
                player.addGold(-20);
                System.out.println("You place 20 gold on the scale. It balances perfectly! The path is safe.");
                room.setPuzzleSolved(true);
            } else {
                System.out.println("You don't have enough gold! The scales tip violently, triggering a trap door!");
                room.setEnemy(Bestiary.goblin());
                room.setPuzzleSolved(true); 
            }
        } else if (answer.equals("2")) {
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

    public void handleBrokenForkPuzzle(Room room, Player player) {
        System.out.println("\nThe cracked masonry features three stone bricks that can be pushed in.");
        System.out.print("1. Push the left brick\n2. Push the center brick\n3. Push the right brick\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("2")) {
            System.out.println("A heavy stone slides away, solving the puzzle!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden dart shoots from the wall!");
            player.takeDamage(5);
            System.out.println("You take 5 damage and step back.");
        }
    }

    public void handleShrinePuzzle(Room room, Player player) {
        System.out.println("\nThe riddle on the altar reads: 'I speak without a mouth and hear without ears. I have nobody, but I come alive with wind. What am I?'");
        System.out.print("1. A ghost\n2. An echo\n3. A snake\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("2")) {
            System.out.println("The shrine hums in approval. The puzzle is solved!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden mechanism strikes you!");
            player.takeDamage(5);
            System.out.println("You take 5 damage and the riddle resets.");
        }
    }
    
    public void handleTotemPuzzle(Room room, Player player) {
        System.out.print("\n1. Feed the Wolf\n2. Feed the Bat\n3. Feed the Snake\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("2")) {
            System.out.println("The portcullis grinds open, solving the puzzle.");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("A hidden spring trap shoots a rusty spear!");
            player.takeDamage(8);
            System.out.println("You take 8 damage.");
        }
    }
    
    public void handleWeepingStatue(Room room, Player player, Random random) {
        System.out.print("\n1. Drink the water\n2. Ignore it\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
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

    public void handleCampsite(Room room, Player player) {
        System.out.print("\n1. Rest by the cold firepit\n2. Keep moving\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            System.out.println("You lay out the dusty bedroll and rest for a while, regaining 20 HP.");
            player.heal(20);
        } else {
            System.out.println("You decide not to rest.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleWishWell(Room room, Player player, Random random) {
        System.out.print("\n1. Toss in 5 gold\n2. Leave it alone\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            if (player.getGold() >= 5) {
                player.addGold(-5);
                System.out.println("You toss 5 gold into the dark well. It vanishes into the depths...");
                if (random.nextBoolean()) {
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

    public void handleObsidianObelisk(Room room, Player player) {
        System.out.print("\n1. Touch the humming obelisk\n2. Leave it alone\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            System.out.println("As you lay your hand on the smooth, black stone, a vial materializes at its base!\nYou received a Small Potion.");
            player.addItem(Items.smallPotion());
        } else {
            System.out.println("You decide it's best not to touch strange magical artifacts.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleSilentLibrary(Room room, Player player, Random random) {
        System.out.print("\n1. Search the rotting shelves\n2. Walk past them\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            int randomOutcome = random.nextInt(3);
            if (randomOutcome == 0) {
                System.out.println("You brush away the thick dust and find an intact magical scroll!");
                player.addItem(Items.scrollOfFireball());
            } else if (randomOutcome == 1) {
                System.out.println("Reading the books you were able to determine that a necromancer is guarding the exit here.");
            } else {
                System.out.println("Even after reading the books you were unable to determine what they meant.");
            }
        } else {
            System.out.println("You leave the ancient books undisturbed in their silence.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleColorPillars(Room room, Player player) {
        System.out.print("\n1. Touch the Red pillar\n2. Touch the Blue pillar\n3. Touch the Yellow pillar\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            System.out.println("The red pillar sinks into the floor, and the door clicks open. The path is clear!");
            room.setPuzzleSolved(true);
        } else {
            System.out.println("Incorrect! A hidden dart shoots out from the wall!");
            player.takeDamage(5);
            System.out.println("You take 5 damage.");
        }
    }

    public void handleHourglassPuzzle(Room room, Player player) {
        System.out.print("\n1. Pull the lever and try to escape!\n2. Do nothing and wait it out.\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("2")) {
            System.out.println("You remain calm. The sand reaches your waist, then suddenly drains away. The door unlocks!");
        } else {
            System.out.println("Panicking, you pull the lever! A trapdoor opens above, dropping heavy rocks on you!");
            player.takeDamage(10);
            System.out.println("You take 10 damage before the door finally opens.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleLucksChance(Room room, Player player, Random random) {
        System.out.print("\n1. Open Chest 1\n2. Open Chest 2\n3. Open Chest 3\nChoose: ");
        DarkHolds.input.nextLine().trim(); 
        int choice = random.nextInt(3); 
        if (choice == 0) {
            System.out.println("A swarm of bats flies out of the chest!");
            room.setEnemy(Bestiary.bat());
        } else if (choice == 1) {
            System.out.println("You found 30 gold inside!");
            player.addGold(30);
        } else {
            System.out.println("You found a greater health potion inside!");
            player.addItem(Items.bigPotion()); 
        }
        room.setPuzzleSolved(true);
    }

    public void handleLakeOfTruth(Room room, Player player, Random random) {
        System.out.print("\n1. Take the Silver Dagger\n2. Take the Gold Dagger\n3. Decline both\nChoose: ");
        String answer = DarkHolds.input.nextLine().trim();
        if (answer.equals("1")) {
            int damage = random.nextInt(7) + 1;
            System.out.println("As you grab the Silver Dagger, it burns with freezing cold and shatters! You take " + damage + " damage.");
            player.takeDamage(damage);
        } else if (answer.equals("2")) {
            int damage = random.nextInt(6) + 4;
            System.out.println("As you grab the Gold Dagger, it sears your flesh and melts away! You take " + damage + " damage.");
            player.takeDamage(damage);
        } else if (answer.equals("3")) {
            System.out.println("You decline both daggers. The pool glows warmly, and you feel a surge of inner strength!");
            player.increaseBaseAttack(2);
            System.out.println("Your base strength permanently increases by 2.");
        } else {
            System.out.println("You hesitate, and the daggers sink back into the pool. You missed your chance.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleStarMapRoom(Room room, Player player) {
        System.out.print("\n1. Wait and observe the stars\n2. Move on\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            System.out.println("You sit quietly and study the alien constellations above.\nA strange, cosmic insight fills your mind, permanently increasing your attack by 1!");
            player.increaseBaseAttack(1);
        } else {
            System.out.println("You glance at the glowing ceiling but decide not to linger.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleCrimsonAltar(Room room, Player player) {
        System.out.print("\n1. Sacrifice your blood (lose 12 HP)\n2. Walk away\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            System.out.println("You cut your palm and let your blood drip onto the pristine marble.");
            player.takeDamage(12);
            System.out.println("You lose 12 HP, but feel a dark power surge through your veins! Attack increased by 3.");
            player.increaseBaseAttack(3);
        } else {
            System.out.println("You decide the toll is too high and step away from the altar.");
        }
        room.setPuzzleSolved(true);
    }

    public void handleAbandonedLab(Room room, Player player, Random random) {
        System.out.print("\n1. Drink the bubbling pink liquid\n2. Leave it alone\nChoose: ");
        if (DarkHolds.input.nextLine().trim().equals("1")) {
            if (random.nextBoolean()) {
                int healAmount = 7 + random.nextInt(6);
                System.out.println("The liquid is sweet and invigorating! You regain " + healAmount + " HP.");
                player.heal(healAmount);
            } else {
                int damageAmount = 5 + random.nextInt(5);
                System.out.println("The liquid burns your throat! It's poison! You take " + damageAmount + " damage.");
                player.takeDamage(damageAmount);
            }
        } else {
            System.out.println("You decide not to drink random alchemical liquids.");
        }
        room.setPuzzleSolved(true);
    }
}