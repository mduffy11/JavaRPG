import java.util.ArrayList;

public final class Items {

    private Items() {}

    public static StickItem stick() { return new StickItem(); }
    public static SwordItem sword() { return new SwordItem(); }
    public static GreatSwordItem greatSword() { return new GreatSwordItem(); }
    public static TorchItem torch() { return new TorchItem(); }
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

    public static int getPassiveAttackBonus(Player player) {
        int total = 0;
        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) total += gear.getPassiveAttackBonus();
        }
        return total;
    }

    public static int getPassiveDefenseBonus(Player player) {
        int total = 0;
        for (Item item : player.getInventory()) {
            if (item instanceof PassiveGearItem gear) total += gear.getPassiveDefenseBonus();
        }
        return total;
    }

    public static boolean playerHasTorch(Player player) { return player.hasItem("Torch"); }

    public static WeaponItem findWeapon(Player player, String weaponName) {
        for (Item item : player.getInventory()) {
            if (item instanceof WeaponItem weapon && item.getName().equalsIgnoreCase(weaponName)) return weapon;
        }
        return null;
    }
}

class Item {
    private final String name;
    private final ItemType type;
    private final int power;
    public Item(String name, ItemType type, int power) { this.name = name; this.type = type; this.power = power; }
    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getPower() { return power; }
    @Override public String toString() { return name + " (" + type + ")"; }
}

enum ItemType { HEALING, WEAPON, SPECIAL }

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

abstract class GameItem extends Item {
    private final String description;
    public GameItem(String name, ItemType type, int power, String description) { super(name, type, power); this.description = description; }
    public String getDescription() { return description; }
    public boolean canUseInBattle() { return false; }
    public boolean canUseOutsideBattle() { return false; }
    public boolean isPassive() { return false; }
    public boolean use(Player player, ItemContext context) { System.out.println(getName() + " has no active use."); return false; }
    public int getPassiveAttackBonus() { return 0; }
    public int getPassiveDefenseBonus() { return 0; }
    public boolean grantsParry() { return false; }
    @Override public String toString() { return getName() + " - " + description; }
}

abstract class WeaponItem extends GameItem {
    private final int accuracy;
    private final String attackName;
    public WeaponItem(String name, int power, int accuracy, String attackName, String description) {
        super(name, ItemType.WEAPON, power, description);
        this.accuracy = accuracy;
        this.attackName = attackName;
    }
    public int getAccuracy() { return accuracy; }
    public String getAttackName() { return attackName; }
    public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy == null) return Math.max(1, getPower());
        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
    public boolean allowsDarkRoomEntry() { return false; }
}

abstract class PassiveGearItem extends GameItem {
    private final int passiveAttackBonus;
    private final int passiveDefenseBonus;
    private final boolean parryEnabled;
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

class StickItem extends WeaponItem {
    public StickItem() { super("Stick", 2, 95, "Stick Attack", "Default weapon. Weak but reliable."); }
    @Override public int getDamageAgainst(Enemy enemy, Player player) {
        if (enemy != null && enemy.getType() == EnemyType.SLIME) return 2;
        return Math.max(1, getPower() + player.getAttack() - enemy.getDefense());
    }
}

class SwordItem extends WeaponItem {
    public SwordItem() { super("Sword", 8, 90, "Sword Slash", "Stronger weapon that can later appear as its own combat action."); }
}

class GreatSwordItem extends WeaponItem {
    public GreatSwordItem() { super("Great Sword", 12, 60, "Great Sword Slash", "A great sword heavier than other weapons but also more effective. Accuracy scales with level."); }
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
    public TorchItem() { super("Torch", 4, 80, "Torch", "Weapon and utility item. Strong against slime and useful for dark rooms later."); }
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
    public ArmorItem() { super("Armor", "Passive +6 Defense while carried.", 0, 6, false); }
}

class ShieldItem extends PassiveGearItem {
    public ShieldItem() { super("Shield", "Passive +3 Defense while carried. Also supports future Parry work.", 0, 3, true); }
}

class DragonScaleShieldItem extends PassiveGearItem {
    public DragonScaleShieldItem() { super("Dragon Scale", "A dragon scale from defeated dragon and it can be used as shield.", 0, 8, true); }
}

class KeyItem extends GameItem {
    private final String keyId;
    public KeyItem(String keyId, String displayName) {
        super(displayName, ItemType.SPECIAL, 0, "Used automatically when checking locked doors. Key ID: " + keyId);
        this.keyId = keyId;
    }
    public String getKeyId() { return keyId; }
}