import java.util.*;

public class BattleManager {
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

    private PlayerAction promptPlayerAction(Player player) {
        while (true) {
            Map<String, PlayerAction> options = new LinkedHashMap<>();
            int optionNumber = 1;
            System.out.println(optionNumber + ". Stick Attack");
            options.put(String.valueOf(optionNumber++), PlayerAction.ATTACK);

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

        int damage = torch != null ? torch.getDamageAgainst(enemy, player) : (enemy.getType() == EnemyType.SLIME ? Math.max(10, calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense()) + 6) : calculatePlayerDamage(player.getAttack(), 4, enemy.getDefense()));
        enemy.takeDamage(damage);
        System.out.println("You lash out with the Torch and deal " + damage + " damage.");
        return false;
    }

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

    private boolean playerCanParry(Player player) {
        for (Item item : player.getInventory()) {
            if (item instanceof GameItem gameItem && gameItem.grantsParry()) return true;
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
                if (item.getName().equalsIgnoreCase("Torch")) return new BattleTurnResult(true, playerTorchAttack(player, enemy, sandInEyes), true, false, false, false);

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

    private int calculatePlayerDamage(int playerAttack, int movePower, int enemyDefense) {
        return Math.max(1, movePower + playerAttack - enemyDefense);
    }

    private int calculateEnemyDamage(Enemy enemy, EnemyMove move, Player player) {
        if (move.getEffect() == MoveEffect.POCKET_SAND) return 1;
        return Math.max(1, move.getPower() + enemy.getAttack() - player.getDefense());
    }
}

enum BattleResult { WON, ESCAPED, DIED }

enum PlayerAction { ATTACK, TORCH, ACTION, INVENTORY, RUN }

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