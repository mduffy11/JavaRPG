import java.util.ArrayList;

/* =========================
   POTIONS
   ========================= */

class SmallHealthPotion extends Item {
    private static final int HEAL_AMOUNT = 15;

    public SmallHealthPotion() {
        super("Small Health Potion", "Heals 15 HP. Cannot heal above max HP.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        int before = player.getCurrentHp();
        player.heal(HEAL_AMOUNT);
        int healed = player.getCurrentHp() - before;
        System.out.println("Used Small Health Potion. Restored " + healed + " HP.");
        return true;
    }
}

class BigHealthPotion extends Item {
    private static final int HEAL_AMOUNT = 40;

    public BigHealthPotion() {
        super("Big Health Potion", "Heals 40 HP. Cannot heal above max HP.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        int before = player.getCurrentHp();
        player.heal(HEAL_AMOUNT);
        int healed = player.getCurrentHp() - before;
        System.out.println("Used Big Health Potion. Restored " + healed + " HP.");
        return true;
    }
}

/* =========================
   SCROLLS
   ========================= */

class ScrollOfEscape extends Item {
    public ScrollOfEscape() {
        super("Scroll of Escape", "Use during battle to return to the previous room.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        if (!game.isInBattle()) {
            System.out.println("Scroll of Escape can only be used during battle.");
            return false;
        }

        if (game.getPreviousRoom() == null) {
            System.out.println("No previous room exists.");
            return false;
        }

        game.setCurrentRoom(game.getPreviousRoom());
        game.setCurrentEnemy(null);
        game.setInBattle(false);

        System.out.println("Escaped to: " + game.getCurrentRoom().getName());
        return true;
    }
}

class ScrollOfFireball extends Item {
    private static final int FIREBALL_DAMAGE = 35;

    public ScrollOfFireball() {
        super("Scroll of Fireball", "One-time use. Deals 35 damage to an enemy during battle.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        if (!game.isInBattle() || game.getCurrentEnemy() == null) {
            System.out.println("Scroll of Fireball can only be used during battle.");
            return false;
        }

        game.getCurrentEnemy().takeDamage(FIREBALL_DAMAGE);
        System.out.println("Fireball hit " + game.getCurrentEnemy().getName()
                + " for " + FIREBALL_DAMAGE + " damage.");
        return true;
    }
}

class ScrollOfStealth extends Item {
    public ScrollOfStealth() {
        super("Scroll of Stealth", "Use before battle to bypass the next non-boss fight.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        if (game.isInBattle()) {
            System.out.println("Scroll of Stealth must be used before battle starts.");
            return false;
        }

        if (game.getCurrentEnemy() != null && game.getCurrentEnemy().isBoss()) {
            System.out.println("Scroll of Stealth cannot bypass boss fights.");
            return false;
        }

        game.setStealthActive(true);
        System.out.println("Stealth activated. Next non-boss encounter can be bypassed.");
        return true;
    }
}

/* =========================
   WEAPONS
   ========================= */

abstract class Weapon extends Item implements Equipable {
    protected int attackBonus;

    public Weapon(String name, String description, int attackBonus) {
        super(name, description);
        this.attackBonus = attackBonus;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("This item must be equipped, not used.");
        return false;
    }
}

class Stick extends Weapon {
    public Stick() {
        super("Stick", "Weapon. +3 Attack.", 3);
    }

    @Override
    public void equip(Player player) {
        player.equipWeapon(this);
    }
}

class Sword extends Weapon {
    public Sword() {
        super("Sword", "Weapon. +8 Attack.", 8);
    }

    @Override
    public void equip(Player player) {
        player.equipWeapon(this);
    }
}

/* =========================
   ARMOR / SHIELD
   ========================= */

class Armor extends Item implements Equipable {
    private int defenseBonus = 6;

    public Armor() {
        super("Armor", "Body equipment. +6 Defense. Can be worn with a shield.");
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public void equip(Player player) {
        player.equipArmor(this);
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("This item must be equipped, not used.");
        return false;
    }
}

class Shield extends Item implements Equipable {
    private int defenseBonus = 3;

    public Shield() {
        super("Shield", "Hand equipment. +3 Defense. Can be equipped with armor.");
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public void equip(Player player) {
        player.equipShield(this);
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("This item must be equipped, not used.");
        return false;
    }
}

/* =========================
   UTILITY ITEMS
   ========================= */

class Torch extends Item {
    public Torch() {
        super("Torch", "Allows the player to enter dark rooms.");
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("Torch is a passive item. If you have it, you can enter dark rooms.");
        return false;
    }
}

class BookOfSpells extends Item {
    private ArrayList<String> storedSpells = new ArrayList<>();

    public BookOfSpells() {
        super("Book of Spells", "Placeholder item. Planned for storing spell scrolls for repeated use.");
    }

    public void addSpell(String spellName) {
        storedSpells.add(spellName);
    }

    public ArrayList<String> getStoredSpells() {
        return storedSpells;
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("Book of Spells is not finished yet.");
        return false;
    }
}

class Key extends Item {
    private String keyId;

    public Key(String keyId) {
        super("Key", "Used to open locked rooms or doors. Key ID: " + keyId);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }

    @Override
    public boolean use(Player player, GameContext game) {
        System.out.println("Keys are usually checked automatically when opening locked doors.");
        return false;
    }
}