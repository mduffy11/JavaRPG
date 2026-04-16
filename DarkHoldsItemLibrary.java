import java.util.ArrayList;
import java.util.List;

/**
 * DarkHoldsItemLibrary.java
 * --------------------------------
 * Hybrid item architecture for Dark Holds.
 *
 * Goals:
 * - Keep the current prototype's simple Item base class.
 * - Add only a few subclasses where behavior actually differs.
 * - Support passive gear, weapon-style combat actions, keys as real items,
 *   and one-use consumables like potions and scrolls.
 *
 * Notes:
 * - This file is meant to live beside the main prototype file.
 * - It assumes the project already has Item, ItemType, Player, Enemy,
 *   EnemyType, and EnemyMove from the current Dark Holds prototype.
 * - It does not rewrite the battle system by itself. It gives the battle
 *   system richer item objects to work with.
 */
public final class DarkHoldsItemLibrary {

    private DarkHoldsItemLibrary() {
    }

    /* =========================
       FACTORY METHODS
       ========================= */

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

    public static KeyItem key(String keyId, String displayName) {
        return new KeyItem(keyId, displayName);
    }

    public static AncientRelicItem ancientRelic() {
        return new AncientRelicItem();
    }

    public static BookOfSpellsItem bookOfSpells() {
        return new BookOfSpellsItem();
    }

    public static KeyItem floorOneIronKey() {
        return new KeyItem("locked_hall_iron_key", "Iron Key");
    }

    /* =========================
       HELPER METHODS
       ========================= */

    public static List<WeaponItem> getAvailableWeaponActions(Player player) {
        List<WeaponItem> actions = new ArrayList<>();

        for (Item item : player.getInventory()) {
            if (item instanceof WeaponItem weapon) {
                actions.add(weapon);
            }
        }

        return actions;
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
        for (Item item : player.getInventory()) {
            if (item instanceof TorchItem) {
                return true;
            }

            if (item.getName().equalsIgnoreCase("Torch")) {
                return true;
            }
        }

        return false;
    }

    public static boolean playerCanParry(Player player) {
        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear && gear.grantsParry()) {
                return true;
            }
        }

        return false;
    }

    public static boolean playerHasKey(Player player, String keyId) {
        for (Item item : player.getInventory()) {
            if (item instanceof KeyItem key && key.getKeyId().equalsIgnoreCase(keyId)) {
                return true;
            }
        }

        return false;
    }

    public static GameItem asGameItem(Item item) {
        if (item instanceof GameItem gameItem) {
            return gameItem;
        }

        return null;
    }
}

/* =========================
   ITEM CONTEXT
   ========================= */

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

/* =========================
   OPTIONAL MOVE TAGS
   ========================= */

enum AttackWeight {
    LIGHT,
    HEAVY
}

enum AttackKind {
    STRIKE,
    BITE,
    LEAP,
    GRAPPLE,
    TRICK,
    ACID,
    SOUND,
    PROJECTILE
}

/* =========================
   BASE ITEM CLASSES
   ========================= */

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
     * Returns true when the item should be removed from inventory after use.
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

/* =========================
   POTIONS
   ========================= */

class SmallPotionItem extends GameItem {
    private static final int HEAL_AMOUNT = 15;

    public SmallPotionItem() {
        super(
                "Small Potion",
                ItemType.HEALING,
                HEAL_AMOUNT,
                "Heals 15 HP. Can be used in or out of battle. Cannot heal above max HP."
        );
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
        System.out.println("Used Small Potion. Restored " + healed + " HP.");
        return true;
    }
}

class BigPotionItem extends GameItem {
    private static final int HEAL_AMOUNT = 40;

    public BigPotionItem() {
        super(
                "Big Potion",
                ItemType.HEALING,
                HEAL_AMOUNT,
                "Heals 40 HP. Can be used in or out of battle. Cannot heal above max HP."
        );
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

/* =========================
   SCROLLS
   ========================= */

class ScrollOfEscapeItem extends GameItem {
    public ScrollOfEscapeItem() {
        super(
                "Scroll of Escape",
                ItemType.SPECIAL,
                0,
                "Use during battle to escape back to the previous room. Consumed on use."
        );
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
        super(
                "Scroll of Fireball",
                ItemType.SPECIAL,
                FIREBALL_DAMAGE,
                "Deals 35 damage to the current enemy during battle. Consumed on use."
        );
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
        super(
                "Scroll of Stealth",
                ItemType.SPECIAL,
                0,
                "Use before battle to sneak past the next non-boss fight. Encounter remains in the room for later."
        );
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

/* =========================
   WEAPONS
   ========================= */

class StickItem extends WeaponItem {
    public StickItem() {
        super(
                "Stick",
                2,
                95,
                "Stick Attack",
                "Default weapon. Power 2. Accuracy 95%. Against Slime it only deals 2 total damage."
        );
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
        super(
                "Sword",
                8,
                90,
                "Sword Slash",
                "Stronger weapon. Power 8. Accuracy 90%. Appears as its own combat action when carried."
        );
    }
}

class TorchItem extends WeaponItem {
    public TorchItem() {
        super(
                "Torch",
                4,
                80,
                "Torch",
                "Weapon and utility item. Power 4. Accuracy 80%. Strong against Slime. Also allows entry into dark rooms later."
        );
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

/* =========================
   PASSIVE GEAR
   ========================= */

class ArmorItem extends PassiveGearItem {
    public ArmorItem() {
        super(
                "Armor",
                "Passive gear. +6 Defense while carried. No equip menu required.",
                0,
                6,
                false
        );
    }
}

class ShieldItem extends PassiveGearItem {
    public ShieldItem() {
        super(
                "Shield",
                "Passive gear. +3 Defense while carried. Also unlocks the Parry action.",
                0,
                3,
                true
        );
    }

    public boolean canParry(AttackWeight weight, AttackKind kind) {
        if (weight != AttackWeight.HEAVY) {
            return false;
        }

        return kind == AttackKind.STRIKE;
    }

    public int getReflectedDamage(int enemyAttack, int movePower, int playerDefense) {
        return Math.max(1, enemyAttack + movePower - playerDefense);
    }

    public String getParryResultMessage(AttackWeight weight, AttackKind kind, boolean hitLands, int reflectedDamage) {
        if (!hitLands) {
            return "The enemy attack missed, so parry was not needed.";
        }

        if (canParry(weight, kind) && reflectedDamage > 0) {
            return "Parry successful! You redirect the heavy strike and deal "
                    + reflectedDamage + " damage back to the enemy.";
        }

        return "Parry failed. Only heavy strike-type moves can be parried.";
    }

    public String getBattleActionName() {
        return "Parry";
    }
}

/* =========================
   SPECIAL / UTILITY
   ========================= */

class KeyItem extends GameItem {
    private final String keyId;

    public KeyItem(String keyId, String displayName) {
        super(
                displayName,
                ItemType.SPECIAL,
                0,
                "Used automatically when checking locked doors. Key ID: " + keyId
        );
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}

class AncientRelicItem extends GameItem {
    public AncientRelicItem() {
        super(
                "Ancient Relic",
                ItemType.SPECIAL,
                0,
                "Placeholder reward item. No active function yet."
        );
    }
}

class BookOfSpellsItem extends GameItem {
    private final ArrayList<String> storedSpells;

    public BookOfSpellsItem() {
        super(
                "Book of Spells",
                ItemType.SPECIAL,
                0,
                "Placeholder item. Planned to store learned spells for repeated use later."
        );
        storedSpells = new ArrayList<>();
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
